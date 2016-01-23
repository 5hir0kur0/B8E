package assembler.arc8051;

import assembler.tokens.LabelToken;
import assembler.tokens.Token;
import assembler.Tokenizer;
import assembler.tokens.Tokens;
import assembler.util.problems.ExceptionProblem;
import assembler.util.problems.Problem;
import assembler.util.problems.TokenizingProblem;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;

/**
 * A Tokenizer for the 8051 architecture.
 *
 * @author Noxgrim
 */
public class Tokenizer8051 implements Tokenizer {

    @Override
    public List<Token> tokenize(StringReader input, List<Problem> problems) {
        List<Token> result = new ArrayList<>(128);

        int lineNumber = 0; Path path = null;

        String line, unModLine;
        try (Scanner s = new Scanner(input)) {
            while (s.hasNextLine()) {
                lineNumber++;
                line = unModLine = s.nextLine();
                Matcher m = MC8051Library.LABEL_PATTERN.matcher(line);

                if (m.find()) {
                    result.add(new LabelToken(m.group(1), lineNumber));
                    line = line.substring(m.end());
                }

                if (line.trim().isEmpty())
                    continue;

                if ((m = MC8051Library.MNEMONIC_NAME_PATTERN.matcher(line)).find()) {
                    result.add(new Tokens.MnemonicNameToken(m.group(1), lineNumber));
                    line = line.substring(m.end());
                    if (line.trim().isEmpty())
                        continue;
                } else {
                    problems.add(new TokenizingProblem("Expected mnemonic or comment!", Problem.Type.ERROR,
                            path, lineNumber, unModLine));
                    continue;
                }

                final String[] split = line.split(",");
                for (int i = 0; i < split.length; ++i) {
                    addToken(split[i], result, problems, path, lineNumber);
                }
            }
        } catch (NoSuchElementException e) {
            problems.add(new ExceptionProblem("Could not read input!", Problem.Type.ERROR, e));
            }
        return result;
    }

    private boolean addToken(String string, List<Token> add, List<Problem> problems, Path path, int line) {
        string = string.trim();
        Matcher m;
        if ((m = MC8051Library.CONSTANT_PATTERN.matcher(string)).matches()) {
            String val = getNumber(m, problems);
            if (val != null && testBounds(0, 0xFFFF, Integer.parseInt(val), "constant", problems, m.group(0))) {
                add.add(new OperandToken8051(MC8051Library.OperandType8051.CONSTANT, val, line));
                return true;
            }
        } else if ((m = MC8051Library.BIT_ADDRESS_PATTERN.matcher(string)).matches()) {
            Matcher byteMatcher = MC8051Library.ADDRESS_PATTERN.matcher(m.group(1));
            final int bitGroup = 2+MC8051Library.ADDRESS_PATTERN.matcher("").groupCount();
            Matcher bitMatcher  = MC8051Library.ADDRESS_PATTERN.matcher(m.group(bitGroup));
            if (!(byteMatcher.matches() && bitMatcher.matches()))
                return false;
            String byteAddr = getNumber(byteMatcher, problems);
            String bitAddr  = getNumber(bitMatcher, problems);
            int result;
            if (byteAddr != null && testBounds(0, 0xFF, Integer.parseInt(byteAddr), "bit address", problems, m.group(1))
                & bitAddr != null && testBounds(0, 7, Integer.parseInt(bitAddr), "bit number", problems,
                    m.group(bitGroup))) {
                int intVal = Integer.parseInt(byteAddr);
                result = intVal + Integer.parseInt(bitAddr);
                if (intVal >= 0x20 && intVal < 0x30)
                    result = (intVal - 0x20) * 8 + Integer.parseInt(bitAddr);
                else if (intVal < 0x80 && (0xF8 & intVal) != 0) {
                    problems.add(new TokenizingProblem("Byte is not bit addressable!", Problem.Type.ERROR,
                            m.group(1)));
                    return false;
                }
                add.add(new OperandToken8051(MC8051Library.OperandType8051.ADDRESS, Integer.toString(result), line));
                return true;
            }
        } else if ((m = MC8051Library.ADDRESS_PATTERN.matcher(string)).matches()) {
            String val = getNumber(m, problems);
            if (val != null && testBounds(0, 0xFF, Integer.parseInt(val), "address", problems, m.group(0))) {
                add.add(new OperandToken8051(MC8051Library.OperandType8051.ADDRESS, val, line));
                return true;
            }
        } else if ((m = MC8051Library.NEGATED_ADDRESS_PATTERN.matcher(string)).matches()) {
            String val = getNumber(m, problems);
            if (val != null && testBounds(0, 0xFF, Integer.parseInt(val), "address", problems, m.group(0))) {
                add.add(new OperandToken8051(MC8051Library.OperandType8051.NEGATED_ADDRESS, val, line));
                return true;
            }

        } else if ((m = MC8051Library.ADDRESS_OFFSET_PATTERN.matcher(string)).matches()) {
            String val = getNumber(m, problems);
            if (val != null && testBounds(0, 0xFFFF, Integer.parseInt(val), "relative offset", problems, m.group(0))) {
                add.add(new OperandToken8051(MC8051Library.OperandType8051.ADDRESS_OFFSET,
                        (m.group(0).charAt(0) == '-' ? '-' : "") + val, line));
                return true;
            }

        } else if ((m = MC8051Library.SYMBOL_PATTERN.matcher(string)).matches()) {
            String val = m.group(1).replaceAll("\\s", "");
            for (String reserved : MC8051Library.RESERVED_NAMES)
                if (val.equalsIgnoreCase(reserved)) {
                    add.add(new OperandToken8051(MC8051Library.OperandType8051.NAME, val, line));
                    return true;
                }
            add.add(new Tokens.SymbolToken(val, line));
            return true;
        } else if ((m = MC8051Library.SYMBOL_INDIRECT_PATTERN.matcher(string)).matches()) {
            add.add(new OperandToken8051(MC8051Library.OperandType8051.INDIRECT_NAME,
                    m.group(1).replaceAll("\\s", ""), line));
            return true;
        } else
            problems.add(new TokenizingProblem("Could not recognize Token Type!", Problem.Type.ERROR,
                    path, line, string));
        return false;
    }

    private String getNumber(Matcher matcher, List<Problem> problems) {
        String result = null;
        String numberSystem = "MISSINGNO-SYSTEM";      // Should be overwritten. (Did you get the joke?)
        try {
            if (matcher.group(2) != null) {
                if (matcher.group(3) != null) {        // Binary
                    numberSystem = "BINARY";
                    final int val = Integer.parseInt(matcher.group(7), 2);
                    result = Integer.toString(val);
                } else if (matcher.group(4) != null) { // Octal
                    numberSystem = "OCTAL";
                    final int val = Integer.parseInt(matcher.group(4)+matcher.group(7), 8);
                    result = Integer.toString(val);
                } else if (matcher.group(5) != null) { // Decimal
                    numberSystem = "DECIMAL";
                    final int val = Integer.parseInt(matcher.group(7), 10);
                    result = Integer.toString(val);
                } else {                               // Hex
                    numberSystem = "HEXADECIMAL";
                    final int val = Integer.parseInt(matcher.group(7), 16);
                    result = Integer.toString(val);
                }
            } else {
                char ns = 'd'; // TODO: Add reading of default from settings.
                if (matcher.group(10) != null)
                    ns = matcher.group(10).charAt(0);
                // All postfixes are taken from Asem-51
                // Reference: http://plit.de/asem-51/constant.htm
                switch (ns) {
                    case 'b': {   // In Asem-51 'b' is used to indicate binary numbers
                        numberSystem = "BINARY";
                        final int val = Integer.parseInt(matcher.group(9), 2);
                        result = Integer.toString(val);
                        break;
                    }
                    case 'q':
                    case 'o': {   // In Asem-51 'o' or 'q' are used to indicate octal numbers
                        numberSystem = "OCTAL";
                        final int val = Integer.parseInt(matcher.group(9), 8);
                        result = Integer.toString(val);
                        break;
                    }
                    case 'd': {   // In Asem-51 'd' is used to indicate decimal numbers
                        numberSystem = "DECIMAL";
                        final int val = Integer.parseInt(matcher.group(9), 10);
                        result = Integer.toString(val);
                        break;
                    }
                    case 'h': {   // In Asem-51 'h' is used to indicate hexadecimal numbers
                        numberSystem = "HEXADECIMAL";
                        final int val = Integer.parseInt(matcher.group(9), 16);
                        result = Integer.toString(val);
                        break;
                    }
                }
            }
        } catch (NumberFormatException e) {
            problems.add(new TokenizingProblem("Illegal digits in "+numberSystem+" number.",
                    Problem.Type.ERROR, matcher.group(0)));
        }
        return result;
    }

    private boolean testBounds(final int min, final int max, final int value,
                               final String type, List<Problem> problems, String cause) {
        if (value < min) {
            problems.add(new TokenizingProblem("Value of " + type + " to small! (Minimal value: " + min + ")",
                    Problem.Type.ERROR, cause));
            return false;
        } else if (value > max) {
            problems.add(new TokenizingProblem("Value of " + type + " to big! (Maximal value: " + max + ")",
                    Problem.Type.ERROR, cause));
            return false;
        } else
            return true;
    }
}
