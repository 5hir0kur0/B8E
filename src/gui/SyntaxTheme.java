package gui;


import javax.swing.text.AttributeSet;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author 5hir0kur0
 */
public interface SyntaxTheme {
    Map<Pattern, AttributeSet> getStyleForType(String type); //type = file extension
}
