package controller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * @author 5hir0kur0, Noxgrim
 */
public class Project implements AutoCloseable {
    private final boolean permanent;
    private final Path projectPath;
    private final Properties settings;

    private Project(boolean permanent, Path projectPath) {
        this.permanent = permanent;
        this.projectPath = projectPath;
        if (null == projectPath) {
            this.settings = new Properties();
        } else {
            //TODO (try to {find project.b8e / .project.b8e)
            this.settings = null;
        }
    }

    public Project(Path projectPath) {
        this(true, projectPath);
        throw new UnsupportedOperationException("nope");
        //TODO
    }

    /**
     * Constructs a temporary project.
     */
    //TODO make private
    public Project() {
        this(false, Paths.get(System.getProperty("user.dir")));
    }

    @Override
    public void close() throws Exception {

    }

    public TextFile requestResource(Path path) {
        return null;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public Path getProjectPath() {
        return projectPath;
    }
}
