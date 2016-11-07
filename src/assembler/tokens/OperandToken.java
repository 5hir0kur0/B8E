package assembler.tokens;

import java.util.Objects;

/**
 * Represents an operand of a mnemonic.
 *
 * @author Noxgrim
 */
public abstract class OperandToken extends Token {

    /** Type of the operand. Should be named via <code>final</code> variables. */
    protected Enum operandType;
    /** Representation of the . */
    protected Enum operandRepresentation;

    /**
     * Constructs a new OperandToken.
     *
     * @param type
     *      the operandType of the OperandToken.<br>
     *      Represents what the OperandToken represents.
     * @param representation
     *      the operandRepresentation of the OperandToken.<br>
     *      Defines how the OperandToken is represented.
     * @param value
     *      the value of the OperandToken as a String.
     * @param line
     *      the line of the token.
     */
    public OperandToken(Enum type, Enum representation, String value, int line) {
        super(value, TokenType.OPERAND, line);
        this.operandType = Objects.requireNonNull(type, "'Type' of operand cannot be 'null'!");
        this.operandRepresentation = Objects.requireNonNull(representation,
                "'Representation' of the operand cannot be 'null'!");
    }

    /**
     * @return
     *      the type of the operand.
     */
    public abstract Enum getOperandType();

    /**
     * @return
     *      the representation of operand.
     */
    public abstract Enum getOperandRepresentation();

    /**
     * @return
     *      the value of the operand with post- or prefixes.
     */
    public abstract String getFullValue();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OperandToken)) return false;
        if (!super.equals(o)) return false;

        OperandToken that = (OperandToken) o;

        if (!operandType.equals(that.operandType)) return false;
        if (!operandRepresentation.equals(that.operandRepresentation)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + operandType.hashCode();
        result = 31 * result + operandRepresentation.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"("+line+")["+type.toString()+", "+getOperandType()+", "+
                getOperandRepresentation()+", "+value+"]";
    }
}
