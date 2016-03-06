package emulator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;
import java.util.Objects;

/**
 * A bit-addressable register with named indexes that is one byte long.
 *
 * @author 5hir0kur0
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ByteFlagRegister extends BitAddressableByteRegister implements FlagRegister {

    @XmlElementWrapper(name = "flags")
    @XmlElement(name = "flag")
    private List<Flag> flags;

    @SuppressWarnings("unused")
    private ByteFlagRegister() { // no-arg constructor for JAXB
        super("missing name");
        this.flags = null;
    }

    /**
     * @param name
     *     the {@code ByteFlagRegister}'s name; must not be {@code null} or empty
     * @param initialValue
     *     the {@code ByteFlagRegister}'s initial value
     * @param flags
     *     the register's {@link Flag}s; must not be {@code null}; the {@code size()} must be 8
     */
    public ByteFlagRegister(String name, byte initialValue, List<Flag> flags) {
        super(name, initialValue);
        if (!(Objects.requireNonNull(flags, "flags must not be null").size() == 8))
            throw new IllegalArgumentException("illegal flag array with length: "+flags.size());
        this.flags = flags;
    }

    /**
     * @param name
     *     the {@code ByteFlagRegister}'s name; must not be {@code null} or empty
     * @param flags
     *     the register's {@link Flag}s; must not be {@code null}; the {@code size()} must be 8
     */
    public ByteFlagRegister(String name, List<Flag> flags) {
        this(name, (byte)0, flags);
    }

    @Override
    public List<Flag> getFlags() {
        return this.flags;
    }
}
