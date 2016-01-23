package assembler.util.assembling;

import assembler.tokens.OperandToken;
import assembler.util.problems.Problem;

import java.util.List;

/**
 * Provides Mnemonics used by the assembler.
 *
 * @author Jannik
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

    OperandToken createNewJumpOperand(long address, int line);
}
