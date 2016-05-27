package gui;



import misc.Pair;

import javax.swing.text.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.awt.Color;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author 5hir0kur0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({Color.class})
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
    @XmlElementWrapper(name = "pairs")
    @XmlElement(name = "pair")
    @XmlJavaTypeAdapter(AttributeSetAdapter.class)
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

class AttributeSetAdapter extends XmlAdapter<LinkedList<Pair<Pattern, DummyAttributeSet>>,
        List<Pair<Pattern, AttributeSet>>> {
    @Override
    public List<Pair<Pattern, AttributeSet>> unmarshal(LinkedList<Pair<Pattern, DummyAttributeSet>> v) {
        List<Pair<Pattern, AttributeSet>> result = new LinkedList<>();
        for (Pair<Pattern, DummyAttributeSet> entry : v)
            result.add(new Pair<>(entry.x, entry.y.toAttributeSet()));
        return result;
    }

    @Override
    public LinkedList<Pair<Pattern, DummyAttributeSet>> marshal(List<Pair<Pattern, AttributeSet>> v) {
        LinkedList<Pair<Pattern, DummyAttributeSet>> result = new LinkedList<>();
        for (Pair<Pattern, AttributeSet> entry : v)
            result.add(new Pair<>(entry.x, new DummyAttributeSet(entry.y)));
        return result;
    }
}
