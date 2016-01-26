package assembler.arc8051;

import assembler.Preprocessor;
import assembler.util.problems.ExceptionProblem;
import assembler.util.problems.PreprocessingProblem;
import assembler.util.problems.Problem;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Preprocesses input for assembly language written for the
 * 8051 family.
 *
 * @author Jannik
 */
public class Preprocessor8051 implements Preprocessor {
    @Override
    public List<? extends Problem> preprocess(BufferedReader input, StringWriter output) {
        String line;
        List<Problem> problems = new ArrayList<>();
        try {
            while ((line = input.readLine()) != null) {
                output.write(lowerCase(line, problems)+'\n');
            }

        } catch (IOException e) {
            problems.add(new ExceptionProblem("Could not read input.", Problem.Type.ERROR, e));
        }
        return problems;
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
     *          <code>'"'</code> or <code>'\''</code></li>
     *          <li>WARNING: a quote is not closed if the end of the line or
     *          a comment is reached.</li>
     *      </ul>
     *
     * @return
     *      a String that is a lowercased representation of the given one
     *      with possible comments removed.
     */
    private String lowerCase(String line, List<Problem> problems) {

        StringBuffer result = new StringBuffer(line.length());
        boolean simpQuoted = false, doubQuoted = false;

        int last = 0;
        for (int cp : line.codePoints().toArray()) {
            if (cp == ';') // Cut comments
                break;
            else if (last == '\\' && !(cp == '\'' || cp == '"'))
                problems.add(new PreprocessingProblem("Illegal escape!", Problem.Type.ERROR,
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
            problems.add(new PreprocessingProblem("Unclosed quote!", Problem.Type.WARNING, line));
        return result.toString();
    }
}
