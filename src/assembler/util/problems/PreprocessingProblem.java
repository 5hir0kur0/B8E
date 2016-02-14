package assembler.util.problems;

import java.nio.file.Path;

/**
 * @author Noxgrim
 */
public class PreprocessingProblem extends Problem<String> {
    public PreprocessingProblem(String message, Type type, Path file, int line, String cause) {
        super(message, type, file, line, cause);
    }
    public PreprocessingProblem(Path file, int line, String cause) {
        super("???", Type.ERROR, file, line, cause);
    }

}
