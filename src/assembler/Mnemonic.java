package assembler;

/**
 * Represents a mnemonic in an assembler language.
 * This mnemonic can assemble itself, if it knows
 * its operands.
 *
 * @author Noxgrim
 */
public abstract class Mnemonic {

    /** The name of the mnemonic. e.g: "mov". */
    private final String name;
    /** The minimum number of operands. */
    private final int minOp;

    private final boolean positionSensitive;

    /**
     * Constructs a new Mnemonic.
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
    protected Mnemonic(String name, int minOp, boolean positionSensitive) {
        if (name == null)
            throw new NullPointerException("'Name' cannot be 'null'!");
        if (name.trim().isEmpty())
            throw new IllegalArgumentException("'Name' cannot be empty.");
        this.name = name.toLowerCase();
        if (minOp < 0)
            throw new IllegalArgumentException("The minimum number of operands cannot be negative.");
        this.minOp = minOp;
        this.positionSensitive = positionSensitive;
    }

    /**
     * Constructs a new Mnemonic that is not position sensitive.
     *
     * @param name
     *      The name of the mnemonic. It is used to differentiate
     *      this mnemonic from other ones with other functions.<br>
     *      The name will be converted to lower case.
     * @param minOp
     *      The minimum number of operands this mnemonic needs to work
     *      properly.
     */
    protected Mnemonic(String name, int minOp) {
        this(name, minOp, false);
    }

    /**
     * @return
     *      the name of the mnemonic in lower case.
     */
    public String getName() {
        return name;
    }

    /**
     * @return
     *      The minimum number of operands for this mnemonic to work.
     */
    public int getMinimumOperands() {
        return minOp;
    }

    /**
     * @return
     *      Whether the value of this mnemonic changes with its position in the position
     *      in code memory.
     */
    public boolean isPositionSensitive() {
        return positionSensitive;
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
     *      This method calls {@link #getInstructionFromOperands(long, OperandToken...)}
     *      and returns the length of the resulting array.
     */
    public int getByteNumber(long codePoint, OperandToken... operands) {
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
     *      an assembled representation of this mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    public abstract byte[] getInstructionFromOperands(long codePoint, OperandToken ... operands);
}
