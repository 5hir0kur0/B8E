package gui;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.awt.*;
import static javax.swing.text.StyleConstants.*;

/**
 * @author 5hir0kur0
 */
@XmlRootElement(name = "style")
@XmlAccessorType(XmlAccessType.FIELD)
public class DummyAttributeSet {
    @SuppressWarnings("unused")
    private DummyAttributeSet() { }
    private Integer alignment;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    private Color background;
    private Boolean bold;
    private String  fontFamily;
    private Integer fontSize;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    private Color   foreground;
    private Boolean italic;
    private Float leftIndent;
    private Float lineSpacing;
    private Float rightIndent;
    private Float spaceAbove;
    private Float spaceBelow;
    private Boolean strikeThrough;
    private Boolean subscript;
    private Boolean superscript;
    private Boolean underline;
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
