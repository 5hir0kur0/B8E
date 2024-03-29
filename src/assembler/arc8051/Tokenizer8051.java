package assembler.arc8051;

import assembler.Tokenizer;
import assembler.tokens.LabelToken;
import assembler.tokens.OperandToken;
import assembler.tokens.Token;
import assembler.tokens.Tokens;
import assembler.util.assembling.Directive;
import assembler.util.problems.Problem;
import assembler.util.problems.TokenizingProblem;
import misc.Logger;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;

import static assembler.arc8051.OperandToken8051.OperandRepresentation8051;
import static assembler.arc8051.OperandToken8051.OperandType8051;

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
    private List<Problem<?>> problems;

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
                        tokens.add(new DirectiveTokens.OriginChangeToken(Long.parseLong(args[0]), line));
                    } catch (NumberFormatException e) {
                        problems.add(new TokenizingProblem("Illegal number format!", Problem.Type.ERROR,
                                file, line, args[0]));
                        return false;
                    }

                    return true;
                }
            }
    };


    @Override
    public List<Token> tokenize(List<String> input, List<Problem<?>> problems) {
        Logger.log("Start tokenizer…", Tokenizer.class, Logger.LogLevel.INFO);
        tokens = new LinkedList<>();
        this.problems.clear();

        file = null;
        line = 0;

        // Handle directives
        for (String s : input) {
            line++;
            Scanner scanner = new Scanner(s);
            for (int id = 0; scanner.hasNextLine(); ++id) {
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
                int tokenStartPos = 1, lastLineLength = lineString.length();

                while (!lineString.toString().trim().isEmpty()) {
                    if ((token = findLabelOrMnemonic(lineString, tokenStartPos)) != null) {
                        token.setInstructionId(id);
                        tokens.add(token);
                        tokenStartPos += lastLineLength - (lastLineLength = lineString.length());
                        if (token.getType() == Token.TokenType.MNEMONIC_NAME)
                            break;
                    }
                }
                while (!lineString.toString().trim().isEmpty()) {
                    if ((token = findOperandToken(lineString, tokenStartPos)) != null) {
                        token.setInstructionId(id);
                        tokens.add(token);
                        tokenStartPos += lastLineLength - (lastLineLength = lineString.length());
                    }
                }
            }

        }

        problems.addAll(this.problems);
        Logger.log("Tokenizing finished. Found " + tokens.size() + " token" + (tokens.size() == 1 ? "" : "s") + ".",
                Tokenizer.class, Logger.LogLevel.INFO);
        return tokens;
    }

    private Token findLabelOrMnemonic(StringBuilder line, final int pos) {
        int length = -1;
        Token result = null;
        StringBuilder symbol = new StringBuilder();

        final int leadingWhiteSpace  = 0;
        final int inSymbol           = 1;
        final int findSuffix         = 2;
        final int trailingWhiteSpace = 3;

        int state = leadingWhiteSpace;
        boolean fine = true;
        boolean isLabel = false;

        outer:
        {
            for (int cp : line.codePoints().toArray()) {
                ++length;
                switch (state) {
                    case leadingWhiteSpace: {
                        if (Character.isWhitespace(cp))
                            continue;
                        else {
                            if (Character.isLetterOrDigit(cp) || cp == '_') {
                                if (Character.isDigit(cp)) {
                                    problems.add(new TokenizingProblem("The first character of a instruction or label must" +
                                            " not be a digit!",
                                            Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                            Character.toChars(cp)) + "' at column " + (pos + length)));
                                    fine = false;
                                } else if (Character.isLetter(cp) || cp == '_') {
                                    symbol.appendCodePoint(cp);
                                }
                                state = inSymbol;
                            } else {
                                problems.add(new TokenizingProblem("Expected a valid letter as the start of a instruction " +
                                        "or label!",
                                        Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                        Character.toChars(cp)) + "' at column " + (pos + length)));
                                fine = false;
                                state = trailingWhiteSpace;
                            }
                        }
                        break;
                    }
                    case inSymbol: {
                        if (Character.isLetterOrDigit(cp) || cp == '_')
                            symbol.appendCodePoint(cp);
                        else if (Character.isWhitespace(cp))
                            state = findSuffix;
                        else if (cp == ':') {
                            isLabel = true;
                            state = trailingWhiteSpace;
                        } else {
                            problems.add(new TokenizingProblem("Unexpected character after symbol! Expected a colon ':'.",
                                    Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                    Character.toChars(cp)) + "' at column " + (pos + length)));
                            fine = false;
                            state = trailingWhiteSpace;
                        }
                        break;
                    }
                    case findSuffix: {
                        if (Character.isWhitespace(cp))
                            continue;
                        else if (cp == ':') {
                            isLabel = true;
                            state = trailingWhiteSpace;
                        } else
                            break outer;
                        break;
                    }
                    case trailingWhiteSpace: {
                        if (!Character.isWhitespace(cp))
                            break outer;
                        break;
                    }
                    default:
                        throw new IllegalStateException("'Illegal state: " + state);
                }
            }
            ++length;
        }

        if (fine)
            if (isLabel)
                result = new LabelToken(symbol.toString(), this.line);
            else
                result = new Tokens.MnemonicNameToken(symbol.toString(), this.line);
        line.delete(0, length);
        return result;
    }

    private OperandToken findOperandToken(StringBuilder line, final int pos) {
        int length = -1;
        OperandToken result = null;
        StringBuilder value = new StringBuilder(), bitNr = new StringBuilder();

        final int leadingWhiteSpace  = 0;
        final int foundPrefix        = 1;
        final int inValue            = 2;
        final int findPlus           = 3; // Used by indirect symbols
        final int findBitOperator    = 4;
        final int findBitNumber      = 5;
        final int inBitNumber        = 6;
        final int findDelimiter      = 7; // The delimiter is the ',' char
        final int trailingWhiteSpace = 8;

        OperandType8051 type = null;
        OperandRepresentation8051 repr = null;
        int state = leadingWhiteSpace;
        boolean fine = true;

        // start reading characters
        outer:
        {
            for (int cp : line.chars().toArray()) {
                ++length;
                switch (state) {
                    case leadingWhiteSpace: {
                        if (Character.isWhitespace(cp))
                            continue;
                        else if (Character.isLetterOrDigit(cp) || cp == '_') {
                            type = OperandType8051.ADDRESS;
                            if (Character.isDigit(cp))
                                if (type.isCompatible(OperandRepresentation8051.NUMBER))
                                    repr = OperandRepresentation8051.NUMBER;
                                else {
                                    problems.add(new TokenizingProblem("A '" + type + "' cannot be represented by a " +
                                            "'NUMBER'!",
                                            Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                            Character.toChars(cp)) + "' at column " + (pos + length)));
                                    fine = false;
                                }
                            else {
                                if (type.isCompatible(OperandRepresentation8051.SYMBOL))
                                    repr = OperandRepresentation8051.SYMBOL;
                                else {
                                    problems.add(new TokenizingProblem("A '" + type + "' cannot be represented by a " +
                                            "'SYMBOL'!",
                                            Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                            Character.toChars(cp)) + "' at column " + (pos + length)));
                                    fine = false;
                                }
                            }
                            value.appendCodePoint(cp);
                            state = inValue;
                        } else if (cp == ',') {
                            problems.add(new TokenizingProblem("Unexpected end of operand!", Problem.Type.ERROR, file,
                                    this.line, "',' at column " + (pos + length)));
                            fine = false;
                            state = trailingWhiteSpace;
                        } else if (cp == '.') {
                            problems.add(new TokenizingProblem("Expected a value before addressing a bit!",
                                    Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                    Character.toChars(cp)) + "' at column " + (pos + length)));
                            state = findBitNumber;
                            fine = false;
                        } else {
                            switch (cp) {
                                case '@': {
                                    type = OperandType8051.INDIRECT;
                                    break;
                                }
                                case '#': {
                                    type = OperandType8051.CONSTANT;
                                    break;
                                }
                                case '+':
                                case '-': {
                                    type = OperandType8051.ADDRESS_OFFSET;
                                    break;
                                }
                                case '/': {
                                    type = OperandType8051.NEGATED_ADDRESS;
                                    break;
                                }
                                case '$': {
                                    type = OperandType8051.ADDRESS;
                                    break;
                                }
                                default:
                                    problems.add(new TokenizingProblem("Unknown Type prefix!",
                                            Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                            Character.toChars(cp)) + "' at column " + (pos + length)));
                                    type = OperandType8051.ADDRESS; // Default to ADDRESS
                                    fine = false;
                            }
                            state = foundPrefix;
                        }
                        break;
                    }
                    case foundPrefix: {
                        if (Character.isWhitespace(cp))
                            continue;
                        else if (Character.isLetterOrDigit(cp) || cp == '_') {
                            if (Character.isDigit(cp))
                                if (type.isCompatible(OperandRepresentation8051.NUMBER))
                                    repr = OperandRepresentation8051.NUMBER;
                                else {
                                    problems.add(new TokenizingProblem("A '" + type + "' cannot be represented by a " +
                                            "'NUMBER'!",
                                            Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                            Character.toChars(cp)) + "' at column " + (pos + length)));
                                    fine = false;
                                }
                            else {
                                if (type.isCompatible(OperandRepresentation8051.SYMBOL))
                                    repr = OperandRepresentation8051.SYMBOL;
                                else {
                                    problems.add(new TokenizingProblem("A '" + type + "' cannot be represented by a " +
                                            "'SYMBOL'!",
                                            Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                            Character.toChars(cp)) + "' at column " + (pos + length)));
                                    fine = false;
                                }
                            }
                            value.appendCodePoint(cp);
                            state = inValue;
                        } else if (cp == ',') {
                            problems.add(new TokenizingProblem("Unexpected end of operand!", Problem.Type.ERROR, file,
                                    this.line, "',' at column " + (pos + length)));
                            fine = false;
                            state = trailingWhiteSpace;
                        } else if (cp == '.') {
                            problems.add(new TokenizingProblem("Expected a value before addressing a bit!",
                                    Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                    Character.toChars(cp)) + "' at column " + (pos + length)));
                            state = findBitNumber;
                            fine = false;
                        } else if (cp == '@' || cp == '+' || cp == '-' || cp == '#' || cp == '/' || cp == '$') {
                            problems.add(new TokenizingProblem("Type of the operand cannot be defined at this place!",
                                    Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                    Character.toChars(cp)) + "' at column " + (pos + length)));
                            fine = false;
                        } else {
                            problems.add(new TokenizingProblem("Expected a letter or digit!",
                                    Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                    Character.toChars(cp)) + "' at column " + (pos + length)));
                            state = findDelimiter;
                            fine = false;
                        }
                        break;
                    }
                    case inValue: {
                        if (Character.isLetterOrDigit(cp) || cp == '_') {
                            value.appendCodePoint(cp);
                        } else if (cp == '+') {
                            if (type.isIndirect()) {
                                value.appendCodePoint(cp);
                                state = foundPrefix; // Reuse 'FoundPrefix' to save an extra state
                            } else {
                                problems.add(new TokenizingProblem("'" + type + "' does not support a '+' in the value!",
                                        Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                        Character.toChars(cp)) + "' at column " + (pos + length)));
                                fine = false;
                            }
                        } else if (cp == '.') {
                            state = findBitNumber;
                        } else if (Character.isWhitespace(cp)) {
                            if (type.isIndirect())
                                state = findPlus;
                            else
                                state = findBitOperator;
                        } else if (cp == ',') {
                            state = trailingWhiteSpace;
                        } else {
                            problems.add(new TokenizingProblem("Expected a letter or digit!",
                                    Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                    Character.toChars(cp)) + "' at column " + (pos + length)));
                            state = findDelimiter;
                            fine = false;
                        }
                        break;
                    }
                    case findPlus: {
                        if (Character.isWhitespace(cp))
                            continue;
                        else if (cp == '+') {
                            value.appendCodePoint(cp);
                            state = foundPrefix;
                        } else if (cp == ',') {
                            state = trailingWhiteSpace;
                        } else {
                            problems.add(new TokenizingProblem("Expected a '+' or ',' here!",
                                    Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                    Character.toChars(cp)) + "' at column " + (pos + length)));
                            state = findDelimiter;
                            fine = false;
                        }
                        break;
                    }
                    case findBitOperator: {
                        if (Character.isWhitespace(cp))
                            continue;
                        else if (cp == '.') {
                            state = findBitNumber;
                        } else if (cp == ',') {
                            state = trailingWhiteSpace;
                        } else {
                            problems.add(new TokenizingProblem("Expected a '.' or ',' here!",
                                    Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                    Character.toChars(cp)) + "' at column " + (pos + length)));
                            state = findDelimiter;
                            fine = false;
                        }
                        break;
                    }
                    case findBitNumber: {
                        if (Character.isWhitespace(cp))
                            continue;
                        else if (Character.isLetterOrDigit(cp)) {
                            if (Character.isLetter(cp)) {
                                problems.add(new TokenizingProblem("Bit number must be a valid number.",
                                        Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                        Character.toChars(cp)) + "' at column " + (pos + length)));
                                state = findDelimiter;
                                fine = false;
                            } else {
                                bitNr.appendCodePoint(cp);
                                state = inBitNumber;
                            }
                        } else if (cp == ',') {
                            problems.add(new TokenizingProblem("End of operand before specifying the bit number!",
                                    Problem.Type.ERROR, file, this.line, "',' at column " + (pos + length)));
                            fine = false;
                            state = trailingWhiteSpace;
                        } else {
                            problems.add(new TokenizingProblem("Expected a number here!",
                                    Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                    Character.toChars(cp)) + "' at column " + (pos + length)));
                            state = findDelimiter;
                            fine = false;
                        }
                        break;
                    }
                    case inBitNumber: {
                        if (Character.isLetterOrDigit(cp)) {
                            bitNr.appendCodePoint(cp);
                        } else if (cp == ',') {
                            state = trailingWhiteSpace;
                        } else {
                            problems.add(new TokenizingProblem("Expected a number or ',' here!",
                                    Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                    Character.toChars(cp)) + "' at column " + (pos + length)));
                            state = findDelimiter;
                            fine = false;
                        }
                        break;
                    }
                    case findDelimiter: {
                        if (Character.isWhitespace(cp))
                            continue;
                        else if (cp == ',')
                            state = trailingWhiteSpace;
                        else if (fine) {
                            problems.add(new TokenizingProblem("Expected a ',' here!",
                                    Problem.Type.ERROR, file, this.line, "'" + String.valueOf(
                                    Character.toChars(cp)) + "' at column " + (pos + length)));
                            fine = false;
                        }
                        break;
                    }
                    case trailingWhiteSpace: {
                        if (Character.isWhitespace(cp))
                            continue;
                        else
                            break outer;
                    }
                    default:
                        throw new IllegalStateException("Illegal state: " + state);
                }
            }
            length++;
        }


        outer:
        if (fine) {
            // Bit addressing
            if (!bitNr.toString().isEmpty()) {
                if (type.isAddress() || type.isNegatedAddress()) { // Names do not exist at this point

                    int address;
                    int bit;

                    if (repr.isSymbol())
                        if (value.toString().equals("a"))
                            address = MC8051Library.A & 0xFF;
                        else if (value.toString().equals("c")){
                            problems.add(new TokenizingProblem("\""+value+"\" cannot be bit addressed!",
                                    Problem.Type.ERROR, file, this.line, value.toString()));
                            break outer;
                        } else {
                            problems.add(new TokenizingProblem("Symbols cannot be bit addressed!",
                                    Problem.Type.ERROR, file, this.line, value.toString()));
                            break outer;
                        }
                    else
                        try {
                            address = Integer.parseInt(value.toString());
                        } catch (NumberFormatException e) {
                            problems.add(new TokenizingProblem(
                                    "Expected a valid decimal number for the address!",
                                    Problem.Type.ERROR, file, this.line, value.toString()));
                            break outer;
                        }
                    try {
                        bit = Integer.parseInt(bitNr.toString());
                    } catch (NumberFormatException e) {
                        problems.add(new TokenizingProblem(
                                "Expected a valid decimal number for the bit number!",
                                Problem.Type.ERROR, file, this.line, bitNr.toString()));
                        break outer;
                    }
                    if (!testBounds(0, 0xFF, address, "bit address", value.toString()) ||
                        !testBounds(0,    7,     bit, "bit number", bitNr.toString()))
                        break outer;

                    int intVal = address + bit; // Assume SFR-region by default.
                    if (address >= 0x20 && address < 0x30) // Byte is in the bit memory

                        intVal = (address - 0x20) * 8 + bit;

                    else if (address <= 0x7f || (0x07 & address) != 0) {
                        problems.add(new TokenizingProblem("Byte is not bit addressable!", Problem.Type.ERROR, file,
                                this.line, value.toString()));
                        break outer;
                    }

                    result = new OperandToken8051(type, repr, Integer.toString(intVal), this.line);

                } else {
                    problems.add(new TokenizingProblem("Operand type cannot be bit addressed!",
                            Problem.Type.ERROR, file, this.line, value.toString()));
                }
            } // Everything else
            else {
                try {
                    String val;
                    if (repr.isNumber())
                        val = Integer.toString(Integer.parseInt(value.toString())); // Validate Number
                    else {
                        val = value.toString();
                        if (type.isAddress()) {
                            for (String name : MC8051Library.RESERVED_NAMES)
                                if (name.equalsIgnoreCase(val)) {
                                    type = OperandType8051.NAME;
                                    break;
                                }
                        } else if (type.isNegatedAddress()) {
                            if (val.equals("c")) {
                                val = Integer.toString(MC8051Library.C & 0xFF);
                                repr = OperandRepresentation8051.NUMBER;
                            } else {
                                for (String name : MC8051Library.RESERVED_NAMES)
                                    if (name.equalsIgnoreCase(val)) {
                                        problems.add(new TokenizingProblem("Reserved name cannot be used in a " +
                                                "negated address!", Problem.Type.ERROR, file, this.line, val));
                                        break outer;
                                    }
                            }
                        }
                    }

                    result = new OperandToken8051(type, repr, val, this.line);

                } catch (NumberFormatException e) {
                    problems.add(new TokenizingProblem(
                            "Expected a valid decimal number!",
                            Problem.Type.ERROR, file, this.line, bitNr.toString()));
                }
            }
        }

        line.delete(0, length);
        return result;

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
