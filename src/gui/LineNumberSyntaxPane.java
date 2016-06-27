package gui;

import misc.Pair;
import misc.Settings;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.*;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @author 5hir0kur0
 */
public class LineNumberSyntaxPane extends JPanel {

    private final static String FONT_SETTING = "gui.editor.font";
    private final static String FONT_SETTING_DEFAULT = Font.MONOSPACED;
    private final static Pattern FONT_NAME_PATTERN = Pattern.compile("\\w+");
    private final static Predicate<String> IS_VALID_FONT_NAME = s -> FONT_NAME_PATTERN.matcher(s).matches();
    private final static String FONT_SIZE_SETTING = "gui.editor.font-size";
    private final static String FONT_SIZE_SETTING_DEFAULT = "12";
    private final static IntPredicate IS_VALID_FONT_SIZE = i -> i > 0 && i < 9001;

    final static String LINE_END = "\n";


    static {
        Settings.INSTANCE.setDefault(FONT_SETTING, FONT_SETTING_DEFAULT);
        Settings.INSTANCE.setDefault(FONT_SIZE_SETTING, FONT_SIZE_SETTING_DEFAULT);
    }

    private final JTextArea lineNumbers;
    private final JTextPane code;
    private final UndoManager undoManager;
    private final Highlighter highlighter;
    private int lastLine = 1;

    private String fileExtension;
    private int savedHash = 0;

    /**
     * Create a new {@code LineNumberSyntaxPane}.
     * NOTE: This component does not provide scrolling support. If you want scrolling, you have to add it to a
     * {@link JScrollPane}.
     * @param fileExtension the displayed file's extension; must not be {@code null}
     */
    public LineNumberSyntaxPane(String fileExtension) {
        super(new BorderLayout());

        this.lineNumbers = new JTextArea("1");
        this.lineNumbers.setLineWrap(false);
        this.lineNumbers.setEditable(false);
        this.lineNumbers.setMargin(new Insets(1, 4, 1, 4));

        this.code = new JTextPane();
        this.highlighter = new Highlighter(this.code);

        this.add(lineNumbers, BorderLayout.LINE_START);
        this.add(code, BorderLayout.CENTER);

        SyntaxHighlightedDocument shDoc = new SyntaxHighlightedDocument(Collections.emptyList(),
                (Observable, Object) -> updateLineNumbers());
        this.code.setContentType("text/plain");
        this.code.setDocument(shDoc);
        this.setFileExtension(fileExtension);

        this.undoManager = new UndoManager() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                final UndoableEdit u = e.getEdit();
                if (u instanceof DocumentEvent) {
                    DocumentEvent de = (DocumentEvent) u;
                    if (de.getType() == DocumentEvent.EventType.INSERT ||
                        de.getType() == DocumentEvent.EventType.REMOVE) {
                        super.undoableEditHappened(e);
                    }
                } else {
                    String pres = u.getPresentationName();
                    if (pres.equals(UIManager.getString("AbstractDocument.additionText")) ||
                        pres.equals(UIManager.getString("AbstractDocument.deletionText")))
                        super.undoableEditHappened(e);
                }
            }
        };
        this.undoManager.setLimit(2958);
        this.code.getDocument().addUndoableEditListener(undoManager);
    }

    public void setFontSize(int newSize) {
        if (!IS_VALID_FONT_SIZE.test(newSize)) throw new IllegalArgumentException("Illegal font size: " + newSize);
        final Font oldFont = this.lineNumbers.getFont();
        final Font newFont = oldFont.deriveFont((float) newSize);
        this.lineNumbers.setFont(newFont);
        this.code.setFont(newFont);
    }

    public int getFontSize() {
        return this.code.getFont().getSize();
    }

    public void updateTheme() {
        Font f = new Font(Settings.INSTANCE.getProperty(FONT_SETTING, FONT_SETTING_DEFAULT, IS_VALID_FONT_NAME),
                Font.PLAIN,
                Settings.INSTANCE.getIntProperty(FONT_SIZE_SETTING,
                        Integer.parseInt(FONT_SIZE_SETTING_DEFAULT), IS_VALID_FONT_SIZE));
        this.lineNumbers.setFont(f);
        this.code.setFont(f);

        // fix a bug in the "Nimbus" theme (It ignores setBackground(...)).
        // adapted from: https://stackoverflow.com/questions/22674575/jtextpane-background-color
        final UIDefaults defaults = new UIDefaults();
        final Color codeBackground = SyntaxThemes.INSTANCE.getCurrentTheme().getCodeBackground();
        defaults.put("TextPane[Enabled].backgroundPainter", codeBackground);
        this.code.putClientProperty("Nimbus.Overrides", defaults);
        this.code.putClientProperty("Nimbus.Overrides.InheritDefaults", true);

        this.lineNumbers.setBackground(SyntaxThemes.INSTANCE.getCurrentTheme().getLineNumberBackground());
        this.code.setBackground(codeBackground);
        this.lineNumbers.setForeground(SyntaxThemes.INSTANCE.getCurrentTheme().getLineNumberForeground());
        this.code.setForeground(SyntaxThemes.INSTANCE.getCurrentTheme().getCodeForeground());
        final List<Pair<Pattern, AttributeSet>> style = verifyStyle(SyntaxThemes.INSTANCE.getCurrentTheme()
                .getStyleForType(this.fileExtension).getStyle());
        ((SyntaxHighlightedDocument)this.code.getDocument()).setStyle(style);
        this.updateSyntaxHighlighting();
    }

    public void updateSyntaxHighlighting() {
        ((SyntaxHighlightedDocument)this.code.getDocument()).updateCompleteSyntaxHighlighting();
    }

    public final void setFileExtension(String fileExtension) {
        this.fileExtension = Objects.requireNonNull(fileExtension);
        this.updateLineNumbers();
        this.updateTheme();
    }

    public void store(Writer w) throws IOException {
        this.code.write(w);
        this.savedHash = this.code.getText().hashCode();
    }

    public void load(Reader r) throws IOException {
        // create a new model to discard already existing contents (in case the file is reloaded)
        SyntaxHighlightedDocument shDoc = new SyntaxHighlightedDocument(Collections.emptyList(),
                (Observable, Object) -> updateLineNumbers());
        this.code.setContentType("text/plain");
        this.code.setDocument(shDoc);
        try {
            this.code.getEditorKit().read(r, this.code.getStyledDocument(), 0);
        } catch (BadLocationException impossible) {
            System.err.println("an impossible exception occurred:");
            impossible.printStackTrace();
        }
        this.code.setCaretPosition(0);
        this.updateLineNumbers();
        this.updateTheme();
        this.undoManager.discardAllEdits();
        this.savedHash = this.code.getText().hashCode();

        this.highlighter.reload();
        shDoc.addUndoableEditListener(this.undoManager);
    }

    public void copy() {
        this.code.copy();
    }

    public void paste() {
        this.code.paste();
    }

    public void cut() {
        this.code.cut();
    }

    public void undo() {
        if (this.undoManager.canUndo()) {
            this.undoManager.undo();
            this.updateLineNumbers();
        }
    }

    public void redo() {
        if (this.undoManager.canRedo()) {
            this.undoManager.redo();
            this.updateLineNumbers();
        }
    }

    public void setCaret(int line, int column) {
        if (line < 0 || column < 0)
            throw new IllegalArgumentException("'line' or column must not be negative!");

        int offset;
        Element elements = this.code.getDocument().getDefaultRootElement();
        Element ele = (line < elements.getElementCount()) ?
                elements.getElement(line) : elements.getElement(elements.getElementCount()-1);

        if (column < ele.getEndOffset() - ele.getStartOffset())
            offset = ele.getStartOffset() + column;
        else
            offset = ele.getEndOffset();
        this.code.setCaretPosition(offset);

        this.requestFocusInWindow();
        this.code.grabFocus();
    }

    private void addLines(int count) {
        if (count == 0) return;
        if (count < 0) {
            if (lastLine + count <= 0) throw new IllegalArgumentException("Illegal remove count: " + count);
            final int lastVisibleLineAfterOperation = lastLine + count;
            try {
                final int removeStartOffset = this.lineNumbers.getLineEndOffset(lastVisibleLineAfterOperation - 1) - 1;
                final int removeEndOffset = this.lineNumbers.getLineEndOffset(this.lastLine - 1);
                this.lineNumbers.replaceRange(null, removeStartOffset, removeEndOffset);
            } catch (BadLocationException e) {
                throw new IllegalArgumentException("Error during line number deletion.", e);
            }
        } else {
            for (int i = this.lastLine + 1, loopStop = lastLine + count; i <= loopStop; ++i) {
                String tmpInt = Integer.toUnsignedString(i);
                this.lineNumbers.append(LINE_END);
                this.lineNumbers.append(tmpInt);
            }
        }
        this.lastLine += count;
    }

    private void updateLineNumbers() {
        int lineCount = lineCount(this.code);
        if (lineCount != lastLine) addLines(lineCount - lastLine);
    }

    private static int lineCount(JTextComponent jtc) {
        return jtc.getDocument().getDefaultRootElement().getElementCount(); //swing voodoo magic
    }

    private static List<Pair<Pattern, AttributeSet>> verifyStyle(List<Pair<Pattern, AttributeSet>> style) {
        Objects.requireNonNull(style).forEach(p -> {
            if (null == p) throw new IllegalArgumentException("pair must not be null");
            if (null == p.x || null == p.y) throw new IllegalArgumentException("invalid style");
            if (p.x.matcher("").groupCount() < 1) throw new IllegalArgumentException("invalid style");
        });
        return style;
    }

    public void highlightLines(Map<Integer, Color> linesAndColors) {
        this.highlighter.updateHighlightedLines(linesAndColors);
    }

    public boolean isChanged() {
        return this.savedHash != this.code.getText().hashCode();
    }

    /**
     * @author Noxgrim
     *
     * Partially inspired by: http://www.camick.com/java/source/LinePainter.java
     */
    private class Highlighter implements javax.swing.text.Highlighter.HighlightPainter, DocumentListener {

        private final JTextComponent parent;
        private Map<Pair<Integer, Element>, Color> highlights;

        private Highlighter(JTextComponent parent) {
            this.highlights = new HashMap<>();
            this.parent = Objects.requireNonNull(parent, "Parent componet cannot be 'null'!");
            reload();
        }

        final void reload() {
            try {
                parent.getDocument().addDocumentListener(this);
                parent.getHighlighter().addHighlight(0, 0, this);
                highlights.clear();
            } catch (BadLocationException ignored) {
                ignored.printStackTrace();
            }

        }

        @Override
        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
            for (Pair<Integer, Element> key : highlights.keySet())
                try {
                    Rectangle box =  c.modelToView(key.y.getStartOffset());
                    g.setColor(highlights.get(key));
                    g.drawRect(0, box.y, c.getWidth(), box.height); // Rectangle not filled for better visibility.

                } catch (BadLocationException e) {e.printStackTrace();}
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            int lineCount = lineCount(code);
            if (lineCount != lastLine) updateLines(
                    e.getDocument().getDefaultRootElement().getElementIndex(e.getOffset()),
                    lineCount - lastLine);

        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            int lineCount = lineCount(code);
            if (lineCount != lastLine) updateLines(
                    e.getDocument().getDefaultRootElement().getElementIndex(e.getOffset()),
                    lineCount - lastLine);

        }

        @Override
        public void changedUpdate(DocumentEvent e) {
        }

        void updateHighlightedLines(Map<Integer, Color> linesAndColors) {
            highlights.clear();
            final Element elements = parent.getDocument().getDefaultRootElement();
            final int count = elements.getElementCount();
            for (Integer key : linesAndColors.keySet()) {
                if (key < 0)
                    throw new IllegalArgumentException("Line cannot be negative!: " + key);
                else if (key >= count)
                    highlights.put(new Pair<>(count-1, elements.getElement(count-1)), linesAndColors.get(key));
                else
                    highlights.put(new Pair<>(key, elements.getElement(key)), linesAndColors.get(key));
            }

        }

        private void updateLines(int atLine, int lineOffset) {
            Map<Pair<Integer, Element>, Color> newLines = new HashMap<>(highlights.size());
            final Element elements;
            if (highlights.size() == 0)
                return;
            else
                elements = parent.getDocument().getDefaultRootElement();

            for (Pair<Integer, Element> key : highlights.keySet()) {
                int line = key.x;
                if (key.x > atLine)
                    if (key.x + lineOffset < 0)
                        line = 0;
                    else if (key.x + lineOffset >= elements.getElementCount())
                        line = elements.getElementCount() - 1;
                    else
                        line += lineOffset;

                final Color value = highlights.get(key);
                key.x = line;
                key.y = elements.getElement(line);

                newLines.put(key, value);
            }

            this.highlights = newLines;
        }
    }
}
