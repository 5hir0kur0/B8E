package assembler;

import assembler.util.SimpleAssemblyError;

import java.util.Objects;

/**
 * @author Jannik
 */
public class Token {

    protected TokenType type;
    protected String value;

    protected SimpleAssemblyError error = null;


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

    public enum TokenType {
        MNEMONIC_NAME, OPERAND, LABEL
    }

}
