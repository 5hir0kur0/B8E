package assembler;

import assembler.util.problems.Problem;

import java.nio.file.Path;
import java.util.List;

/**
 * This interface allows a class to preprocess input from a file for the
 * tokenizer.
 *
 * @author Noxgrim
 */
public interface Preprocessor {

    /**
     * Preprocesses an input.
     *
     * @param workingDirectory
     *      The directory from which to assume relative paths.
     * @param file
     *      The input that will be preprocessed.
     * @param output
     *      The output the result will be written to.<br>
     *      The result can be interpreted by an assembler.
     *
     * @return
     *      all warnings and/or errors that occur while assembling.
     */
    List<Problem<?>> preprocess(Path workingDirectory, Path file, List<String> output);
}
