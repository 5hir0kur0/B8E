package emulator;

/**
 * This class represents Random Access Memory (RAM)
 *
 * @author 5hir0kur0
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
    RAM(int size, int minAddress, int maxAddress) {
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
    RAM(int size) {
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

    @Override
    public int getMinAddress() {
        return this.minIndex;
    }

    @Override
    public int getMaxAddress() {
        return this.maxIndex;
    }

    @Override
    public void setMinAddress(int address) {
        if (address < 0 || address >= this.maxIndex || address >= this.memory.length)
            throw new IllegalArgumentException("address out of range");
        this.minIndex = address;
    }

    @Override
    public void setMaxAddress(int address) {
        if (address >= this.memory.length || address <= this.minIndex || address < 0)
            throw new IllegalArgumentException("address out of range");
        this.maxIndex = address;
    }

    public void set(int index, byte value) {
        if (index < this.minIndex || index > this.maxIndex)
            throw new IllegalArgumentException("index out of range");
        this.memory[index] = value;
    }

    @Override
    public int getSize() {
        return this.memory.length;
    }
}
