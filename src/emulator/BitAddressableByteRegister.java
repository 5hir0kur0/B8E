package emulator;

/**
 * This class represents a bit addressable {@code Register} (such as e.g. the accumulator)
 *
 * @author Gordian
 */
public class BitAddressableByteRegister extends ByteRegister implements BitAddressable {

    @SuppressWarnings("unused")
    private BitAddressableByteRegister() { // no-arg constructor for JAXB
        super("missing name");
    }

    /**
     * @param name
     *     the {@code ByteRegister}'s name; must not be {@code null} or empty
     * @param initialValue
     *     the {@code ByteRegister}'s initial value
     */
    public BitAddressableByteRegister(String name, byte initialValue) {
        super(name, initialValue);
    }

    /**
     * @param name the {@code ByteRegister}'s name; must not be {@code null} or empty
     */
    public BitAddressableByteRegister(String name) {
        this(name, (byte)0);
    }

    @Override
    public boolean getBit(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index > 7) throw new IndexOutOfBoundsException("Index of out of range (getBit()).");
        return (this.getValue() & 1 << index) != 0;
    }

    @Override
    public void setBit(boolean newValue, int index) throws IndexOutOfBoundsException {
        if (index < 0 || index > 7) throw new IndexOutOfBoundsException("Index of out of range (setBit()).");
        this.setValue((byte)(newValue ? this.getValue() & 0xFF | 1 << index : this.getValue() & ~(1 << index)));
    }
}
