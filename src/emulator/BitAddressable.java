package emulator;

/**
 * Interface that enables single bits of {@code Register}s to be set or received.
 *
 * @author Gordian
 */
public interface BitAddressable {
    /**
     * Get the value of a single bit.
     * @param index The bit's index. Cannot be smaller than 0 and must be within the range of the register-type.
     * @return The bit's value (1 -> true; 0 -> false).
     * @throws IndexOutOfBoundsException If {@code index} is smaller than 0 or larger than the register's number of
     *     bits.
     */
    boolean getBit(int index) throws IndexOutOfBoundsException;

    /**
     * Set the value of a single bit.
     * @param index The bit's index. Cannot be smaller than 0 and must be within the range of the register-type.
     * @param value The bit's new value. (true -> 1; false -> 0)
     * @throws IndexOutOfBoundsException If {@code index} is smaller than 0 or larger than the register's number of
     *     bits.
     */
    void setBit(boolean value, int index) throws IndexOutOfBoundsException;
}
