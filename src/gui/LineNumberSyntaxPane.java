package gui;

import misc.Pair;
import misc.Settings;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
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
    //{ String tmp = System.getProperty("line.separator"); this.LINE_END = null == tmp || tmp.isEmpty() ? "\n" : tmp; }
    //Apparently, \n is always used at runtime --^
    //TODO: Test this on windows!
    private int lastLine = 1;

    private List<Pair<Pattern, AttributeSet>> style;

    private String fileExtension;

    public LineNumberSyntaxPane(String fileExtension) throws IOException {
        super(new BorderLayout());

        this.lineNumbers = new JTextArea("1");
        this.lineNumbers.setLineWrap(false);
        this.lineNumbers.setEditable(false);
        //this.lineNumbers.setEnabled(false);
        this.lineNumbers.setMargin(new Insets(1, 4, 1, 4));

        this.code = new JTextPane();
        this.code.setMargin(new Insets(1, 1, 1, 1));

        this.add(lineNumbers, BorderLayout.LINE_START);
        this.add(code, BorderLayout.CENTER);

        SyntaxHighlightedDocument shDoc = new SyntaxHighlightedDocument(SyntaxThemes.EMPTY_LIST,
                (Observable, Object) -> updateLineNumbers());
        this.code.setContentType("text/plain");
        this.code.setDocument(shDoc);
        this.setFileExtension(fileExtension);
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
        this.lineNumbers.setBackground(SyntaxThemes.getCurrentTheme().getLineNumberBackground());
        this.code.setBackground(SyntaxThemes.getCurrentTheme().getCodeBackground());
        this.lineNumbers.setForeground(SyntaxThemes.getCurrentTheme().getLineNumberForeground());
        this.code.setForeground(SyntaxThemes.getCurrentTheme().getCodeForeground());
        this.style = verifyStyle(SyntaxThemes.getCurrentTheme().getStyleForType(this.fileExtension));
        ((SyntaxHighlightedDocument)this.code.getDocument()).setStyle(this.style);
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
    }

    public void load(Reader r) throws IOException {
        // create a new model to discard already existing contents (in case the file is reloaded)
        SyntaxHighlightedDocument shDoc = new SyntaxHighlightedDocument(SyntaxThemes.EMPTY_LIST,
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

    private void addLines(int count) {
        //final int columnsBefore = this.lineNumbers.getColumns();
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
        //final int columnsAfter = Integer.toUnsignedString(this.lastLine).length();
        //if (columnsBefore != columnsAfter)
        //    this.lineNumbers.setColumns(columnsAfter);
        //this.lineNumbers.setRows(this.lastLine);
        //This turned out to be unnecessary --^
    }

    private static int numDigits(long number) {
        if (!(number >= 0))
            throw new IllegalArgumentException("Cannot calculate the number of digits of a negative number.");
        int digits;
        for (digits = 1; (number /= 10) != 0; ++digits) ;
        return digits;
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
}
