package emulator;

/**
 * A bit-addressable register with named indexes that is one byte long.
 *
 * @author 5hir0kur0
 */
public abstract class ByteFlagRegister extends BitAddressableByteRegister implements FlagRegister {

    /**
     * @param name
     *     the {@code ByteFlagRegister}'s name; must not be {@code null} or empty
     * @param initialValue
     *     the {@code ByteFlagRegister}'s initial value
     */
    public ByteFlagRegister(String name, byte initialValue) {
        super(name, initialValue);
    }

    /**
     * @param name
     *     the {@code ByteFlagRegister}'s name; must not be {@code null} or empty
     */
    public ByteFlagRegister(String name) {
        this(name, (byte)0);
    }
}
