package assembler;

import java.util.Objects;

/**
 * Represents an operand of a mnemonic.
 *
 * @author Noxgrim
 */
public abstract class OperandToken extends Token {

    /** Type of the operand. Should be named via <code>final</code> variables. */
    protected Enum operandType;

    /**
     * Constructs a new OperandToken.
     *
     * @param type
     *      the operandType of the OperandToken.<br>
     *      To remember the operandType better its value should be
     *      saved in a named final variable or an enum's ordinal
     *      and it should be used instead.<br>
     * @param value
     *      the value of the OperandToken as a String.
     * @param line
     * the line of the token.
     */
    public OperandToken(Enum type, String value, int line) {
        super(value, TokenType.OPERAND, line);
        this.operandType = Objects.requireNonNull(type, "'Type' of operand cannot be 'null'!");

    }

    /**
     * @return
     *      the type of the operand.
     */
    public abstract Enum getOperandType();

    /**
     * @return
     *      the value of this operand as a String.
     */
    public String getValue() {
        return value;
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"("+line+")["+type.toString()+", "+getOperandType()+", "+value+"]";
    }
}
