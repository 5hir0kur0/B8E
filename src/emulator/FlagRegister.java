package emulator;

/**
 * A bit-addressable register with named indexes.
 *
 * @author Gordian
 */
public interface FlagRegister extends Register, BitAddressable {
    /**
     * @return the register's flags as {@code Enum}s. The ordinal represents the index and the name is
     *         the symbolic name of the flag.
     */
    Enum[] getFlags();

    /**
     * @param index
     *     the flag's index; must be within the range of the register and bigger than 0
     * @return
     *     the flag's name as a {@code String}
     */
    default String getName(int index) {
        return getFlags()[index].name();
    }
}
