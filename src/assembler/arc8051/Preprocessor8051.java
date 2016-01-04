package assembler.arc8051;

import assembler.Preprocessor;
import assembler.util.ExceptionProblem;
import assembler.util.PreprocessingProblem;
import assembler.util.Problem;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Noxgrim
 */
public class Preprocessor8051 implements Preprocessor {
    @Override
    public List<Problem> preprocess(BufferedReader input, StringWriter output) {
        String line;
        List<Problem> problems = new ArrayList<>();
        try {
            while ((line = input.readLine()) != null) {
                output.write(lowerCase(line, problems)+'\n');
            }

        } catch (IOException e) {
            problems.add(new ExceptionProblem("Could not read input", Problem.Type.ERROR, e));
        }
        return problems;
    }

    private String lowerCase(String line, List<Problem> problems) {

        StringBuffer result = new StringBuffer(line.length());
        boolean simpQuoted = false, doubQuoted = false;

        int last = 0;
        for (int cp : line.codePoints().toArray()) {
            if (last == '\\')
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
            if (doubQuoted || simpQuoted)
                problems.add(new PreprocessingProblem("Unclosed quote!", Problem.Type.ERROR, line));
        }
        return result.toString();
    }
}
