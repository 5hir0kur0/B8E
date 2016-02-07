package emulator;

/**
 * Interface that enables single bits of {@code Register}s to be set or received.
 *
 * @author 5hir0kur0
 */
public interface BitAddressable {
    /**
     * Get the value of a single bit.
     * @param index
     *     the bit's index; cannot be smaller than 0 and must be within the range of the register-type
     * @return
     *     the bit's value (1 -> {@code true}; 0 -> {@code false})
     * @throws IndexOutOfBoundsException
     *     if {@code index} is smaller than 0 or larger than the register's number of bits
     */
    boolean getBit(int index) throws IndexOutOfBoundsException;

    /**
     * Set the value of a single bit.
     * @param index
     *     the bit's index; cannot be smaller than 0 and must be within the range of the register-type
     * @param value
     *     the bit's new value ({@code true} -> 1; {@code false} -> 0)
     * @throws IndexOutOfBoundsException
     *     if {@code index} is smaller than 0 or larger than the register's number of bits
     */
    void setBit(boolean value, int index) throws IndexOutOfBoundsException;
}
