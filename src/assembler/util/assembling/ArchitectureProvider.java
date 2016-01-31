package assembler.util.assembling;

import assembler.tokens.OperandToken;
import assembler.util.problems.Problem;

import java.util.List;

/**
 * Provides Mnemonics used by the assembler.
 *
 * @author Noxgrim
 */
public interface ArchitectureProvider {

    /**
     * Returns a List of Mnemonics that can be used
     * by the assembler.
     */
    Mnemonic[] getMnemonics();

    /**
     * Returns the Problems that the mnemonics stored
     * in a List if they occurred.
     */
    List<? extends Problem> getProblems();

    /**
     * Clears the Problem List.
     */
    void clearProblems();

    /**
     * Creates a new Token that is used from Mnemonics that
     * use Labels to replace the label with a proper jump
     * address.
     *
     * @param address
     *      the address the newly created operand should
     *      refer to.
     * @param line
     *      the line label.
     *
     * @return
     *      a architecture specific operand that holds the
     *      desired address and is used as a replacement for
     *      the label.
     */
    OperandToken createNewJumpOperand(long address, int line);
}
