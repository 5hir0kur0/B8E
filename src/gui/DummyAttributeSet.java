package gui;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.awt.*;
import static javax.swing.text.StyleConstants.*;

/**
 * @author 5hir0kur0
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class DummyAttributeSet {
    @SuppressWarnings("unused")
    private DummyAttributeSet() { }
    @XmlAttribute private Integer alignment;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    @XmlAttribute private Color background;
    @XmlAttribute private Boolean bold;
    @XmlAttribute private String  fontFamily;
    @XmlAttribute private Integer fontSize;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    @XmlAttribute private Color   foreground;
    @XmlAttribute private Boolean italic;
    @XmlAttribute private Float leftIndent;
    @XmlAttribute private Float lineSpacing;
    @XmlAttribute private Float rightIndent;
    @XmlAttribute private Float spaceAbove;
    @XmlAttribute private Float spaceBelow;
    @XmlAttribute private Boolean strikeThrough;
    @XmlAttribute private Boolean subscript;
    @XmlAttribute private Boolean superscript;
    @XmlAttribute private Boolean underline;
    DummyAttributeSet(AttributeSet as) {
        this.alignment     = getAlignment(as);
        this.background    = getBackground(as);
        this.bold          = isBold(as);
        this.fontFamily    = getFontFamily(as);
        this.fontSize      = getFontSize(as);
        this.foreground    = getForeground(as);
        this.italic        = isItalic(as);
        this.leftIndent    = getLeftIndent(as);
        this.lineSpacing   = getLineSpacing(as);
        this.rightIndent   = getRightIndent(as);
        this.spaceAbove    = getSpaceAbove(as);
        this.spaceBelow    = getSpaceBelow(as);
        this.strikeThrough = isStrikeThrough(as);
        this.subscript     = isSubscript(as);
        this.superscript   = isSuperscript(as);
        this.underline     = isUnderline(as);
    }

    AttributeSet toAttributeSet() {
        MutableAttributeSet res = StyleContext.getDefaultStyleContext().addStyle(null, null);
        setAlignment(res, this.alignment);
        setBackground(res, this.background);
        setBold(res, this.bold     );
        setFontFamily(res, this.fontFamily);
        setFontSize(res, this.fontSize );
        setForeground(res, this.foreground);
        setItalic(res, this.italic   );
        setLeftIndent(res, this.leftIndent);
        setLineSpacing(res, this.lineSpacing);
        setRightIndent(res, this.rightIndent);
        setSpaceAbove(res, this.spaceAbove);
        setSpaceBelow(res, this.spaceBelow);
        setStrikeThrough(res, this.strikeThrough);
        setSubscript(res, this.subscript);
        setSuperscript(res, this.superscript);
        setUnderline(res, this.underline);
        return res;
    }
}
