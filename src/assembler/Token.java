package assembler;

import assembler.util.SimpleAssemblyError;

import java.util.Objects;

/**
 * Represents a token that is process by the assembler.<br>
 * Every token has a type and can be used as the cause for a TokenError.
 *
 * @author Jannik
 */
public class Token {
    /** The type of the token. */
    protected TokenType type;
    /** The value of the token. */
    protected String value;

    protected SimpleAssemblyError error = null;

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

    public boolean hasError() { return error != null;}
    public SimpleAssemblyError getError() { return error; }
    public void setError(SimpleAssemblyError error) {this.error = error;}

    /**
     * The different types a token can have.
     */
    public enum TokenType {
        MNEMONIC_NAME, OPERAND, LABEL
    }

}
