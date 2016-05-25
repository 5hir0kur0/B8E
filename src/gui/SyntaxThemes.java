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
 * @author Gordian
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
        List<Pair<Pattern, AttributeSet>> asmTheme = null;

        @Override public SyntaxTheme getSyntaxTheme() {
            if (null == DEFAULT.syntaxTheme) {
                DEFAULT.syntaxTheme = new SyntaxTheme() {
                    @Override
                    public List<Pair<Pattern, AttributeSet>> getStyleForType(String type) {
                        switch (Objects.requireNonNull(type).toLowerCase()) {
                            case "asm":
                            case "asm51":
                                if (null == asmTheme) asmTheme = SyntaxThemes.createAsmSourceFileTheme(
                                        DEFAULT.base0A, // to do foreground
                                        DEFAULT.base06, // to do background
                                        DEFAULT.base05, // comments
                                        DEFAULT.base0D, // strings
                                        DEFAULT.base0A, // labels
                                        DEFAULT.base09, // mnemonics
                                        DEFAULT.base09, // commas
                                        DEFAULT.base03, // number radix (pre- or suffixes)
                                        DEFAULT.base0E, // decimal numbers
                                        DEFAULT.base0E, // binary numbers
                                        DEFAULT.base0E, // octal numbers
                                        DEFAULT.base0E, // hexadecimal numbers
                                        DEFAULT.base08, // number error
                                        DEFAULT.base09, // symbol indirect reserved
                                        DEFAULT.base0A, // symbol reserved
                                        DEFAULT.base08, // symbol reserved error
                                        DEFAULT.base0B, // type prefix
                                        DEFAULT.base0C, // symbols (in general)
                                        DEFAULT.base09, // dot operator
                                        DEFAULT.base05, // directive background
                                        DEFAULT.base09, // directive color
                                        DEFAULT.base08  // errors
                                );
                                return asmTheme;
                            case "hex":
                                if (null == hexTheme) hexTheme = SyntaxThemes.createIntelHexTheme(
                                        DEFAULT.base04, // irrelevant
                                        DEFAULT.base08, // error
                                        DEFAULT.base04, // start code
                                        DEFAULT.base09, // data byte
                                        DEFAULT.base04, // address
                                        DEFAULT.base08, // valid record type
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
    private static final Pattern HEX_EOF = Pattern.compile("^:(00000001)FF\\s*$");
    private static final Pattern HEX_START_CODE = Pattern.compile("^(:)");
    private static final Pattern HEX_DATA_BYTE_COUNT = Pattern.compile("^:([\\da-f]{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_ADDRESS = Pattern.compile("^:..([\\da-f]{4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_VALID_RECORD_TYPE = Pattern.compile("^:.{6}(0[0-5])");
    private static final Pattern HEX_DATA =Pattern.compile("^:.{8}((?:[\\da-f]{2}(?!$\\s*))*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_CHECKSUM = Pattern.compile("^:.{8}(?:..)*?([\\da-f]{2})\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_IRRELEVANT = Pattern.compile("^(.*?)\\s*$");
    private static final Pattern HEX_ERROR = Pattern.compile("^:(.*?)\\s*$");

    //properties file patterns
    private static final Pattern PROP_COMMENT = Pattern.compile("([#!].*)$");
    private static final Pattern PROP_KEY = Pattern.compile("^\\s*((?:[\\w\\.\\-]|\\\\\\s)+)\\s*[=:]\\s*");
    private static final Pattern PROP_UNICODE =
            Pattern.compile("^\\s*(?:[\\w\\.\\-]|\\\\\\s)+\\s*[=:]\\s*(\\\\u[\\da-fA-F]{4})");
    private static final Pattern PROP_VALUE =
            Pattern.compile("^\\s*(?:[\\w\\.\\-]|\\\\\\s)+\\s*[=:]\\s*([^\\s].*)$|^\\s*([^\\s].*)\\s*$");
    private static final Pattern PROP_END_ESCAPE = Pattern.compile("(\\\\)$");

    //asm source file patterns
    private static final Pattern ASM_COMMENT = Pattern.compile("\\s*(;.*)$");
    private static final Pattern ASM_TODO = Pattern.compile("\\s*;.*?\\b(TODO\\b.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_STRING = Pattern.compile("((?<!\\\\)\".*?(?<!\\\\)\"|(?<!\\\\)'.*?(?<!\\\\)')");
    private static final String ASM_LABEL_STRING = "(?:[\\w&&[\\D]]\\w*:)";
    private static final Pattern ASM_LABEL = Pattern.compile("^\\s*("+ASM_LABEL_STRING+")");
    private static final String ASM_MNEMONIC_STRING =
            "(?:[al]?call|[als]?jmp|mov[cx]?|reti?|swap|xchd?|addc?|subb|mul|div|da|setb|clr|cpl|anl|[ox]rl|rlc?|rrc?|" +
            "nop|push|pop|inc|dec|cjne|djnz|jn?[bcz]|jbc?)\\b";
    private static final Pattern ASM_MNEMONIC =
            Pattern.compile("^(?:\\s*"+ASM_LABEL_STRING+")?\\s*("+ ASM_MNEMONIC_STRING +")", Pattern.CASE_INSENSITIVE);
    private static final String ASM_MNEMONIC_PREFIX = "^(?:\\s*"+ASM_LABEL_STRING+")?\\s*"+ ASM_MNEMONIC_STRING;
    private static final Pattern ASM_COMMAS =
            Pattern.compile(ASM_MNEMONIC_PREFIX+"\\s+[^\\s]*?\\s*(,)\\s*[^\\s,]+(?:(,)\\s*[^\\s,]+)?",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern ASM_NUMBER_ERROR =
            Pattern.compile("(?:(?<=\\w)\\.|(?<=[\\w,])\\s*[-+#/]?)\\b(\\d+\\w*?)[boqdh]?\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_NUMBER_RADIX =
            Pattern.compile("(?:(?<=\\w)\\.|(?<=[\\w,])\\s*[-+#/]?)\\b(?:(0x)\\w*?|\\d+\\w*?([boqdh])?)\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_NUMBER_BINARY =
            Pattern.compile("(?:(?<=\\w)\\.|(?<=[\\w,])\\s*[-+#/]?)\\b([01]+b)\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_NUMBER_OCTAL =
            Pattern.compile("(?:(?<=\\w)\\.|(?<=[\\w,])\\s*[-+#/]?)\\b([0-7]+[oq])\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_NUMBER_DECIMAL =
            Pattern.compile("(?:(?<=\\w)\\.|(?<=[\\w,])\\s*[-+#/]?)\\b(\\d+d?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_NUMBER_HEX =
            Pattern.compile("(?:(?<=\\w)\\.|(?<=[\\w,])\\s*[-+#/]?)\\b(0x[\\da-f]*|\\d+[\\da-f]*h)\\b",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern ASM_SYMBOL_RESERVED =
            Pattern.compile("(?<=[\\w,])\\s*(?:/?\\b(a(?:cc)?|c)|\\b(ab|dptr|r[0-7]))\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_SYMBOL_RESERVED_ERROR =
            Pattern.compile("(?<=[\\w,])\\s*([#+-]\\b(?:a(?:cc)?|c)|(?:[/#+-]\\bab|dptr|r[0-7]))\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_SYMBOL_RESERVED_INDIRECT =
            Pattern.compile("(?<=[\\w,])\\s*(@(?:a\\s*\\+\\s*dptr|a\\s*\\+\\s*pc|dptr|r[01]))\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final String ASM_SYMBOL_STRING = "(?:(?:(?<=\\w)\\.|(?<=[\\w,])\\s*[-+#/]?)\\b([\\w&&[\\D]]\\w*)\\b)";
    private static final Pattern ASM_SYMBOL = Pattern.compile(ASM_SYMBOL_STRING);

    private static final Pattern ASM_DOT_OPERATOR = Pattern.compile("\\w(\\.)\\w");
    private static final Pattern ASM_TYPE_PREFIX  = Pattern.compile("(?<=[\\w,])\\s*+([/#+-])[\\w\"'\\(]+");

    private static final Pattern ASM_DIRECTIVE_LINE =
            Pattern.compile("^(\\s*(?:[$#\\.].*?|[$#\\.]?(?:if|elif|else|regex|end|file|line|org|end|d[bws])\\s+.*?|" +
                    "\\S*?(?<!"+ASM_MNEMONIC_STRING+")\\s+(?:equ|set|bit|code|[ix]?data)\\s+.*?))\\s*(?:(?<!\\\\);|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_DIRECTIVE = Pattern.compile(
            "^\\s*(?:([$#\\.]?org|end|d[bws]|if|elif|else|regex|end|file|line|equ|set|bit|code|[ix]?data)" +
                    "|\\S*?(?<!"+ASM_MNEMONIC_STRING+")\\s+(equ|set|bit|code|[ix]?data))", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_DIRECTIVE_SYMBOL = Pattern.compile(
            "^\\s*\\b([\\w&&[\\D]]\\w*)\\b\\s*(?:equ|set|bit|code|[ix]?data)\\s*", Pattern.CASE_INSENSITIVE);

    private static final Pattern ASM_ERRORS = Pattern.compile("(\\S+)");

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
    private static List<Pair<Pattern, AttributeSet>> createIntelHexTheme(
                                                                  Color irrelevantColor,
                                                                  Color errorColor,
                                                                  Color startCodeColor,
                                                                  Color dataByteCountColor,
                                                                  Color addressColor,
                                                                  Color validRecordTypeColor,
                                                                  Color dataColor,
                                                                  Color checksumForegroundColor,
                                                                  Color checksumBackgroundColor,
                                                                  Color eofForegroundColor,
                                                                  Color eofBackgroundColor
                                                                 ) {
        List<Pair<Pattern, AttributeSet>> tmp = new LinkedList<>();
        tmp.add(new Pair<>(HEX_IRRELEVANT, create(StyleConstants.Foreground, irrelevantColor)));
        tmp.add(new Pair<>(HEX_ERROR, create(StyleConstants.Foreground, errorColor,
                StyleConstants.StrikeThrough, true)));
        tmp.add(new Pair<>(HEX_START_CODE, create(StyleConstants.Foreground, startCodeColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(HEX_DATA_BYTE_COUNT, create(StyleConstants.Foreground, dataByteCountColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(HEX_ADDRESS, create(StyleConstants.Foreground, addressColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(HEX_VALID_RECORD_TYPE, create(StyleConstants.Foreground, validRecordTypeColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(HEX_DATA, create(StyleConstants.Foreground, dataColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(HEX_CHECKSUM, create(StyleConstants.Foreground, checksumForegroundColor,
                StyleConstants.Background, checksumBackgroundColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(HEX_EOF, create(StyleConstants.Foreground, eofForegroundColor,
                StyleConstants.Background, eofBackgroundColor,
                StyleConstants.StrikeThrough, false)));
        return tmp;
    }

    private static List<Pair<Pattern, AttributeSet>> createAsmSourceFileTheme(Color todoForegroundColor,
                                                                              Color todoBackgroundColor,
                                                                              Color commentColor,
                                                                              Color stringColor,
                                                                              Color labelColor,
                                                                              Color mnemonicColor,
                                                                              Color commaColor,
                                                                              Color numberRadixColor,
                                                                              Color numberDecimalColor,
                                                                              Color numberBinaryColor,
                                                                              Color numberOctalColor,
                                                                              Color numberHexadecimalColor,
                                                                              Color numberErrorColor,
                                                                              Color symbolReservedIndirectColor,
                                                                              Color symbolReservedColor,
                                                                              Color symbolReservedErrorColor,
                                                                              Color typePrefixColor,
                                                                              Color symbolColor,
                                                                              Color dotColor,
                                                                              Color directiveBackgroundColor,
                                                                              Color directiveColor,
                                                                              Color errorColor
                                                                             ) {
        List<Pair<Pattern, AttributeSet>> tmp = new LinkedList<>();
        tmp.add(new Pair<>(ASM_ERRORS, create(StyleConstants.Foreground, errorColor,
                StyleConstants.StrikeThrough, true)));
        tmp.add(new Pair<>(ASM_DOT_OPERATOR, create(StyleConstants.Foreground, dotColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(ASM_SYMBOL, create(StyleConstants.Foreground, symbolColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(ASM_TYPE_PREFIX, create(StyleConstants.Foreground, typePrefixColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(ASM_SYMBOL_RESERVED_ERROR, create(StyleConstants.Foreground, symbolReservedErrorColor,
                StyleConstants.StrikeThrough, true)));
        tmp.add(new Pair<>(ASM_SYMBOL_RESERVED, create(StyleConstants.Foreground, symbolReservedColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(ASM_SYMBOL_RESERVED_INDIRECT, create(StyleConstants.Foreground, symbolReservedIndirectColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(ASM_DIRECTIVE_LINE, create(StyleConstants.Background, directiveBackgroundColor)));
                // Do not disable error strike through because errors still could occur in the directive
        tmp.add(new Pair<>(ASM_DIRECTIVE, create(StyleConstants.Foreground, directiveColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(ASM_DIRECTIVE_SYMBOL, create(StyleConstants.Foreground, symbolColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(ASM_NUMBER_ERROR, create(StyleConstants.Foreground, numberErrorColor,
                StyleConstants.Italic, true)));
        tmp.add(new Pair<>(ASM_NUMBER_HEX, create(StyleConstants.Foreground, numberHexadecimalColor,
                StyleConstants.StrikeThrough, false, StyleConstants.Italic, false)));
        tmp.add(new Pair<>(ASM_NUMBER_OCTAL, create(StyleConstants.Foreground, numberOctalColor,
                StyleConstants.StrikeThrough, false, StyleConstants.Italic, false)));
        tmp.add(new Pair<>(ASM_NUMBER_BINARY, create(StyleConstants.Foreground, numberBinaryColor,
                StyleConstants.StrikeThrough, false, StyleConstants.Italic, false)));
        tmp.add(new Pair<>(ASM_NUMBER_DECIMAL, create(StyleConstants.Foreground, numberDecimalColor,
                StyleConstants.StrikeThrough, false, StyleConstants.Italic, false)));
        tmp.add(new Pair<>(ASM_NUMBER_RADIX, create(StyleConstants.Foreground, numberRadixColor,
                StyleConstants.StrikeThrough, false, StyleConstants.Italic, false)));
        tmp.add(new Pair<>(ASM_COMMAS, create(StyleConstants.Foreground, commaColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(ASM_MNEMONIC, create(StyleConstants.Foreground, mnemonicColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(ASM_LABEL,create(StyleConstants.Foreground, labelColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(ASM_STRING, create(StyleConstants.Foreground, stringColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(ASM_COMMENT, create(StyleConstants.Foreground, commentColor,
                StyleConstants.StrikeThrough, false)));
        tmp.add(new Pair<>(ASM_TODO, create( StyleConstants.StrikeThrough, false, StyleConstants.Italic, true,
                StyleConstants.Foreground, todoForegroundColor, StyleConstants.Background, todoBackgroundColor)));
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
