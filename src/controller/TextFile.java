package controller;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Gordian, Jannik
 */
public class TextFile {
    private boolean changed = false;

    TextFile(Path p) {
        //TODO: throw if not readable
    }

    public boolean exists() {
        throw new UnsupportedOperationException("nope");
    }

    public Reader getReader() {
        throw new UnsupportedOperationException("nope");
    }

    public Writer getWriter() {
        throw new UnsupportedOperationException("nope");
    }

    public Path getPath() {
        throw new UnsupportedOperationException("nope");
    }

    public boolean isWritable() {
        throw new UnsupportedOperationException("nope");
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public boolean isChanged() {
        return this.changed;
    }
}
