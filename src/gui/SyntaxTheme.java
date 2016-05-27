package gui;



import misc.Pair;

import javax.swing.text.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.awt.Color;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Gordian
 */
@XmlRootElement(namespace = "https://github.com/5hir0kur0/B8E/tree/master/src/gui")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso(Style.class)
final class SyntaxTheme {
    /**
     * This field associates file types (as {@link String} with their respective {@link Style}s.
     */
    final Map<String, Style> styleMap;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    final Color defaultLineNumberBackground;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    final Color defaultLineNumberForeground;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    final Color defaultCodeBackground;
    @XmlJavaTypeAdapter(ColorAdapter.class)
    final Color defaultCodeForeground;

    SyntaxTheme(Map<String, Style> styleMap, Color defaultLineNumberBackground, Color defaultLineNumberForeground,
                Color defaultCodeBackground, Color defaultCodeForeground) {
        this.styleMap = Objects.requireNonNull(styleMap);
        this.defaultLineNumberBackground = Objects.requireNonNull(defaultLineNumberBackground);
        this.defaultLineNumberForeground = Objects.requireNonNull(defaultLineNumberForeground);
        this.defaultCodeBackground = Objects.requireNonNull(defaultCodeBackground);
        this.defaultCodeForeground = Objects.requireNonNull(defaultCodeForeground);
    }

    // constructor for JAXB
    @SuppressWarnings("unused")
    SyntaxTheme() {
        this.styleMap = Collections.emptyMap();
        this.defaultLineNumberBackground = Color.WHITE;
        this.defaultLineNumberForeground = Color.BLACK;
        this.defaultCodeBackground = Color.WHITE;
        this.defaultCodeForeground = Color.BLACK;
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

    Style getStyleForType(String type) {
        Style result = this.styleMap.get(type);
        if (result == null) return new Style();
        return result;
    }

    boolean isValid() {
        if (this.styleMap == null || this.defaultLineNumberBackground == null
                || this.defaultLineNumberForeground == null || this.defaultCodeBackground == null
                || this.defaultCodeForeground == null) return false;
        for (Map.Entry<String, Style> entry : this.styleMap.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) return false;
            if (entry.getValue() == null || !entry.getValue().isValid()) return false;
        }
        return true;
    }
}


@XmlRootElement(name = "style")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({Pair.class, Pattern.class, DummyAttributeSet.class, LinkedList.class})
class Style {
    @XmlElement(name = "style")
    final List<StylePair> style;

    // constructor for JAXB
    Style() {
        this.style = Collections.emptyList();
    }

    Style(List<Pair<Pattern, AttributeSet>> style) {
        this.style = new LinkedList<>();
        style.forEach(e -> this.style.add(new StylePair(e.x.pattern(), new DummyAttributeSet(e.y))));
        if (!this.isValid()) throw new IllegalArgumentException("invalid style");
    }

    List<Pair<Pattern, AttributeSet>> getStyle() {
        List<Pair<Pattern, AttributeSet>> res =  new LinkedList<>();
        this.style.forEach(e -> res.add(new Pair<>(Pattern.compile(e.pattern), (AttributeSet)e.attributes)));
        return res;
    }

    final boolean isValid() {
        if (this.style == null) return false;
        boolean[] result = {true};
        this.style.forEach(pair -> {
            if (pair == null) { result[0] = false; return; }
            if (pair.pattern == null || pair.attributes == null) { result[0] = false; return; }
        });
        return result[0];
    }
}

@XmlRootElement
class StylePair {
    @XmlAttribute String pattern;
    @XmlElement DummyAttributeSet attributes;

    @SuppressWarnings("unused") private StylePair() { }

    StylePair(String pattern, DummyAttributeSet attributes) {
        this.pattern = Objects.requireNonNull(pattern);
        this.attributes = Objects.requireNonNull(attributes);
    }
}