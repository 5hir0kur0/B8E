package assembler.arc8051;

import assembler.tokens.OperandToken;
import assembler.arc8051.MC8051Library.OperandType8051;

/**
 * @author Jannik
 */
public class OperandToken8051 extends OperandToken {
    /**
     * Constructs a new OperandToken.
     *
     * @param type  the operandType of the OperandToken.<br>
     * @param value the value of the token as a string.
     * @param line the line of the token.
     */
    public OperandToken8051(OperandType8051 type, String value, int line) {
        super(type, value, line);
    }

    @Override
    public OperandType8051 getOperandType() {
        return (OperandType8051) operandType;
    }
}
