package assembler.util;

import assembler.arc8051.MC8051Library;
import misc.Settings;

import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * A temporary class to store the assembler settings.
 * @author Noxgrim
 */
public class AssemblerSettings {

    /**
     * The assumed base if no prefix or postfix is given.<br>
     * This setting has a higher weight than the user provided
     * suffixes and postfixes. Because of that <code>0b1010</code>
     * will always be recognised as a hexadecimal number instead
     * of a binary number if the value of this setting is set to
     * <code>16</code>.<br>
     * A side effect of this behavior is that every valid number
     * will be recognised as a hexadecimal number if <code>16</code>
     * is used as a value and the notation is set to prefix.
     * <br>
     * Valid values: 2, 8, 10, 16, "auto"<br>
     * Defaults to: 10
     */
    public static final String RADIX = "assembler.parsing.default-number-radix";
    /**
     * The used notations for numbers.<br>
     * Can be either "prefix" (0, 0x or 0b) or
     * "postfix"/"suffix" (b, o, q, d or h).<br>
     * <br>
     * Valid values: "prefix", "postfix", "suffix"<br>
     * Defaults to: "postfix"
     */
    public static final String NOTATION = "assembler.parsing.notation";
    /**
     * The behaviour if some "obvious" operands are encountered.<br>
     * Obvious operands are operands that aren't needed to specify the
     * mnemonic-operand combination.<br>
     *     E.g. in <code>add a, R1</code> does not need the <code>a</code>
     *     to be recognised so <code>add R1</code> is valid as well.<br>
     * <br>
     * Valid values: "error", "warn", "ignore"<br>
     * Defaults to: "error"
     */
    public static final String OBVIOUS_OPERANDS = "assembler.errors.obvious-operands";
    /**
     * The behavior if some "unnecessary" operands.<br>
     * Unnecessary operands are operands that aren't needed for mnemonic
     * encoding e.g operands for the <code>NOP</code> mnemonic.<br>
     * Valid values: "error", "warn", "ignore"<br>
     * Defaults to: "error"
     */
    public static final String UNNECESSARY_OPERANDS = "assembler.errors.unnecessary-operands";
    /**
     * The extension of assembly source files.<br>
     * <br>
     * Valid values: All values that start with a dot '.' and are then followed
     *               by word characters.<br>
     * Defaults to: ".asm"
     */
    public static final String SOURCE_FILE_EXTENSION = "assembler.output.file-extensions.asm";

    /**
     * The extension of Intel-HEX files.<br>
     * <br>
     * Valid values: All values that start with a dot '.' and are then followed
     *               by word characters.<br>
     * Defaults to: ".hex"
     */
    public static final String HEX_FILE_EXTENSION = "assembler.output.file-extensions.hex";

    /**
     * The behavior if non comment or white-space line are found after the use of an
     * end directive.<br>
     * Normally code isn't allowed the use of an end directive and will result in a
     * Problem. Also any code after an end directive will be ignored by the assembler.<br>
     * <br>
     * Valid values: "error", "warn", "ignore"<br>
     * Defaults to: "warn"
     */
    public static final String END_CODE_AFTER = "assembler.directives.end.code-after";

    /**
     * The behaviour if multiple problem modifiers that react to the same circumstance
     * (not matching or matching) are defined.<br>
     * This setting defaults to "error" because a circumstance would generate two
     * different ProblemTypes what is hardly intended behaviour for the most time.<br>
     * <br>
     * Valid values: "error", "warn", "ignore"<br>
     * Defaults to: "error"
     */
    public static final String MULTIPLE_SAME_MATCH_CASE = "assembler.errors.regex.same-match-case";

    /**
     * The behaviour if a given <code>Regex</code> encounters segments that are not used.
     * All segments after the flags-segment count as unused and are ignored.<br>
     * <br>
     * Valid values: "error", "warn", "ignore"<br>
     * Defaults to: "warn"
     */
    public static final String UNNECESSARY_SEGMENTS = "assembler.errors.regex.unnecessary-segments";

    /**
     * The default flags of a <code>Regex</code>.<br>
     * This setting is realized by always inserting the default flags at the start of any
     * flags-segment. The preset flag then can be "deactivated" on a specific regex by
     * using the corresponding negated flag on it. Example: <code>'I'</code> can be used
     * to negate the effect of a preset <code>'i'</code>.<br>
     * <br>
     * Valid values: <i>all Strings</i><br>
     * Defaults to: ""
     */
    public static final String DEFAULT_FLAGS = "assembler.directives.regex.default-flags";


    /**
     * The behavior if no end directive was found.<br>
     * <br>
     * Valid values: "error", "warn", "ignore"<br>
     * Defaults to: "warn"
     */
    public static final String END_MISSING = "assembler.errors.missing-end-directive";

    /**
     * The default behaviour if an address offset operator is used.<br>
     * <br>
     * Valid values: "error", "warn", "ignore"<br>
     * Defaults to: "warn"
     */
    public static final String ADDRESS_OFFSET = "assembler.errors.address-offset-used";

    /**
     * Defines how many files can be included inside included files before
     * resulting in a Problem.<br>
     * If the value is <code>0</code> including within a included file is
     * not possible!<br>
     * <br>
     * Valid values: <i>all integer non negative values.</i><br>
     * Defaults to: 256
     */
    public static final String INCLUDE_DEPTH = "assembler.directives.include.max-depth";

    /**
     * The path that is used to search for path includes (in angle brackets
     * <code>&lt;&gt;</code>).<br>
     * Multiple paths can be separated with a semicolon (<code>;</code>)<br>
     * <br>
     * Valid values:
     * Defaults to: "include"
     */
    public static final String INCLUDE_PATH = "assembler.directives.include.path";

    /**
     * Determines whether the preprocessor searches recursively for a given file
     * in the given path.<br>
     * Valid values: true, false<br>
     * Defaults to: false
     */
    public static final String INCLUDE_RECURSIVE_SEARCH = "assembler.directives.include.path.recursive-search";

    /**
     * If this value is set to <code>true</code> the default include file <code>'default.asm'</code>
     * will be included automatically on each run of the assembler.<br>
     * Valid values: true, false<br>
     * Defaults to: true
     */
    public static final String INCLUDE_DEFAULT_FILE = "assembler.include-default-file";

    public static final String OUTPUT_HEX = "assembler.output.hex";
    public static final String OUTPUT_HEX_EXTENSION = "assembler.output.hex.extension";
    public static final String OUTPUT_HEX_BUFFER_LENGTH = "assembler.output.hex.buffer-length";
    public static final String OUTPUT_HEX_WRAP = "assembler.output.hex.instruction-wrap";

    public static final String OUTPUT_BIN = "assembler.output.binary";
    public static final String OUTPUT_BIN_EXTENSION = "assembler.output.binary.extension";

    /**
     * If this value is set to <code>true</code>, the preprocessor will be skipped
     * (by stripping it down to a basic file reader).<br>
     * Without the preprocessor features like
     * <ul>
     *     <li>directives</li>
     *     <li>removal of comments</li>
     *     <li>automatic lower casing</li>
     *     <li>obvious operands</li>
     *     <li>etc …</li>
     * </ul>
     * are not possible.<br>
     * <br>
     * A possible gain of deactivating the preprocessor is a potential gain of
     * disabling the potential gain of speed.<br>
     * Valid values: true, false<br>
     * Defaults to: false
     */
    public static final String SKIP_PREPROCESSING = "assembler.skip-preprocessing";


    /**
     * Initialize all settings that are used by the assembler.
     */
    static {
        Settings s = Settings.INSTANCE;

        s.setDefault(RADIX, "10");
        s.setDefault(NOTATION, "postfix");

        s.setDefault(OBVIOUS_OPERANDS, "error");
        s.setDefault(UNNECESSARY_OPERANDS, "error");
        s.setDefault(ADDRESS_OFFSET, "warn");
        s.setDefault(END_MISSING, "warn");
        s.setDefault(MULTIPLE_SAME_MATCH_CASE, "error");
        s.setDefault(UNNECESSARY_SEGMENTS, "warn");

        s.setDefault(SOURCE_FILE_EXTENSION, ".asm");
        s.setDefault(HEX_FILE_EXTENSION, ".hex");

        s.setDefault(END_CODE_AFTER, "warn");
        s.setDefault(DEFAULT_FLAGS, "");
        s.setDefault(INCLUDE_DEPTH, "256");
        s.setDefault(INCLUDE_PATH, "includes");
        s.setDefault(INCLUDE_RECURSIVE_SEARCH, "false");

        s.setDefault(INCLUDE_DEFAULT_FILE, "true");
        s.setDefault(SKIP_PREPROCESSING, "false");

        s.setDefault(OUTPUT_HEX, "true");
        s.setDefault(OUTPUT_HEX_EXTENSION, ".hex");
        s.setDefault(OUTPUT_HEX_BUFFER_LENGTH, "16");
        s.setDefault(OUTPUT_HEX_WRAP, "true");

        s.setDefault(OUTPUT_BIN, "true");
        s.setDefault(OUTPUT_BIN_EXTENSION, ".bin");
    }

    /**
     * Initializes the Settings by ensuring that the static block
     * is initialized before the defaults or settings are accessed.
     */
    public static void init() {}

    /**
     * Whether a String is a valid error setting that only can have the values "error", "warn" or "ignore".
     */
    public static final Predicate<String> VALID_ERROR = x -> x.equalsIgnoreCase("error") || x.equalsIgnoreCase("warn")
            || x.equalsIgnoreCase("ignore");

    public static final Predicate<String> VALID_FILE_EXTENSION = x -> MC8051Library.FILE_EXTENSION_PATTERN.matcher(x)
            .matches();

    /**
     * Whether the value is valid radix.<br>
     * Valid radixes: 2: binary, 8: octal, 10: decimal and 16: hexadecimal
     * @see #RADIX
     */
    private static final IntPredicate VALID_RADIX = x -> x == 2 || x == 8 || x == 10 || x == 16;

    /**
     * Gets the corresponding radix char from the radix setting.
     * @see #RADIX
     */
    public static char getRadix() {
        int radix = Settings.INSTANCE.getIntProperty(RADIX, 10, VALID_RADIX);
        switch (radix) {
            case  2: return 'b';
            case  8: return 'o';
            case 16: return 'h';
            default: return 'd';
        }
    }
}
