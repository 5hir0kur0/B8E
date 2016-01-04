package assembler;

import java.util.Objects;

/**
 * Represents a token that is process by the assembler.<br>
 * Every token has a type and can be used as the cause for a TokenError.
 *
 * @author Noxgrim
 */
public class Token implements Comparable<Token> {
    /** The type of the token. */
    protected TokenType type;
    /** The value of the token. */
    protected String value;


    /**
     * Constructs a new Token.
     *
     * @param value the value of the token.
     * @param type the type of the token.
     */
    public Token(String value, TokenType type) {
        Objects.requireNonNull(value, "'Value' of token cannot be 'null'!");
        if (value.trim().isEmpty())
            throw new IllegalArgumentException("'Value' of token cannot be empty or only white space!");
        this.value = value;

        this.type  = Objects.requireNonNull(type, "'Type' of token cannot be 'null'!");
    }


    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int compareTo(Token o) {
        if (type != o.type)
            return type.compareTo(o.type);
        else
            return value.compareTo(o.value);
    }

    /**
     * The different types a token can have.
     */
    public enum TokenType {
        MNEMONIC_NAME, OPERAND, LABEL, SYMBOL, COMMENT
    }

}
