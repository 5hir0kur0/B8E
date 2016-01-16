package assembler.util.problems;

import assembler.tokens.Token;

/**
 * @author Noxgrim
 */
public class TokenProblem extends Problem<Token> {

    public TokenProblem(String message, Type type, Token cause) {
        super(message, type, null, cause.getLine(), cause);
        //TODO: Add File attribute to Token and integrate it here.
    }

}
