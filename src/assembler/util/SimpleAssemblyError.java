package assembler.util;

import java.util.Objects;

/**
 * Represents an error that has occurred while assembling.
 *
 * @author Jannik
 */
public class SimpleAssemblyError implements Comparable<SimpleAssemblyError> {

    /** The type of an assembly error. */
    public enum Type {
        ERROR, WARNING
    }


    /** The type of the assembly error. */
    protected Type type;
    /** A detailed description of the error. */
    protected String message;

    /**
     * A Object that represents an error that occurred
     * while assembling.<br>
     *
     * @param type
     *      The type of the error.
     * @param message
     *      The message of the error that contains
     *      a detailed description of the error and
     *      its cause.
     *
     * @throws IllegalArgumentException
     *      if the message is empty or only contains white
     *      space.
     */
    public SimpleAssemblyError(Type type, String message) {

        this.type = Objects.requireNonNull(type, "Type cannot be 'null'.");

        if (message == null)
            throw new NullPointerException("The message cannot be 'null'.");
        if (message.trim().isEmpty())
            throw new IllegalArgumentException("The message cannot be empty or only white space.");
    }

    /**
     * @return the type of the error.
     */
    public Type getType() { return type; }

    /**
     * @return whether the error type is an error.
     */
    public boolean isError() { return type == Type.ERROR; }

    /**
     * @return whether the error type is a warning.
     */
    public boolean isWarning() { return type == Type.WARNING; }

    /**
     * @return a detailed description of the error.
     */
    public String getMessage() { return message; }

    /**
     * Compares two assembly errors.
     * First the errors types are compared. If they are equal the line
     * numbers are compared instead.
     */
    @Override
    public int compareTo(SimpleAssemblyError o) {
        if (type != o.type)
            return type.compareTo(o.type);
        else
            return message.compareTo(o.message);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof SimpleAssemblyError) {
            SimpleAssemblyError other = (SimpleAssemblyError) obj;
            if (type == other.type)
                return message.equals(other.message);
        }
        return false;
    }
}
