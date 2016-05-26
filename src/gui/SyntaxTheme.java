package gui;



import misc.Pair;

import javax.swing.text.*;
import javax.swing.text.html.HTMLDocument;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author Gordian
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
final class SyntaxTheme {
    /**
     * This field associates file types (as {@link String} with their respective {@link Style}s.
     */
    final Map<String, Style> styleMap;
    final Color defaultLineNumberBackground;
    final Color defaultLineNumberForeground;
    final Color defaultCodeBackground;
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

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({Pair.class, Pattern.class,
        SimpleAttributeSet.class,
})
final class Style {
    final List<Pair<Pattern, AttributeSet>> style;

    // constructor for JAXB
    Style() {
        this.style = Collections.emptyList();
    }

    Style(List<Pair<Pattern, AttributeSet>> style) {
        this.style = Objects.requireNonNull(style);
        if (!this.isValid()) throw new IllegalArgumentException("invalid style");
    }

    final boolean isValid() {
        if (this.style == null) return false;
        boolean[] result = {true};
        this.style.forEach(pair -> {
            if (pair == null) { result[0] = false; return; }
            if (pair.x == null || pair.y == null) { result[0] = false; return; }
        });
        return result[0];
    }
}
