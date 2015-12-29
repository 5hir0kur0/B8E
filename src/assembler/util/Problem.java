package assembler.util;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Jannik
 */
public class Problem<T> {

    private Path path;
    private int line;
    private Type type;

    private String message;
    private T cause;

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
        if ((this.line = line) < 0)
            throw new IllegalArgumentException("'line' cannot be negative or zero!");
        this.cause = cause;
    }

    public Problem(String message, Type type, T cause) {
        this(message, type, null, 0, cause);
    }

    public Problem(String message, Type type) {
        this(message, type, null, 0, null);
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
