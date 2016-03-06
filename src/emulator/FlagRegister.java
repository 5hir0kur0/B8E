package emulator;

import java.util.List;

/**
 * A bit-addressable register with named indexes.
 *
 * @author 5hir0kur0
 */
public interface FlagRegister extends Register, BitAddressable {
    /**
     * @return the register's flags as {@code Enum}s. The ordinal represents the index and the name is
     *         the symbolic name of the flag.
     */
    List<Flag> getFlags();

    /**
     * @param index
     *     the flag's index; must be within the range of the register and bigger than 0
     * @return
     *     the flag's name as a {@code String}
     */
    @Deprecated
    default String getName(int index) {
        return getFlags().get(index).name;
    }
}
