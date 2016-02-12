package gui;

import misc.Settings;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final static String SYNTAX_THEME_SETTING = "gui.editor.syntax-theme";
    private final static String SYNTAX_THEME_SETTING_DEFAULT = "base16-default";
    private final static Pattern THEME_NAME_PATTERN = Pattern.compile("[\\w\\-]+");
    private final static Predicate<String> IS_VALID_THEME_NAME = s -> THEME_NAME_PATTERN.matcher(s).matches();

    static {
        Settings.INSTANCE.setDefault(FONT_SETTING, FONT_SETTING_DEFAULT);
        Settings.INSTANCE.setDefault(FONT_SIZE_SETTING, FONT_SIZE_SETTING_DEFAULT);
        Settings.INSTANCE.setDefault(SYNTAX_THEME_SETTING, SYNTAX_THEME_SETTING_DEFAULT);
    }

    private final JTextArea lineNumbers;
    private final JTextPane code;
    private final String LINE_END = "\n";
    //{ String tmp = System.getProperty("line.separator"); this.LINE_END = null == tmp || tmp.isEmpty() ? "\n" : tmp; }
    //Apparently, \n is always used at runtime --^
    //TODO: Test this on windows!
    private int lastLine = 1;
    private final Writer out;

    public LineNumberSyntaxPane(Map<Pattern, AttributeSet> style, Reader in, Writer out) throws IOException {
        super(new BorderLayout());

        this.lineNumbers = new JTextArea("1");
        this.lineNumbers.setLineWrap(false);
        this.lineNumbers.setEditable(false);
        this.lineNumbers.setEnabled(false);
        this.lineNumbers.setMargin(new Insets(1, 1, 1, 2));

        this.code = new JTextPane();
        this.code.setMargin(new Insets(1, 1, 1, 1));

        this.add(lineNumbers, BorderLayout.LINE_START);
        this.add(code, BorderLayout.CENTER);

        if (in != null) this.code.read(in, null);
        this.out = out;

        SyntaxHighlightedDocument shDoc = new SyntaxHighlightedDocument(style,
                (Observable, Object) -> updateLineNumbers());
        this.code.setContentType("text/plain");
        this.code.setDocument(shDoc);
        this.code.setText("public class Test {\npublic static void main(String[] args) {\nSystem.out.println(\"This is a test\");\n}\n}");
        updateLineNumbers();
        updateComponentStyle();
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

    public void setStyle(HashMap<String, String> style) {
        //TODO
        //this.style = verifyAndConvertStyle(style);
        updateComponentStyle();
        updateSyntaxHighlighting();
    }


    public void updateComponentStyle() {
        Font f = new Font(Settings.INSTANCE.getProperty(FONT_SETTING, FONT_SETTING_DEFAULT, IS_VALID_FONT_NAME),
                Font.PLAIN,
                Settings.INSTANCE.getIntProperty(FONT_SIZE_SETTING,
                        Integer.parseInt(FONT_SIZE_SETTING_DEFAULT), IS_VALID_FONT_SIZE));
        this.lineNumbers.setFont(f);
        this.code.setFont(f);
        Color background = Color.WHITE; //getSpecialExpressionColor("background-color", Color.WHITE);
        Color foreground = Color.BLACK; //getSpecialExpressionColor("foreground-color", Color.BLACK);
        this.lineNumbers.setBackground(background);
        this.code.setBackground(background);
        this.lineNumbers.setForeground(foreground);
        this.code.setForeground(foreground);
    }

    public void updateSyntaxHighlighting() {
        //TODO
    }

    public void write() throws IOException {
        this.code.write(Objects.requireNonNull(this.out, "Attempting to write to null-Writer."));
    }

    public void write(Writer out) throws IOException {
        this.code.write(Objects.requireNonNull(out, "Attempting to write to null-Writer."));
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

    private Color getSpecialExpressionColor(String expr, Color defaultValue) {
        //TODO
        return Color.BLACK;
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
}
