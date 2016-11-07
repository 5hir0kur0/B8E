package controller;

import assembler.Assembler;
import emulator.Emulator;
import emulator.RAM;
import emulator.arc8051.MC8051;
import misc.Logger;
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

    public static final String PROJECT_NAME_KEY = "project.name";
    public static final String PROJECT_MAIN_FILE_KEY = "project.main-file";

    Project(Path projectPath, boolean permanent, boolean create) throws IOException,
            IllegalArgumentException {
        this.projectPath = Objects.requireNonNull(projectPath, "project path must not be null");
        Path path = Project.findProjectSettings(projectPath);
        if (null == path) {
            if (!create) {
                this.permanent = false; // No new project should be created so the project has to be non-permanent
                this.projectFile = null;
                Logger.log("No project file found. Loading non-permanent project.", Project.class, Logger.LogLevel.WARNING);
            } else {
                this.projectFile = projectPath.resolve(PROJECT_SETTINGS_NAMES[0]);
                if (!Files.exists(projectPath)) {
                    Files.createDirectories(projectPath);
                    Files.createFile(this.projectFile);
                    Files.setAttribute(this.projectFile, "dos:hidden", true);
                    store(); // Write user given properties even if project is temporary (I think that would be
                             // expected...)
                }
                this.permanent = permanent;
            }
        }  else {
            this.projectFile = path;
            this.permanent = permanent;
            try (Reader r = Files.newBufferedReader(this.projectFile, CHARSET)) {
                Settings.INSTANCE.load(r);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (this.projectFile != null && Files.exists(this.projectFile) && this.isPermanent())
            store();
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
        return Settings.INSTANCE.getProperty(PROJECT_NAME_KEY, this.projectPath.getFileName().toString(),
                s -> !s.trim().isEmpty()) + (this.isPermanent() ? "" : " [temporary]") ;
    }

    public static Path findProjectSettings(Path projectPath) {
        for (String name : PROJECT_SETTINGS_NAMES) {
            Path tmp = projectPath.resolve(name);
            if (Files.exists(tmp)) {
                return  tmp;
            }
        }
        return null;
    }
     private void store() throws IOException {
         Settings.INSTANCE.store(Files.newBufferedWriter(this.projectFile, CHARSET),
                 " Project File for " + this.getName() + "\n" +
                 " vim: ft=cfg"); // Write Vim modeline. Only takes effect if the 'modeline' option is set.
                                 // (Deactivated by default on Debian systems.)
     }
}
