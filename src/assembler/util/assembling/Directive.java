package assembler.util.assembling;

import assembler.util.problems.Problem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Assembler directive.
 *
 * @author Jannik
 */
public abstract class Directive {

    /** The name of the directive. */
    private final String name;
    /**
     * Whether this directive should fall through to the next
     * assembling level.
     */
    private final boolean fallthrough;

    /** The number of required arguments. */
    private final int minArgs;
    /** The maximum number of required arguments. */
    private final int maxArgs;
    /** The chars that are used to indicate quotation. */
    private final String[] quoteChars;

    /** The characters that are used for quoting if not specified otherwise. */
    public static final String[] DEFAULT_QUOTE_CHARS = {"x\"\"", "x''"};

    /**
     * Constructs a new Directive.
     *
     * @param name
     *      the name of the Directive.
     * @param minArgs
     *      the number of required arguments
     * @param maxArgs
     *      the maximum number of required arguments
     * @param argQuote
     *      defines the characters that can be used
     *      to quote arguments.<br>
     *      Each String in the String array represents a character pair.<br>
     *      The first character of the String specifies whether the quote characters
     *      should be (i)ncluded or e(x)cluded from the resulting argument.
     *      (Valid chars: "ix")<br>
     *      The second and third character represent the first and second character of
     *      the character pair respectively.
     * @param fallthrough
     *      marks this directive as 'fall through' that
     *      means that it should be passed to the the next
     *      level of assembling.<br>
     *      A directive should not fall through their
     *      <code>perform()</code> method returned
     *      <code>false</code>.
     */
    public Directive(String name, int minArgs, int maxArgs, String[] argQuote, boolean fallthrough) {
        if ((this.name = Objects.requireNonNull(name, "Name cannot be 'null'.").toLowerCase()).trim().isEmpty())
            throw new IllegalArgumentException("Name cannot be empty or white space only.");
        this.fallthrough = fallthrough;
        if ((this.minArgs = minArgs) < 0)
            throw new IllegalArgumentException("Minimum number of arguments cannot be negative!");
        if ((this.maxArgs = maxArgs) < 0)
            throw new IllegalArgumentException("Maximum number of arguments cannot be negative!");
        if (minArgs > maxArgs)
            throw new IllegalArgumentException(
                    "Minimum number of arguments must not be bigger than maximum number of arguments!");

        for (String quotePair : argQuote)
            if (quotePair.toCharArray().length != 3) // Code point are not supported
                throw new IllegalArgumentException("Quote pair String is not 3 characters long.");
        else if (quotePair.charAt(0) != 'x' && quotePair.charAt(0) != 'i')
                throw new IllegalArgumentException("First character is neither 'i' nor 'x'!");
        quoteChars = argQuote;
    }


    /**
     * Constructs a new Directive that does not fall
     * through to the next assembling level.
     *
     * @param name
     *      the name of the Directive.
     * @param minArgs
     *      the number of required arguments
     * @param maxArgs
     *      the maximum number of required arguments
     */
    public Directive(String name, int minArgs, int maxArgs) {
        this(name, minArgs, maxArgs, DEFAULT_QUOTE_CHARS, false);
    }

    /**
     * Constructs a new Directive.
     *
     * @param name
     *      the name of the Directive.
     * @param minArgs
     *      the number of required arguments
     * @param maxArgs
     *      the maximum number of required arguments
     * @param fallthrough
     *      marks this directive as 'fall through' that
     *      means that it should be passed to the the next
     *      level of assembling.<br>
     *      A directive should not fall through their
     *      <code>perform()</code> method returned
     *      <code>false</code>.
     */
    public Directive(String name, int minArgs, int maxArgs, boolean fallthrough) {
        this(name, minArgs, maxArgs, DEFAULT_QUOTE_CHARS, fallthrough);
    }
    /**
     * Constructs a new Directive that expects only one
     * argument.
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
     */
    public Directive(String name, boolean fallthrough) {
        this(name, 1, 1, DEFAULT_QUOTE_CHARS, fallthrough);
    }

    /**
     * Constructs a new Directive that does not fall
     * through to the next assembling level and expects
     * only one argument.
     *
     * @param name
     *      the name of the Directive.
     */
    public Directive(String name) {
        this(name, 1, 1, DEFAULT_QUOTE_CHARS, false);
    }

    /**
     * Constructs a new Directive that does not fall
     * through to the next assembling level.<br>
     * It can cope wih any number of arguments as long
     * as the minimum number of arguments is given.
     *
     * @param name
     *      the name of the Directive.
     * @param minArgs
     *      the maximum number of required arguments
     */
    public Directive(String name, int minArgs) {
        this(name, minArgs, Integer.MAX_VALUE, DEFAULT_QUOTE_CHARS, false);
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
    public boolean perform(final String args, Problem<String> p, List<Problem> problems) {
        Objects.requireNonNull(args, "Argument String cannot be 'null'!");
        Objects.requireNonNull(p, "Problem cannot be 'null'!");
        Objects.requireNonNull(problems, "Problem List cannot be 'null'!");

        String[] arguments = extractArguments(args, this.quoteChars);

        if (arguments.length < minArgs) {
            p.setMessage("Expected at least "+minArgs+" argument" + (minArgs == 1 ? "s" : "" ) + " for '" + name +
                    "' directive!");
            p.setType(Problem.Type.ERROR);
            p.setCause(arguments.length == 0 ? null : Arrays.toString(arguments));
            problems.add(p);
            return false;
        }

        if (!perform(arguments)) return false;

        if (arguments.length > maxArgs) {
            p.setMessage("Too many arguments for '" + name + "' directive!");
            p.setType(Problem.Type.WARNING);
            p.setCause(Arrays.toString(Arrays.copyOfRange(arguments, maxArgs, arguments.length)));
            problems.add(p);
        }

        return true;
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
    protected abstract boolean perform(final String[] args);

    /**
     * Converts a String to a String-array of arguments that can be
     * processed further.<br>
     * Everything that is divided by white space will be extracted in
     * unique arguments except if it surrounded by one of the specified
     * quote character pairs.<br>
     * A missing quote character will be added if the quote is unclosed
     * at the end of the String if the character pair allows that.<br>
     * Every character after a backslash will be escaped and the backslash
     * wont be added to the resulting argument.
     *
     * @param args
     *      the String that should be used.
     * @param quoteChars
     *      the character pairs that indicate quotation.<br>
     *      In every String the first character indicates whether the
     *      quote character will be (i)ncluded in the resulting argument
     *      or e(x)cluded from it.<br>
     *      The second and third character represent the first and second
     *      character of the character pair.
     *
     * @return
     *      an array of arguments.
     */
    private static String[] extractArguments(final String args, final String[] quoteChars) {
        StringBuilder argument = new StringBuilder();
        List<String> arguments = new ArrayList<>();

        int lastCP = 0;
        int quoteIndex = -1;
        boolean quoted = false;
        outer:
        for (int cp : args.codePoints().toArray()) {
            if (lastCP == '\\')
                argument.appendCodePoint(cp);
            else {
                if (quoteIndex == -1) {
                    for (quoteIndex = 0; quoteIndex < quoteChars.length; ++quoteIndex) {
                        final String charPair = quoteChars[quoteIndex];
                        if (charPair.charAt(1) == cp) {
                            if (!quoted) {
                                quoted = true;
                                if (charPair.charAt(0) == 'i')
                                    argument.appendCodePoint(cp);
                            } else
                                argument.appendCodePoint(cp); // Just to be sure.
                            continue outer;
                        }
                    }
                    quoteIndex = -1;
                } else {
                    final String charPair = quoteChars[quoteIndex];
                    if (charPair.charAt(2) == cp) {
                        quoted = false;
                        if (charPair.charAt(0) == 'i')
                            argument.appendCodePoint(cp);
                        quoteIndex = -1;
                        continue;
                    }
                }
                // Detect white space.
                if (Character.isWhitespace(cp) && !quoted) {
                    if (!Character.isWhitespace(lastCP)) {
                        arguments.add(argument.toString());
                        argument.setLength(0);
                    }
                } else
                    argument.appendCodePoint(cp);
            }
            lastCP = cp;
        }

        if (quoteIndex != -1)
            if (quoteChars[quoteIndex].charAt(0) == 'i')
                argument.append(quoteChars[quoteIndex].charAt(2));
        if (argument.length() != 0)
            arguments.add(argument.toString());

        return arguments.toArray(new String[arguments.size()]);
    }
}
