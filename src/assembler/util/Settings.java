package assembler.util;

/**
 * A temporary class to store the assembler settings.
 * @author Noxgrim
 */
public class Settings {

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

        public enum  ErrorHandling {
            IGNORE, WARN, ERROR;
        }
    }

}
