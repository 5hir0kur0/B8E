package gui;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.awt.Color;

import static javax.swing.text.StyleConstants.*;

/**
 * @author 5hir0kur0
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class DummyAttributeSet {
    @SuppressWarnings("unused")
    private DummyAttributeSet() {}
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

    DummyAttributeSet(Color background, Boolean bold, Color foreground, Boolean italic, Boolean strikeThrough,
                      Boolean underline) {
        this.background = background;
        this.bold = bold;
        this.foreground = foreground;
        this.italic = italic;
        this.strikeThrough = strikeThrough;
        this.underline = underline;
    }

    AttributeSet toAttributeSet() {
        MutableAttributeSet res = StyleContext.getDefaultStyleContext().addStyle(null, null);
        if (this.background != null) setBackground(res, this.background);
        if (this.bold != null) setBold(res, this.bold);
        if (this.foreground != null) setForeground(res, this.foreground);
        if (this.italic != null) setItalic(res, this.italic);
        if (this.strikeThrough != null) setStrikeThrough(res, this.strikeThrough);
        if (this.underline != null) setUnderline(res, this.underline);
        return res;
    }
}
