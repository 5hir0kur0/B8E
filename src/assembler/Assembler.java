package assembler;

import assembler.util.Problem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.List;

/**
 * This interface allows a class to assemble
 * assembler code.
 *
 * @author Jannik
 */
public interface Assembler {

    /**
     * Assembles a given input and writes the result in an output.<br>
     *
     * First the input is preprocessed and then assembled.
     *
     * @param input
     *      The input that will be assembled.
     * @param output
     *      The output the result will be written to.<br>
     *      The resulting bytes can be directly interpreted
     *      by the emulator or a microcomputer.
     *
     * @return
     *      All warnings and/or errors that occur while assembling will
     *      be returned.
     */
    boolean assemble(BufferedReader input, BufferedWriter output);
}
