package assembler.util.problems;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

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

    /**
     * Calls the <code>'printStackTrace()'</code> method
     * of the Exception.
     */
    public void printStackTrace() {
       getCause().printStackTrace();
    }
    /**
     * Calls the <code>'printStackTrace(PrintWriter s)'</code> method
     * of the Exception.
     */
    public void printStackTrace(PrintWriter s) {
        getCause().printStackTrace(s);
    }
    /**
     * Calls the <code>'printStackTrace(PrintStream s)'</code> method
     * of the Exception.
     */
    public void printStackTrace(PrintStream s) {
        getCause().printStackTrace(s);
    }

    /**
     * Returns the StackTrace as it would be printed by the
     * <code>'printStaceTrace()'</code> method of the exception as a
     * String.<br>
     */
    public String getStackTraceAsString() {
        StringWriter sw = new StringWriter();
        getCause().printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}
