package gui;

import misc.Pair;
import misc.Settings;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.swing.text.StyleConstants;

/**
 * @author 5hir0kur0
 */
public enum SyntaxThemes {
    DEFAULT("base16-default",
            new Color(0x181818), // base00
            new Color(0x282828), // base01
            new Color(0x383838), // base02
            new Color(0x585858), // base03
            new Color(0xB8B8B8), // base04
            new Color(0xD8D8D8), // base05
            new Color(0xE8E8E8), // base06
            new Color(0xF8F8F8), // base07
            new Color(0xAB4642), // base08
            new Color(0xDC9656), // base09
            new Color(0xF7CA88), // base0A
            new Color(0xA1B56C), // base0B
            new Color(0x86C1B9), // base0C
            new Color(0x7CAFC2), // base0D
            new Color(0xBA8BAF), // base0E
            new Color(0xA16946)) // base0F
    {
        List<Pair<Pattern, AttributeSet>> hexTheme = null;
        List<Pair<Pattern, AttributeSet>> propertiesTheme = null;

        @Override public SyntaxTheme getSyntaxTheme() {
            if (null == DEFAULT.syntaxTheme) {
                DEFAULT.syntaxTheme = new SyntaxTheme() {
                    @Override
                    public List<Pair<Pattern, AttributeSet>> getStyleForType(String type) {
                        switch (Objects.requireNonNull(type).toLowerCase()) {
                            case "asm":
                            case "asm51":
                                throw new UnsupportedOperationException();
                            case "hex":
                                if (null == hexTheme) hexTheme = SyntaxThemes.createIntelHexTheme(
                                        DEFAULT.base04, // start code
                                        DEFAULT.base09, // data byte
                                        DEFAULT.base04, // address
                                        DEFAULT.base08, // valid record type
                                        DEFAULT.base08, // invalid record type
                                        DEFAULT.base0E, // data
                                        DEFAULT.base04, // checksum foreground
                                        DEFAULT.base0A, // checksum background
                                        DEFAULT.base01, // EOF foreground
                                        DEFAULT.base0C  // EOF background
                                );
                                return hexTheme;
                            case "properties":
                                if (null == propertiesTheme) propertiesTheme = createPropertiesTheme(
                                        DEFAULT.base04, // comment
                                        DEFAULT.base08, // key
                                        DEFAULT.base0B, // value
                                        DEFAULT.base0D, // end escape
                                        DEFAULT.base0F  // unicode
                                );
                                return propertiesTheme;
                            default:
                                //TODO: Log
                                return SyntaxThemes.EMPTY_LIST;
                        }
                    }

                    @Override
                    public Color getLineNumberBackground() {
                        return DEFAULT.base06;
                    }

                    @Override
                    public Color getLineNumberForeground() {
                        return DEFAULT.base04;
                    }

                    @Override
                    public Color getCodeBackground() {
                        return DEFAULT.base07;
                    }

                    @Override
                    public Color getCodeForeground() {
                        return DEFAULT.base02;
                    }
                };
            }
            return DEFAULT.syntaxTheme;
        }
    };

    private final String name;
    private final Color base00;
    private final Color base01;
    private final Color base02;
    private final Color base03;
    private final Color base04;
    private final Color base05;
    private final Color base06;
    private final Color base07;
    private final Color base08;
    private final Color base09;
    private final Color base0A;
    private final Color base0B;
    private final Color base0C;
    private final Color base0D;
    private final Color base0E;
    private final Color base0F;
    private SyntaxTheme syntaxTheme = null;
    private static List<String> names;
    static final List<Pair<Pattern, AttributeSet>> EMPTY_LIST = Collections.emptyList();

    //intel hex patterns
    private static final Pattern EOF = Pattern.compile("^:(00000001)FF\\s*$");
    private static final Pattern START_CODE = Pattern.compile("^(:)");
    private static final Pattern DATA_BYTE_COUNT = Pattern.compile("^:([\\da-fA-F]{2})");
    private static final Pattern ADDRESS = Pattern.compile("^:[\\da-fA-F]{2}([\\da-fA-F]{4})");
    private static final Pattern VALID_RECORD_TYPE = Pattern.compile("^:[\\da-fA-F]{2}[\\da-fA-F]{4}(0[0-5])");
    private static final Pattern INVALID_RECORD_TYPE =
            Pattern.compile("^:[\\da-fA-F]{2}[\\da-fA-F]{4}(0[6-9a-fA-F]|[1-9a-fA-F][\\da-fA-F])");
    private static final Pattern DATA =
            Pattern.compile("^:[\\da-fA-F]{2}[\\da-fA-F]{4}[\\da-fA-F]{2}.*?((?:[\\da-fA-F]{2}(?!$\\s*))*)");
    private static final Pattern CHECKSUM =
            Pattern.compile("^:[\\da-fA-F]{2}[\\da-fA-F]{4}[\\da-fA-F]{2}(?:[\\da-fA-F]{2})*([\\da-fA-F]{2})\\s*$");

    //properties file patterns
    private static final Pattern PROP_COMMENT = Pattern.compile("([#!].*)$");
    private static final Pattern PROP_KEY = Pattern.compile("^\\s*((?:[\\w]|\\\\\\s)+)\\s*[=:]\\s*");
    private static final Pattern PROP_UNICODE =
            Pattern.compile("^\\s*(?:[\\w]|\\\\\\s)+\\s*[=:]\\s*(\\\\u[\\da-fA-F]{4})");
    private static final Pattern PROP_VALUE =
            Pattern.compile("^\\s*(?:[\\w]|\\\\\\s)+\\s*[=:]\\s*([^\\s].*)$|^\\s*([^\\s].*)\\s*$");
    private static final Pattern PROP_END_ESCAPE = Pattern.compile("(\\\\)$");


    SyntaxThemes(String name,
                 Color base00, // the colors are set in the constructor, because they are final and
                 Color base01, // the compiler complains when they are set in an initializer block
                 Color base02,
                 Color base03,
                 Color base04,
                 Color base05,
                 Color base06,
                 Color base07,
                 Color base08,
                 Color base09,
                 Color base0A,
                 Color base0B,
                 Color base0C,
                 Color base0D,
                 Color base0E,
                 Color base0F
                 ) {
        this.name = Objects.requireNonNull(name);
        this.base00 = Objects.requireNonNull(base00);
        this.base01 = Objects.requireNonNull(base01);
        this.base02 = Objects.requireNonNull(base02);
        this.base03 = Objects.requireNonNull(base03);
        this.base04 = Objects.requireNonNull(base04);
        this.base05 = Objects.requireNonNull(base05);
        this.base06 = Objects.requireNonNull(base06);
        this.base07 = Objects.requireNonNull(base07);
        this.base08 = Objects.requireNonNull(base08);
        this.base09 = Objects.requireNonNull(base09);
        this.base0A = Objects.requireNonNull(base0A);
        this.base0B = Objects.requireNonNull(base0B);
        this.base0C = Objects.requireNonNull(base0C);
        this.base0D = Objects.requireNonNull(base0D);
        this.base0E = Objects.requireNonNull(base0E);
        this.base0F = Objects.requireNonNull(base0F);
    }

    // settings
    private final static String SYNTAX_THEME_SETTING = "gui.editor.syntax-theme";
    private final static String SYNTAX_THEME_SETTING_DEFAULT = "base16-default";
    private final static Pattern THEME_NAME_PATTERN = Pattern.compile("[\\w\\-]+");
    private final static Predicate<String> IS_VALID_THEME_NAME = s -> THEME_NAME_PATTERN.matcher(s).matches();

    static {
        Settings.INSTANCE.setDefault(SYNTAX_THEME_SETTING, SYNTAX_THEME_SETTING_DEFAULT);
        SyntaxThemes.setSyntaxThemeByName(Settings.INSTANCE.getProperty(SYNTAX_THEME_SETTING,
                                                                        SYNTAX_THEME_SETTING_DEFAULT,
                                                                        IS_VALID_THEME_NAME));
    }

    private static SyntaxTheme currentTheme;

    public static void setSyntaxThemeByName(String name) {
        SyntaxThemes.currentTheme = getSyntaxThemeByName(name);
    }

    public static SyntaxTheme getCurrentTheme() {
        return SyntaxThemes.currentTheme;
    }

    static SyntaxTheme getSyntaxThemeByName(String name) {
        for (SyntaxThemes st : SyntaxThemes.values())
            if (st.name.equals(name))
                return st.getSyntaxTheme();
        throw new IllegalArgumentException("invalid syntax theme name");
    }

    public static List<String> getThemeNames() {
        if (SyntaxThemes.names == null) {
            List<String> tmpList = new LinkedList<>();
            for (SyntaxThemes st : SyntaxThemes.values()) {
                tmpList.add(st.name);
            }
            SyntaxThemes.names = tmpList;
        }
        return SyntaxThemes.names;
    }

    /** creates the light intel hex theme */
    private static List<Pair<Pattern, AttributeSet>> createIntelHexTheme(Color startCodeColor,
                                                                  Color dataByteCountColor,
                                                                  Color addressColor,
                                                                  Color validRecordTypeColor,
                                                                  Color invalidRecordTypeColor,
                                                                  Color dataColor,
                                                                  Color checksumForegroundColor,
                                                                  Color checksumBackgroundColor,
                                                                  Color eofForegroundColor,
                                                                  Color eofBackgroundColor
                                                                 ) {
        List<Pair<Pattern, AttributeSet>> tmp = new LinkedList<>();
        tmp.add(new Pair<>(START_CODE, create(StyleConstants.Foreground, startCodeColor)));
        tmp.add(new Pair<>(DATA_BYTE_COUNT, create(StyleConstants.Foreground, dataByteCountColor)));
        tmp.add(new Pair<>(ADDRESS, create(StyleConstants.Foreground, addressColor)));
        tmp.add(new Pair<>(VALID_RECORD_TYPE, create(StyleConstants.Foreground, validRecordTypeColor)));
        tmp.add(new Pair<>(INVALID_RECORD_TYPE, create(StyleConstants.StrikeThrough, true,
                StyleConstants.Foreground, invalidRecordTypeColor)));
        tmp.add(new Pair<>(DATA, create(StyleConstants.Foreground, dataColor)));
        tmp.add(new Pair<>(CHECKSUM, create(StyleConstants.Foreground, checksumForegroundColor,
                StyleConstants.Background, checksumBackgroundColor)));
        tmp.add(new Pair<>(EOF, create(StyleConstants.Foreground, eofForegroundColor,
                StyleConstants.Background, eofBackgroundColor)));
        return tmp;
    }

    private static List<Pair<Pattern, AttributeSet>> createAsmSourceFileTheme(Color commentColor,
                                                                              Color stringColor,
                                                                              Color labelColor,
                                                                              Color mnemonic) {
        List<Pair<Pattern, AttributeSet>> tmp = new LinkedList<>();
        return tmp;
    }

    private static List<Pair<Pattern, AttributeSet>> createPropertiesTheme(Color comment,
                                                                           Color key,
                                                                           Color value,
                                                                           Color endEscape,
                                                                           Color unicode) {
        LinkedList<Pair<Pattern, AttributeSet>> tmp = new LinkedList<>();
        tmp.add(new Pair<>(PROP_VALUE, create(StyleConstants.Foreground, value)));
        tmp.add(new Pair<>(PROP_KEY, create(StyleConstants.Foreground, key)));
        tmp.add(new Pair<>(PROP_UNICODE, create(StyleConstants.Foreground, unicode)));
        tmp.add(new Pair<>(PROP_END_ESCAPE, create(StyleConstants.Foreground, endEscape)));
        tmp.add(new Pair<>(PROP_COMMENT, create(StyleConstants.Foreground, comment)));
        return tmp;
    }

    public abstract SyntaxTheme getSyntaxTheme();

    static AttributeSet create(Object... nameValuePairs) {
        MutableAttributeSet result = StyleContext.getDefaultStyleContext().addStyle(null, null);
        for (int i = 0; i < nameValuePairs.length-1; ++i) {
            result.addAttribute(nameValuePairs[i], nameValuePairs[++i]);
        }
        return result;
    }
}
