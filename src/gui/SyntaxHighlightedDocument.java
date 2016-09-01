package gui;

import misc.Pair;

import javax.swing.text.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 5hir0kur0
 */
class SyntaxHighlightedDocument extends DefaultStyledDocument {
    private List<Pair<Pattern, AttributeSet>> style;
    private final Observer observer;

    private boolean autoIndent;

    private ArrayList<Edit> edits;
    private int editIndex;

    SyntaxHighlightedDocument(List<Pair<Pattern, AttributeSet>> style, Observer observer) {
        this.style = Objects.requireNonNull(style);
        this.observer = observer;

        edits      = new ArrayList<>(42);
        editIndex  = 0;
        autoIndent = true;
    }


    @Override
    public void insertString(int offset, String str, AttributeSet as) throws BadLocationException {
        if (str.endsWith(LineNumberSyntaxPane.LINE_END) && autoIndent) {
            final String lineEnd = LineNumberSyntaxPane.LINE_END;
            final Element element = super.getParagraphElement(offset + str.length());
            String line;
            if (str.indexOf(lineEnd) == str.lastIndexOf(lineEnd)) {
                line = super.getText(element.getStartOffset(),
                        element.getEndOffset() - element.getStartOffset());
            } else {
                line = str.substring(str.lastIndexOf(lineEnd, str.lastIndexOf(lineEnd)-1)+lineEnd.length());
            }
            String prefix = line.substring(0, line.indexOf(line.trim()));
            if (line.trim().length() == 0) {
               int cursorPos = offset - element.getStartOffset();
                if (cursorPos < line.length())
                    str += line.substring(0, cursorPos);
                else {
                    str += String.format("%" + cursorPos + "s", line);
                }
            }
            else
                str += prefix;
        }
        addEdit(offset, str, true);
        _insertString(offset, str);
    }

    private void _insertString(int offset, String str) throws BadLocationException {
        super.insertString(offset, str, StyleContext.getDefaultStyleContext().getEmptySet());
            updateSyntaxHighlighting(offset, str.length()  + (str.endsWith(LineNumberSyntaxPane.LINE_END) ? 1 : 0));
        if (str.contains(LineNumberSyntaxPane.LINE_END)) this.observer.update(null, null);
    }

    @Override
    public void remove(int offset, int length) throws BadLocationException {
        addEdit(offset, super.getText(offset, length), false);
        _remove(offset, length);
    }

    private void _remove(int offset, int length) throws BadLocationException {
        final boolean lineChange = super.getText(offset, length).contains(LineNumberSyntaxPane.LINE_END);
        super.remove(offset, length);
        updateSyntaxHighlighting(offset, 1); //update the line starting from which text was deleted
        if (lineChange) this.observer.update(null, null);
    }

    public void setStyle(List<Pair<Pattern, AttributeSet>> style) {
        this.style = Objects.requireNonNull(style);
    }

    private void updateSyntaxHighlighting(int offset, int length) throws BadLocationException {
        for (int currentOffset = offset, loopStop = offset + length; currentOffset < loopStop;) {
            final Element e = super.getParagraphElement(currentOffset);
            final int tmpStart = e.getStartOffset();
            final int tmpEnd = e.getEndOffset();
            final int tmpLength = tmpEnd - tmpStart;
            final String line = super.getText(tmpStart, tmpLength);
            currentOffset = tmpEnd;

            super.setCharacterAttributes(tmpStart, tmpLength, SimpleAttributeSet.EMPTY, true); //reset attributes

            for (Pair<Pattern, AttributeSet> p : style) {
                final Matcher m = p.x.matcher(line);
                while (m.find()) {
                    for (int i = 1; i <= m.groupCount(); ++i) {
                        final int matchStart = tmpStart + m.start(i);
                        final int matchStop = tmpStart + m.end(i);
                        final int matchLength = matchStop - matchStart;
                        if (matchStart >= tmpStart)
                            super.setCharacterAttributes(matchStart, matchLength, p.y, false);
                    }
                }
            }
        }
    }

    void setAutoIndent(boolean autoIndent) {
        this.autoIndent = autoIndent;
    }

    public Optional<Edit> undo() {
          if (++editIndex > 0 && editIndex <= edits.size()) {
              Edit e = edits.get(edits.size() - editIndex);
              try {
                  if (e.insert)
                      this._remove(e.offset, e.data.length());
                  else
                      this._insertString(e.offset, e.data);
              } catch (BadLocationException ex) {
                  ex.printStackTrace();
              }
              return Optional.of(e);
          }
        return Optional.empty();
    }

    public Optional<Edit> redo() {
        if (editIndex > 0 && editIndex <= edits.size()) {
            Edit e = edits.get(edits.size() - editIndex);
            try {
                if (e.insert)
                    this._insertString(e.offset, e.data);
                else
                    this._remove(e.offset, e.data.length());
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
            --editIndex;
            return Optional.of(e);
        }
        return Optional.empty();
    }

    public void discardAllEdits() {
        edits.clear();
        editIndex = 0;
    }

    void updateCompleteSyntaxHighlighting() {
        try {
            updateSyntaxHighlighting(0, super.getLength());
        } catch (BadLocationException e) { //this is basically impossible to happen
            //TODO: Log exception
            e.printStackTrace();
        }
    }

    private void addEdit(int offset, String text, boolean insert) {
        if (editIndex != 0) {
            edits.subList(edits.size() - editIndex, edits.size()).clear(); // This is is the official method do  delete a range in a list...
            editIndex = 0;
        }
        edits.add(new Edit(offset, insert, text));

    }

    static final class Edit {
        private  int offset;
        private boolean insert;
        private String data;

        private Edit(int offset, boolean insert, String data) {
            this.offset = offset;
            this.data = Objects.requireNonNull(data);
            this.insert = insert;
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return data.length();
        }

        public String getData() {
            return data;
        }

        public boolean isInsert() {
            return insert;
        }
    }
}
