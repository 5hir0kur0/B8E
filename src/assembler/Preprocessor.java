package assembler;

import assembler.util.Problem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.List;

/**
 * @author Jannik
 */
public interface Preprocessor {

    /**
     * Preprocessos an input.
     *
     * @param input
     *      The input that will be preprocessed.
     * @param output
     *      The output the result will be written to.<br>
     *      The result can be interpreted by an assembler.
     * @param problems
     *      All warnings and/or errors that occur while assembling will
     *      be added to this List.
     *
     * @return
     *      whether the preprocessing was successful.<br>
     *      This method should return <code>false</code>, if the preprocessor detected
     *      one or more errors.
     */
    boolean preprocess(BufferedReader input, BufferedWriter output, List<Problem> problems);
}
