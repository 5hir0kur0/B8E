package gui;

import misc.Settings;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Gordian
 */
public class LineNumberSyntaxPane extends JPanel {

    private final JTextArea lineNumbers = new JTextArea();
    {
        this.lineNumbers.setLineWrap(false);
        this.lineNumbers.setEditable(false);
        this.lineNumbers.setEnabled(false);
        this.lineNumbers.setMargin(new Insets(1,1,1,2));
        this.lineNumbers.setText("1");
    }
    private final JTextPane code = new JTextPane();
    { this.code.setMargin(new Insets(1,1,1,1)); }
    private final String LINE_END = "\n";
    //{ String tmp = System.getProperty("line.separator"); this.LINE_END = null == tmp || tmp.isEmpty() ? "\n" : tmp; }
    //Apparently, \n is always used at runtime --^
    //TODO: Test this on windows!
    private int lastLine = 1;
    private HashMap<Pattern, Color> style;
    private final Writer out;
    private static Pattern decInteger = Pattern.compile("(\\d+)");
    private static Pattern hexInteger = Pattern.compile("#([a-fA-F0-9]{6})");

    static {
        Settings s = Settings.INSTANCE;
        s.setDefault("gui.editor.font", Font.MONOSPACED);
        s.setDefault("gui.editor.font-size", "12");
    }

    private class EditListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateLineNumbers();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateLineNumbers();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updateLineNumbers();
        }
    }

    public LineNumberSyntaxPane(HashMap<String, String> style, Reader in, Writer out) throws IOException {
        super(new BorderLayout());
        this.add(lineNumbers, BorderLayout.LINE_START);
        this.add(code, BorderLayout.CENTER);
        this.style = verifyAndConvertStyle(style);
        if (in != null) this.code.read(in, null);
        this.out = out;
        this.code.getDocument().addDocumentListener(new EditListener());
        this.code.setText("public class Test {\npublic static void main(String[] args) {\nSystem.out.println(\"This is a test\");\n}\n}");
        updateLineNumbers();
        updateComponentStyle();
    }

    public void setFontSize(int newSize) {
        if (newSize <= 0) throw new IllegalArgumentException("Illegal font size: "+newSize);
        Font oldFont = this.lineNumbers.getFont();
        Font newFont = new Font(oldFont.getName(), oldFont.getStyle(), newSize);
        this.lineNumbers.setFont(newFont);
        this.code.setFont(newFont);
    }

    public int getFontSize() { return this.code.getFont().getSize(); }

    public void setStyle(HashMap<String, String> style) {
        this.style = verifyAndConvertStyle(style);
    }

    public void updateComponentStyle() {
        Font f = new Font(Settings.INSTANCE.getProperty("gui.editor.font", Font.MONOSPACED), Font.PLAIN,
                Settings.INSTANCE.getIntProperty("gui.editor.font-size", 12, i -> i > 0));
        this.lineNumbers.setFont(f);
        this.code.setFont(f);
        Color background = getSpecialExpressionColor("background-color", Color.WHITE);
        Color foreground = getSpecialExpressionColor("foreground-color", Color.BLACK);
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
            if (lastLine + count <= 0) throw new IllegalArgumentException("Illegal remove count: "+count);
            final int lastVisibleLineAfterOperation = lastLine + count;
            try {
                final int removeStartOffset = this.lineNumbers.getLineEndOffset(lastVisibleLineAfterOperation-1)-1;
                final int removeEndOffset = this.lineNumbers.getLineEndOffset(this.lastLine-1);
                this.lineNumbers.replaceRange(null, removeStartOffset, removeEndOffset);
            } catch (BadLocationException e) {
                throw new IllegalArgumentException("Error during line number deletion.",e);
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
        for(digits = 1; (number /= 10) != 0; ++digits);
        return digits;
    }

    private void updateLineNumbers() {
        int lineCount = lineCount(this.code);
        if (lineCount != lastLine) addLines(lineCount - lastLine);
    }

    private Color getSpecialExpressionColor(String expr, Color defaultValue) {
        final Pattern search = Pattern.compile("`"+Objects.requireNonNull(expr)+"`");
        Color res = this.style.get(search);
        return null == res ? defaultValue : res;
    }

    private static Color parseColor(String color) {
        if (decInteger.matcher(color).matches()) return new Color(Integer.parseInt(color));
        else if (hexInteger.matcher(color).matches()) return new Color(Integer.parseInt(color.substring(1), 16));
        else throw new IllegalArgumentException("Invalid color value");
    }

    private static int lineCount(JTextComponent jtc) {
        return jtc.getDocument().getDefaultRootElement().getElementCount(); //swing voodoo magic
    }

    private static HashMap<Pattern, Color> verifyAndConvertStyle(HashMap<String, String> style) {
        Set<String> keys = Objects.requireNonNull(style, "Style HashMap must not be null.").keySet();
        HashMap<Pattern, Color> result = new HashMap<>();
        for (String k : keys) {
            String entry = Objects.requireNonNull(
                    style.get(Objects.requireNonNull(k, "Style HashMap must not contain null-keys.")),
                    "Style HashMap must not contain null-entries.");
            final Color c = parseColor(entry);
            result.put(Pattern.compile(k), c);
        }
        return result;
    }
}
