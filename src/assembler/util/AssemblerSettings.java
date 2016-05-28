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
     * Valid values: Any valid path
     * Defaults to: ""
     */
    public static final String INCLUDE_PATH = "assembler.directives.include.path";

    /**
     * Determines whether the preprocessor searches recursively for a given file
     * in the directory.<br>
     * <br>
     * Valid values: true, false<br>
     * Defaults to: false
     */
    public static final String INCLUDE_RECURSIVE_SEARCH = "assembler.directives.include.recursive-search";

    /**
     * If this value is set to <code>true</code> the default include file <code>'default.asm'</code>
     * will be included automatically on each run of the assembler.<br>
     * <br>
     * Valid values: true, false<br>
     * Defaults to: true
     */
    public static final String INCLUDE_DEFAULT_FILE = "assembler.include-default-file";

    /**
     * The preprocessor tries to include every file specified. The files will be treated as path includes
     * (<code>&lt;&gt;</code>).
     * Multiple files can be separated with a ';'<br>
     * <br>
     * Valid values: Any valid path
     * Defaults to: ""
     */
    public static final String AUTO_INCLUDES = "assembler.automatic-included-files";

    /**
     * The MCU file to include. If empty, the file will not be included. The file will be treated like
     * a normal include.<br>
     * <br>
     * Valid values: Any valid path<br>
     * Defaults to: "mcu/8051.mcu"
     */
    public static final String MCU_FILE = "assembler.mcu-file";

    /**
     * The output directory relative to the <code>directory</code> given to the assembler (usually
     * the project directory).<br>
     * <br>
     * Valid values: Any valid path<br>
     * Special values: "." - Project directory,
     *                 "" - Directory of the source file.
     * Defaults to: "."
     */
    public static final String OUTPUT_DIR = "assembler.output.directory";

    /**
     * Whether to write Intel-HEX files to disk at all.<br>
     * <br>
     * Valid values: true, false<br>
     * Defaults to: true
     */
    public static final String OUTPUT_HEX = "assembler.output.hex";

    /**
     * The extension of Intel-HEX files.<br>
     * <br>
     * Valid values: All values that start with a dot '.' and are then followed
     *               by word characters.<br>
     * Defaults to: ".hex"
     */
    public static final String OUTPUT_HEX_EXTENSION = "assembler.output.hex.extension";

    /**
     * The maximum number of data bytes in a line of the output file.<br>
     * <br>
     * Valid values: All values bigger than 0
     * Defaults to: 16
     */
    public static final String OUTPUT_HEX_BUFFER_LENGTH = "assembler.output.hex.buffer-length";

    /**
     * If <code>true</code> the HEX writer will start a new line if the bytes of a
     * instructions pass the maximum buffer length.<br>
     * <br>
     * Valid values: true, false<br>
     * Defaults to: true
     */
    public static final String OUTPUT_HEX_WRAP = "assembler.output.hex.instruction-wrap";

    /**
     * Whether to write binary files to disk at all. <br>
     * <br>
     * Valid values: true, false<br>
     * Defaults to: true
     */
    public static final String OUTPUT_BIN = "assembler.output.binary";

    /**
     * The extension of binary files.<br>
     * <br>
     * Valid values: All values that start with a dot '.' and are then followed
     *               by word characters.<br>
     * Defaults to: ".bin"
     */
    public static final String OUTPUT_BIN_EXTENSION = "assembler.output.binary.extension";

    /**
     * Whether to only write instructions. If <code>false</code>, the whole code memory will be written
     * to file. <br>
     * <br>
     * Valid values: true, false<br>
     * Defaults to: true
     */
    public static final String OUTPUT_BIN_NECESSARY = "assembler.output.binary.only-write-necessary";

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
     *
     * Valid values: true, false<br>
     * Defaults to: false
     */
    public static final String SKIP_PREPROCESSING = "assembler.skip-preprocessing";


    /**
     * Initialize all settings that are used by the assembler.
     */
    static {
        Settings s = Settings.INSTANCE;

        s.setDefault(OBVIOUS_OPERANDS, "error");
        s.setDefault(UNNECESSARY_OPERANDS, "error");
        s.setDefault(ADDRESS_OFFSET, "warn");
        s.setDefault(END_MISSING, "warn");
        s.setDefault(MULTIPLE_SAME_MATCH_CASE, "error");
        s.setDefault(UNNECESSARY_SEGMENTS, "warn");

        s.setDefault(END_CODE_AFTER, "warn");
        s.setDefault(DEFAULT_FLAGS, "");
        s.setDefault(INCLUDE_DEPTH, "256");
        s.setDefault(INCLUDE_PATH, "");
        s.setDefault(MCU_FILE, "mcu/8051.mcu");
        s.setDefault(AUTO_INCLUDES, "");
        s.setDefault(INCLUDE_RECURSIVE_SEARCH, "false");

        s.setDefault(INCLUDE_DEFAULT_FILE, "true");
        s.setDefault(SKIP_PREPROCESSING, "false");

        s.setDefault(OUTPUT_DIR, ".");

        s.setDefault(OUTPUT_HEX, "true");
        s.setDefault(OUTPUT_HEX_EXTENSION, ".hex");
        s.setDefault(OUTPUT_HEX_BUFFER_LENGTH, "16");
        s.setDefault(OUTPUT_HEX_WRAP, "true");

        s.setDefault(OUTPUT_BIN, "true");
        s.setDefault(OUTPUT_BIN_EXTENSION, ".bin");
        s.setDefault(OUTPUT_BIN_NECESSARY, "true");
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

}
