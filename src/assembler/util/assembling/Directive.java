package assembler.util.assembling;

import java.util.Objects;

/**
 * Represents a Assembler directive.
 *
 * @author Jannik
 */
public abstract class Directive {

    /** The name of the directive. */
    private String name;

    /**
     * Constructs a new Directive.
     *
     * @param name
     *      the name of the Directive.
     */
    public Directive(String name) {
        if ((this.name = Objects.requireNonNull(name, "Name cannot be 'null'.")).trim().isEmpty())
            throw new IllegalArgumentException("Name cannot be empty or white space only.");
    }

    /**
     * Returns the name of the directive.
     */
    public String getName() {
        return name;
    }

    /**
     * Performs the directive.
     *
     * @param args
     *      the arguments of the directive.
     *
     * @return
     *      whether the directive was performed
     *      successfully.
     */
    public abstract boolean perform(String ... args);
}
