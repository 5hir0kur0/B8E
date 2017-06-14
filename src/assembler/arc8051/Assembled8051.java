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
 * @author Noxgrim
 */
public class Assembled8051 implements Assembled {

    private final int origin;

    private int originOffset;

    private final Token[] tokens;

    private final Path file;

    private byte[] codes;

    private boolean isStatic;

    private final int[] convertibleNamesIndexes;

    private Mnemonic8051 mnemonicCache;

    private final LabelToken[] labels;

    public Assembled8051(int origin, int originOffset, List<Token> tokens, List<LabelToken> labels, Path file) {
        if ((this.origin = origin) < 0)
            throw new IllegalArgumentException("Origin address cannot be negative");
        if ((this.originOffset = originOffset) < 0)
            throw new IllegalArgumentException("Origin address offset cannot be negative");
        if (tokens.size() == 0)
            throw new IllegalArgumentException("Must have at least one associated Token!");
        this.tokens = tokens.toArray(new Token[tokens.size()]);
        this.codes = new byte[0];

        List<Integer> convertible = new LinkedList<>();
        for (int i = 1; i < this.tokens.length; i++) {
            if (this.tokens[i] instanceof OperandToken8051) {
                OperandToken8051 token = (OperandToken8051) this.tokens[i];
                if (token.getValue().equals("a") || token.getValue().equals("c")) {
                    convertible.add(i-1);       // Without mnemonic name
                    if (convertible.size() >= 2) // Max operand length of all mnemonics: 3 Effective: 2
                        break;                   // (all of them contain at least 1 jump label at the end)
                }
            } else
                throw new IllegalArgumentException("Expected a OperandToken here instead of " + this.tokens[i]);
        }
        this.convertibleNamesIndexes = new int[convertible.size()];
        for (int i = 0; i < convertibleNamesIndexes.length; i++)
            convertibleNamesIndexes[i] = convertible.get(i);

        this.labels = labels.toArray(new LabelToken[labels.size()]);
        for (LabelToken lt : labels)
            lt.setAttachedTo(this);
        this.file = file;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    @Override
    public int compile(List<Problem<?>> problems, List<LabelToken> labels) {
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
                problems.add(new TokenProblem("Found " + (tokens.length-1) + " operand" + (tokens.length == 2?"":"s") +
                        " but mnemonic must have at least " + mnemonicCache.getMinimumOperands() +
                        " operand" + (mnemonicCache.getMinimumOperands() != 1 ? "s" : "" ) + "!",
                        Problem.Type.ERROR, file, tokens[0]));
                isStatic = true;
                return 0;
            }


            OperandToken8051[] tmpTokens = new OperandToken8051[tokens.length - 1];
            boolean isStatic = !mnemonicCache.isPositionSensitive(), unresolved = false;

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
                                tmpTokens[i - 1] = token.toNumber(l.getAddress());
                                continue outer;
                            }
                        problems.add(new TokenProblem("Unresolved symbol!", Problem.Type.ERROR, file, token));
                        unresolved = true;
                    } else {
                        tmpTokens[i - 1] = token;
                    }
                }
            if (unresolved) {          // At this point it is assumed, that all tokens have been
                this.isStatic = true;  // converted into Assembled and all labels have been found
                return 0;              // so if a symbol has not been resolved it will never be
            }                          // resolved marking the whole Assembled invalid.
            this.isStatic = isStatic;

            // Compile
            if (convertibleNamesIndexes.length == 0) {// No uses of A or C
                clearRelatedProblems(problems);
                result = mnemonicCache.getInstructionFromOperands((origin + originOffset) & 0xFFFF, // Prevent out of bounds
                        (Tokens.MnemonicNameToken) tokens[0], tmpTokens, file, problems);
            } else { // Try to compile it. If no success, try to replace As and Cs with their associated addresses.
                result = null;
                OperandToken8051[] tmpTmpTokens;
                for (int pos = 0, length = (int) Math.pow(2, convertibleNamesIndexes.length); pos < length; ++pos) {
                    tmpTmpTokens =  Arrays.copyOf(tmpTokens, tmpTokens.length);
                    for (int j = 0; j < convertibleNamesIndexes.length; ++j)
                        if ((pos & 1 << j) > 0) {
                            OperandToken8051 ot = tmpTmpTokens[convertibleNamesIndexes[j]];
                            if (ot.getValue().equals("a"))
                                ot = ot.toNumberAddress(MC8051Library.A & 0xFF);
                            else
                                ot = ot.toNumberAddress(MC8051Library.C & 0xFF);
                            tmpTmpTokens[convertibleNamesIndexes[j]] = ot;
                        }
                    clearRelatedProblems(problems);
                    result = mnemonicCache.getInstructionFromOperands((origin + originOffset) & 0xFFFF, // Prevent out of bounds
                            (Tokens.MnemonicNameToken) tokens[0], tmpTmpTokens, file, problems);
                    if (result.length > 0)
                        break;
                }
            }

            int len = this.codes.length;
            this.codes = result;

            return result.length - len;

        } else if (tokens[0] instanceof DirectiveTokens.DataToken) {
            isStatic = true;

            this.codes = ((DirectiveTokens.DataToken) tokens[0]).getData();

            return this.codes.length;
        } else {
            throw new IllegalArgumentException("Cannot be compiled!: " + tokens[0]);
        }
    }

    private void clearRelatedProblems(List<Problem<?>> problems) {
        Problem p;
        for (Iterator<Problem<?>> i = problems.iterator(); i.hasNext();) {
            p = i.next();
            if (p instanceof TokenProblem) {
                Token cause = ((TokenProblem) p).getCause();
                Token compare = tokens[0];
                if (p.getPath().equals(file) &&
                    cause.getLine() == compare.getLine() &&
                    cause.getInstructionId() == compare.getInstructionId())
                {
                    i.remove();
                }
            }
        }
    }

    public boolean hasLabels() {
        return labels.length > 0;
    }

    @Override
    public LabelToken[] getLabels() {
        return labels;
    }

    @Override
    public byte[] getCodes() {
        return codes;
    }

    @Override
    public long getAddress() {
        return origin+originOffset;
    }

    @Override
    public Path getFile() {
        return file;
    }

    @Override
    public long getOrigin() {
        return origin;
    }

    @Override
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
        for (LabelToken lt : labels)
            lt.moveAddress(amount);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[Address:"+getAddress()+"("+origin+":"+originOffset+") Codes:"+
                Arrays.toString(codes)+" Labels:"+Arrays.toString(labels)+" Tokens:"+Arrays.toString(tokens)+" File:"+file+"]";
    }
}
