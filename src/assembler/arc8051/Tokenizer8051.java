package assembler.arc8051;

import assembler.LabelToken;
import assembler.Token;
import assembler.Tokenizer;
import assembler.Tokens;
import assembler.util.ExceptionProblem;
import assembler.util.Problem;
import assembler.util.TokenizingProblem;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;

/**
 * A Tokenizer for the 8051 architecture.
 *
 * @author Jannik
 */
public class Tokenizer8051 implements Tokenizer {

    @Override
    public List<Token> tokenize(StringReader input, List<Problem> problems) {
        List<Token> result = new ArrayList<>(128);

        String line, unModLine;
        try (Scanner s = new Scanner(input)) {
            while ((line = unModLine = s.nextLine()) != null) {
                Matcher m = MC8051Library.LABEL_PATTERN.matcher(line);

                if (m.find()) {
                    result.add(new LabelToken(m.group(1)));
                    line = line.substring(m.end());
                }

                if (line.trim().isEmpty())
                    continue;

                if ((m = MC8051Library.MNEMONIC_NAME_PATTERN.matcher(line)).find()) {
                    result.add(new Tokens.MnemonicNameToken(m.group(1)));
                    line = line.substring(m.end());
                } else if ((m = MC8051Library.COMMENTARY_PATTERN.matcher(line)).find()) {
                    result.add(new Tokens.CommentToken(m.group(1)));
                    continue;
                } else {
                    problems.add(new TokenizingProblem("Expected mnemonic or comment!", Problem.Type.ERROR, unModLine));
                    continue;
                }

                final String[] split = line.split(",");
                for (int i = 0; i < split.length; ++i) {
                    if (i == split.length - 1) {
                        if (split[i].contains(";")) {
                            String[] split2 = split[i].split(";", 2);
                            addToken(split2[0], result, problems);
                            result.add(new Tokens.CommentToken(split2[1]));
                        } else
                            addToken(split[i], result, problems);
                    } else
                        addToken(split[i], result, problems);
                }
            }
        } catch (NoSuchElementException e) {
            problems.add(new ExceptionProblem("Could not read input!", Problem.Type.ERROR, e));
            }
        return result;
    }

    private boolean addToken(String string, List<Token> add, List<Problem> problems) {
        string = string.trim();
        Matcher m;
        if ((m = MC8051Library.CONSTANT_PATTERN.matcher(string)).matches()) {
            String val = getNumber(m, problems);
            if (val != null) {
                add.add(new OperandToken8051(MC8051Library.OperandType8051.CONSTANT, val));
                return true;
            }
        } else if ((m = MC8051Library.ADDRESS_PATTERN.matcher(string)).matches()) {
            String val = getNumber(m, problems);
            if (val != null) {
                add.add(new OperandToken8051(MC8051Library.OperandType8051.ADDRESS, val));
                return true;
            }
        } else if ((m = MC8051Library.NEGATED_ADDRESS_PATTERN.matcher(string)).matches()) {
            String val = getNumber(m, problems);
            if (val != null) {
                add.add(new OperandToken8051(MC8051Library.OperandType8051.NEGATED_ADDRESS, val));
                return true;
            }

        } else if ((m = MC8051Library.ADDRESS_OFFSET_PATTERN.matcher(string)).matches()) {
            String val = getNumber(m, problems);
            if (val != null) {
                add.add(new OperandToken8051(MC8051Library.OperandType8051.ADDRESS_OFFSET, val));
                return true;
            }

        } else if ((m = MC8051Library.SYMBOL_PATTERN.matcher(string)).matches()) {
            String val = m.group(1).replaceAll("\\s", "");
            for (String reserved : MC8051Library.RESERVED_NAMES)
                if (val.equalsIgnoreCase(reserved))
                    add.add(new OperandToken8051(MC8051Library.OperandType8051.NAME, val));
            add.add(new Tokens.SymbolToken(val));
            return true;
        } else if ((m = MC8051Library.SYMBOL_INDIRECT_PATTERN.matcher(string)).matches()) {
            add.add(new OperandToken8051(MC8051Library.OperandType8051.INDIRECT_NAME,
                    m.group(0).replaceAll("\\s", "")));
            return true;
        } else
            problems.add(new TokenizingProblem("Could not recognize Token Type!", Problem.Type.ERROR, string));
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
}
