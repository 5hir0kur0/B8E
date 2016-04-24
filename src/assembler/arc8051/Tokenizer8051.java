package assembler.arc8051;

import assembler.Tokenizer;
import assembler.tokens.LabelToken;
import assembler.tokens.Token;
import static assembler.arc8051.OperandToken8051.OperandType8051;
import assembler.tokens.Tokens;
import assembler.util.assembling.Directive;
import assembler.util.problems.Problem;
import assembler.util.problems.TokenizingProblem;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;

/**
 * A Tokenizer for the 8051 architecture.
 *
 * @author Noxgrim
 */
public class Tokenizer8051 implements Tokenizer {

    /** The current file. */
    private Path file;
    /** The current line in the current file. */
    private int line;

    private List<Token> tokens;
    /** A list of Problems that occurred while tokenizing. */
    private List<Problem> problems;

    /**
     * Constructs a new Tokenizer for the 8051 architecture.
     */
    public Tokenizer8051() {
        line = 0;
        file = null;
        problems = new LinkedList<>();
    }

    /**
     * A list of supported directives.<br>
     * Supported directives:<br>
     * <ul>
     *     <li><code>file</code>: to set the current file</li>
     *     <li><code>line</code>: to set the current line</li>
     * </ul>
     */
    private final Directive[] directives = {
            new Directive("file", 1, 2, true) {
                @Override
                public boolean perform(String... args) {
                    final String fileString = args[0];
                    try {
                        Path newPath = Paths.get(fileString);
                        line = 0;

                        if (args.length > 1)
                            directives[1].perform(args[1], new TokenizingProblem("?", Problem.Type.ERROR, file, line,
                                    null), problems);

                        file = newPath;

                        tokens.add(new DirectiveTokens.FileChangeToken(file, line)); // Tell assembler to change the
                                                                                     // file as well

                        return true;
                    } catch (InvalidPathException e) {
                        problems.add(new TokenizingProblem("Invalid Path!", Problem.Type.ERROR,
                                file, line, fileString));
                        return false;
                    }
                }
            },

            new Directive("line") {
                @Override
                public boolean perform(String... args) {
                    String number = args[0];
                    try {

                        final boolean relative = number.charAt(0) == '+' || number.charAt(0) == '-';

                        int newLine = Integer.parseInt(number);

                        if (relative) {
                            if (line - newLine < 1) {
                                line = 0;
                                problems.add(new TokenizingProblem("Resulting line number cannot be smaller than 1!",
                                        Problem.Type.ERROR, file, line, number));
                                return false;
                            } else
                                line += --newLine;
                        } else {
                            if (newLine < 1) {
                                problems.add(new TokenizingProblem("New line number cannot be smaller than 1!",
                                        Problem.Type.ERROR, file, line, number));
                                return false;
                            } else
                                line = --newLine;
                        }

                        return true;

                    } catch (NumberFormatException e) {
                        problems.add(new TokenizingProblem("Illegal number format!", Problem.Type.ERROR,
                                file, line, number));
                        return false;
                    }
                }
            },

            new Directive("db", 1, Integer.MAX_VALUE, Directive.DEFAULT_QUOTE_CHARS, true) {
                @Override
                protected boolean perform(String[] args) {

                    byte[] data = new byte[args.length];

                    for (int i = 0; i < args.length; ++i)
                        try {
                            data[i] = Byte.parseByte(args[i]);
                        } catch (NumberFormatException e) {
                            problems.add(new TokenizingProblem("Illegal number format!", Problem.Type.ERROR,
                                    file, line, args[i]));
                            data[i] = 0;
                        }

                    tokens.add(new DirectiveTokens.DataToken(data, line));

                    return true;
                }
            },

            new Directive("org", true) {
                @Override
                protected boolean perform(String[] args) {

                    try {
                        tokens.add(new DirectiveTokens.OrganisationToken(Long.parseLong(args[0]), line));
                    } catch (NumberFormatException e) {
                        problems.add(new TokenizingProblem("Illegal number format!", Problem.Type.ERROR,
                                file, line, args[1]));
                        return false;
                    }

                    return true;
                }
            }
    };


    @Override
    public List<Token> tokenize(List<String> input, List<Problem> problems) {
        tokens = new LinkedList<>();
        this.problems.clear();

        file = null;
        line = 0;

        // Handle directives
        for (String s : input) {
            line++;
            Scanner scanner = new Scanner(s);
            while (scanner.hasNextLine()) {
                StringBuilder lineString = new StringBuilder(scanner.nextLine());
                {
                    Matcher m = MC8051Library.DIRECTIVE_PATTERN.matcher(lineString);
                    if (m.matches()) {
                        String name = m.group(1);

                        String args = m.group(2) == null ? "" : m.group(2);

                        for (Directive d : directives) {
                            if (d.getName().equalsIgnoreCase(name)) {
                                d.perform(args, new TokenizingProblem("?", Problem.Type.ERROR, file, line, null), problems);

                                break;
                            }
                        }
                        continue;
                    }
                }

                Token token;

                while (!lineString.toString().trim().isEmpty()) {
                    if ((token = findMnemonic(lineString)) != null) {
                        tokens.add(token);
                        if (token.getType() == Token.TokenType.MNEMONIC_NAME)
                            break;
                    }
                }
            }

        }

        problems.addAll(this.problems);
        return tokens;
    }

    private Token findMnemonic(StringBuilder line) {
        int length = 0;
        Token result = null;
        StringBuilder symbol = new StringBuilder();

        final int leadingWhiteSpace  = 0;
        final int inSymbol = 1;
        final int findSuffix = 2;
        final int trailingWhiteSpace = 3;

        int state = leadingWhiteSpace;
        boolean error = false;
        boolean isLabel = false;

        outer:
        for (int cp : line.codePoints().toArray()) {
            ++length;
            switch (state) {
                case leadingWhiteSpace:
                {
                    if (Character.isWhitespace(cp))
                        continue;
                    else {
                        if (Character.isLetterOrDigit(cp)) {
                            if (Character.isDigit(cp)) {
                                problems.add(new TokenizingProblem("The first character of a instruction or label must" +
                                        " not be a digit!",
                                        Problem.Type.ERROR, file, this.line, String.valueOf(Character.toChars(cp))));
                                error = true;
                            } else if (Character.isLetter(cp))
                                symbol.appendCodePoint(cp);
                        } else {
                            problems.add(new TokenizingProblem("Expected a valid letter as the start of a instruction " +
                                    "or label!",
                                    Problem.Type.ERROR, file, this.line, String.valueOf(Character.toChars(cp))));
                            error = true;
                        }
                        state = inSymbol;
                    }
                    break;
                }
                case inSymbol:
                {
                    if (Character.isLetterOrDigit(cp))
                        symbol.appendCodePoint(cp);
                    else if (Character.isWhitespace(cp))
                        state = findSuffix;
                    else if (cp == ':') {
                        isLabel = true;
                        state = trailingWhiteSpace;
                    } else {
                        problems.add(new TokenizingProblem("Unexpected character after symbol! Expected a colon ':'.",
                                Problem.Type.ERROR, file, this.line, String.valueOf(Character.toChars(cp))));
                        error = true;
                        state = trailingWhiteSpace;
                    }
                    break;
                }
                case findSuffix:
                {
                    if (Character.isWhitespace(cp))
                        continue;
                    else if (cp == ':') {
                        isLabel = true;
                        state = trailingWhiteSpace;
                    } else
                        break outer;
                    break;
                }
                case trailingWhiteSpace:
                {
                    if (!Character.isWhitespace(cp))
                        break outer;
                }
                default:
                    throw new IllegalStateException("'findMnemonic()' is in an illegal state: "+state)
            }
        }

        if (!error)
            if (isLabel)
                result = new LabelToken(symbol.toString(), this.line);
            else
                result = new Tokens.MnemonicNameToken(symbol.toString(), this.line);
        line.delete(0, length);
        return result;
    }


    private String findToken(final String input, Token.TokenType ... espected) {
        StringBuilder buffer = new StringBuilder(input.length());
        OperandType8051 type = null;


        for (int cp : input.codePoints().toArray()) {
            if (Character.isWhitespace(cp))
                continue;
            else if (true /* */)



        }
    }

    /**
     * Tries to generate a valid Token from a given String.<br>
     * Patters from {@link MC8051Library} will be used to determine the token type
     * and then the values will be validated and a the resulting Token will be added
     * to a given List.<br>
     * <br>
     * Possible sources of Problems:
     * <ul>
     *      <li>ERROR: the TokenType of the String could not be recognized.</li>
     *      <li>ERROR: the format of a number is wrong (illegal digits were used).</li>
     *      <li>ERROR: the value of a number is out of bounds (negative or bigger than
     *                 <code>0xFF/0xFFFF</code>).</li>
     *      <li>ERROR: a bit addressed byte is not bit addressable.</li>
     * </ul>
     *
     * @param string
     *      the String that should be analysed.
     * @param add
     *      the List the resulting Token will be added to.
     *
     * @return
     *      <code>true</code> if this method was successful, <code>false</code> otherwise.
     */
    private boolean addToken(String string, List<Token> add) {
        string = string.trim();
        Matcher m;
        if ((m = MC8051Library.CONSTANT_PATTERN.matcher(string)).matches()) {
            String val = m.group();
            if (val != null && testBounds(0, 0xFFFF, Integer.parseInt(val), "constant", m.group(0))) {
                add.add(new OperandToken8051(OperandToken8051.OperandType8051.CONSTANT, val, line));
                return true;
            }
        } else if ((m = MC8051Library.BIT_ADDRESSING_PATTERN.matcher(string)).matches()) {
            Matcher byteMatcher = MC8051Library.NUMBER_PATTERN.matcher(m.group(1));
            final int bitGroup = 1+byteMatcher.groupCount();
            Matcher bitMatcher  = MC8051Library.NUMBER_PATTERN.matcher(m.group(bitGroup));
            if (!(byteMatcher.matches() && bitMatcher.matches()))
                return false;
            String byteAddr = byteMatcher.group();
            String bitAddr  = bitMatcher.group();
            int result;
            if (byteAddr != null && testBounds(0, 0xFF, Integer.parseInt(byteAddr), "bit address", m.group(1))
                & bitAddr != null && testBounds(0, 7, Integer.parseInt(bitAddr), "bit number",
                    m.group(bitGroup))) {
                int intVal = Integer.parseInt(byteAddr);
                result = intVal + Integer.parseInt(bitAddr);
                if (intVal >= 0x20 && intVal < 0x30)
                    result = (intVal - 0x20) * 8 + Integer.parseInt(bitAddr);
                else if (intVal < 0x80 || (0x07 & intVal) != 0) {
                    problems.add(new TokenizingProblem("Byte is not bit addressable!", Problem.Type.ERROR,
                            m.group(1)));
                    return false;
                }
                add.add(new OperandToken8051(OperandToken8051.OperandType8051.ADDRESS, Integer.toString(result), line));
                return true;
            }
        } else if ((m = MC8051Library.ADDRESS_PATTERN.matcher(string)).matches()) {
            String val = m.group();
            if (val != null && testBounds(0, 0xFF, Integer.parseInt(val), "address", m.group(0))) {
                add.add(new OperandToken8051(OperandToken8051.OperandType8051.ADDRESS, val, line));
                return true;
            }
        } else if ((m = MC8051Library.NEGATED_ADDRESS_PATTERN.matcher(string)).matches()) {
            String val = m.group();
            if (val != null && testBounds(0, 0xFF, Integer.parseInt(val), "address", m.group(0))) {
                add.add(new OperandToken8051(OperandToken8051.OperandType8051.NEGATED_ADDRESS, val, line));
                return true;
            }

        } else if ((m = MC8051Library.ADDRESS_OFFSET_PATTERN.matcher(string)).matches()) {
            String val = m.group();
            if (val != null && testBounds(0, 0xFFFF, Integer.parseInt(val), "relative offset", m.group(0))) {
                add.add(new OperandToken8051(OperandToken8051.OperandType8051.ADDRESS_OFFSET,
                        (m.group(0).charAt(0) == '-' ? '-' : "") + val, line));
                return true;
            }

        } else if ((m = MC8051Library.SYMBOL_PATTERN.matcher(string)).matches()) {
            String val = m.group(1).replaceAll("\\s", "");
            for (String reserved : MC8051Library.RESERVED_NAMES)
                if (val.equalsIgnoreCase(reserved)) {
                    add.add(new OperandToken8051(OperandToken8051.OperandType8051.NAME, val, line));
                    return true;
                }
            add.add(new Tokens.SymbolToken(val, line));
            return true;
        } else if ((m = MC8051Library.SYMBOL_INDIRECT_PATTERN.matcher(string)).matches()) {
            add.add(new OperandToken8051(OperandToken8051.OperandType8051.INDIRECT,
                    m.group(1).replaceAll("\\s", ""), line));
            return true;
        } else
            problems.add(new TokenizingProblem("Could not recognize Token Type!", Problem.Type.ERROR,
                    file, line, string));
        return false;
    }


    /**
     * Tests if a number lies in specified range and adds a problem to a given
     * List if it is out of bounds.<br>
     * <br>
     * Possible sources of Problems:
     * <ul>
     *      <li>ERROR: the number is smaller than the specified minimum.</li>
     *      <li>ERROR: the number is bigger than the specified maximum.</li>
     * </ul>
     *
     * @param min
     *      the minimal value the number can have (inclusive).
     * @param max
     *      the maximal value the number can have (inclusive).
     * @param value
     *      the actual value of the number.
     * @param type
     *      the type of the number (e.g. "address").
     * @param cause
     *      the cause of the number used for the creation of the Problem.
     *
     * @return
     *      <code>true</code> if the value lies within the bounds.
     */
    private boolean testBounds(final int min, final int max, final int value,
                               final String type, String cause) {
        if (value < min) {
            problems.add(new TokenizingProblem("Value of " + type + " to small! (Minimal value: " + min + ")",
                    Problem.Type.ERROR, file, line, cause));
            return false;
        } else if (value > max) {
            problems.add(new TokenizingProblem("Value of " + type + " to big! (Maximal value: " + max + ")",
                    Problem.Type.ERROR, file, line, cause));
            return false;
        } else
            return true;
    }
}
