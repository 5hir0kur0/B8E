package assembler.arc8051;

import assembler.Preprocessor;
import assembler.util.Regex;
import assembler.util.assembling.Directive;
import assembler.util.problems.ExceptionProblem;
import assembler.util.problems.PreprocessingProblem;
import assembler.util.problems.Problem;

import java.io.*;
import java.nio.file.Path;
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

    private final Directive[] direktives = {
            new Directive("equ") {
                @Override
                public boolean perform(String... args) {
                    //Regex test = new Regex("");
                    return false;
                }
            }
    };

    public Preprocessor8051() {
        line = 0;
        problems = new LinkedList<>();
    }

    @Override
    public List<? extends Problem> preprocess(BufferedReader input, StringWriter output) {
        problems.clear();
        line = 0;
        String line, original;
        try {
            while ((line = original = input.readLine()) != null) {
                this.line++;
                line = lowerCase(line, problems);

                line = handleDirective(line, problems);

                output.write(line+"\n");
            }

        } catch (IOException e) {
            problems.add(new ExceptionProblem("Could not read input.", Problem.Type.ERROR, e));
        }
        return problems;
    }

    private String handleDirective(final String line, List<Problem> problems) {
        Matcher m = MC8051Library.DIRECTIVE_PATTERN.matcher(line);
        if (m.matches()) {

            return "";
        } else
            return line;
    }

    /**
     * Turns every character into its lowercase representation if possible.<br>
     * The character wont be lowercased ignored if it's quoted in <code>'"'</code> or
     * <code>'\''</code>.A quoted character can be escaped with a <code>'\'</code>.<br>
     * Also comments (everything after and including  a<code>';'</code>) will be cut
     * from the resulting String.<br>
     *
     * @param line the line that should be lowercased.
     * @param problems
     *      a List to witch occurring Problems will be added.<br>
     *      Possible sources of Problems:
     *      <ul>
     *          <li>ERROR: the user tries to escape a character that is not a
     *          <code>'"'</code> or <code>'\''</code> (or <code>'\\'</code>)</li>
     *          <li>WARNING: a quote is not closed if the end of the line or
     *          a comment is reached.</li>
     *      </ul>
     *
     * @return
     *      a String that is a lowercased representation of the given one
     *      with possible comments removed.
     */
    private String lowerCase(String line, List<Problem> problems) {

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
