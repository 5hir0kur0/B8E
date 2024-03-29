package controller;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author 5hir0kur0, Noxgrim
 */
public class TextFile {
    private final Path path;

    TextFile(Path path, boolean create) throws IOException {
        this.path = Objects.requireNonNull(path, "path must not be null");
        if (!Files.isReadable(path) && !create) throw new IOException("given path is not readable: " + path);
        if (!Files.exists(path) && create) Files.createFile(path);
    }

    public Reader getReader() throws IOException {
        return Files.newBufferedReader(this.path, Project.CHARSET);
    }

    public Writer getWriter() throws IOException {
        return Files.newBufferedWriter(this.path, Project.CHARSET);
    }

    public Path getPath() {
        return this.path;
    }

    public boolean isWritable() {
        return Files.isWritable(this.path);
    }
}
