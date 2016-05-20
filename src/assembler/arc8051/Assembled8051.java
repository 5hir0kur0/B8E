package assembler.arc8051;

import assembler.tokens.LabelToken;
import assembler.tokens.OperandToken;
import assembler.tokens.Token;
import assembler.tokens.Tokens;
import assembler.util.problems.Problem;
import assembler.util.problems.TokenProblem;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * @author Noxgrim
 */
public class Assembled8051 {

    private int origin;

    private int originOffset;

    private final Token[] tokens;

    private Path file;

    private byte[] result;

    private boolean isStatic;

    private final int[] convertibleNamesIndexes;

    private Mnemonic8051 mnemonicCache;

    public Assembled8051(int origin, byte[] result, List<Token> tokens, int originOffset, Path file) {
        this.origin = origin;
        this.result = result;
        if (tokens.size() == 0)
            throw new IllegalArgumentException("Must have at least one associated Token!");
        this.tokens = tokens.toArray(new Token[tokens.size()]);

        List<Integer> convertible = new LinkedList<>();
        for (int i = 1; i < this.tokens.length; i++) {
            if (this.tokens[i] instanceof OperandToken8051) {
                OperandToken8051 token = (OperandToken8051) this.tokens[i];
                if (token.getValue().equals("c") || token.getValue().equals("c")) {
                    convertible.add(i-1);       // Without mnemonic name
                    if (convertible.size() < 3) // Max operand length of all mnemonics: 3
                        break;                  // (all of them contain at least 1 label)
                }
            } else
                throw new IllegalArgumentException("Expected a OperandToken here instead of " + this.tokens[i]);
        }
        this.convertibleNamesIndexes = new int[convertible.size()];
        for (int i = 0; i < convertibleNamesIndexes.length; i++)
            convertibleNamesIndexes[i] = convertible.get(i);

        this.file = file;
        this.originOffset = originOffset;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public int compile(List<Problem> problems, List<LabelToken> labels) {
        if (isStatic)
            return 0;

        byte[] result;

        if (tokens[0] instanceof Tokens.MnemonicNameToken) {
            if (mnemonicCache == null) {
                Optional<Mnemonic8051> o = Arrays.stream(MC8051Library.mnemonics)
                        .filter(x -> x.getName().equals(tokens[0].getValue()))
                        .findFirst();

                if (o.isPresent())
                    mnemonicCache = o.get();
                else {
                    problems.add(new TokenProblem("Unknown mnemonic!", Problem.Type.ERROR, file, tokens[0]));
                    isStatic = true;
                    return 0;
                }
            }
            OperandToken[] tmpTokens = new OperandToken[tokens.length - 1];
            boolean isStatic = true;

            // Preparation
            outer:
            for (int i = 1; i < tokens.length; ++i)
                if (!(tokens[i] instanceof OperandToken))
                    throw new IllegalStateException("Expected a OperandToken here instead of " + tokens[i]);
                else {
                    OperandToken8051 token = (OperandToken8051) tokens[i];
                    if (token.getOperandRepresentation().isSymbol() &&
                            !token.getOperandType().isName() && !token.getOperandType().isIndirect()) {
                        isStatic = false;
                        for (LabelToken l : labels)
                            if (token.getValue().equals(l.getValue())) {
                                tmpTokens[i - 1] = token.toNumber(l.getCodePoint());
                                continue outer;
                            }
                        problems.add(new TokenProblem("Unresolved symbol!", Problem.Type.ERROR, file, token));
                        return 0;
                    } else {
                        tmpTokens[i - 1] = token;
                    }
                }
            this.isStatic = isStatic;

            // Compile
            if (convertibleNamesIndexes.length == 0)
                result = mnemonicCache.getInstructionFromOperands(0, (Tokens.MnemonicNameToken) tokens[0], tmpTokens);
            else {
                result = null;
                OperandToken8051[] tmpTmpTokens;
                for (int pos = 0, length = (int) Math.pow(2, convertibleNamesIndexes.length); pos < length; ++pos) {
                    tmpTmpTokens = (OperandToken8051[]) Arrays.copyOf(tmpTokens, tmpTokens.length);
                    for (int j = 0; j < convertibleNamesIndexes.length; ++j)
                        if ((1 << j | pos) > 0) {
                            OperandToken8051 ot = tmpTmpTokens[convertibleNamesIndexes[j]];
                            if (ot.getValue().equals("a"))
                                ot = ot.toNumber(MC8051Library.A);
                            else
                                ot = ot.toNumber(MC8051Library.C);
                            tmpTmpTokens[convertibleNamesIndexes[j]] = ot;
                        }

                    result = mnemonicCache.getInstructionFromOperands(0,
                            (Tokens.MnemonicNameToken) tokens[0], tmpTmpTokens);
                    if (result.length > 0)
                        break;
                }
            }

            return result.length - this.result.length;

        } else if (tokens[0] instanceof DirectiveTokens.DataToken) {
            isStatic = true;

            this.result = ((DirectiveTokens.DataToken) tokens[0]).getData();

            return this.result.length;
        } else {
            throw new IllegalArgumentException("Cannot be compiled!: " + tokens[0]);
        }
    }
}
