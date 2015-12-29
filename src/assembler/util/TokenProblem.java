package assembler.util;

import assembler.Token;

/**
 * @author Polymehr
 */
public class TokenProblem extends Problem<Token> {

    public TokenProblem(String message, Type type, Token cause) {
        super(message, type, cause);
        //TODO: Add Line and File attribute to Token and integrate it here.
    }

}
