package emulator;

import java.util.Arrays;
import java.util.Iterator;

/**
 * This class represents Random Access Memory (RAM)
 *
 * @author Gordian
 */
public class RAM implements ROM {
    protected byte[] memory;

    /**
     * Create a new {@code RAM} object.
     *
     * @param size
     *     the number of bytes in the created object; must be &gt; 0
     */
    public RAM(int size) {
        if (size <= 0)
            throw new IllegalArgumentException("Cannot create ROM of size smaller than or equal to 0");
        this.memory = new byte[size];
    }

    /**
     * @see ROM#get(int)
     */
    @Override
    public byte get(int index) {
        return this.memory[index];
    }

    /**
     * Get a number of {@code byte}s from memory.
     * @param index
     *     the start index
     * @param length
     *     the number of {@code Byte}s to be returned
     * @return
     *     an array of {@code byte}s
     * @see ROM#get(int,int)
     */
    @Override
    @Deprecated
    public byte[] get(int index, int length) throws IndexOutOfBoundsException {
        if (length <= 0)
            throw new IllegalArgumentException("length cannot be smaller than or equal to 0");
        if (index < 0)
            throw new IndexOutOfBoundsException("index must not be smaller than 0");
        if (index + length >= this.memory.length)
            throw new IndexOutOfBoundsException("index + length must be smaller than the number of bytes stored");
        return Arrays.copyOfRange(this.memory, index, index + length);
    }

    /**
     * @return an {@code Iterator<Byte>} to be used in enhanced for loops; <b>{@code remove()} is not supported</b>
     */
    @Override
    public Iterator<Byte> iterator() {
        return new Iterator<Byte>() {
            int index = 0;
            @Override public boolean hasNext() { return index < RAM.this.memory.length; }
            @Override public Byte    next()    { return RAM.this.memory[index++]; }
        };
    }

    /**
     * Set a new value for an index.
     * @param index
     *     the index to be used; must be &gt;= 0 and &lt; {@code getSize()}
     * @param value
     *     the value the index will be set to
     */
    public void set(int index, byte value) {
        this.memory[index] = value;
    }

    @Override
    public int getSize() {
        return this.memory.length;
    }

    @Override
    public boolean equals(Object other) {
        if (null == other) return false;
        if (this == other) return true;
        if (!(other instanceof RAM)) return false;
        RAM tmp = (RAM)other;
        return Arrays.equals(this.memory, tmp.memory);
    }
}