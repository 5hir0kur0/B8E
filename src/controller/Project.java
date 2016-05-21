package controller;

import assembler.Assembler;
import emulator.Emulator;
import emulator.RAM;
import emulator.arc8051.MC8051;
import misc.Settings;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Gordian, Jannik
 */
public class Project implements AutoCloseable {
    private final boolean permanent;
    private final Path projectPath;
    private final Path projectFile;

    private Emulator emulator;
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
            if (Files.exists(tmp)) path = tmp;
        }
        if (null == path && this.isPermanent())
            this.projectFile = projectPath.resolve(PROJECT_SETTINGS_NAMES[0]);
        else this.projectFile = path;
        if (projectFile != null) Settings.INSTANCE.load(Files.newBufferedReader(this.projectFile, CHARSET));
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
        if (this.assembler == null) throw new UnsupportedOperationException("todo"); //TODO
        return this.assembler;
    }

    public Emulator getEmulator() {
        if (this.emulator == null) this.emulator = new MC8051(new RAM(65_536), new RAM(65_536));
        return this.emulator;
    }

    public String getName() {
        return Settings.INSTANCE.getProperty("project.name", this.projectPath.getFileName().toString(),
                s -> !s.trim().isEmpty());
    }
}
