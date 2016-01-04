package assembler;

import assembler.util.ExceptionProblem;
import assembler.util.MnemonicProvider;
import assembler.util.Problem;
import sun.plugin2.jvm.CircularByteBuffer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents an unit that can assemble (compile) written
 * assembly into machine code and hex files.
 *
 * @author Jannik
 */
public class Assembler {

    /**
     * The tokenizer used by the assembler.
     */
    private Tokenizer tokenizer;
    /**
     * Provides the needed Mnemonics
     */
    private MnemonicProvider provider;
    /**
     * The preprocessor used by the assembler.
     */
    private Preprocessor preprocessor;


    public Assembler(Mnemonic[] mnemonics, Preprocessor preprocessor, Tokenizer tokenizer) {
        this.tokenizer = Objects.requireNonNull(tokenizer, "Tokenizer cannot be 'null'!");
        this.preprocessor = Objects.requireNonNull(preprocessor, "Preprocessor cannot be 'null'");
        this.provider = Objects.requireNonNull(provider, "Mnemonic Provider cannot be 'null'");
    }
    /**
     * Assembles a given input and writes the result in an output.<br>
     *
     * First the input is preprocessed and then assembled.
     *
     * @param path
     *      The file that will be assembled.
     * @param output
     *      The output the result will be written to.<br>
     *      The resulting bytes can be directly interpreted
     *      by the emulator or a microcomputer.
     *
     * @return
     *      All warnings and/or errors that occur while assembling will
     *      be returned.
     */
    public List<Problem> assemble(Path path, BufferedWriter output) {
        List<Problem> problems = new ArrayList<>();
        try (BufferedReader input = Files.newBufferedReader(path);
             StringWriter prepOutput  = new StringWriter()){

            problems.addAll(preprocessor.preprocess(input, prepOutput));

            try (StringReader tokenInput = new StringReader(prepOutput.toString())) {

                List<Token> tokens = tokenizer.tokenize(tokenInput, problems);
                problems.addAll(_assemble(tokens, output));
            }

            Collections.sort(problems);
        } catch (IOException e) {
            problems.add(new ExceptionProblem("Could not read properly from File!", Problem.Type.ERROR, e));
        }
        return problems;
    }

    private List<Problem> _assemble(List<Token> tokens, BufferedWriter output) {
        List<Problem> problems = new ArrayList<>();
        provider.clearProblems();

        List<LabelToken> labels;
        long codePoint;

        return problems;
    }
}
