package assembler.util;

import assembler.arc8051.MC8051Library;
import misc.Settings;

import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A temporary class to store the assembler settings.
 * @author Jannik
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
     * Valid values: 2, 8, 10, 16, "auto"<br>
     * Defaults to: 10
     */
    public static final String RADIX = "assembler.parsing.default-number-radix";
    /**
     * The used notations for numbers.<br>
     * Can be either "prefix" (0, 0x or 0b) or
     * "postfix"/"suffix" (b, o, q, d or h).<br>
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
     * Valid values: All values that start with a dot '.' and are then followed
     * by word characters.<br>
     * Defaults to: ".asm"
     */
    public static final String SOURCE_FILE_EXTENSION = "assembler.output.file-extensions.asm";
    /** The default value of the assembler source file. */
    public static final String DEFAULT_SOURCE_FILE_EXTENSION = ".asm";

    /**
     * The extension of Intel-HEX files.<br>
     * Valid values: All values that start with a dot '.' and are then followed
     * by word characters.<br>
     * Defaults to: ".hex"
     */
    public static final String HEX_FILE_EXTENSION = "assembler.output.file-extensions.hex";
    /** The default value of the Intel-HEX files. */
    public static final String DEFAULT_HEX_FILE_EXTENSION = ".hex";

    /**
     * Enforce the intended behaviour of the end directive:<br>
     * The intended behaviour is that all lines after the end directive
     * must contain only whitespace or comments and mustn't contain other
     * directives or instructions.<br>
     * If this behaviour has been turned off a missing end directive will
     * only result in an error and instructions after the end directive will
     * be assembled normally.<br>
     * Valid values: true, false<br>
     * Defaults to: true
     */
    public static final String END_ENFORCEMENT = "assembler.directives.end.enforce";

    /**
     * The behavior if no end directive was found.<br>
     * Valid values: "error", "warn", "ignore"<br>
     * Defaults to: "warn"
     */
    public static final String END_MISSING = "assembler.errors.missing-end-directive";

    /**
     * The default behaviour if an address offset operator is used.<br>
     * Valid values: "error", "warn", "ignore"<br>
     * Defaults to: "warn"
     */
    public static final String ADDRESS_OFFSET = "assembler.errors.address-offset-used";


    /**
     * Initialize all settings that are used by the assembler.
     */
    static {
        Settings.INSTANCE.setDefault(RADIX, "10");
        Settings.INSTANCE.setDefault(NOTATION, "postfix");
        Settings.INSTANCE.setDefault(OBVIOUS_OPERANDS, "error");
        Settings.INSTANCE.setDefault(UNNECESSARY_OPERANDS, "error");
        Settings.INSTANCE.setDefault(SOURCE_FILE_EXTENSION, DEFAULT_SOURCE_FILE_EXTENSION);
        Settings.INSTANCE.setDefault(HEX_FILE_EXTENSION, DEFAULT_HEX_FILE_EXTENSION);
        Settings.INSTANCE.setDefault(END_ENFORCEMENT, "true");
        Settings.INSTANCE.setDefault(END_MISSING, "warn");
    }

    /**
     * Whether a String is a valid error setting that only can have the values "error", "warn" or "ignore".
     */
    public static final Predicate<String> VALID_ERROR = x -> x.equalsIgnoreCase("error") || x.equalsIgnoreCase("warn")
            || x.equalsIgnoreCase("ignore");

    public static final Predicate<String> VALID_FILE_EXTENSION = x -> MC8051Library.FILE_EXTENSION_PATTER.matcher(x)
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


    public static class Errors {
        /**
         * How additional operands like
         * <pre>
         *     NOP <i><b>#42 #21</b></i>
         * </pre>
         * should be handled.
         */
        public static ErrorHandling ADDITIONAL_OPERANDS = ErrorHandling.ERROR;
        /**
         * How the ignoring of obvious operands like
         * <pre>
         *     ADD <i><b>a</b></i>, #42
         *     ==
         *     ADD #42
         * </pre>
         * should be handled.<br>
         * The <code>a</code> operand is obvious in this case because
         * all variants of <code>ADD</code> use it at this position.
         */
        public static ErrorHandling IGNORE_OBVIOUS_OPERANDS = ErrorHandling.ERROR;

        /**
         * How using of address offsets like
         * <pre>
         *     SJMP -02h
         * </pre>
         * instead of labels is handled.
         */
        public static ErrorHandling ADDRESS_OFFSET_USED = ErrorHandling.WARN;

        public enum  ErrorHandling {
            IGNORE, WARN, ERROR;
        }
    }

}
