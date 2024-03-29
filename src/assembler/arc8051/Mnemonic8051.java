package assembler.arc8051;

import assembler.tokens.OperandToken;
import assembler.tokens.Tokens;
import assembler.util.assembling.Mnemonic;
import assembler.util.problems.Problem;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

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
    public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name, OperandToken[] operands,
                                             Path file, List<Problem<?>> problems) {
        return getInstructionFromOperands(codePoint, name,
                Arrays.asList(operands).toArray(new OperandToken8051[operands.length]), file, problems);
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
     * @param file
     *      the file in witch the mnemonic is located.
     * @param problems
     *      a to witch occurring Problems can be added.
     *
     * @return
     *      an assembled representation of this mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    public abstract byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                      OperandToken8051[] operands, Path file, List<Problem<?>> problems);

    @Override
    protected String getClassName() {
        return "Mnemonic8051";
    }
}
