package assembler.util;

import java.util.Objects;

/**
 * Represents an error that has occurred while assembling.
 *
 * @author Noxgrim
 */
public class AssemblyError implements Comparable<AssemblyError> {

    /** The type of an assembly error. */
    public enum Type {
        ERROR, WARNING
    }


    /** The type of the assembly error. */
    private Type type;
    /** The line where the error occurred. */
    private int line;
    /** A detailed description of the error. */
    private String message;

    /**
     * A Object that represents an error that occurred
     * while assembling.<br>
     *
     * @param type
     *      The type of the error.
     * @param line
     *      The line of the error.<br>
     *      This contains information where the
     *      error happened.<br>
     *
     *      This number cannot be negative.
     * @param message
     *      The message of the error that contains
     *      a detailed description of the error and
     *      its cause.
     *
     * @throws IllegalArgumentException
     *      if the line number is negative.
     */
    AssemblyError(Type type, int line, String message) {

        Objects.requireNonNull(type, "Type cannot be 'null'.");
        this.type = type;

        if (line < 0)
            throw new IllegalArgumentException("The line number cannot be negative.");
        this.line = line;

        Objects.requireNonNull(message, "The message cannot be 'null'.");

        this.message = message;
    }

    /**
     * @return the line where the occurred.
     */
    public int getLine() {
        return line;
    }

    /**
     * @return the type of the error.
     */
    public Type getType() {
        return type;
    }

    /**
     * @return a detailed description of the error.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Compares two assembly errors.
     * First the errors types are compared. If they are equal the line
     * numbers are compared instead.
     */
    @Override
    public int compareTo(AssemblyError o) {
        if (type != o.type)
            return type.compareTo(o.type);
        else
            return line - o.line;
    }
}
