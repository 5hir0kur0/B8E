package assembler.tokens;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents a token that indicates that the current file should be
 * changed.
 *
 * @author Jannik
 */
public class FileChangeToken extends Token{

    /** The path the file should be changed to.*/
    private Path file;

    /**
     * Constructs a new <code>FileChangeToken</code> that indicates that the
     * current file should be changed to a new file.<br>
     *
     * @param file
     *      the new file.
     */
    public FileChangeToken(Path file) {
        super(Objects.requireNonNull(file, "'file' cannot be 'null'!").toString(), TokenType.DIRECTIVE, 0);
        this.file = file;
    }

    /**
     * Returns the file the current file should be
     * changed to.
     */
    public Path getFile() {
        return file;
    }
}
