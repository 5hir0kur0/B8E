package emulator;

/**
 * This interface represents Read Only Memory (ROM).
 *
 * @author Gordian
 */
public interface ROM {

    byte get(int index);

    /**
     * Get a number of {@code Byte}s from memory.
     * Whenever a value does not exist in memory, {@code  null} is written to
     * the respective index in the returned array.
     * @param index The start index.
     * @param length The number of {@code Byte}s to be returned.
     * @return an array of {@code Byte}s (if a value in the array is {@code null} this means that it does not exist in
     *         memory.
     */
    Byte[] get(int index, int length);

    int getMinAddress();

    int getMaxAddress();

    int getSize();

    /**
     * @param address The smallest address that can be accessed. Must be bigger than 0 and smaller than size and
     *                smaller than maxAddress.
     */
    void setMinAddress(int address);

    /**
     * @param address The biggest address that can be accessed. Must be bigger than 0 and bigger than minAddress and
     *                smaller than size.
     */
    void setMaxAddress(int address);
}
