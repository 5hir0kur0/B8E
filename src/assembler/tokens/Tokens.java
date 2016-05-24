package assembler.tokens;

import java.nio.file.Path;

/**
 * Contains token types that do not expand the normal token type.
 *
 * @author Noxgrim
 */
public class Tokens {

    /**
     * Represents the mnemonic (as a String) itself.
     */
    public static class MnemonicNameToken extends Token {

        /**
         * Constructs a new Mnemonic Name Token.
         *
         * @param value the value of the token.
         * @param line the line of the token.
         */
        public MnemonicNameToken(String value, int line) {
            super(value, TokenType.MNEMONIC_NAME, line);
        }
    }
}
