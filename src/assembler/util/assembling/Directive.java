package assembler.util.assembling;

import java.util.ArrayList;
import java.util.List;
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
     * Whether this directive should fall through to the next
     * assembling level.
     */
    private boolean fallthrough;

    /**
     * Constructs a new Directive.
     *
     * @param name
     *      the name of the Directive.
     * @param fallthrough
     *      marks this directive as 'fall through' that
     *      means that it should be passed to the the next
     *      level of assembling.<br>
     *      A directive should not fall through their
     *      <code>perform()</code> method returned
     *      <code>false</code>.
     *
     */
    public Directive(String name, boolean fallthrough) {
        if ((this.name = Objects.requireNonNull(name, "Name cannot be 'null'.")).trim().isEmpty())
            throw new IllegalArgumentException("Name cannot be empty or white space only.");
        this.fallthrough = fallthrough;
    }

    /**
     * Constructs a new Directive that does not fall
     * through to the next assembling level.
     *
     * @param name
     *      the name of the Directive.
     */
    public Directive(String name) {
        this(name, false);
    }

    /**
     * Returns the name of the directive.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether this directive is 'fall through' that
     * means that it should be passed to the the next
     * level of assembling.<br>
     * A directive should not fall through their
     * <code>perform()</code> method returned
     * <code>false</code>.
     */
    public boolean isFallthrough() {
        return fallthrough;
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
    public abstract boolean perform(final String ... args);

    public static String[] extractArguments(String args) {
        StringBuilder argument = new StringBuilder();
        List<String> arguments = new ArrayList<>();

        int lastCP = 0;
        boolean bracketEsc = false, quoteEsc = false;
        for (int cp : args.chars().toArray()) {
            if (lastCP == '\\')
                argument.appendCodePoint(cp);
            else if (cp == '<') {
                    argument.appendCodePoint(cp);
                if (!bracketEsc && !quoteEsc)
                    bracketEsc = true;
            } else if (cp == '>') {
                argument.appendCodePoint(cp);
                if (bracketEsc)
                    bracketEsc = false;
            } else if (cp == '"')
                if (!bracketEsc)
                    quoteEsc = !quoteEsc;
                else
                    argument.appendCodePoint(cp);
            else if (Character.isWhitespace(cp) && !bracketEsc && !quoteEsc) {
                if (!Character.isWhitespace(lastCP)) {
                    arguments.add(argument.toString());
                    argument.setLength(0);
                }
            } else
                argument.appendCodePoint(cp);
            lastCP = cp;
        }

        if (bracketEsc)
            argument.append('>');
        if (argument.length() != 0)
            arguments.add(argument.toString());

        return arguments.toArray(new String[arguments.size()]);
    }
}
