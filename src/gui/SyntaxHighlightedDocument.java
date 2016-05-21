package gui;

import misc.Pair;

import javax.swing.text.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Observer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 5hir0kur0
 */
class SyntaxHighlightedDocument extends DefaultStyledDocument {
    private List<Pair<Pattern, AttributeSet>> style;
    private final Observer observer;

    SyntaxHighlightedDocument(List<Pair<Pattern, AttributeSet>> style, Observer observer) {
        this.style = Objects.requireNonNull(style);
        this.observer = observer;
    }


    @Override
    public void insertString(int offset, String str, AttributeSet as) throws BadLocationException {
        super.insertString(offset, str, StyleContext.getDefaultStyleContext().getEmptySet());
        updateSyntaxHighlighting(offset, str.length());
        if (str.contains(LineNumberSyntaxPane.LINE_END)) this.observer.update(null, null);
    }

    @Override
    public void remove(int offset, int length) throws BadLocationException {
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

    void updateCompleteSyntaxHighlighting() {
        try {
            updateSyntaxHighlighting(0, super.getLength());
        } catch (BadLocationException e) { //this is basically impossible to happen
            //TODO: Log exception
            e.printStackTrace();
        }
    }
}
