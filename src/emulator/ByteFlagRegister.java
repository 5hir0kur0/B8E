package emulator;

/**
 * A bit-addressable register with named indexes that is one byte long.
 *
 * @author Gordian
 */
public abstract class ByteFlagRegister extends BitAddressableByteRegister implements FlagRegister {

    /**
     * @param name the {@code ByteFlagRegister}'s name. Must not be {@code null} or empty.
     * @param initialValue the {@code ByteFlagRegister}'s initial value. All values are allowed.
     */
    public ByteFlagRegister(String name, byte initialValue) {
        super(name, initialValue);
    }

    /**
     * @param name the {@code ByteFlagRegister}'s name. Must not be {@code null} or empty.
     */
    public ByteFlagRegister(String name) {
        this(name, (byte)0);
    }

    @Override
    abstract public Enum[] getFlags();
}
