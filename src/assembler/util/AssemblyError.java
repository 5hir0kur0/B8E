package assembler.util;

import java.io.File;

/**
 * @author Jannik
 */
public class AssemblyError extends SimpleAssemblyError {
    /** The line where the error occurred. */
    private int line;
    /** The file where the error occurred. */
    private File file;
    /**
     * The first token on which the error occurred first.<br>
     * Starts by <code>0</code>.
     */
    private int startToken;
    /**
     * The number of tokens that are affected from this error starting
     * with the start token.
     */
    private int count;


    /**
     * A Object that represents an error that occurred
     * while assembling.<br>
     *
     * @param line
     *      The line of the error.<br>
     *      This contains information where the
     *      error happened.<br>
     *
     *      This number cannot be negative.
     *
     * @throws IllegalArgumentException if the line number is negative.
     */
    public AssemblyError(SimpleAssemblyError error, int line,
                         int startToken, int count, File file) {
        super(error.getType(), error.getMessage());

        if ((this.line = line) < 0)
            throw new IllegalArgumentException("The line number cannot be negative.");
    }

    @Override
    public int compareTo(SimpleAssemblyError o) {
        if (o instanceof AssemblyError) {
            AssemblyError other = (AssemblyError) o;

            if (!file.equals(other.file))
                return file.compareTo(other.file);
            else if (!type.equals(other.type))
                return type.compareTo(other.type);
            else if (line != other.line)
                return other.line-line;
            else if (startToken != other.startToken)
                return other.startToken-startToken;
            else
                return message.compareTo(other.message);

        } else
            return super.compareTo(o);
    }

    /**
     * @return the line where the occurred.
     */
    public int getLine() { return line; }
}
