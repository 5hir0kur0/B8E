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
        List<Watched> watched   = new ArrayList<>();
        long codePoint = 0;

        for (int index = 0; index < tokens.size(); ++index) {
            Token t = tokens.get(index);
            final int startIndex = index;

            switch (t.getType()) {
                case MNEMONIC_NAME: {
                    Mnemonic m = Arrays.stream(provider.getMnemonics()).filter((x) -> x.getName().equals(t.getValue()))
                            .findFirst().orElse(null);

                    if (m == null) {
                        problems.add(new TokenProblem("Unknown Mnemonic!", Problem.Type.ERROR, t));
                        continue;
                    }

                    ArrayList<Token> operands = new ArrayList<>();

                    while (index+1 < tokens.size() &&
                            (tokens.get(index+1).getType() == Token.TokenType.OPERAND ||
                             tokens.get(index+1).getType() == Token.TokenType.SYMBOL))
                        operands.add(tokens.get(++index));

                    if (operands.size() < m.getMinimumOperands()) {
                        problems.add(new TokenProblem("Mnemonic must have at least " + m.getMinimumOperands() +
                                " operands!", Problem.Type.ERROR, t));
                        continue;
                    }

                    int length = 0;

                    if (m instanceof LabelConsumer) {
                        outer:
                        for (int i = 0; i < operands.size(); i++) {
                            if (operands.get(i) instanceof Tokens.SymbolToken) {
                                Tokens.SymbolToken st = (Tokens.SymbolToken) operands.get(i);
                                for (LabelToken lt : labels)
                                    // Replace Symbol token with actual address if label is already stored
                                    if (lt.getValue().equalsIgnoreCase(st.getValue())) {
                                        operands.set(i, provider.createNewJumpOperand(lt.getCodePoint()));
                                        continue outer;
                                    }
                                // Some labels are not resolved.
                                length =((LabelConsumer) t).getLength(codePoint,
                                        (OperandToken[]) operands.stream().toArray());
                                unres.add(new Unresolved(codePoint, length, startIndex, st.getValue()));
                                for (int j = length; j > 0; --j)
                                    result.add((byte)0);
                            }
                        }
                    }

                    // TODO: Check for position dependent mnemonics
                    // TODO: Check for minimal operands

                    byte[] codes = m.getInstructionFromOperands(codePoint, (Tokens.MnemonicNameToken) t,
                            (OperandToken[]) operands.stream().toArray());


                    length = codes.length != 0 ? codes.length : length;

                    if (m.isPositionSensitive())
                        watched.add(new Watched(codePoint, length, startIndex));

                    for (byte b : codes)
                        result.add(b);
                    updateUnresolved(labels, unres, result);
                    codePoint+= length;

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

    private boolean updateUnresolved(List<LabelToken> labels, List<Unresolved> unres, List<Byte> result) {
        boolean ret = false;
        for (Unresolved u : unres) {
            LabelToken lt = labels.stream().filter(x -> x.getValue().equalsIgnoreCase(u.label)).findFirst().orElse(null);
            if (lt != null) {

            }
        }
        return ret;
    }

    private boolean updateWatched() {
        return false;
    }

    private static class Unresolved extends Watched{
        String label;

        public Unresolved(long codePoint, int length, int tokensPos, String label) {
            super(codePoint, length, tokensPos);
            this.label = label;
        }
    }

    private static class Watched {
        long codePoint;
        int length;
        int tokensPos;

        public Watched(long codePoint, int length, int tokensPos) {
            this.codePoint = codePoint;
            this.length = length;
            this.tokensPos = tokensPos;
        }
    }
}
