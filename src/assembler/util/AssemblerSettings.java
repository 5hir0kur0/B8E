package assembler.util;

import misc.Settings;

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
     * Valid values: 2, 8, 10, 16, "auto"<br>
     * Defaults to: 10
     */
    private static final String RADIX = "assembler.parsing.default-number-radix";
    /**
     * The used notations for numbers.<br>
     * Can be either "prefix" (0, 0x or 0b) or
     * "postfix"/"suffix" (b, o, q, d or h).<br>
     * Valid values: "prefix", "postfix", "suffix"<br>
     * Defaults to: "postfix"
     */
    private static final String NOTATION = "assembler.parsing.notation";
    /**
     * The behaviour if some "obvious" operands are encountered.<br>
     * Obvious operands are operands that aren't needed to specify the
     * mnemonic-operand combination.<br>
     *     E.g. in <code>add a, R1</code> does not need the <code>a</code>
     *     to be recognised so <code>add R1</code> is valid as well.<br>
     * Valid values: "error", "warn", "ignore"<br>
     * Defaults to: "error"
     */
    private static final String OBVIOUS_OPERANDS = "assembler.errors.obvious-operands";
    /**
     * The behavior if some "unnecessary" operands.<br>
     * Unnecessary operands are operands that aren't needed for mnemonic
     * encoding e.g operands for the <code>NOP</code> mnemonic.<br>
     * Valid values: "error", "warn", "ignore"<br>
     * Defaults to: "error"
     */
    private static final String UNNECESSARY_OPERANDS = "assembler.errors.unnecessary-operands";
    /**
     * The extension of assembly source files.<br>
     * Valid values: All values that start with a dot '.' and are then followed
     * by word characters.
     */
    private static final String SOURCE_FILE_EXTENSION = "assembler.output.asm.extension";

    static {
        Settings.INSTANCE.setDefault(RADIX, "10");
        Settings.INSTANCE.setDefault(NOTATION, "postfix");
        Settings.INSTANCE.setDefault(OBVIOUS_OPERANDS, "error");
        Settings.INSTANCE.setDefault(UNNECESSARY_OPERANDS, "error");
        Settings.INSTANCE.setDefault(SOURCE_FILE_EXTENSION, ".asm");
    }

    public static String FILE_EXTENSION = ".asm";

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
