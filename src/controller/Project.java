package controller;

import assembler.Assembler;
import emulator.Emulator;
import emulator.RAM;
import emulator.arc8051.MC8051;
import misc.Settings;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author 5hir0kur0, Noxgrim
 */
public class Project implements AutoCloseable {
    private final boolean permanent;
    private final Path projectPath;
    private final Path projectFile;

    private Assembler assembler;

    private final static String[] PROJECT_SETTINGS_NAMES = {".project.b8e", "project.b8e"};
    final static Charset CHARSET = StandardCharsets.UTF_8;

    Project(Path projectPath, boolean permanent) throws IOException,
            IllegalArgumentException {
        this.permanent = permanent;
        this.projectPath = Objects.requireNonNull(projectPath, "project path must not be null");
        Path path = null;
        for (String name : PROJECT_SETTINGS_NAMES) {
            Path tmp = projectPath.resolve(name);
            if (Files.exists(tmp)) {
                path = tmp;
                break;
            }
        }
        if (null == path && this.isPermanent())
            this.projectFile = projectPath.resolve(PROJECT_SETTINGS_NAMES[0]);
        else this.projectFile = path;
        if (projectFile != null)
            try (Reader r = Files.newBufferedReader(this.projectFile, CHARSET)) {
                Settings.INSTANCE.load(r);
            }
    }

    @Override
    public void close() throws IOException {
        if (this.projectFile != null && Files.exists(this.projectFile) && this.isPermanent())
            Settings.INSTANCE.store(Files.newBufferedWriter(this.projectFile, CHARSET),
                    "Project File for " + this.getName());
    }

    public TextFile requestResource(Path path, boolean create) throws IOException {
        return new TextFile(path, create);
    }

    public boolean isPermanent() {
        return permanent;
    }

    public Path getProjectPath() {
        return projectPath;
    }

    public Assembler getAssembler() {
        if (this.assembler == null) assembler = Assembler.of("8051");
        return this.assembler;
    }

    public Emulator makeEmulator(byte[] code) {
        final RAM codeMemory = new RAM(65536);
        final RAM externalMemory = new RAM(65536);
        int i = 0;
        for (byte b : code) codeMemory.set(i++, b);
        return new MC8051(codeMemory, externalMemory);
    }

    public String getName() {
        return Settings.INSTANCE.getProperty("project.name", this.projectPath.getFileName().toString() +
                (this.permanent ? "" : " [temporary]"),
                s -> !s.trim().isEmpty());
    }
}
