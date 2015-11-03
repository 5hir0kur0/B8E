package assembler;

import assembler.util.AssemblyError;

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
     * @param problems
     *      All warnings and/or errors that occur while assembling will
     *      be added to this List.
     *
     * @return
     *      whether the assembling was successful.<br>
     *      This method should return <code>false</code>, if the assembler detected
     *      one or more errors.
     */
    boolean assemble(BufferedReader input, BufferedWriter output, List<AssemblyError> problems);
}
