package assembler.arc8051;

import assembler.OperandToken;
import assembler.arc8051.MC8051Libary.OperandType8051;

/**
 * @author Jannik
 */
public class OperandToken8051 extends OperandToken {
    /**
     * Constructs a new OperandToken.
     *
     * @param type  the operandType of the OperandToken.<br>
     *              To remember the operandType better its value should be
     *              saved in a named final variable or an enum's ordinal
     *              and it should be used instead.<br>
     * @param value
     */
    public OperandToken8051(OperandType8051 type, String value) {
        super(type, value);
    }

    @Override
    public OperandType8051 getOperandType() {
        return (OperandType8051) operandType;
    }
}
