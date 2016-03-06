package emulator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a flag name of a CPU register.
 * @author Gordian
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Flag {
    @XmlAttribute public final String name;
    @XmlAttribute public final int index;

    @SuppressWarnings("unused")
    private Flag() { // no-arg constructor for JAXB
        this.name = null;
        this.index = -42;
    }

    Flag(String name, int index) {
        if (Objects.requireNonNull("flag name must not be null").trim().isEmpty())
            throw new IllegalArgumentException("flag name must not be empty");
        if (index < 0)
            throw new IllegalArgumentException("flag index must not be smaller than 0");
        this.name = name;
        this.index = index;
    }

    public static List<Flag> fromNames(String... names) {
        if (Objects.requireNonNull(names, "flag names must not be null").length < 1)
            throw new IllegalArgumentException("there must be at least one flag");
        List<Flag> result = new ArrayList<>(names.length);
        int index = 0;
        for (String name : names) result.add(new Flag(name, index++));
        return Collections.unmodifiableList(result);
    }
}
