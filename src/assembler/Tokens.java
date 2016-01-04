package assembler;

/**
 * Contains token types that do not expand the normal token type.
 *
 * @author Jannik
 */
public class Tokens {
    /** Represents a Comment. */
    public static class CommentToken extends Token {

        /**
         * Constructs a new Comment Token.
         *
         * @param value the value of the token.
         */
        public CommentToken(String value) {
            super(value, TokenType.COMMENT);
        }
    }

    /**
     * Represents the mnemonic (as a String) itself.
     */
    public static class MnemonicNameToken extends Token {

        /**
         * Constructs a new Mnemonic Name Token.
         *
         * @param value the value of the token.
         */
        public MnemonicNameToken(String value) {
            super(value, TokenType.MNEMONIC_NAME);
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
         */
        public SymbolToken(String value) {
            super(value, TokenType.SYMBOL);
        }
    }
}
