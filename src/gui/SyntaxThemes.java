package gui;

import misc.Pair;
import misc.Settings;

import javax.swing.text.AttributeSet;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.bind.*;


public enum SyntaxThemes {
    INSTANCE;

    private final static String SYNTAX_THEMES_SETTING_NAME = "gui.syntax-theme-folder";
    private final static String SYNTAX_THEMES_SETTING_DEFAULT = ".";
    private final static String SYNTAX_THEME_NAME_SETTING_NAME = "gui.syntax-theme-name";
    private final static String SYNTAX_THEME_NAME_SETTING_DEFAULT = "DEFAULT";
    private static final char FILE_EXTENSION_SEPARATOR = '.';

    private final Map<String, SyntaxTheme> themes;
    private String currentThemeName;

    SyntaxThemes() throws IllegalArgumentException {
        // no need for a static block, since INSTANCE is static
        Settings.INSTANCE.setDefault(SYNTAX_THEMES_SETTING_NAME, SYNTAX_THEMES_SETTING_DEFAULT);
        Settings.INSTANCE.setDefault(SYNTAX_THEME_NAME_SETTING_NAME, SYNTAX_THEME_NAME_SETTING_DEFAULT);

        this.themes = new HashMap<>();
        try {
            final String pathSetting = Settings.INSTANCE.getProperty(SYNTAX_THEMES_SETTING_NAME);
            final Path themePath = Paths.get(".".equals(pathSetting) ? System.getProperty("user.dir") : pathSetting);
            final JAXBContext jaxbContext = JAXBContext.newInstance(SyntaxTheme.class);
            final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(themePath)) {
                for (Path path : stream) {
                    if (Files.isReadable(path) && Files.isRegularFile(path) && path.toString().endsWith(".xml"))
                        try (Reader in = Files.newBufferedReader(path)){
                            final SyntaxTheme tmp = (SyntaxTheme)jaxbUnmarshaller.unmarshal(in);
                            final String tmpName = getThemeName(themePath);
                            if (tmpName.trim().isEmpty())
                                throw new IllegalArgumentException("theme name must not be null or empty");
                            if (tmp == null) throw new IllegalArgumentException("theme must not be null");
                            themes.put(getThemeName(path), tmp);
                        } catch (IOException|IndexOutOfBoundsException|ClassCastException|JAXBException e) {
                            System.err.println("An exception occurred while parsing the theme '" + themePath + "':");
                            e.printStackTrace();
                        }
                }
            }
        } catch (InvalidPathException|IOException|JAXBException e) {
            System.err.println("An exception occurred while reading the syntax themes:");
            e.printStackTrace();
        }

        if (!this.themes.containsKey("DEFAULT")) this.themes.put("DEFAULT", makeFallbackTheme());

        this.currentThemeName = Settings.INSTANCE.getProperty(SYNTAX_THEME_NAME_SETTING_NAME);
    }

    public SyntaxTheme getCurrentTheme() {
        if (SYNTAX_THEME_NAME_SETTING_DEFAULT.equals(this.currentThemeName)
                || !this.themes.containsKey(this.currentThemeName)) return makeFallbackTheme();
        SyntaxTheme result = this.themes.get(this.currentThemeName);
        if (result == null || !result.isValid()) return makeFallbackTheme();
        return result;
    }

    public void setCurrentTheme(String name) throws IllegalArgumentException {
        if (!this.themes.containsKey(name)) throw new IllegalArgumentException(name + " is not a valid theme name");
        this.currentThemeName = name;
    }

    public Set<String> getAvailableThemes() {
        return Collections.unmodifiableSet(this.themes.keySet());
    }

    private static String getThemeName(Path p) {
        final String tmpName = p.getFileName().toString();
        if (tmpName.indexOf(FILE_EXTENSION_SEPARATOR) < 0) return tmpName;
        return tmpName.substring(0, tmpName.lastIndexOf(FILE_EXTENSION_SEPARATOR));
    }

    private static SyntaxTheme makeFallbackTheme() {
        return FallbackSyntaxThemes.DEFAULT.getSyntaxTheme();
    }

    private void storeSyntaxTheme(Path path) throws JAXBException, IOException {
        //try (Writer out = Files.newBufferedWriter(path)) {
        //    JAXB.marshal(this.getCurrentTheme(), out);
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}
        JAXBContext jaxbContext = JAXBContext.newInstance(SyntaxTheme.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        try (BufferedWriter bw = Files.newBufferedWriter(path)) {
            jaxbMarshaller.marshal(this.getCurrentTheme(), bw);
        }
    }

    public static void main(String[] imaTest) {
        try {
            SyntaxThemes.INSTANCE.storeSyntaxTheme(Paths.get("/tmp/test.path"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/**
 * @author 5hir0kur0
 */
enum FallbackSyntaxThemes {
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

        @Override public SyntaxTheme getSyntaxTheme() {
            if (null == DEFAULT.syntaxTheme) {
                final LinkedHashMap<String, Style> tmpMap = new LinkedHashMap<>();
                final Style tmpAsmStyle = new Style(FallbackSyntaxThemes.createAsmSourceFileTheme(
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
                        DEFAULT.base0E, // directive include path file
                        DEFAULT.base03, // directive include path brackets
                        DEFAULT.base04, // parentheses content
                        DEFAULT.base0C, // parentheses
                        DEFAULT.base08  // errors
                ));
                tmpMap.put("asm", tmpAsmStyle);
                final Style tmpHexStyle = new Style(FallbackSyntaxThemes.createIntelHexTheme(
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
                ));
                tmpMap.put("hex", tmpHexStyle);
                final Style tmpPropertiesStyle = new Style(FallbackSyntaxThemes.createPropertiesTheme(
                        DEFAULT.base04, // comment
                        DEFAULT.base08, // key
                        DEFAULT.base0B, // value
                        DEFAULT.base0D, // end escape
                        DEFAULT.base0F  // unicode
                ));
                tmpMap.put("properties", tmpPropertiesStyle);
                final Color tmpLineNumberBackground = DEFAULT.base06;
                final Color tmpLineNumberForeground = DEFAULT.base04;
                final Color tmpCodeBackground = DEFAULT.base07;
                final Color tmpCodeForeground = DEFAULT.base02;
                final Color tmpCaretColor = DEFAULT.base02;
                final Color tmpSelectionColor = DEFAULT.base0D;
                final Color tmpSelectedTextColor = DEFAULT.base06;
                final Color tmpErrorColor = DEFAULT.base08;
                final Color tmpWarningColor = DEFAULT.base09;
                final Color tmpInformationColor = DEFAULT.base03;

                DEFAULT.syntaxTheme = new SyntaxTheme(tmpMap, tmpLineNumberBackground, tmpLineNumberForeground,
                        tmpCodeBackground, tmpCodeForeground, tmpCaretColor, tmpSelectionColor, tmpSelectedTextColor,
                        tmpErrorColor, tmpWarningColor, tmpInformationColor);
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
    private static final Pattern HEX_EOF = Pattern.compile("^:(00000001)ff\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_START_CODE = Pattern.compile("^(:)");
    private static final Pattern HEX_DATA_BYTE_COUNT = Pattern.compile("^:([\\da-f]{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_ADDRESS = Pattern.compile("^:..([\\da-f]{4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_VALID_RECORD_TYPE = Pattern.compile("^:.{6}(0[0-5])");
    private static final Pattern HEX_DATA =Pattern.compile("^:.{8}((?:[\\da-f]{2}(?!$\\s*))*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_CHECKSUM = Pattern.compile("^:.{8}(?:..)*?([\\da-f]{2})\\s*$",
            Pattern.CASE_INSENSITIVE);
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
    private static final String ASM_LABEL_STRING = "(?:[\\w&&[\\D]]\\w*\\s*:\\s*)+";
    private static final Pattern ASM_LABEL = Pattern.compile("^\\s*("+ASM_LABEL_STRING+")");
    private static final String ASM_MNEMONIC_STRING =
            "(?:[al]?call|[als]?jmp|mov[cx]?|reti?|swap|xchd?|addc?|subb|mul|div|da|setb|clr|cpl|anl|[ox]rl|rlc?|rrc?|"
                    + "nop|push|pop|inc|dec|cjne|djnz|jn?[bcz]|jbc?)\\b";
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
    private static final String ASM_SYMBOL_STRING =
            "(?:(?:(?<=\\w)\\.|(?<=[\\w,])\\s*[-+#/]?)\\b([\\w&&[\\D]]\\w*)\\b)";
    private static final Pattern ASM_SYMBOL = Pattern.compile(ASM_SYMBOL_STRING);

    private static final Pattern ASM_DOT_OPERATOR = Pattern.compile("\\w(\\.)\\w");
    private static final Pattern ASM_TYPE_PREFIX  = Pattern.compile("(?<=[\\w,])\\s*+([/#+-])[\\w\"'\\(]+");

    private static final Pattern ASM_DIRECTIVE_LINE =
            Pattern.compile("^(\\s*(?:[\\$#\\.].*?|[\\$#\\.]?(?:if|elif|else|endif|regex|end|file|line|include|org|"
                    + "end|d[bws])\\s+.*?|\\S*?(?<!"+ASM_MNEMONIC_STRING+"|;)\\s+(?:equ|set|bit|code|[ix]?data)\\s+" +
                    ".*?))\\s*(?:(?<!\\\\);|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_DIRECTIVE = Pattern.compile("^\\s*(?:([\\$#\\.]?\\s*(?:org|end|d[bws]|if|elif|"
            + "else|endif|regex|end|file|line|include|equ|set|bit|code|[ix]?data))\\b" +
                    "|\\S*?(?<!"+ASM_MNEMONIC_STRING+")\\s+(equ|set|bit|code|[ix]?data))", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_DIRECTIVE_SYMBOL = Pattern.compile(
            "^\\s*\\b([\\w&&[\\D]]\\w*)\\b\\s*(?:equ|set|bit|code|[ix]?data)\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_DIRECTIVE_PATH_INCLUDE_BRACKETS =
            Pattern.compile("^\\s*[\\$#\\.]?\\s*?include\\s+(<).*?(>)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_DIRECTIVE_PATH_INCLUDE_FILE =
            Pattern.compile("^\\s*[\\$#\\.]?\\s*?include\\s+?<(.*?)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_PARENTHESES = Pattern.compile("(\\().*(\\)),|(\\().*(\\))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ASM_PARENTHESES_CONTENT = Pattern.compile("\\((.*)\\),|\\((.*)\\)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ASM_ERRORS = Pattern.compile("(\\S+)");

    FallbackSyntaxThemes(String name,
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

    /** creates the light intel hex theme */
    private static List<Pair<Pattern, DummyAttributeSet>> createIntelHexTheme(
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
        final List<Pair<Pattern, DummyAttributeSet>> tmp = new LinkedList<>();
        tmp.add(new Pair<>(HEX_IRRELEVANT, create(null, null, irrelevantColor, null, null, null)));
        tmp.add(new Pair<>(HEX_ERROR, create(null, null, errorColor, null, true, null)));
        tmp.add(new Pair<>(HEX_START_CODE, create(null, null, startCodeColor, null, false, null)));
        tmp.add(new Pair<>(HEX_DATA_BYTE_COUNT, create(null, null, dataByteCountColor, null, false, null)));
        tmp.add(new Pair<>(HEX_ADDRESS, create(null, null, addressColor, null, false, null)));
        tmp.add(new Pair<>(HEX_VALID_RECORD_TYPE, create(null, null, validRecordTypeColor, null, false, null)));
        tmp.add(new Pair<>(HEX_DATA, create(null, null, dataColor, null, false, null)));
        tmp.add(new Pair<>(HEX_CHECKSUM, create(checksumBackgroundColor, null, checksumForegroundColor, null, false,
                null)));
        tmp.add(new Pair<>(HEX_EOF, create(eofBackgroundColor, null, eofForegroundColor, null, false, null)));
        return tmp;
    }

    private static List<Pair<Pattern, DummyAttributeSet>> createAsmSourceFileTheme(
            Color todoForegroundColor,
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
            Color includePathFileColor,
            Color includePathBracketsColor,
            Color parenthesesContentColor,
            Color parenthesesColor,
            Color errorColor
    ) {
        final List<Pair<Pattern, DummyAttributeSet>> tmp = new LinkedList<>();
        tmp.add(new Pair<>(ASM_ERRORS, create(null, null, errorColor, null, true, null)));
        tmp.add(new Pair<>(ASM_PARENTHESES_CONTENT, create(null, null, parenthesesContentColor, null, false, null)));
        tmp.add(new Pair<>(ASM_PARENTHESES, create(null, null, parenthesesColor, null, false, null)));
        tmp.add(new Pair<>(ASM_DOT_OPERATOR, create(null, null, dotColor, null, false, null)));
        tmp.add(new Pair<>(ASM_SYMBOL, create(null, null, symbolColor, null, false, null)));
        tmp.add(new Pair<>(ASM_TYPE_PREFIX, create(null, null, typePrefixColor, false, null, null)));
        tmp.add(new Pair<>(ASM_SYMBOL_RESERVED_ERROR, create(null, null, symbolReservedErrorColor, null, true, null)));
        tmp.add(new Pair<>(ASM_SYMBOL_RESERVED, create(null, null, symbolReservedColor, null, false, null)));
        tmp.add(new Pair<>(ASM_SYMBOL_RESERVED_INDIRECT, create(null, null, symbolReservedIndirectColor,
                null, false, null)));
        tmp.add(new Pair<>(ASM_DIRECTIVE_LINE, create(directiveBackgroundColor, null, null, null, null, null)));
                // Do not disable error strike through because errors still could occur in the directive
        tmp.add(new Pair<>(ASM_DIRECTIVE, create(null, null, directiveColor, null, false, null)));
        tmp.add(new Pair<>(ASM_DIRECTIVE_PATH_INCLUDE_FILE, create(null, null, includePathFileColor, null,
                false, null)));
        tmp.add(new Pair<>(ASM_DIRECTIVE_PATH_INCLUDE_BRACKETS, create(null, null, includePathBracketsColor,
                null, false, null)));
        tmp.add(new Pair<>(ASM_DIRECTIVE_SYMBOL, create(null, null, symbolColor, null, false, null)));
        tmp.add(new Pair<>(ASM_NUMBER_ERROR, create(null, null, numberErrorColor, true, null, null)));
        tmp.add(new Pair<>(ASM_NUMBER_HEX, create(null, null, numberHexadecimalColor, false, false, null)));
        tmp.add(new Pair<>(ASM_NUMBER_OCTAL, create(null, null, numberOctalColor, false, false, null)));
        tmp.add(new Pair<>(ASM_NUMBER_BINARY, create(null, null, numberBinaryColor, false, false, null)));
        tmp.add(new Pair<>(ASM_NUMBER_DECIMAL, create(null, null, numberDecimalColor, false, false, null)));
        tmp.add(new Pair<>(ASM_NUMBER_RADIX, create(null, null, numberRadixColor, false, false, null)));
        tmp.add(new Pair<>(ASM_COMMAS, create(null, null, commaColor, null, false, null)));
        tmp.add(new Pair<>(ASM_MNEMONIC, create(null, null, mnemonicColor, null, false, null)));
        tmp.add(new Pair<>(ASM_LABEL,create(null, null, labelColor, null, false, null)));
        tmp.add(new Pair<>(ASM_STRING, create(null, null, stringColor, null, false, null)));
        tmp.add(new Pair<>(ASM_COMMENT, create(null, null, commentColor, null, false, null)));
        tmp.add(new Pair<>(ASM_TODO, create(todoBackgroundColor, null, todoForegroundColor, true, false, null)));
        return tmp;
    }

    private static List<Pair<Pattern, DummyAttributeSet>> createPropertiesTheme(Color comment,
                                                                           Color key,
                                                                           Color value,
                                                                           Color endEscape,
                                                                           Color unicode) {
        final LinkedList<Pair<Pattern, DummyAttributeSet>> tmp = new LinkedList<>();
        tmp.add(new Pair<>(PROP_VALUE, create(null, null, value, null, null, null)));
        tmp.add(new Pair<>(PROP_KEY, create(null, null, key, null, null, null)));
        tmp.add(new Pair<>(PROP_UNICODE, create(null, null, unicode, null, null, null)));
        tmp.add(new Pair<>(PROP_END_ESCAPE, create(null, null, endEscape, null, null, null)));
        tmp.add(new Pair<>(PROP_COMMENT, create(null, null, comment, null, null, null)));
        return tmp;
    }

    public abstract SyntaxTheme getSyntaxTheme();

    static DummyAttributeSet create(Color background, Boolean bold, Color foreground, Boolean italic,
                               Boolean strikeThrough, Boolean underline) {
        return new DummyAttributeSet(background, bold, foreground, italic, strikeThrough, underline);
    }
}
