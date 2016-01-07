package assembler;

import assembler.util.ExceptionProblem;
import assembler.util.MnemonicProvider;
import assembler.util.Problem;
import assembler.util.TokenProblem;
import com.sun.corba.se.impl.io.TypeMismatchException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Represents a unit that can assemble (compile) written
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


    public Assembler(MnemonicProvider provider, Preprocessor preprocessor, Tokenizer tokenizer) {
        this.tokenizer = Objects.requireNonNull(tokenizer, "Tokenizer cannot be 'null'!");
        this.preprocessor = Objects.requireNonNull(preprocessor, "Preprocessor cannot be 'null'!");
        this.provider = Objects.requireNonNull(provider, "Mnemonic Provider cannot be 'null'!");
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
        List<Byte> result = new ArrayList<>(tokens.size());

        List<LabelToken> labels = new ArrayList<>();
        List<Unresolved> unres  = new ArrayList<>();
        long codePoint = 0;

        for (int index = 0; index < tokens.size(); ++index) {
            Token t = tokens.get(index);

            switch (t.getType()) {
                case MNEMONIC_NAME: {
                    ArrayList<OperandToken> operands = new ArrayList<>();

                    while (index+1 < tokens.size() && tokens.get(index+1).getType() == Token.TokenType.OPERAND)
                        operands.add((OperandToken)tokens.get(++index));
                    Mnemonic m = Arrays.stream(provider.getMnemonics()).filter((x) -> x.getName().equals(t.getValue()))
                            .findFirst().orElse(null);

                    if (m == null) {
                        problems.add(new TokenProblem("Unknown Mnemonic!", Problem.Type.ERROR, t));
                        continue;
                    }

                    byte[] codes = m.getInstructionFromOperands(codePoint, (Tokens.MnemonicNameToken) t,
                            (OperandToken[]) operands.stream().toArray());

                    int lenght = codes.length;

                    for (byte b : codes)
                        result.add(b);

                    break;
                }
                case OPERAND:
                    break;
                case LABEL: {
                    if (!(t instanceof LabelToken))
                        throw new TypeMismatchException("Token is of Type is 'Label' but Token itself " +
                                "isn't a LabelToken!");
                    LabelToken lt = (LabelToken) t;
                    lt.setCodePoint(codePoint);
                    labels.add(lt);
                    updateUnresolved(labels, unres, result);
                    break;
                }
                case SYMBOL:
                    break;
                case COMMENT:
                    break;
            }
        }

        return problems;
    }

    private boolean updateUnresolved(List<LabelToken> labels, List<Unresolved> unres, List<Byte> output) {

        return false;
    }

    private static class Unresolved {
        long codePoint;
        int length;

        String wantedLabel;

        public Unresolved(long codePoint, int length, String wantedLabel) {
            this.codePoint = codePoint;
            this.length = length;
            this.wantedLabel = wantedLabel;
        }
    }
}
