package emulator;

/**
 * This class represents Random Access Memory (RAM)
 *
 * @author Gordian
 */
public class RAM implements ROM {
    protected byte[] memory;
    protected int minIndex;
    protected int maxIndex;

    /**
     * Create a new RAM object.
     *
     * @param size The number of bytes in the created object. Must be bigger than 0.
     * @param minAddress The smallest address that can be accessed. Must be bigger than 0 and smaller than size and
     *                   smaller than maxAddress.
     * @param maxAddress The biggest address that can be accessed. Must be bigger than 0 and bigger than minAddress and
     *                   smaller than size.
     */
    public RAM(int size, int minAddress, int maxAddress) {
        if (size <= 0)
            throw new IllegalArgumentException("Cannot create ROM of size smaller than or equal to 0");
        if (minAddress < 0 || minAddress >= size)
            throw new IllegalArgumentException("The minimal address is out of range");
        if (maxAddress >= size || maxAddress < 0)
            throw new IllegalArgumentException("The maximal address is out of range");
        if (minAddress >= maxAddress)
            throw new IllegalArgumentException("The minimal address must not be smaller than or equal to the maximal"+
                                               "address.");
        this.memory = new byte[size];
        this.minIndex = minAddress;
        this.maxIndex = maxAddress;
    }

    /**
     * Create a new RAM object.
     *
     * @param size The number of bytes in the created object. Must be bigger than 0.
     */
    public RAM(int size) {
        this(size, 0, size - 1);
    }

    @Override
    public byte get(int index) {
        return this.memory[index];
    }

    /**
     * Get a number of {@code Byte}s from memory.
     * Whenever a value does not exist in memory, {@code  null} is written to
     * the respective index in the returned array.
     * @param index The start index.
     * @param length The number of {@code Byte}s to be returned.
     * @return an array of {@code Byte}s (if a value in the array is {@code null} this means that it does not exist in
     *         memory.
     */
    @Override
    public Byte[] get(int index, int length) {
        if (length <= 0)
            throw new IllegalArgumentException("length cannot be smaller than or equal to 0");
        if (index < 0)
            throw new IllegalArgumentException("index must not be smaller than 0");
        Byte[] ret = new Byte[length];
        for (int i = 0; i < ret.length; i++) {
            int tmpIndex = i + index;
            if (tmpIndex < minIndex || tmpIndex > maxIndex) {
                ret[i] = null;
                continue;
            }
            ret[i] = this.memory[tmpIndex];
        }
        return ret;
    }

    public void set(int index, byte value) {
        if (index < this.minIndex || index > this.maxIndex)
            throw new IllegalArgumentException("index out of range: "+index);
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
        if (tmp.minIndex != this.minIndex || tmp.maxIndex != this.maxIndex) return false;
        for (int i = 0; i < this.memory.length; ++i) {
            try {
                if (tmp.memory[i] != this.memory[i]) return false;
            } catch (IndexOutOfBoundsException e) {
                return false;
            }
        }
        return true;
    }
}
