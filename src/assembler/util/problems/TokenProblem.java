package assembler.util.problems;

import assembler.tokens.Token;

import java.nio.file.Path;

/**
 * @author Jannik
 */
public class TokenProblem extends Problem<Token> {

    public TokenProblem(String message, Type type, Path file, Token cause) {
        super(message, type, file, cause.getLine(), cause);
    }

}
