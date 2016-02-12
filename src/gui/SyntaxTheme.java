package gui;


import javax.swing.text.AttributeSet;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Gordian
 */
public interface SyntaxTheme {
    Map<Pattern, AttributeSet> getStyleForType(String type); //type = file extension
}
