package gui;



import javax.swing.text.AttributeSet;
import java.awt.Color;
import java.util.List;
import java.util.regex.Pattern;
import misc.Pair;

/**
 * @author 5hir0kur0
 */
public interface SyntaxTheme {
    List<Pair<Pattern, AttributeSet>> getStyleForType(String type); //type = file extension
    Color getLineNumberBackground();
    Color getLineNumberForeground();
    Color getCodeBackground();
    Color getCodeForeground();
}
