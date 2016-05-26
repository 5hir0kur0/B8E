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

    byte[] assemble(Path source, Path directory, List<Problem<?>> problems);

    Listing getListing();

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
