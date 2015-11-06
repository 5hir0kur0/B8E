package emulator;

/**
 * This interface represents Read Only Memory (ROM).
 *
 * @author 5hir0kur0
 */
public interface ROM {

    byte get(int index) throws IndexOutOfBoundsException;

    /**
     * Get a number of {@code Byte}s from memory.
     * Whenever a value does not exist in memory, {@code  null} is written to
     * the respective index in the returned array.
     * @param index The start index.
     * @param length The number of {@code Byte}s to be returned.
     * @return an array of {@code Byte}s (if a value in the array is {@code null} this means that it does not exist in
     *         memory.
     */
    Byte[] get(int index, int length) throws IndexOutOfBoundsException;

    int getSize();
}
