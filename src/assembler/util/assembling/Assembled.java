package assembler.util.assembling;

import assembler.tokens.LabelToken;
import assembler.tokens.OperandToken;
import assembler.tokens.Token;
import assembler.tokens.Tokens;
import assembler.util.problems.Problem;

import java.nio.file.Path;
import java.util.List;

/**
 * Represents a collection of tokens as an instruction group.
 * An instruction group contains labels that refer to it
 * (in front or above it) and either an instruction
 * and operands or pure data (like "$db" directives).<br>
 * The instruction group represents an entity that can be
 * assembled.
 *
 * @author Noxgrim
 */
public interface Assembled {

    /**
     * @return
     *      the assembled representation of the last
     *      compilation of the instruction group.<br>
     *      Returns an empty byte array if there was no
     *      successful compilation.
     *
     * @see Mnemonic#getInstructionFromOperands(long, Tokens.MnemonicNameToken, OperandToken[], Path, List)
     */
    byte[] getCodes();

    /**
     * @return
     *      the address at which the assembled instruction
     *      group resides in the program memory.<br>
     *      This address is aways bigger than the origin address.
     */
    long getAddress();

    /**
     * Moves the assembled instruction around in program memory.<br>
     * The resulting address cannot be smaller than the origin address!
     *
     * @param amount
     *      the amount to move the address around. This number may be negative
     *      to move the address "backwards" in memory.
     */
    void moveAddress(long amount);

    /**
     * @return
     *      the origin address. The address of the instruction group cannot
     *      be smaller than this address. The origin could be moved when
     *      using the "org" or similar directives.
     */
    long getOrigin();

    /**
     * @return
     *      the tokens in this instruction group.
     *      (Does not contain the labels.)
     *
     * @see #getLabels()
     */
    Token[] getTokens();

    /**
     * @return
     *      the labels in this instruction group.
     *
     * @see #getTokens()
     */
    LabelToken[] getLabels();

    /**
     * @return
     *      the file in which the instruction group resides.
     */
    Path getFile();

    /**
     * Assemble the instruction group in this <code>Assembled</code>
     * and update the codes accordingly.
     *
     * @param problems
     *      problems occurring while assembling will be added
     *      to this {@link List}.<br>
     *      If the Assembled has been compiled multiple times
     *      old {@link Problem}s from the same instruction
     *      group should be removed before compiling again to
     *      prevent multiple or outdated Problems in this List.
     * @param labels
     *      All labels in the current assembler scope.
     *
     * @return
     *     The difference between the newly compiled codes and the
     *     old ones.<br>
     *     (Positive gain of length; Negative loss in length; Zero
     *     no change.)
     *
     * @see #getCodes()
     */
    int compile(List<Problem<?>> problems, List<LabelToken> labels);
}
