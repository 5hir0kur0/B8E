package assembler;

/**
 * Represents a mnemonic in an assembler language.
 * This mnemonic can assemble itself, if it knows
 * its operands.
 *
 * @author Jannik
 */
public abstract class Mnemonic<C extends Number> {

    /** The name of the mnemonic. e.g: "mov". */
    private final String name;

    /**
     * Constructs a new Mnemonic.
     *
     * @param name
     *      The name of the mnemonic. It is used to differentiate
     *      this mnemonic from other ones with other functions.<br>
     *      The name will be converted to lower case.
     */
    protected Mnemonic(String name) {
        this.name = name.toLowerCase();
    }

    /**
     * @return
     *      the name of the mnemonic in lower case.
     */
    public String getName() {
        return name;
    }

    /**
     * @param codePoint
     *      The location of the mnemonic in the program
     *      memory. This can be used by some mnemonics
     *      to perform further calculations.<br>
     * @param operands
     *      The operands of the mnemonic.
     *
     * @return
     *      the byte length of the resulting instruction.<br>
     *      This method calls {@link #getInstructionFromOperands(C codePoint)}
     *      and returns the length of the resulting array.
     */
    public int getByteNumber(C codePoint, Operand ... operands) {
        return getInstructionFromOperands(codePoint).length;
    }

    /**
     * @param codePoint
     *      The location of the mnemonic in the program
     *      memory. This can be used by some mnemonics
     *      to perform further calculations.<br>
     * @param operands
     *      The operands of the mnemonic.
     *
     * @return
     *      a assembled representation of this mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    public abstract byte[] getInstructionFromOperands(C codePoint, Operand ... operands);
}
