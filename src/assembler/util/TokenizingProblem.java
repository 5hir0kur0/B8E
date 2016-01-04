package assembler.util;

import java.nio.file.Path;

/**
 * @author Noxgrim
 */
public class TokenizingProblem extends Problem<String> {
    public TokenizingProblem(String message, Type type, String cause) {
        super(message, type, cause);
    }

    public TokenizingProblem(String message, Type type, Path path, int line, String cause) {
        super(message, type, path, line, cause);
    }
}
