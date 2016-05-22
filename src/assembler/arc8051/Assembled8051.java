package assembler.arc8051;

import assembler.tokens.LabelToken;
import assembler.tokens.OperandToken;
import assembler.tokens.Token;
import assembler.tokens.Tokens;
import assembler.util.assembling.Assembled;
import assembler.util.problems.Problem;
import assembler.util.problems.TokenProblem;

import java.nio.file.Path;
import java.util.*;

/**
 * @author Jannik
 */
public class Assembled8051 implements Assembled{

    private final int origin;

    private int originOffset;

    private final Token[] tokens;

    private Path file;

    private byte[] codes;

    private boolean isStatic;

    private final int[] convertibleNamesIndexes;

    private Mnemonic8051 mnemonicCache;

    public Assembled8051(int origin, List<Token> tokens, int originOffset, Path file) {
        this.origin = origin;
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
        clearRelatedProblems(problems);

        if (tokens[0] instanceof Tokens.MnemonicNameToken) {
            if (mnemonicCache == null) {
                Optional<Mnemonic8051> o = Arrays.stream(MC8051Library.mnemonics)
                        .filter(x -> x.getName().equals(tokens[0].getValue()))
                        .findFirst();

                if (o.isPresent())
                    mnemonicCache = o.get();
                else {
                    problems.add(new TokenProblem("Unknown mnemonic!", Problem.Type.ERROR, file, tokens[0]));
                    isStatic = true; // Prevent future recompilation.
                    return 0;
                }
            }
            // Test minimum operands.
            if (mnemonicCache.getMinimumOperands() > tokens.length-1) {
                problems.add(new TokenProblem("Found " + (tokens.length-1) + " operands but mnemonic must have at " +
                        "least " + mnemonicCache.getMinimumOperands() +
                        " operand" + (mnemonicCache.getMinimumOperands() != 1 ? "s" : "" ) + "!",
                        Problem.Type.ERROR, file, tokens[0]));
                isStatic = true;
                return 0;
            }


            OperandToken8051[] tmpTokens = new OperandToken8051[tokens.length - 1];
            boolean isStatic = !mnemonicCache.isPositionSensitive();

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
            if (convertibleNamesIndexes.length == 0) // No uses of A or C
                result = mnemonicCache.getInstructionFromOperands(origin+originOffset, (Tokens.MnemonicNameToken) tokens[0],
                        tmpTokens, file, problems);
            else { // Try to compile it. If no success, try to replace As and Cs with their associated addresses.
                result = null;
                OperandToken8051[] tmpTmpTokens;
                for (int pos = 0, length = (int) Math.pow(2, convertibleNamesIndexes.length); pos < length; ++pos) {
                    tmpTmpTokens =  Arrays.copyOf(tmpTokens, tmpTokens.length);
                    for (int j = 0; j < convertibleNamesIndexes.length; ++j)
                        if ((1 << j | pos) > 0) {
                            OperandToken8051 ot = tmpTmpTokens[convertibleNamesIndexes[j]];
                            if (ot.getValue().equals("a"))
                                ot = ot.toNumberAddress(MC8051Library.A);
                            else
                                ot = ot.toNumberAddress(MC8051Library.C);
                            tmpTmpTokens[convertibleNamesIndexes[j]] = ot;
                        }

                    result = mnemonicCache.getInstructionFromOperands(origin+originOffset,
                            (Tokens.MnemonicNameToken) tokens[0], tmpTmpTokens, file, problems);
                    if (result.length > 0)
                        break;
                }
            }

            return result.length - this.codes.length;

        } else if (tokens[0] instanceof DirectiveTokens.DataToken) {
            isStatic = true;

            this.codes = ((DirectiveTokens.DataToken) tokens[0]).getData();

            return this.codes.length;
        } else {
            throw new IllegalArgumentException("Cannot be compiled!: " + tokens[0]);
        }
    }

    private void clearRelatedProblems(List<Problem> problems) {
        Problem p;
        for (Iterator<Problem> i = problems.iterator(); i.hasNext();) {
            p = i.next();
            if (p instanceof TokenProblem) {
                Token cause = ((TokenProblem) p).getCause();
                for (Token t : this.tokens)
                    if (t.equals(cause)) {
                        i.remove();
                        break;
                    }
            }
        }
    }

    @Override
    public byte[] getCodes() {
        return codes;
    }

    @Override
    public long getAddress() {
        return origin+originOffset;
    }

    public Path getFile() {
        return file;
    }


    @Override
    public long getOrigin() {
        return origin;
    }

    public Token[] getTokens() {
        return tokens;
    }

    @Override
    public void moveAddress(long amount) {
        this.originOffset += amount;
        if (this.originOffset < 0) {
            originOffset = 0;
            throw new IllegalArgumentException("Resulting address cannot be smaller than the origin address!");
        }
    }
}
