package assembler.util;

/**
 * A temporary class to store the assembler settings.
 * @author Jannik
 */
public class Settings {


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
