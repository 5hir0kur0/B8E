package assembler;

import assembler.arc8051.Assembler8051;
import assembler.util.Listing;
import assembler.util.problems.Problem;

import java.nio.file.Path;
import java.util.List;

/**
 * Represents a unit that can assemble (compile) written
 * assembly into machine code and hex files.
 *
 * @author Noxgrim
 */
public interface Assembler {

    /**
     * Assembles the given file.
     *
     * @param source
     *      the file that should be assembled.
     * @param directory
     *      the reference directory.
     * @param problems
     *      occurring {@link Problem}s will be added to this {@link List}.
     * @return
     *      a resulting array of bytes representing the code memory.
     */
    byte[] assemble(Path source, Path directory, List<Problem<?>> problems);

    Listing getListing();

    /**
     * @return
     *      whether the last assembling was successful.
     */
    boolean wasSuccessful();

    /**
     * @return
     *      the last result of the assembling process.
     *      An array of bytes representing the code memory.
     */
    byte[] getResult();

    static Assembler of(final String modelName) {

        switch (modelName) {
            case "8051":
                return new Assembler8051();
            default:
                throw new UnsupportedOperationException("Unknown Assembler Model!");
        }

    }
}
