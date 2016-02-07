package emulator;

/**
 * This interface represents Read Only Memory (ROM).
 *
 * @author Gordian
 */
public interface ROM extends Iterable<Byte> {

    /**
     * @param index
     *     the index from which a {@code byte} will be retrieved
     * @return
     *     the byte at {@code index}
     * @throws IndexOutOfBoundsException
     *     if {@code index} &lt; 0 or {@code index} &gt;= {@code getSize()}
     */
    byte get(int index) throws IndexOutOfBoundsException;

    /**
     * Get a number of {@code byte}s from memory.<br>
     * NOTE: This method usually creates a copy of the desired part of the internal array.
     * Use {@code iterator} instead if you can.
     * @param index
     *     the start index; must be &gt;= 0 and &lt; {@code getSize()}
     * @param length
     *     the number of {@code byte}s to be returned; {@code index} + {@code length} must be &lt;= {@code getSize()}
     * @return
     *     an array of {@code byte}s
     * @throws IndexOutOfBoundsException
     *     if {@code index} &lt; 0 or {@code index} + {@code length} &gt; {@code getSize()}
     */
    @Deprecated byte[] get(int index, int length) throws IndexOutOfBoundsException;

    /**
     * @return the number of bytes held by the {@code ROM} object
     */
    int getSize();

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     * @param other
     *     the other object
     * @return
     *     {@code true} if the objects are equal
     */
    boolean equals(Object other);
}
