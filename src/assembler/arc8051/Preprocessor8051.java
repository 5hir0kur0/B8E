package assembler.arc8051;

import assembler.Preprocessor;
import assembler.util.AssemblerSettings;
import assembler.util.assembling.Directive;
import assembler.util.problems.ExceptionProblem;
import assembler.util.problems.PreprocessingProblem;
import assembler.util.problems.Problem;

import java.io.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Preprocesses input for assembly language written for the
 * 8051 family.
 *
 * @author Jannik
 */
public class Preprocessor8051 implements Preprocessor {

    private List<Problem> problems;

    private Path currentFile;
    private int line;

    private byte endState; // 1: Running, 0: End Reached, -1: End Problem created

    private final Directive[] directives = {
            new Directive("file", true) {
                @Override
                public boolean perform(String... args) {
                    if (args.length < 1) {
                        problems.add(new PreprocessingProblem("Expected at least one argument for 'file' directive!",
                                Problem.Type.ERROR, currentFile, line, null));
                        return false;
                    } else {
                        final String fileString = args[0];
                        try {
                            Path newPath = Paths.get(fileString);
                            line = 0;

                            if (args.length > 2)
                                problems.add(new PreprocessingProblem("Too many arguments for 'file' directive!",
                                        Problem.Type.WARNING, currentFile, line,
                                        Arrays.toString(Arrays.copyOfRange(args, 2, args.length))));

                            if (args.length > 1)
                                directives[1].perform(args[1]);

                            currentFile = newPath;
                            return true;
                        } catch (InvalidPathException e) {
                            problems.add(new PreprocessingProblem("Invalid Path!", Problem.Type.ERROR,
                                    currentFile, line, fileString));
                            return false;
                        }
                    }
                }
            },

            new Directive("line", true) {
                @Override
                public boolean perform(String... args) {
                    if (args.length < 1) {
                        problems.add(new PreprocessingProblem("Expected at least one argument for 'line' directive!",
                                Problem.Type.ERROR, currentFile, line, null));
                        return false;
                    } else {
                        String number = args[0];
                        try {

                            final boolean relative = number.startsWith("~");

                            if (relative) number = number.substring(1);

                            int newLine = Integer.parseInt(number);

                            if (relative) {
                                if (line - newLine < 1) {
                                    line = 0;
                                    problems.add(new PreprocessingProblem("Resulting line number cannot be smaller than 1!",
                                            Problem.Type.ERROR, currentFile, line, number));
                                    return false;
                                } else
                                    line += --newLine;
                            } else {
                                if (newLine < 1) {
                                    problems.add(new PreprocessingProblem("New line number cannot be smaller than 1!",
                                            Problem.Type.ERROR, currentFile, line, number));
                                    return false;
                                } else
                                    line = --newLine;
                            }
                            if (args.length > 1)
                                problems.add(new PreprocessingProblem("Too many arguments for 'line' directive!",
                                        Problem.Type.WARNING, currentFile, line,
                                        Arrays.toString(Arrays.copyOfRange(args, 1, args.length))));
                            return true;

                        } catch (NumberFormatException e) {
                            problems.add(new PreprocessingProblem("Illegal number format!", Problem.Type.ERROR,
                                    currentFile, line, number));
                            return false;
                        }
                    }
                }
            },

            new Directive("end") {
                @Override
                public boolean perform(String... args) {
                    if (endState > 0)
                        endState = 0;
                    if (args.length > 0)
                        problems.add(new PreprocessingProblem("Too many arguments for 'end' directive!",
                                Problem.Type.WARNING, currentFile, line,
                                Arrays.toString(args)));
                    return true;
                }
            },

            new Directive("equ") {
                @Override
                public boolean perform(String... args) {
                    boolean result = true;
                    if (args.length < 2) {
                        problems.add(new PreprocessingProblem("Expected at least 2 arguments for 'equ' directive!",
                                Problem.Type.ERROR, currentFile, line, Arrays.toString(args)));
                        return false;
                    }

                    if (!MC8051Library.SYMBOL_PATTERN.matcher(args[0]).matches()) {
                        problems.add(new PreprocessingProblem("First argument of 'equ' is not a valid symbol!",
                                Problem.Type.ERROR, currentFile, line, args[0]));
                        result = false;
                    }

                    if (!result) return false;

                    //Regex test = new Regex("");
                    return result;
                }
            }
    };

    public Preprocessor8051() {
        problems = new LinkedList<>();
    }

    @Override
    public List<? extends Problem> preprocess(BufferedReader input, List<String> output) {
        problems.clear();

        line = 0;
        endState = 1;

        String line, original;
        output.add("$file \""+currentFile.toString()+"\""); // Add directive for main source file
                                                            // for Tokenizer and Assembler
        try {
            while ((line = original = input.readLine()) != null) {
                this.line++;

                if (endState > 0) {

                    //TODO: Perform Regexes

                    line = line.split(";", 2)[0];     // Cut comments

                    line = convertNumbers(line);      // Convert any numbers into the decimal system

                    // TODO: Evaluate mathematical expressions with SimpleMath

                    if (MC8051Library.DIRECTIVE_PATTERN.matcher(line).matches())
                        line = handleDirective(line); // Line is a directive: handle it
                    else
                        line = lowerCase(line);       // Only convert lines to lowercase if they
                                                      // are not a directive because fallthrough
                                                      // directives may be case sensitive.

                    output.add(line);
                } else
                    if (!line.split(";", 2)[0].trim().isEmpty() && endState == 0) {
                        // If the line contains more than just comments or white space
                        // and no Problem has been created yet
                        MC8051Library.getGeneralErrorSetting(new PreprocessingProblem(currentFile, this.line, line),
                                AssemblerSettings.END_CODE_AFTER, "No code allowed after use of 'end' directive!",
                                "All code after an 'end' directive will be ignored.", problems);
                        endState = -1;
                    }

            }

            if (endState > 0)
                MC8051Library.getGeneralErrorSetting(new PreprocessingProblem(currentFile, this.line, line),
                        AssemblerSettings.END_MISSING, "'end' directive not found!", "Missing 'end' directive!",
                        problems);

        } catch (IOException e) {
            problems.add(new ExceptionProblem("Could not read input.", Problem.Type.ERROR, e));
        }
        return problems;
    }

    private String handleDirective(final String line) {
        Matcher m = MC8051Library.DIRECTIVE_PATTERN.matcher(line);
        if (m.matches()) {

            return "";
        } else
            return line;
    }

    /**
     * Coverts every occurrence of a valid number in a String to
     * the decimal system.
     *
     * @param source
     *      the String to be used.
     *
     * @return
     *      the modified source String.
     *
     * @see #getNumber(String)
     */
    private String convertNumbers(final String source) {
        StringBuffer sb = new StringBuffer(source.length());

        Matcher m = MC8051Library.NUMBER_PATTERN.matcher(source);

        // Variation of 'replaceAll()' in Matcher
        boolean found;
        if (found = m.find()) {
            do {
                final String number = getNumber(m.group());
                m.appendReplacement(sb, getNumber(number == null ? "0" : number));
            } while (found = m.find());
            return m.appendTail(sb).toString();
        }

        return source;
    }

    /**
     * Coverts a number from a given match to a decimal number and
     * returns it as a String (so it can be of the value <code>null</code>).<br>
     * <br>
     * Possible sources of Problems:
     * <ul>
     *      <li>ERROR: the number uses digits that are not used in the number
     *                 system specified.</li>
     * </ul>
     * @param number
     *      the number that should be converted.<br>
     *      The number system will be obtained from the number's suffix if it
     *      has any.<br>
     *      Possible number systems:<br>
     *      <table>
     *          <tr><th>Suffix</th><th>Name</th><th>Base</th><th>Valid Digits</th></tr>
     *          <tr><td>h</td><td>Hexadecimal</td><td>16</td><td>0-9a-f</td></tr>
     *          <tr><td>d or none</td><td>Decimal</td><td>10</td><td>0-9</td></tr>
     *          <tr><td>o or q</td><td>Octal</td><td>8</td><td>0-8</td></tr>
     *          <tr><td>b</td><td>Binary</td><td>2</td><td>0-1</td></tr>
     *
     *          <tr><th>Prefix</th><th>Name</th><th>Base</th><th>Valid Digits</th></tr>
     *          <tr><td>0x</td><td>Hexadecimal</td><td>16</td><td>0-9a-f</td></tr>
     *      </table>
     *      <br>
     *      If the number contains digits that are illegal for its number
     *      system the number is considered invalid.
     * @return
     *      the value of the String as a decimal String or <code>null</code>
     *      if the String is invalid.
     *
     * @see MC8051Library#NUMBER_PATTERN
     */
    private String getNumber(final String number) {
        String result = null;
        String numberSystem = "MISSINGNO-SYSTEM";      // Should be overwritten. (Did you get the joke?)

        try {
            if (number.startsWith("0x")) {
                numberSystem = "HEXADECIMAL";

                result = Integer.toString(Integer.parseInt(number.substring(2), 16));
            } else {
                final char postfix = number.charAt(number.length()-1);
                // All postfixes are taken from Asem-51
                // Reference: http://plit.de/asem-51/constant.htm
                switch (postfix) {
                    case 'b': {   // In Asem-51 'b' is used to indicate binary numbers
                        numberSystem = "BINARY";
                        final int val = Integer.parseInt(number.substring(0, number.length()-1), 2);
                        result = Integer.toString(val);
                        break;
                    }
                    case 'q':
                    case 'o': {   // In Asem-51 'o' or 'q' are used to indicate octal numbers
                        numberSystem = "OCTAL";
                        final int val = Integer.parseInt(number.substring(0, number.length()-1), 8);
                        result = Integer.toString(val);
                        break;
                    }
                    case 'h': {   // In Asem-51 'h' is used to indicate hexadecimal numbers
                        numberSystem = "HEXADECIMAL";
                        final int val = Integer.parseInt(number.substring(0, number.length()-1), 16);
                        result = Integer.toString(val);
                        break;
                    }
                    case 'd': {   // In Asem-51 'd' or missing prefixes is used to indicate decimal numbers
                        numberSystem = "DECIMAL";
                        final int val = Integer.parseInt(number.substring(0, number.length()-1), 10);
                        result = Integer.toString(val);
                        break;
                    }
                    default: {   // No known postfix recognised, assuming decimal
                        numberSystem = "DECIMAL";
                        final int val = Integer.parseInt(number, 10);
                        result = Integer.toString(val);
                    }
                }
            }
        } catch (NumberFormatException e) {
            problems.add(new PreprocessingProblem("Illegal digits in " + numberSystem + " number.",
                    Problem.Type.ERROR, currentFile, line, number));
        }
        return result;
    }

    /**
     * Turns every character into its lowercase representation if possible.<br>
     * The character wont be lowercased ignored if it's quoted in <code>'"'</code> or
     * <code>'\''</code>.A quoted character can be escaped with a <code>'\'</code>.<br>
     * Also comments (everything after and including  a<code>';'</code>) will be cut
     * from the resulting String.<br>
     * <br>
     * Possible sources of Problems:<br>
     * <ul>
     *      <li>ERROR: the user tries to escape a character that is not a
     *      <code>'"'</code> or <code>'\''</code> (or <code>'\\'</code>)</li>
     *      <li>WARNING: a quote is not closed if the end of the line or
     *      a comment is reached.</li>
     * </ul>
     *
     * @param line the line that should be lowercased.
     *
     * @return
     *      a String that is a lowercased representation of the given one
     *      with possible comments removed.
     */
    private String lowerCase(String line) {

        StringBuilder result = new StringBuilder(line.length());
        boolean simpQuoted = false, doubQuoted = false;

        int last = 0;
        for (int cp : line.codePoints().toArray()) {
            if (cp == ';') // Cut comments
                break;
            else if (last == '\\' && !(cp == '\'' || cp == '"' || cp != '\\'))
                problems.add(new PreprocessingProblem("Illegal escape!", Problem.Type.ERROR,
                        currentFile, this.line,
                        new StringBuilder().appendCodePoint(last).appendCodePoint(cp).toString()));
            else if (last == '\\')
                result.appendCodePoint(cp);
            else if (cp != '\\') {
                if (cp == '"' && !simpQuoted)
                    doubQuoted = !doubQuoted;
                else if (cp == '\'' && !doubQuoted)
                    simpQuoted = !simpQuoted;

                result.appendCodePoint(!(doubQuoted || simpQuoted) ?
                        Character.toLowerCase(cp) : cp);
            }
            last = cp;
        }
        if (doubQuoted || simpQuoted)
            problems.add(new PreprocessingProblem("Unclosed quote!",
                    Problem.Type.WARNING, currentFile, this.line, line));
        return result.toString();
    }

}
