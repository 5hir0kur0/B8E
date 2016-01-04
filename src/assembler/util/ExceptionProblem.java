package assembler.util;

/**
 * @author Noxgrim
 */
public class ExceptionProblem extends Problem<Exception> {
    public ExceptionProblem(String message, Type type, Exception cause) {
        super(message, type, cause);
    }

}
