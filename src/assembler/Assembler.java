package assembler;

import assembler.util.ExceptionProblem;
import assembler.util.MnemonicProvider;
import assembler.util.Problem;
import assembler.util.TokenProblem;
import com.sun.corba.se.impl.io.TypeMismatchException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Represents a unit that can assemble (compile) written
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
                List<Byte> codes = new ArrayList<>();
                problems.addAll(_assemble(path, tokens, codes));

                for (byte b : codes)
                    output.write((char) b);
            }

            Collections.sort(problems);
        } catch (IOException e) {
            problems.add(new ExceptionProblem("Could not read properly from File!", Problem.Type.ERROR, e));
        }
        return problems;
    }

    private List<Problem> _assemble(Path path, List<Token> tokens, List<Byte> output) {
        List<Problem> problems = new ArrayList<>();
        provider.clearProblems();
        List<Assembled> assembled = new ArrayList<>();

        List<LabelToken> labels = new ArrayList<>();
        long codePoint = 0;

        for (int index = 0; index < tokens.size(); ++index) {
            Token t = tokens.get(index);

            switch (t.getType()) {
                case MNEMONIC_NAME: {
                    if (!(t instanceof Tokens.MnemonicNameToken))
                        throw new TypeMismatchException("Token is of Type is 'MnemonicName' but Token itself " +
                                "isn't a MnemonicNameToken!");

                    Mnemonic m = Arrays.stream(provider.getMnemonics()).filter((x) -> x.getName().equals(t.getValue()))
                            .findFirst().orElse(null);

                    if (m == null) {
                        problems.add(new TokenProblem("Unknown Mnemonic!", Problem.Type.ERROR, t));
                        continue;
                    }

                    List<Token> usedTokens = new ArrayList<>();
                    usedTokens.add(t);
                    ArrayList<Token> operands = new ArrayList<>();

                    while (index+1 < tokens.size() &&
                            (tokens.get(index+1).getType() == Token.TokenType.OPERAND ||
                             tokens.get(index+1).getType() == Token.TokenType.SYMBOL))
                        operands.add(tokens.get(++index));
                    usedTokens.addAll(operands);

                    if (operands.size() < m.getMinimumOperands()) {
                        problems.add(new TokenProblem("Mnemonic must have at least " + m.getMinimumOperands() +
                                " operands!", Problem.Type.ERROR, t));
                        continue;
                    }

                    int length = 0;
                    usedTokens.add(0, t);
                    Assembled assem = new Assembled(codePoint, usedTokens, m);

                    assem.setPositionSensitive(m.isPositionSensitive());

                    if (m instanceof LabelConsumer) {
                        int maxLength = ((LabelConsumer) m).getSpecificLength(codePoint,
                                (OperandToken[]) operands.stream().toArray());
                        assem.setLength(maxLength);
                        codePoint+=maxLength;
                        assem.setUnresolved(true);
                        assem.setCodes(new byte[maxLength]);
                        assembled.add(assem); // Resolve later.
                        continue;
                    }


                    byte[] codes = m.getInstructionFromOperands(codePoint, (Tokens.MnemonicNameToken) t,
                            (OperandToken[]) operands.stream().toArray());


                    length = codes.length != 0 ? codes.length : length;
                    assem.setLength(length);


                    if (m.isPositionSensitive() || codes.length != 0)
                        assembled.add(assem);

                    codePoint+= length;

                    break;
                }
                case LABEL: {
                    if (!(t instanceof LabelToken))
                        throw new TypeMismatchException("Token is of Type is 'Label' but Token itself " +
                                "isn't a LabelToken!");
                    LabelToken lt = (LabelToken) t;
                    lt.setCodePoint(codePoint);
                    labels.add(lt);
                    break;
                }
                case COMMENT:
                    break;
                default:
                    problems.add(new TokenProblem("Token does not belong here!", Problem.Type.ERROR, t));
            }
        }

        resolveLabelConsuming(assembled, labels);
        output.addAll(link(assembled));

        try (HexWriter hex = new HexWriter(Files.newBufferedWriter(path.resolveSibling(path.getFileName()+".hex")))) {
            hex.writeAll(assembled);
        } catch (Exception e) {
            e.printStackTrace();
        }



        return problems;
    }

    public void resolveLabelConsuming(List<Assembled> assembled, List<LabelToken> labels) {
            assembled.stream().filter(x->x.getMnemonic() instanceof LabelConsumer).forEach(x -> {
                List<OperandToken> operands = new ArrayList<>(x.getTokens().size());
                for (int i = 0; i < x.getTokens().size(); ++i) {
                    List<Token> tokens = new ArrayList<>(x.getTokens());
                    if (tokens.get(i) instanceof Tokens.SymbolToken) {
                        Tokens.SymbolToken st = (Tokens.SymbolToken) tokens.get(i);
                        for (LabelToken lt : labels)
                            // Replace Symbol token with actual address and test if the length has shortened.
                            if (lt.getValue().equalsIgnoreCase(st.getValue()))
                                tokens.set(i, provider.createNewJumpOperand(lt.getCodePoint()));
                    }
                    if (tokens.get(i) instanceof OperandToken)
                        operands.add((OperandToken) tokens.get(i));
                }
                // All Labels are resolved.
                final byte[] codes = x.getMnemonic().getInstructionFromOperands(x.getCodePoint(),
                        (Tokens.MnemonicNameToken)x.getTokens().get(0), (OperandToken[]) operands.stream().toArray());
                x.setCodes(codes);
            });
    }

    public List<Byte> link(List<Assembled> assembled) {
        List<Byte> result = new ArrayList<>();
        assembled.forEach(x->{
            final byte[] codes = x.getCodes();
            for (int i = 0; i < codes.length; i++) {
                long codePoint = x.getCodePoint()+i;
                result.set((int) codePoint, codes[i]);
            }
        });
        return result;
    }

    public void shiftLabels(long offset, long fromCodePoint, List<LabelToken> labels) {
        labels.stream().filter(x->x.getCodePoint()>=fromCodePoint).forEach(x->x.setCodePoint(x.getCodePoint()+offset));
    }

}
