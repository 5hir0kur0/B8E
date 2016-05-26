package assembler.util.problems;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Jannik
 */
public class Problem<T> implements Comparable<Problem<?>> {

    private Path path;
    private int line;
    private Type type;

    private String message;
    private T cause;

    @Override
    public int compareTo(Problem<?> o) {
        Objects.requireNonNull(o, "Object to be compared cannot be 'null'.");
        if (type != o.type)
            return type.compareTo(o.type);
        else if (path != null && o.path != null && !path.equals(o.path))
            return path.compareTo(o.path);
        else if (line != o.line)
            return line - o.line;
        else
            return message.compareTo(o.message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Problem)) return false;

        Problem<?> problem = (Problem<?>) o;

        if (line != problem.line) return false;
        if (path != null ? !path.equals(problem.path) : problem.path != null) return false;
        if (type != problem.type) return false;
        if (!message.equals(problem.message)) return false;
        return !(cause != null ? !cause.equals(problem.cause) : problem.cause != null);

    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + line;
        result = 31 * result + type.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + (cause != null ? cause.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "("+type+")["+(path == null ? "?":path.toString())+":"+
                (line!=-1?line:"?")+"]:"+" \""+message+"\""+(cause != null ? " (Caused by: "+cause.toString()+")" : "");
    }

    public enum Type {
        ERROR, WARNING, INFORMATION;
    }

    public Problem(String message, Type type, Path path, int line, T cause) {
        if (message == null)
            throw new NullPointerException("'message' cannot be 'null'!");
        else if ((this.message = message).trim().isEmpty())
            throw new IllegalArgumentException("'message' cannot be empty (or only white space)!");

        this.type = Objects.requireNonNull(type, "'type' cannot be 'null'!");
        this.path = path;
        if ((this.line = line) < -1)
            throw new IllegalArgumentException("'line' cannot be smaller than -1!");
        this.cause = cause;
    }

    public Problem(String message, Type type, T cause) {
        this(message, type, null, -1, cause);
    }

    public Problem(String message, Type type) {
        this(message, type, null, -1, null);
    }


    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        if (line >= 0)
            this.line = line;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        if (type != null)
            this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        if (message != null && !message.trim().isEmpty())
            this.message = message;
    }

    public T getCause() {
        return cause;
    }

    public void setCause(T cause) {
        this.cause = cause;
    }


    public boolean isError() {
        return type == Type.ERROR;
    }

    public boolean isWarning() {
        return type == Type.WARNING;
    }

    public boolean isInformation() {
        return  type == Type.INFORMATION;
    }
}
