package gui;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.awt.*;
import static javax.swing.text.StyleConstants.*;

/**
 * @author Gordian
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class DummyAttributeSet {
    @SuppressWarnings("unused")
    private DummyAttributeSet() {
        // set stupid defaults, so the user notices something went wrong
        this.background    = Color.CYAN;
        this.bold          = true;
        this.foreground    = Color.RED;
        this.italic        = true;
        this.strikeThrough = true;
        this.underline     = true;
    }
    @XmlJavaTypeAdapter(ColorAdapter.class)
    @XmlAttribute(required = true) private Color background;
    @XmlAttribute private Boolean bold;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    @XmlAttribute(required = true) private Color   foreground;
    @XmlAttribute private Boolean italic;
    @XmlAttribute private Boolean strikeThrough;
    @XmlAttribute private Boolean underline;
    DummyAttributeSet(AttributeSet as) {
        this.background    = getBackground(as);
        this.bold          = isBold(as);
        this.foreground    = getForeground(as);
        this.italic        = isItalic(as);
        this.strikeThrough = isStrikeThrough(as);
        this.underline     = isUnderline(as);
    }

    AttributeSet toAttributeSet() {
        MutableAttributeSet res = StyleContext.getDefaultStyleContext().addStyle(null, null);
        setBackground(res, this.background);
        setBold(res, this.bold);
        setForeground(res, this.foreground);
        setItalic(res, this.italic);
        setStrikeThrough(res, this.strikeThrough);
        setUnderline(res, this.underline);
        return res;
    }
}
