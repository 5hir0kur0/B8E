package assembler.util;

import assembler.Mnemonic;
import assembler.OperandToken;

import java.util.List;

/**
 * Provides Mnemonics used by the assembler.
 *
 * @author Noxgrim
 */
public interface MnemonicProvider {

    /**
     * Returns a List of Mnemonics that can be used
     * by the assembler.
     */
    Mnemonic[] getMnemonics();

    /**
     * Returns the Problems that the mnemonics stored
     * in a List if they occurred.
     */
    List<Problem> getProblems();

    /**
     * Clears the Problem List.
     */
    void clearProblems();

    OperandToken createNewJumpOperand(long address);
}
