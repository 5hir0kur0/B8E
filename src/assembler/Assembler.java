package assembler;

import assembler.util.Problem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents an unit that can assemble (compile) written
 * assembly into machine code and hex files.
 *
 * @author Noxgrim
 */
public class Assembler {

    /**
     * The tokenizer used by the assembler.
     */
    private Tokenizer tokenizer;
    /**
     * An array of mnemonics to lookup the specific
     * mnemonics.
     */
    private Mnemonic[] mnemonics;
    /**
     * The preprocessor used by the assembler.
     */
    private Preprocessor preprocessor;


    public Assembler(Mnemonic[] mnemonics, Preprocessor preprocessor, Tokenizer tokenizer) {
        this.tokenizer = Objects.requireNonNull(tokenizer, "Tokenizer cannot be 'null'!");
        this.preprocessor = Objects.requireNonNull(preprocessor, "Preprocessor cannot be 'null'");
        this.mnemonics = Objects.requireNonNull(mnemonics, "Mnemonics cannot be 'null'");
    }
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
    public List<Problem> assemble(BufferedReader input, BufferedWriter output) {
        List<Problem> problems = preprocessor.preprocess(input, output, );


        Collections.sort(problems);
        return problems;
    }
}
