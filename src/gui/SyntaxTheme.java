package gui;


import misc.Pair;

import javax.swing.text.AttributeSet;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.awt.Color;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author 5hir0kur0
 */
@XmlRootElement(namespace = "https://github.com/5hir0kur0/B8E/tree/master/src/gui")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({Style.class, HashMap.class, ColorAdapter.class, Color.class})
final class SyntaxTheme {
    /**
     * This field associates file types (as {@link String} with their respective {@link Style}s.
     */
    final LinkedHashMap<String, Style> styleMap;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    final Color defaultLineNumberBackground;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    final Color defaultLineNumberForeground;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    final Color defaultCodeBackground;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    final Color defaultCodeForeground;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    final Color defaultCaretColor;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    final Color defaultSelectionColor;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    final Color defaultSelectedTextColor;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    final Color defaultErrorColor;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    final Color defaultWarningColor;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    final Color defaultInformationColor;


    SyntaxTheme(LinkedHashMap<String, Style> styleMap, Color defaultLineNumberBackground,
                Color defaultLineNumberForeground, Color defaultCodeBackground, Color defaultCodeForeground,
                Color defaultCaretColor, Color defaultSelectionColor, Color defaultSelectedTextColor,
                Color defaultErrorColor, Color defaultWarningColor, Color defaultInformationColor) {
        this.styleMap = Objects.requireNonNull(styleMap);
        this.defaultLineNumberBackground = Objects.requireNonNull(defaultLineNumberBackground);
        this.defaultLineNumberForeground = Objects.requireNonNull(defaultLineNumberForeground);
        this.defaultCodeBackground = Objects.requireNonNull(defaultCodeBackground);
        this.defaultCodeForeground = Objects.requireNonNull(defaultCodeForeground);
        this.defaultCaretColor = Objects.requireNonNull(defaultCaretColor);
        this.defaultSelectionColor = Objects.requireNonNull(defaultSelectionColor);
        this.defaultSelectedTextColor = Objects.requireNonNull(defaultSelectedTextColor);
        this.defaultErrorColor = Objects.requireNonNull(defaultErrorColor);
        this.defaultWarningColor = Objects.requireNonNull(defaultWarningColor);
        this.defaultInformationColor = Objects.requireNonNull(defaultInformationColor);
    }

    // constructor for JAXB
    @SuppressWarnings("unused")
    SyntaxTheme() {
        this.styleMap = new LinkedHashMap<>();
        this.defaultLineNumberBackground = Color.WHITE;
        this.defaultLineNumberForeground = Color.BLACK;
        this.defaultCodeBackground = Color.WHITE;
        this.defaultCodeForeground = Color.BLACK;
        this.defaultCaretColor = Color.BLACK;
        this.defaultSelectionColor = Color.CYAN;
        this.defaultSelectedTextColor = Color.WHITE;
        this.defaultErrorColor = Color.RED;
        this.defaultWarningColor = Color.ORANGE;
        this.defaultInformationColor = Color.LIGHT_GRAY;
    }

    Color getLineNumberBackground() {
        return this.defaultLineNumberBackground;
    }

    Color getLineNumberForeground() {
        return this.defaultLineNumberForeground;
    }

    Color getCodeBackground() {
        return this.defaultCodeBackground;
    }

    Color getCodeForeground() {
        return this.defaultCodeForeground;
    }

    public Color getWarningColor() {
        return defaultWarningColor;
    }

    public Color getInformationColor() {
        return defaultInformationColor;
    }

    public Color getErrorColor() {
        return defaultErrorColor;
    }

    public Color getSelectionColor() {
        return defaultSelectionColor;
    }

    public Color getSelectedTextColor() {
        return defaultSelectedTextColor;
    }

    public Color getCaretColor() {
        return defaultCaretColor;
    }

    Style getStyleForType(String type) {
        Style result = this.styleMap.get(type);
        if (result == null) return new Style();
        return result;
    }

    boolean isValid() {
        if (this.styleMap == null || this.defaultLineNumberBackground == null
                || this.defaultLineNumberForeground == null || this.defaultCodeBackground == null
                || this.defaultCodeForeground == null || this.defaultCaretColor == null
                || this.defaultSelectionColor == null || this.defaultErrorColor == null
                || this.defaultWarningColor == null || this.defaultInformationColor == null) return false;
        for (Map.Entry<String, Style> entry : this.styleMap.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) return false;
            if (entry.getValue() == null || !entry.getValue().isValid()) return false;
        }
        return true;
    }
}


@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({Pair.class, Pattern.class, DummyAttributeSet.class, LinkedList.class})
class Style {
    @XmlElement(name = "style")
    final LinkedList<StylePair> style;

    // constructor for JAXB
    Style() {
        this.style = new LinkedList<>();
    }

    Style(List<Pair<Pattern, DummyAttributeSet>> style) {
        this.style = new LinkedList<>();
        style.forEach(e -> this.style.add(new StylePair(e.x.pattern(), e.x.flags(), e.y)));
        if (!this.isValid()) throw new IllegalArgumentException("invalid style");
    }

    List<Pair<Pattern, AttributeSet>> getStyle() {
        List<Pair<Pattern, AttributeSet>> res =  new LinkedList<>();
        this.style.forEach(e -> res.add(
                new Pair<>(Pattern.compile(e.pattern, e.flags == null ? 0 : e.flags), e.attributes.toAttributeSet())));
        return res;
    }

    final boolean isValid() {
        if (this.style == null) return false;
        boolean[] result = {true};
        this.style.forEach(pair -> {
            if (pair == null) { result[0] = false; return; }
            if (pair.pattern == null || pair.attributes == null) { result[0] = false; }
        });
        return result[0];
    }
}

@XmlRootElement
class StylePair {
    @XmlAttribute String pattern;
    @XmlAttribute Integer flags;
    @XmlElement DummyAttributeSet attributes;

    @SuppressWarnings("unused") private StylePair() { }

    StylePair(String pattern, int flags, DummyAttributeSet attributes) {
        this.pattern = Objects.requireNonNull(pattern);
        this.attributes = Objects.requireNonNull(attributes);
        this.flags = flags == 0 ? null : flags;
    }
}