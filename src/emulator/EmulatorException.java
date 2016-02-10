package emulator;

/**
 * Simple class for exceptions thrown by {@code Emulator}s (in particular by the {@code next()} method.
 * @author Gordian
 */
public class EmulatorException extends Exception {
    public EmulatorException() {
        super();
    }

    public EmulatorException(String message) {
        super(message);
    }

    public EmulatorException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmulatorException(Throwable cause) {
        super(cause);
    }
}
