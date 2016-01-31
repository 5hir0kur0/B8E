package assembler.util.problems;

/**
 * Represents a Problem is created if an exception
 * occurred.
 *
 * @author Noxgrim
 */
public class ExceptionProblem extends Problem<Exception> {
    public ExceptionProblem(String message, Type type, Exception cause) {
        super(message, type, cause);
    }

}
