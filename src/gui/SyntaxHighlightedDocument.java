package gui;

import javax.swing.text.*;
import java.awt.*;
import java.util.Map;
import java.util.Observer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static javax.swing.text.StyleConstants.*;
import static java.util.regex.Pattern.compile;

/**
 * @author Gordian
 */
public class SyntaxHighlightedDocument extends DefaultStyledDocument {
    private final Map<Pattern, AttributeSet> style;
    private final Observer observer;

    SyntaxHighlightedDocument(Map<Pattern, AttributeSet> style, Observer observer) {
        this.style = style;
        this.observer = observer;
        style.put(compile("(?i)hello"), create(Foreground, Color.BLUE));
        style.put(compile("(?i)world"), create(Foreground, Color.RED));
        style.put(compile("(?i)foobar"), create(Foreground, Color.CYAN, Background, Color.BLACK, Bold, true));
    }

    static AttributeSet create(Object... nameValuePairs) {
        MutableAttributeSet result = StyleContext.getDefaultStyleContext().addStyle(null, null);
        for (int i = 0; i < nameValuePairs.length-1; ++i) {
            result.addAttribute(nameValuePairs[i], nameValuePairs[++i]);
        }
        return result;
    }

    @Override
    public void insertString(int offset, String str, AttributeSet as) throws BadLocationException {
        super.insertString(offset, str, StyleContext.getDefaultStyleContext().getEmptySet());
        updateSyntaxHighlighting(offset, str.length());
        if (str.contains("\n")) this.observer.update(null, null);
        System.out.println("InserString called");
    }

    @Override
    public void remove(int offset, int length) throws BadLocationException {
        final boolean lineChange = super.getText(offset, length).contains("\n");
        super.remove(offset, length);
        updateSyntaxHighlighting(offset, 1); //update the line starting from which text was deleted
        if (lineChange) this.observer.update(null, null);
        System.out.println("Remove called");
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
            //super.setParagraphAttributes(tmpStart, tmpLength, SimpleAttributeSet.EMPTY, true);

            for (Pattern p : style.keySet()) {
                final Matcher m = p.matcher(line);
                while (m.find()) {
                    final int matchStart  = tmpStart + m.start();
                    final int matchLength = m.end() - m.start();
                    System.out.println("matched: " + super.getText(matchStart, matchLength));
                    super.setCharacterAttributes(matchStart, matchLength, this.style.get(p), false);
                }
            }
        }
    }
}
