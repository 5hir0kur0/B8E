package assembler.arc8051;

import assembler.Mnemonic;
import assembler.OperandToken;

/**
 * A mnemonic for a microcomputer with a
 * 8051-family architecture.
 *
 * @author Noxgrim
 */
public abstract class Mnemonic8051 extends Mnemonic {
    /**
     * Constructs a new 8051 Mnemonic.
     *
     * @param name
     *      The name of the mnemonic. It is used to differentiate
     *      this mnemonic from other ones with other functions.<br>
     *      The name will be converted to lower case.
     */
    protected Mnemonic8051(String name) {
        super(name);
    }

    @Override
    public byte[] getInstructionFromOperands(long codePoint, OperandToken... operands) {
        return getInstructionFromOperands(codePoint, operands);
    }

    public abstract byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands);
}
