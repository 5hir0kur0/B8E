package assembler;

import assembler.util.problems.Problem;

import java.io.BufferedReader;
import java.io.StringWriter;
import java.util.List;

/**
 * @author Jannik
 */
public interface Preprocessor {

    /**
     * Preprocesses an input.
     *
     * @param input
     *      The input that will be preprocessed.
     * @param output
     *      The output the result will be written to.<br>
     *      The result can be interpreted by an assembler.
     *
     * @return
     *      All warnings and/or errors that occur while assembling will
     *      be returned.
     */
    List<? extends Problem> preprocess(BufferedReader input, List<String> output);
}
