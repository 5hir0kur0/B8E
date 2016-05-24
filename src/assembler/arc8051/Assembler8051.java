package assembler.arc8051;

import assembler.Assembler;
import assembler.tokens.LabelToken;
import assembler.tokens.Token;
import assembler.util.assembling.Assembled;
import assembler.util.problems.Problem;
import assembler.util.problems.TokenProblem;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Jannik
 */
public class Assembler8051 implements Assembler {

    private Preprocessor8051 preprocessor;
    private Tokenizer8051  tokenizer;

    private byte[] result;

    public Assembler8051() {
        this.preprocessor  = new Preprocessor8051();
        this.tokenizer = new Tokenizer8051();
    }

    private List<Token> getTokens(Path source, List<Problem> problems) {
        Path directory = Paths.get("."); // TODO: Use Project.getWorkingDirectory
        List<String> inputOutput = new LinkedList<>();
        problems.addAll(preprocessor.preprocess(directory, source, inputOutput));


        return tokenizer.tokenize(inputOutput, problems);
    }

    @Override
    public byte[] assemble(Path source, List<Problem> problems) {
        result = new byte[0xFFFF+1];

        List<LabelToken> labels = new LinkedList<>();

        List<Token> tokens = getTokens(source, problems);

        List<Assembled8051> assembled = toAssembled(tokens, labels, problems, source);





        return result;
    }

    private List<Assembled8051> toAssembled(List<Token> tokens, List<LabelToken> labels,
                                            List<Problem> problems, Path source) {
        int origin = 0;
        Path currentFile = source;

        List<Assembled8051> result = new LinkedList<>();

        List<Token> tokenList = new LinkedList<>();
        List<LabelToken> localLabels = new LinkedList<>();

        for (Token t : tokens) {
            switch (t.getType()) {
                case MNEMONIC_NAME:
                {
                    if (tokenList.size() > 0) {
                        result.add(new Assembled8051(origin, 0, tokenList, localLabels, currentFile));
                        tokenList.clear();
                        localLabels.clear();
                    }
                    tokenList.add(t);
                    break;
                }
                case OPERAND:
                {
                    tokenList.add(t); // Assume that the tokenizer worked
                    break;
                }
                case LABEL:
                {
                    if (tokenList.size() > 0) { // Assume that a new Mnemonic has started
                        result.add(new Assembled8051(origin, 0, tokenList, localLabels, currentFile));
                        tokenList.clear();
                        localLabels.clear();
                    }
                    labels.add((LabelToken) t);
                    localLabels.add((LabelToken) t);
                    break;
                }
                case DIRECTIVE:
                {
                    if (t instanceof DirectiveTokens.FileChangeToken)
                        currentFile = ((DirectiveTokens.FileChangeToken) t).getFile();
                    else if (t instanceof DirectiveTokens.DataToken) {
                        if (tokenList.size() > 0) {
                            result.add(new Assembled8051(origin, 0, tokenList, localLabels, currentFile));
                            tokenList.clear();
                            localLabels.clear();
                        }

                        tokenList.add(t);   // A data token is always a single Assembled.
                        result.add(new Assembled8051(origin, 0, tokenList, localLabels, currentFile));
                        tokenList.clear();
                        localLabels.clear();

                    } else if (t instanceof DirectiveTokens.OriginChangeToken) {
                        origin = (int) ((DirectiveTokens.OriginChangeToken) t).getAddress();
                        for (LabelToken lt : localLabels) {
                            problems.add(new TokenProblem("Label has no associated instruction!", Problem.Type.ERROR,
                                    currentFile, lt)); // Origin and all addresses would change afterwards, making the
                            labels.remove(lt);         // label referring to nothing,
                        }
                        localLabels.clear();
                    } else
                        throw new IllegalStateException("Unknown directive token:" + t);
                    break;
                }
            }
        }

        return result;
    }

    private void resolve(List<Assembled8051> assembled, List<Problem> problems) {

    }
}
