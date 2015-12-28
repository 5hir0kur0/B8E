package assembler.arc8051;

import assembler.Mnemonic;
import assembler.MnemonicNameToken;
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
     * @param minOp
     *      The minimum number of operands this mnemonic needs to work
     *      properly.
     * @param positionSensitive
     *      Whether this mnemonic's value changes with its position in
     *      code memory, e.g jumps or calls.
     */
    protected Mnemonic8051(String name, int minOp, boolean positionSensitive) {
        super(name, minOp, positionSensitive);
    }

    /**
     * Constructs a new 8051 Mnemonic that is not position sensitive.
     *
     * @param name
     *      The name of the mnemonic. It is used to differentiate
     *      this mnemonic from other ones with other functions.<br>
     *      The name will be converted to lower case.
     * @param minOp
     *      The minimum number of operands this mnemonic needs to work
     *      properly.
     */
    protected Mnemonic8051(String name, int minOp) {
        super(name, minOp, false);
    }

    @Override
    public byte[] getInstructionFromOperands(long codePoint, MnemonicNameToken name, OperandToken... operands) {
        return getInstructionFromOperands(codePoint, name, operands);
    }

    /**
     * @param codePoint
     *      The location of the mnemonic in the program
     *      memory. This can be used by some mnemonics
     *      to perform further calculations.<br>
     * @param name
     *      the name token used for this specific call.
     *      Can be used as cause for an error if no
     *      operands are present.
     * @param operands
     *      The operands of the mnemonic.
     *
     * @return
     *      an assembled representation of this mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    public abstract byte[] getInstructionFromOperands(long codePoint, MnemonicNameToken name,
                                                      OperandToken8051... operands);
}
