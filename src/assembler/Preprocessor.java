package assembler;

import assembler.util.problems.Problem;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Noxgrim
 */
public interface Preprocessor {

    /**
     * Preprocesses an input.
     *
     * @param file
     *      The input that will be preprocessed.
     * @param output
     *      The output the result will be written to.<br>
     *      The result can be interpreted by an assembler.
     *
     * @return
     *      All warnings and/or errors that occur while assembling will
     *      be returned.
     */
    List<Problem> preprocess(Path workingDirectory, Path file, List<String> output);
}
