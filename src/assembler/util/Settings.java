package assembler.util;

/**
 * A temporary class to store the assembler settings.
 * @author Jannik
 */
public class Settings {

    public enum  ErrorHandling {
        IGNORE, WARN, ERROR;
    }

    public static class Errors {
        public static ErrorHandling ADDITIONAL_OPERANDS = ErrorHandling.WARN;
    }

}
