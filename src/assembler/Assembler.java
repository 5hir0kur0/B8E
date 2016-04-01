package assembler;

import assembler.arc8051.DirectiveTokens;
import assembler.tokens.*;
import assembler.util.*;
import assembler.util.assembling.ArchitectureProvider;
import assembler.util.assembling.Assembled;
import assembler.util.assembling.LabelConsumer;
import assembler.util.assembling.Mnemonic;
import assembler.util.problems.ExceptionProblem;
import assembler.util.problems.PreprocessingProblem;
import assembler.util.problems.Problem;
import assembler.util.problems.TokenProblem;
import com.sun.corba.se.impl.io.TypeMismatchException;
import misc.Settings;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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
     * Provides architecture specific operations.
     */
    private ArchitectureProvider provider;
    /**
     * The preprocessor used by the assembler.
     */
    private Preprocessor preprocessor;

    /** The source file. */
    private Path srcFile;


    public Assembler(ArchitectureProvider provider, Preprocessor preprocessor, Tokenizer tokenizer) {
        this.tokenizer = Objects.requireNonNull(tokenizer, "Tokenizer cannot be 'null'!");
        this.preprocessor = Objects.requireNonNull(preprocessor, "Preprocessor cannot be 'null'!");
        this.provider = Objects.requireNonNull(provider, "Architecture Provider cannot be 'null'!");
    }
    /**
     * Assembles a given input and writes the result in an output.<br>
     *
     * First the input is preprocessed and then assembled.
     *
     * @param directory
     *      The directory in which the that will be assembled lies.<br>
     *      Missing non default includes will be searched in this directory.
     * @param file
     *      The raw name of the target file. This file in the directory will be used to
     *      generate the output.<br>
     *      The raw file name does not include the file extension.<br>
     *      The file extension (with extension delimiter will be read from the configuration.
     * @param output
     *      The output the result will be written to.<br>
     *      The resulting bytes can be directly interpreted
     *      by the emulator or a microcomputer.
     *
     * @return
     *      All warnings and/or errors that occur while assembling will
     *      be returned.
     */
    public List<Problem> assemble(Path directory, String file, BufferedOutputStream output) {
        List<Problem> problems = new ArrayList<>();
        try {
            srcFile = Paths.get(directory.toString(),
                file + Settings.INSTANCE.getProperty(AssemblerSettings.SOURCE_FILE_EXTENSION,
                        AssemblerSettings.VALID_FILE_EXTENSION));
        } catch (InvalidPathException e) {
            problems.add(new PreprocessingProblem("Invalid source file path!", Problem.Type.ERROR, directory, 0,
                    directory.toString() + file + AssemblerSettings.SOURCE_FILE_EXTENSION));
            return problems;
        }

        try {
            List<String> inputOutput = new LinkedList<>();
            problems.addAll(preprocessor.preprocess(directory, srcFile, inputOutput));


            List<Token> tokens = tokenizer.tokenize(inputOutput, problems);
            List<Byte> codes = new ArrayList<>();
            problems.addAll(_assemble(directory, file, tokens, codes));

            for (byte b : codes)
                output.write(b);
            output.close();


            Collections.sort(problems);
        } catch (IOException e) {
            problems.add(new ExceptionProblem("Error while writing in output file!", Problem.Type.ERROR, e));
        }
        return problems;
    }

    private List<Problem> _assemble(Path directory, String fileName, List<Token> tokens, List<Byte> output) {
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
                        problems.add(new TokenProblem("Unknown Mnemonic!", Problem.Type.ERROR, srcFile, t));
                        continue;
                    }

                    List<Token> usedTokens = new ArrayList<>(4);
                    usedTokens.add(t);
                    ArrayList<Token> operands = new ArrayList<>(3);

                    while (index+1 < tokens.size() &&
                            (tokens.get(index+1).getType() == Token.TokenType.OPERAND ||
                             tokens.get(index+1).getType() == Token.TokenType.SYMBOL))
                        operands.add(tokens.get(++index));
                    usedTokens.addAll(operands);

                    if (operands.size() < m.getMinimumOperands()) {
                        problems.add(new TokenProblem("Mnemonic must have at least " + m.getMinimumOperands() +
                                " operand"+(m.getMinimumOperands() != 1?"s":"")+"!", Problem.Type.ERROR, srcFile, t));
                        continue;
                    }

                    int length = 0;
                    Assembled assem = new Assembled(codePoint, usedTokens, m);

                    assem.setPositionSensitive(m.isPositionSensitive());

                    if (m instanceof LabelConsumer) {

                        int maxLength = ((LabelConsumer) m).getSpecificLength(codePoint,
                                operands.stream().filter(tkn -> tkn instanceof OperandToken)
                                        .map(tkn -> (OperandToken) tkn).collect(Collectors.toList())
                                        .toArray(new OperandToken[1]));
                        assem.setLength(maxLength);
                        codePoint+=maxLength;
                        assem.setUnresolved(true);
                        assem.setCodes(new byte[maxLength]);
                        assembled.add(assem); // Resolve later.
                        continue;
                    }

                    if (operands.stream().anyMatch(x->x instanceof Tokens.SymbolToken)) {
                        problems.add(new TokenProblem("Unresolved Symbol!", Problem.Type.ERROR, srcFile, operands.stream().
                                filter(x -> x instanceof Tokens.SymbolToken).findFirst().get()));
                        continue;
                    }

                    byte[] codes = m.getInstructionFromOperands(codePoint, (Tokens.MnemonicNameToken) t,
                            operands.stream().toArray(OperandToken[]::new));


                    length = codes.length != 0 ? codes.length : length;
                    assem.setLength(length);
                    assem.setCodes(codes);


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
                case DIRECTIVE: {
                    if (t instanceof DirectiveTokens.FileChangeToken) {
                        provider.setAssembledFile(srcFile = ((DirectiveTokens.FileChangeToken) t).getFile());
                    } else if (t instanceof DirectiveTokens.DataToken) {
                        DirectiveTokens.DataToken d = (DirectiveTokens.DataToken) t;
                        assembled.add(new Assembled(false, false, Collections.singletonList(t), codePoint, d.getData(),
                                d.getData().length, null));
                    } else
                        problems.add(new TokenProblem("Unknown directive token!", Problem.Type.ERROR, srcFile, t));
                }
                default:
                    problems.add(new TokenProblem("Token does not belong here!", Problem.Type.ERROR, srcFile, t));
            }
        }

        resolveLabelConsuming(assembled, labels, problems);
        problems.addAll(provider.getProblems());
        output.addAll(link(assembled));

        try (HexWriter hex = new HexWriter(Files.newBufferedWriter(Paths.get(directory.toString(),
                fileName+Settings.INSTANCE.getProperty(AssemblerSettings.HEX_FILE_EXTENSION,
                        AssemblerSettings.VALID_FILE_EXTENSION))))) {
            hex.writeAll(assembled);
        } catch (Exception e) {
            e.printStackTrace();
        }



        return problems;
    }

    private void resolveLabelConsuming(List<Assembled> assembled, List<LabelToken> labels, List<Problem> problems) {
        assembled.stream().filter(x -> x.getMnemonic() instanceof LabelConsumer).forEach(x -> {
            List<OperandToken> operands = new ArrayList<>(x.getTokens().size());
            boolean unresolved = true;

            for (int i = 0; i < x.getTokens().size(); ++i) {
                List<Token> tokens = x.getTokens();
                if (tokens.get(i) instanceof Tokens.SymbolToken) {
                    Tokens.SymbolToken st = (Tokens.SymbolToken) tokens.get(i);
                    for (LabelToken lt : labels)
                        // Replace Symbol token with actual address and test if the length has shortened.
                        if (lt.getValue().equalsIgnoreCase(st.getValue())) {
                            tokens.set(i, provider.createNewJumpOperand(lt.getCodePoint(), lt.getLine()));
                            unresolved = false;
                        }
                    if (unresolved)
                        problems.add(new TokenProblem("Unresolved Symbol!", Problem.Type.ERROR, srcFile, tokens.get(i)));
                }
                if (tokens.get(i) instanceof OperandToken)
                    operands.add((OperandToken) tokens.get(i));
            }
            if (unresolved) return;
            x.setUnresolved(false);
            // All Labels are resolved.
            final byte[] codes = x.getMnemonic().getInstructionFromOperands(x.getCodePoint(),
                    (Tokens.MnemonicNameToken) x.getTokens().get(0), operands.toArray(new OperandToken[0]));
            x.setCodes(codes);
        });
    }

    private List<Byte> link(List<Assembled> assembled) {
        final int lines;
        {
            Assembled ass = assembled.get(assembled.size()-1);
            lines = ass.getTokens().get(ass.getTokens().size()-1).getLine();
        }
        ArrayList<Byte> result = new ArrayList<>(lines*2);
        for (Assembled a : assembled) {
            final byte[] codes = a.getCodes();

            while (a.getCodePoint()>result.size())
                result.add((byte)0);

            for (int i = 0; i < codes.length; i++) {
                // long codePoint = a.getCodePoint() + i;
                result.add(codes[i]);
            }
        }
        return result;
    }

    private void shiftLabels(long offset, long fromCodePoint, List<LabelToken> labels) {
        labels.stream().filter(x->x.getCodePoint()>=fromCodePoint).forEach(x->x.setCodePoint(x.getCodePoint()+offset));
    }

}
