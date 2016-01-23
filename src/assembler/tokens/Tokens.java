package assembler.tokens;

/**
 * Contains token types that do not expand the normal token type.
 *
 * @author Jannik
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

    /**
     * Represents a Symbol, user- or assembler-defined
     * names for constants (e.g.: addresses).
     */
    public static class SymbolToken extends Token {

        /**
         * Constructs a new Symbol Token.
         *
         * @param value the value of the token.
         * @param line the line of the token.
         */
        public SymbolToken(String value, int line) {
            super(value, TokenType.SYMBOL, line);
        }
    }
}
