package assembler;

/**
 * An interface to define Mnemonics that work with
 * labels.
 *
 * @author Jannik
 */
public interface LabelConsumer {
    /**
     * Returns the maximal byte length for valid
     * operand combinations.
     */
    int getMaxLength();

    /**
     * Returns the expected length for a specific operand combination.<br>
     * Defaults to the value that is returned by {@link #getMaxLength()}.<br>
     * This method should be overridden if different length are be to expected
     * from the Mnemonic.<br>
     * If the operation (e.g. the operand combination) are invalid, the method
     * should return the value of {@link #getMaxLength()}.
     *
     * @param codePoint
     *      The position of the mnemonic in the code memory.
     */
    default int getSpecificLength(long codePoint, OperandToken... operands) {
        return getMaxLength();
    }
}
