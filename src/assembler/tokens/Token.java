package assembler.tokens;

import java.util.Objects;

/**
 * Represents a token that is process by the assembler.<br>
 * Every token has a type and can be used as the cause for a TokenError.
 *
 * @author Jannik
 */
public class Token implements Comparable<Token> {
    /** The type of the token. */
    protected final TokenType type;
    /** The value of the token. */
    protected String value;
    /** The line of the token.*/
    protected int line;
    /** Number to differentiate tokens in the same line but different instructions. */
    protected int instructionId;


    /**
     * Constructs a new Token.
     *
     * @param value the value of the token.
     * @param type the type of the token.
     * @param line the line of the token.
     */
    public Token(String value, TokenType type, int line) {
        Objects.requireNonNull(value, "'Value' of token cannot be 'null'!");
        if ((this.value = value).trim().isEmpty())
            throw new IllegalArgumentException("'Value' of token cannot be empty or only white space!");
        if ((this.line = line) < 0)
            throw new IllegalArgumentException("Line of token cannot be negative!");

        this.type  = Objects.requireNonNull(type, "'Type' of token cannot be 'null'!");
        this.instructionId = 0;
    }

    /**
     * Returns the type of the Token.
     * @see Token.TokenType
     */
    public TokenType getType() {
        return type;
    }

    /**
     * Returns the value of the Token as a String.
     */
    public String getValue() {
        return value;
    }

    /**
     * @param instructionId
     *      the number to differentiate tokens in the same line
     *      but different instructions to be set.
     */
    public void setInstructionId(int instructionId) {
        this.instructionId = instructionId;
    }

    /**
     * @return
     *      the number to differentiate tokens in the same line
     *      but different instructions.
     */
    public int getInstructionId() {
        return instructionId;
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
        MNEMONIC_NAME, OPERAND, LABEL, DIRECTIVE
    }

    /**
     * Returns the line of the file in which the token resides.
     */
    public int getLine() {
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Token)) return false;

        Token token = (Token) o;

        if (line != token.line) return false;
        if (instructionId != token.instructionId) return false;
        if (type != token.type) return false;
        return value.equals(token.value);

    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + line;
        result = 31 * result + instructionId;
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"("+line+")["+type.toString()+", "+value+"]";
    }
}
