package assembler.arc8051;

import assembler.tokens.Token;
import assembler.tokens.Tokens;
import assembler.util.AssemblerSettings;
import assembler.util.problems.Problem;
import assembler.util.problems.Problem.Type;
import assembler.util.problems.TokenProblem;
import misc.Settings;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static assembler.arc8051.OperandToken8051.OperandType8051;


/**
 * A simple class that contains all resources that are needed by classes that are
 * a specification of the 8051 architecture.<br>
 *
 * This class contains:<br>
 * - The Architecture Provider<br>
 * &nbsp; - A List of Problems<br>
 * &nbsp; - All Mnemonics<br>
 * - Patterns for the Tokenizer and Preprocessor<br>
 * - Some other utility methods<br>
 *
 * @author Noxgrim
 */
public class MC8051Library {
    /** Class cannot be initialized. */
    private MC8051Library() {}

    /**
     * Pattern for a valid symbol.<br>
     * A symbol is any word character that is not a digit followed
     * by any amount of valid word characters.<br>
     * Regex: <code>"([\w&&[\D]]\w*?)"</code>
     */
    public static final Pattern SYMBOL_PATTERN          = Pattern.compile("\\b([\\w&&[\\D]]\\w*)\\b");
    /**
     * Pattern for a valid number.<br>
     * Any digit followed by any amount of valid word characters.<br>
     * Regex: <code>"(\d\w*)"</code>
     */
    public static final Pattern NUMBER_PATTERN          = Pattern.compile("\\b(\\d\\w*)\\b");

    /**
     * Pattern for a valid label.<br>
     * White space followed by a valid SYMBOL followed by a <code>':'</code>.<br>
     * Regex: <code>"\s*([\w&&[\D]][\w]*?):"</code>
     * @see #SYMBOL_PATTERN
     */
    public static final Pattern LABEL_PATTERN           = Pattern.compile("\\s*"+SYMBOL_PATTERN.toString()+"\\s*:");
    /**
     * Pattern for a valid mnemonic name.<br>
     * Basically a SYMBOL surrounded by possible white space.<br>
     * Regex: <code>"\s*([\w&&[\D]]\w*?)\s*"</code>
     * @see #SYMBOL_PATTERN
     */
    public static final Pattern MNEMONIC_NAME_PATTERN   = Pattern.compile("\\s*"+SYMBOL_PATTERN.toString()+"\\s*");
    /**
     * Pattern for a valid indirect symbol, a symbol that indicates indirect addressing.<br>
     * An <code>'@'</code> followed by a SYMBOL that optional can be split into two by a
     * <code>'+'</code> that is surrounded by any white space.<br>
     * Regex: <code>"@([\\w&&[\\D]]+?\\w*(?:\\s*\\+\\s*)?\\w*?)"</code>
     * @see #SYMBOL_PATTERN
     */
    public static final Pattern SYMBOL_INDIRECT_PATTERN = Pattern.compile("@([\\w&&[\\D]]+?\\w*(?:\\s*\\+\\s*)?\\w*?)");

    /**
     * Pattern for a valid address.<br>
     * The same as a NUMBER.<br>
     * Regex: <code>"(\d\w*?)"</code>
     * @see #NUMBER_PATTERN
     */
    public static final Pattern ADDRESS_PATTERN         = Pattern.compile(NUMBER_PATTERN.toString());
    /**
     * Pattern for valid bit addressing with a <code>'.'</code>.<br>
     * A <code>'.'</code> enclosed by two NUMBERS.<br>
     * Regex: <code>"(\d\w*?).(\d\w*?)"</code>
     * @see #NUMBER_PATTERN
     */
    public static final Pattern BIT_ADDRESSING_PATTERN = Pattern.compile( NUMBER_PATTERN.toString() + "\\." +
                                                                          NUMBER_PATTERN.toString());
    /**
     * Pattern for a valid constant.<br>
     * A <code>'#'</code> followed by a NUMBER.<br>
     * Regex: <code>"#(\d\w*?)"</code>
     * @see #NUMBER_PATTERN
     */
    public static final Pattern CONSTANT_PATTERN        = Pattern.compile("#" + NUMBER_PATTERN.toString());
    /**
     * Pattern for a valid negated address, a address that implies that the
     * address has to be complemented first.<br>
     * A <code>'/'</code> followed by a NUMBER.<br>
     * Regex: <code>"/(\d\w*?)"</code>
     * @see #NUMBER_PATTERN
     */
    public static final Pattern NEGATED_ADDRESS_PATTERN = Pattern.compile("/" + NUMBER_PATTERN.toString());
    /**
     * Pattern for a valid address offset that is not absolute like a normal
     * address but instead relative to the address of the mnemonic.<br>
     * A <code>'+'</code> or <code>'-'</code> followed by a NUMBER.<br>
     * Regex: <code>"[+-](d\w*?)"</code>
     * @see #NUMBER_PATTERN
     */
    public static final Pattern ADDRESS_OFFSET_PATTERN  = Pattern.compile("[+-]" + NUMBER_PATTERN.toString());


    /**
     * Pattern for a valid assembler directive.<br>
     * An assembler directive always starts with a <code>'$'</code> character.
     * If a line starts with an <code>'$'</code>, the full line will be taken as
     * the directive and no further mnemonics etc. can be written.<br>
     * Regex: <code>"^\s*\$(\w*)\s*([\S].*)?$"</code>
     */
    public static final Pattern DIRECTIVE_PATTERN       = Pattern.compile("^\\s*[\\$\\.](\\w*)\\s*(.*?)?\\s*$");
    /**
     * Pattern to capture a String.<br>
     * A String is any set of characters surrounded by <code>'"'</code> or
     * <code>'\''</code>.<br>
     * Regex: <code>"(?:(?<!\\)".*?(?<!\\)")|(?:(?<!\\)'.*?(?<!\\)')"</code>
     */
    public static final Pattern STRING_PATTERN          = Pattern.compile("(?:(?<!\\\\)\"(.*?)(?<!\\\\)\")|" +
                                                                          "(?:(?<!\\\\)'(.*?)(?<!\\\\)')");

    /**
     * Pattern for a valid file extension.
     */
    public static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("\\.\\w+");

    /** Reserved symbols. Contains "A", "C" and all R-Registers. */
    public static final String[] RESERVED_NAMES = {"A", "C", "DPTR", "AB", "R0", "R1", "R2", "R3", "R4", "R5", "R6", "R7"};
    /** Reserved indirect symbols.*/
    public static final String[] RESERVED_INDIRECT_NAMES = {"DPTR", "A+DPTR", "A+PC", "R0", "R1"};

    /** The address of the accumulator. (<code>0xE0</code>) */
    public static final byte A = (byte) 0xE0;
    /** The bit address of the carry flag. (<code>0xD7</code>) */
    public static final byte C = (byte) 0xD7;

    /**
     * An array of all supported Mnemonics of the 8051 architecture.
     */
    public static final Mnemonic8051[] mnemonics = {

            new Mnemonic8051("acall", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return absoluteCodeJump(0x11, this, codePoint, operands, file, problems);
                }
            },

            new Mnemonic8051("add", 2) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return additiveOperation((byte) 0x24, (byte) 0x26, (byte) 0x25, (byte) 0x28, this, operands,
                                             file, problems);
                }
            },

            new Mnemonic8051("addc", 2) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return additiveOperation((byte) 0x34, (byte) 0x36, (byte) 0x35, (byte) 0x38, this, operands,
                            file, problems);
                }
            },

            new Mnemonic8051("ajmp", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return absoluteCodeJump(0x01, this, codePoint, operands, file, problems);
                }
            },

            new Mnemonic8051("anl", 2) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return bitwiseLogicalOperation((byte) 0x54, (byte) 0x56, (byte) 0x55, (byte) 0x58,
                            (byte) 0xb0, (byte) 0x82, (byte) 0x52, (byte) 0x53, true, this, operands, file, problems);
                }
            },

            new Mnemonic8051("call", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name, OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    byte[] result = new byte[0];

                    OperandToken8051 op = operands[0];
                    if (op.getOperandType().isAddress() || op.getOperandType().isIndirect()) {

                        long jump = Integer.parseInt(op.getValue());

                        if (operands[0].getOperandType() == OperandType8051.ADDRESS_OFFSET) {
                            jump = getFromOffset(codePoint, jump, 0, operands[0], file, problems);
                        }
                        if ((jump >>> 11L // Shift 11 right to clear changing bits
                                & 0x1fL) // Clear all bytes but the first five
                                == (codePoint >>> 11L & 0x1fL)) { // Compare: If equal an absolute jump is possible.
                            result = Arrays.stream(mnemonics).filter(m -> m.getName().equals("acall")).findFirst().get()
                                    .getInstructionFromOperands(codePoint, name, operands, file, problems);

                            if (result.length > 0)
                                return result;
                        }

                        result = Arrays.stream(mnemonics).filter(m -> m.getName().equals("lcall")).findFirst().get()
                                .getInstructionFromOperands(codePoint, name, operands, file, problems);

                    } else
                        problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op));

                    handleUnnecessaryOperands(this.getName(), 1, operands, file, problems);

                    return result;
                }
            },

            new Mnemonic8051("cjne", 3, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    byte[] result = new byte[0];

                    OperandToken8051 op1 = operands[0], op2 = operands[1], op3 = operands[2];
                    OperandType8051 type1 = op1.getOperandType(), type2 = op2.getOperandType();

                    Byte offset = handleShortJump(codePoint, op3, 3, file, problems);
                    if (offset == null)
                        return result;

                    int value;

                    if (type2 == OperandType8051.ADDRESS || type2 == OperandType8051.CONSTANT) {
                        value = Integer.parseInt(op2.getValue());
                        if (value <= 0xff) {
                            if (type1 == OperandType8051.NAME || type1 == OperandType8051.INDIRECT) {
                                if (op1.getValue().equals("a"))
                                    result = new byte[]{(byte) (type2 == OperandType8051.CONSTANT ? 0xB4 : 0xB5),
                                            (byte) value, offset};
                                else if (op1.getValue().startsWith("r")) {
                                    int ordinal = Integer.parseInt(op1.getValue().substring(1));
                                    if (ordinal > (type1 == OperandType8051.NAME ? 7 : 1))
                                        problems.add(new TokenProblem("Register ordinal too high!", Type.ERROR, file, op1));
                                    else {
                                        result = new byte[]{(byte) (ordinal
                                                | (type1 == OperandType8051.NAME ? 0xB8 : 0xB6)), // Set desired bits to ordinal
                                                (byte) value, offset};
                                    }
                                }
                            } else
                                problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op1));
                        } else
                            problems.add(new TokenProblem("Value of " +
                                    (type2 == OperandType8051.ADDRESS ? "direct address" : "constant") + " too big!",
                                    Type.ERROR, file, op2));
                    } else
                        problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op2));

                    handleUnnecessaryOperands(this.getName(), 3, operands, file, problems);

                    return result;
                }
            },

            new Mnemonic8051("clr", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return bitOperation((byte) 0xE4, (byte) 0xC3, (byte) 0xC2, true, this, operands, file, problems);
                }
            },

            new Mnemonic8051("cpl", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return bitOperation((byte) 0xF4, (byte) 0xB3, (byte) 0xB2, true, this, operands, file, problems);
                }
            },

            new Mnemonic8051("da", 0) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return accumulatorOperation((byte) 0xD4, this, operands, file, problems);
                }
            },

            new Mnemonic8051("dec", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return incDecOperation((byte) 0x14, (byte) 0x16, (byte) 0x18, (byte) 0x15, this, operands,
                            file, problems);
                }
            },

            new Mnemonic8051("div", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return multiplicativeOperation((byte) 0x84, this, name, operands, file, problems);
                }
            },

            new Mnemonic8051("djnz", 2, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    byte[] result = new byte[0];
                    OperandToken8051 op1 = operands[0], op2 = operands[1];
                    OperandType8051 type1 = op1.getOperandType();

                    switch (type1) {
                        case ADDRESS: {
                            int value = Integer.parseInt(op1.getValue());
                            Byte offset = handleShortJump(codePoint,op2, 3, file, problems);
                            if (offset == null)
                                return result;
                            if (value <= 0xff)
                                result = new byte[]{(byte) 0xD5, (byte) value, offset};
                            else
                                problems.add(new TokenProblem("Value of direct address too big!", Type.ERROR, file, op1));
                            break;
                        }
                        case NAME:
                            if (op1.getValue().startsWith("r")) {
                                int ordinal = Integer.parseInt(op1.getValue().substring(1));
                                Byte offset = handleShortJump(codePoint,op2, 2, file, problems);
                                if (offset == null)
                                    return result;
                                if (ordinal > 7)
                                    problems.add(new TokenProblem("Register ordinal too big!", Type.ERROR, file, op1));
                                else
                                    return new byte[]{(byte)(ordinal | 0xD8), offset};
                                break;
                            }
                        default:
                            problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op1));
                    }

                    return result;
                }
            },

            new Mnemonic8051("inc", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    if (operands[0].getOperandType() == OperandType8051.NAME && operands[0].getValue().equals("dptr")) {
                        handleUnnecessaryOperands(this.getName(), 1, operands, file, problems);
                        return new byte[]{(byte) 0xA3};
                    } else
                        return incDecOperation((byte) 0x04, (byte) 0x06, (byte) 0x08, (byte) 0x05, this, operands,
                                               file, problems);
                }
            },

            new Mnemonic8051("jb", 2, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return jumpBitRelevant((byte) 0x20, this, codePoint, operands, file, problems);
                }
            },

            new Mnemonic8051("jbc", 2, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return jumpBitRelevant((byte) 0x10, this, codePoint, operands, file, problems);
                }
            },

            new Mnemonic8051("jc", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return jumpNameRelevant((byte) 0x40, this, codePoint, operands, file, problems);
                }
            },

            new Mnemonic8051("jmp", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    byte[] result = new byte[0];
                    OperandToken8051 op = operands[0];
                    if (op.getOperandType() == OperandType8051.INDIRECT) {
                        if (!(op.getValue().equals("a+dptr")))
                            problems.add(new TokenProblem("Incompatible operand! " +
                                    "(Expected \"@a+dptr\")", Type.WARNING, file, op));
                        else
                            result = new byte[]{(byte) 0x73};
                    } else if (op.getOperandType().isAddress() || op.getOperandType().isIndirect()) {

                        long jump = Integer.parseInt(op.getValue());

                        if (operands[0].getOperandType() == OperandType8051.ADDRESS_OFFSET) {
                            jump = getFromOffset(codePoint, jump, 0, operands[0], file, problems);
                        }
                        if ((jump >>> 11L // Shift 11 right to clear changing bits
                                & 0x1fL) // Clear all bytes but the first five
                                == (codePoint >>> 11L & 0x1fL)) { // Compare: If equal an absolute jump is possible.
                            result = Arrays.stream(mnemonics).filter(m -> m.getName().equals("ajmp")).findFirst().get()
                                    .getInstructionFromOperands(codePoint, name, operands, file, problems);

                            if (result.length > 0)
                                return result;
                        } else
                        if (jump >= -128 && jump <= 127) {
                            result = Arrays.stream(mnemonics).filter(m -> m.getName().equals("sjmp")).findFirst().get()
                                .getInstructionFromOperands(codePoint, name, operands, file, problems);

                            if (result.length > 0)
                                return result;
                        }

                        result = Arrays.stream(mnemonics).filter(m -> m.getName().equals("ljmp")).findFirst().get()
                                .getInstructionFromOperands(codePoint, name, operands, file, problems);

                    } else
                        problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op));

                    handleUnnecessaryOperands(this.getName(), 1, operands, file, problems);

                    return result;
                }
            },

            new Mnemonic8051("jnb", 2, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return jumpBitRelevant((byte) 0x30, this, codePoint, operands, file, problems);
                }
            },

            new Mnemonic8051("jnc", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return jumpNameRelevant((byte) 0x50, this, codePoint, operands, file, problems);
                }
            },

            new Mnemonic8051("jnz", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return jumpNameRelevant((byte) 0x70, this, codePoint, operands, file, problems);
                }
            },

            new Mnemonic8051("jz", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return jumpNameRelevant((byte) 0x60, this, codePoint, operands, file, problems);
                }
            },

            new Mnemonic8051("lcall", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return longJump((byte) 0x12, this, codePoint, operands, file, problems);
                }
            },

            new Mnemonic8051("ljmp", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return longJump((byte) 0x02, this, codePoint, operands, file, problems);
                }
            },

            new Mnemonic8051("mov", 2) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    byte[] result = new byte[0];

                    OperandToken8051 op1 = operands[0], op2 = operands[1];
                    OperandType8051 type1 = op1.getOperandType(), type2 = op2.getOperandType();
                    int ord1 = -1, ord2 = -1; // Ordinals for Rn and @Ri operands

                    for (int i = 0; i < 2; ++i) {
                        OperandToken8051 op = operands[i];  OperandType8051 type = op.getOperandType();
                        if ((type == OperandType8051.NAME || type == OperandType8051.INDIRECT) &&
                                op.getValue().startsWith("r")) {
                            int ordinal = Integer.parseInt(op.getValue().substring(1));

                            if (ordinal < (type == OperandType8051.NAME ? 8 : 2))
                                if (i == 0) ord1 = ordinal;
                                else ord2 = ordinal;
                            else
                                problems.add(new TokenProblem("Register ordinal too big!", Type.ERROR, file, op));
                        }
                    }


                    // Begin assembling
                    switch (type1) {
                        case ADDRESS: {
                            int dest = Integer.parseInt(op1.getValue());
                            if (dest > 0xff)
                                problems.add(new TokenProblem("Value of direct address too big!", Type.ERROR, file, op1));
                            else
                                switch (type2) {
                                    case ADDRESS: {
                                        int src = Integer.parseInt(op2.getValue());
                                        if (src > 0xff)
                                            problems.add(new TokenProblem("Value of direct address" +
                                                    " too big!", Type.ERROR, file, op2));
                                        else
                                            result = new byte[]{(byte) 0x85, (byte) src, (byte) dest}; //address, address
                                        break;
                                    }
                                    case CONSTANT: {
                                        int val = Integer.parseInt(op2.getValue());
                                        if (val > 0xff)
                                            problems.add(new TokenProblem("Value of constant too" +
                                                    " big!", Type.ERROR, file, op2));
                                        else
                                            result = new byte[]{(byte) 0x75, (byte) dest, (byte) val}; //address, #constant
                                        break;
                                    }
                                    case INDIRECT:
                                    case NAME: {
                                        final String val = op2.getValue();
                                        if (val.equals("a")) {
                                            result = new byte[]{(byte) 0xF5, (byte) dest};    // address, a
                                            break;
                                        } else if (val.equals("c")) {
                                            result = new byte[]{(byte) 0x92, (byte) dest};    // bit address, c
                                            break;
                                        } else if (val.startsWith("r") && ord2 != -1) {
                                            result = new byte[]{(byte) (ord2
                                                    | (type2 == OperandType8051.NAME ? 0x88 : // address,  Rn
                                                    0x86)),                                   // address, @Ri
                                                    (byte) dest};
                                            break;
                                        } else if (ord2 == -1)
                                            break;
                                    }
                                    default:
                                        problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op2));
                                }
                            break;
                        }
                        case INDIRECT:
                        case NAME: {
                            String val = op1.getValue();
                            if (val.equals("c")) {
                                if (type2 == OperandType8051.ADDRESS) {
                                   int valAd = Integer.parseInt(op2.getValue());
                                    if (valAd > 0xff)
                                        problems.add(new TokenProblem("Value of direct address too" +
                                                " big!", Type.ERROR, file, op2));
                                    else
                                        result = new byte[] {(byte) 0xA2, (byte) valAd};      // c, address
                                } else
                                    problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op2));
                                break;
                            } else if (val.equals("dptr")) {
                                if (type2 == OperandType8051.CONSTANT) {
                                    int val2 = Integer.parseInt(op2.getValue());
                                    result = new byte[] {(byte) 0x90,                         // dptr, #constant16
                                                         (byte)(val2 >>> 8 & 0xff), // Extract high byte
                                                         (byte)(val2 & 0xff)};      // Extract low byte
                                } else
                                    problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op2));
                                break;
                            } else if (val.equals("a")) {
                                switch (type2) {
                                    case CONSTANT:
                                    case ADDRESS: {
                                        int val2 = Integer.parseInt(op2.getValue());
                                        if (val2 > 0xff)
                                            problems.add(new TokenProblem("Value of " +
                                                    (type2 == OperandType8051.ADDRESS ? "direct address" : "constant") +
                                                    " too big!", Type.ERROR, file, op2));
                                        else
                                            return new byte[]{(byte)(type2 == OperandType8051.ADDRESS ? 0xE5 ://a,direct
                                            0x74), (byte) val2};                              // a, #constant
                                        break;
                                    }
                                    case INDIRECT:
                                    case NAME: {
                                        if (op2.getValue().startsWith("r") && ord2 != -1) {
                                            result = new byte[]{(byte) (ord2
                                                    | (type2 == OperandType8051.NAME ? 0xE8 : // a,  Rn
                                                    0xE6))};                                  // a, @Ri
                                            break;
                                        } if (ord2 == -1)
                                            break;
                                    }
                                    default:
                                        problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op2));
                                }
                                break;
                            } else if (val.startsWith("r")) {
                                switch (type2) {
                                    case CONSTANT:
                                    case ADDRESS: {
                                        int valCon = Integer.parseInt(op2.getValue());
                                        if (valCon > 0xff)
                                            problems.add(new TokenProblem("Value of " +
                                                    (type2 == OperandType8051.CONSTANT ? "constant" : "direct address") +
                                                    " too big!", Type.ERROR, file, op2));
                                        else if (ord1 != -1)
                                            result = new byte[]{(byte) (ord1
                                                    | (type2 == OperandType8051.CONSTANT ?
                                                       type1 == OperandType8051.NAME ?
                                                               0x78 :   //  Rn, #constant
                                                               0x76 :   // @Ri, #constant
                                                       type1 == OperandType8051.NAME ?
                                                               0xA8 :   //  Rn, direct
                                                               0xA6 )), // @Ri, direct
                                                    (byte) valCon};
                                        break;
                                    }
                                    case NAME: {
                                        if (op2.getValue().equals("a")) {
                                            if (ord1 != -1)
                                                result = new byte[] {(byte)(ord1
                                                | (type1 == OperandType8051.NAME ?
                                                           0xF8 :       //  Rn, a
                                                           0xF6))};     // @Ri, a
                                            break;
                                        }
                                    }
                                    default:
                                        problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op2));
                                }
                                break;
                            }
                        }
                        default:
                            problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op1));
                    }

                    handleUnnecessaryOperands(this.getName(), 2, operands, file, problems);

                    return result;
                }
            },

            new Mnemonic8051("movc", 2) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    byte[] result = new  byte[0];

                    if (!(operands[0].getOperandType() == OperandType8051.NAME && operands[0].getValue().equals("a")))
                        problems.add(new TokenProblem("Missing 'a' as first operand!", Type.ERROR, file, operands[0]));

                    if (operands.length > 1) {
                        OperandToken8051 op = operands[1];
                        if (op.getOperandType() == OperandType8051.INDIRECT &&
                                (op.getValue().equals("a+dptr") || op.getValue().equals("a+pc")))
                            if (op.getValue().equals("a+dptr"))
                                result = new byte[]{(byte)0x93};
                            else
                                result = new byte[]{(byte)0x83};
                        else
                            problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op));
                    } else
                        problems.add(new TokenProblem(this.getName().toUpperCase() + " must have 2 operands!",
                                Type.ERROR, file, name));

                    handleUnnecessaryOperands(this.getName(), 2, operands, file, problems);

                    return result;
                }
            },

            new Mnemonic8051("movx", 2) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    byte[] result = new byte[0];


                    OperandToken8051 op1 = operands[0], op2 = operands[1];
                    OperandType8051 type1 = op1.getOperandType();

                    switch (type1) {
                        case INDIRECT: {
                            if (op2.getOperandType() != OperandType8051.NAME || !op2.getValue().equals("a"))
                                problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op2));

                            if (op1.getValue().equals("dptr"))
                                result = new byte[]{(byte)0xF0};
                            else if (op1.getValue().startsWith("r")) {
                                int ordinal = Integer.parseInt(op1.getValue().substring(1));
                                if (ordinal > 1)
                                    problems.add(new TokenProblem("Register ordinal too high!", Type.ERROR, file, op1));
                                else result = new byte[]{(byte)(ordinal | 0xF2)};
                            } else
                                problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op1));
                            break;
                        }
                        case NAME: {
                            if (op2.getValue().equals("dptr"))
                                result = new byte[]{(byte)0xE0};
                            else if (op2.getValue().startsWith("r")) {
                                int ordinal = Integer.parseInt(op2.getValue().substring(1));
                                if (ordinal > 1)
                                    problems.add(new TokenProblem("Register ordinal too high!", Type.ERROR, file, op2));
                                else result = new byte[]{(byte)(ordinal | 0xE2)};
                            } else
                                problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op2));

                            if (op1.getValue().equals("a"))
                                break;
                        }
                        default:
                            problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op1));

                    }

                    handleUnnecessaryOperands(this.getName(), 2, operands, file, problems);
                    return result;
                }
            },

            new Mnemonic8051("mul", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return multiplicativeOperation((byte) 0xA4, this, name, operands, file, problems);
                }
            },

            new Mnemonic8051("nop", 0) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return noOperandOperation((byte)0x00, this, operands, file, problems);
                }
            },

            new Mnemonic8051("orl", 2) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return bitwiseLogicalOperation((byte)0x44, (byte)0x46, (byte)0x45, (byte)0x48,
                            (byte)0xA0, (byte)0x72, (byte)0x42, (byte)0x43, true, this, operands, file, problems);
                }
            },

            new Mnemonic8051("pop", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return addressOperation((byte)0xD0,this, operands, file, problems);
                }
            },

            new Mnemonic8051("push", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return addressOperation((byte)0xC0, this, operands, file, problems);
                }
            },

            new Mnemonic8051("ret", 0) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return noOperandOperation((byte)0x22, this, operands, file, problems);
                }
            },

            new Mnemonic8051("reti", 0) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return noOperandOperation((byte) 0x32, this, operands, file, problems);
                }
            },

            new Mnemonic8051("rl", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return accumulatorOperation((byte)0x23, this, operands, file, problems);
                }
            },

            new Mnemonic8051("rlc", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return accumulatorOperation((byte)0x33, this, operands, file, problems);
                }
            },

            new Mnemonic8051("rr", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return accumulatorOperation((byte)0x03, this, operands, file, problems);
                }
            },

            new Mnemonic8051("rrc", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return accumulatorOperation((byte)0x13, this, operands, file, problems);
                }
            },

            new Mnemonic8051("setb", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return bitOperation((byte)0x00, (byte)0xD3, (byte)0xD2, false, this, operands, file, problems);
                }
            },

            new Mnemonic8051("sjmp", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    Byte result = handleShortJump(codePoint, operands[0], 2, file, problems);
                    return (result == null ? new byte[0] : new byte[]{(byte)0x80, result});
                }
            },

            new Mnemonic8051("subb", 2) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return additiveOperation((byte)0x94, (byte)0x96, (byte)0x95, (byte)0x98, this, operands, file,
                            problems);
                }
            },

            new Mnemonic8051("swap", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return accumulatorOperation((byte)0xC4, this, operands, file, problems);
                }
            },

            new Mnemonic8051("xch", 2) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    byte[] result = new byte[0];

                    OperandToken8051 op = operands[0];

                    if (!(op.getOperandType() == OperandType8051.NAME && op.getValue().equals("a")))
                        problems.add(new TokenProblem("Expected 'a' as first operand!", Type.ERROR, file, op));
                        
                    op = operands[1];
                        
                    switch (op.getOperandType()) {
                        case ADDRESS: {
                            int val = Integer.parseInt(op.getValue());
                            if (val > 0xff)
                                problems.add(new TokenProblem("Value of direct address too big!", Type.ERROR, file, op));
                            else
                                result = new byte[]{(byte)0xC5, (byte) val};
                            break;
                        }
                        case INDIRECT:
                        case NAME: {
                            OperandType8051 type = op.getOperandType();
                            if (op.getValue().startsWith("r")) {
                                int ordinal = Integer.parseInt(op.getValue().substring(1));
                                if (ordinal > (type == OperandType8051.NAME? 7 : 1))
                                    problems.add(new TokenProblem("Register ordinal too high!", Type.ERROR, file, op));
                                else {
                                    result = new byte[]{(byte) (ordinal
                                            | (type == OperandType8051.NAME ? 0xC8 : 0xC6))}; // Set desired bits to ordinal
                                }
                                break;
                            }
                        }
                        default:
                            problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op));
                        }

                    handleUnnecessaryOperands(this.getName(), 2, operands, file, problems);
                    return result;
                }
            },

            new Mnemonic8051("xchd", 2) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    byte[] result = new byte[0];
                    boolean firstIgnored = false;
                    for (int i = 0; i<2; ++i) {
                        if (firstIgnored) break;
                        else if (i == 1 && operands.length < 2) {
                            problems.add(new TokenProblem("Expected 2 operands.",Type.ERROR, file, operands[0]));
                            break;
                        }
                        OperandToken8051 op = operands[i];
                        if (op.getOperandType() == OperandType8051.NAME && op.getValue().equals("a")) {
                          if (i == 0) continue;
                          else problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op));
                        } if (op.getOperandType() == OperandType8051.INDIRECT && op.getValue().startsWith("r")) {
                            if (i == 0) firstIgnored = true;
                                int ordinal = Integer.parseInt(op.getValue().substring(1));
                                if (ordinal > 1)
                                    problems.add(new TokenProblem("Register ordinal too big!", Type.ERROR, file, op));
                                else
                                    result = new byte[]{(byte) (ordinal | 0xD6)};
                        } else
                            problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op));
                    }
                    handleUnnecessaryOperands(this.getName(), 2, operands, file, problems);

                    return result;
                }
            },

            new Mnemonic8051("xrl", 2) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, Tokens.MnemonicNameToken name,
                                                         OperandToken8051[] operands, Path file, List<Problem<?>> problems) {
                    return bitwiseLogicalOperation((byte)0x64, (byte)0x66, (byte)0x65, (byte)0x68,
                            (byte)0x00, (byte)0x00, (byte)0x62, (byte)0x63, false, this, operands, file, problems);
                }
            }
    };

    /**
     * Generates instruction codes for all tier 1 arithmetic operations (+ -) because they are all generated the same
     * way with different opcodes.<br>
     * This function is used by the <code>ADD</code>, <code>ADDC</code> and <code>SUBB</code> mnemonics.
     *
     * @param opc1 the opcode for the <code>A, #constant</code> operand combination.
     * @param opc2 the opcode mask for the <code>A, @Ri</code> operand combination.
     * @param opc3 the opcode for the <code>A, direct address</code> operand combination.
     * @param opc4 the opcode mask for the <code>A, Rn</code> operand combination.
     *
     * @param mnemonic the mnemonic that uses this method.
     * @param operands the operands of the mnemonic.
     *
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     *
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] additiveOperation(final byte opc1, final byte opc2, final byte opc3, final byte opc4,
                                            Mnemonic8051 mnemonic, OperandToken8051[] operands,
                                            Path file, List<Problem<?>> problems) {
        byte[] result = new byte[0];

        if (!(operands[0].getOperandType() == OperandType8051.NAME &&
                operands[0].getValue().equals("a"))) {
                    problems.add(new TokenProblem("Missing 'a' as first operand!", Type.ERROR, file, operands[0]));
        }

        final OperandToken8051 op = operands[1];
        final OperandType8051 type = op.getOperandType();
        switch (op.getOperandType()) {
            case CONSTANT:
            case ADDRESS: {
                int value = Integer.parseInt(op.getValue());
                if (value > 0xFF)
                    problems.add(new TokenProblem("Value of " +
                            (type == OperandType8051.ADDRESS ? "direct address" : "constant") + " too big!",
                            Type.ERROR, file, op));
                else
                    result = new byte[]{(type == OperandType8051.ADDRESS ? opc3 : opc1), (byte) value};
                break;
            }
            case INDIRECT:
            case NAME:
                if (op.getValue().startsWith("r")) {
                    int ordinal = Integer.parseInt(op.getValue().substring(1));
                    if (ordinal > (type == OperandType8051.NAME? 7 : 1))
                        problems.add(new TokenProblem("Register ordinal too high!", Type.ERROR, file, op));
                    else {
                        result = new  byte[] {(byte)(ordinal
                                | (type==OperandType8051.NAME? opc4 : opc2))}; // Set desired bits to ordinal
                        break;
                    }
                }
            default:
                problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op));
        }

        handleUnnecessaryOperands(mnemonic.getName(), 2, operands, file, problems);

        return result;
    }

    /**
     * Generates instruction codes for all absolute code jumps. Absolute jumps change the low byte of the PC and 3 bytes
     * of the high byte, changing 11 bytes in total.<br>
     * This function is used by the <code>ACALL</code> and <code>AJUMP</code> mnemonics.
     *
     * @param opc1
     *      a bit mask that sets the bits 0-4 of PCH. The other bits are used to store bits 8 thou 10 of the jump
     *      address.<br>
     *          <code>
     *              PC(A<sub>10</sub>A<sub>9</sub>A<sub>8</sub><i>XXXXX</i>
     *              A<sub>7</sub>A<sub>6</sub>A<sub>5</sub>A<sub>4</sub>A<sub>3</sub>A<sub>2</sub>A<sub>1</sub>A<sub>0</sub>)
     *          </code>
     *
     * @param mnemonic the mnemonic that uses this method.
     * @param codePoint the position of the mnemonic in code memory.
     * @param operands the operands of the mnemonic.
     *
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] absoluteCodeJump(final int opc1,
                                           Mnemonic8051 mnemonic, long codePoint, OperandToken8051[] operands,
                                           Path file, List<Problem<?>> problems) {
        byte[] result = new byte[0];
        if (operands[0].getOperandType() == OperandType8051.ADDRESS ||
            operands[0].getOperandType() == OperandType8051.ADDRESS_OFFSET) {
            codePoint+=2;
            long jump = Long.parseLong(operands[0].getValue());

            if (operands[0].getOperandType() == OperandType8051.ADDRESS_OFFSET) {
                jump = getFromOffset(codePoint, jump, 0, operands[0], file, problems);
            }

            if ((jump >>> 11L // Shift 11 right to clear changing bits
                 & 0x1fL)     // Clear all bytes but the first five
                == (codePoint >>> 11L & 0x1fL)) // Compare: If equal an absolute jump is possible.
                result = new byte[]{((byte)(
                        jump >>> 3L   // Shift jump target 3 bits right to get bytes 3-10 (10 9 8 7 6 5 4 3)
                        & 0xE0L       // Clear first 5 bits (10 9 8 x x x x x)
                        | opc1)),     // Set cleared bits to bit mask (10 9 8 A A A A A)
                        (byte)(
                        jump & 0xffL) // Clear all bits but the low byte (x x x 7 6 5 4 3 2 1 0)
                };
            else
                problems.add(new TokenProblem("Call address too far absolute 11 bit addressing!",
                        Type.ERROR, file, operands[0]));
        } else
            problems.add(new TokenProblem("Operand needs to be an address!", Type.ERROR, file, operands[0]));

        handleUnnecessaryOperands(mnemonic.getName(), 1, operands, file, problems);
        return result;

    }
    /**
     * Generates instruction codes for all bitwise logical operations (& | ^) because they are all generated the same
     * way with different opcodes.<br>
     * This function is used by the <code>ANL</code>, <code>ORL</code> and <code>XRL</code> mnemonics.
     *
     * @param opc1 the opcode for the <code>A, #constant</code> operand combination.
     * @param opc2 the opcode mask for the <code>A, @Ri</code> operand combination.
     * @param opc3 the opcode for the <code>A, direct address</code> operand combination.
     * @param opc4 the opcode mask for the <code>A, Rn</code> operand combination.
     * @param opc5 the opcode for the <code>C, /bit address (negated)</code> operand combination.
     * @param opc6 the opcode for the <code>C, bit address</code> operand combination.
     * @param opc7 the opcode for the <code>direct address, A</code> operand combination.
     * @param opc8 the opcode for the <code>direct address, #constant</code> operand combination.
     *
     * @param cOp whether operations with the <code>C</code> operand are possible.
     * @param mnemonic the mnemonic that uses this method.
     * @param operands the operands of the mnemonic.
     *                 
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     * 
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] bitwiseLogicalOperation(final byte opc1, final byte opc2, final byte opc3, final byte opc4,
                                                  final byte opc5, final byte opc6, final byte opc7, final byte opc8,
                                                  boolean cOp, Mnemonic8051 mnemonic, OperandToken8051[] operands,
                                                  Path file, List<Problem<?>> problems)  {
        byte[] result = new  byte[0];
        OperandToken8051 op = operands[0];

        OperandToken8051 op1 = operands[0], op2 = operands[1];

        outer:
        switch (op1.getOperandType()) {
            case ADDRESS: {
                int value = Integer.parseInt(op1.getValue());
                if (value <= 0xff)
                    switch (op2.getOperandType()) {
                        case CONSTANT: {
                            int constVal = Integer.parseInt(op2.getValue());
                            if (constVal <= 0xff) {
                                result = new byte[]{opc8, (byte) value, (byte) constVal};
                                break;
                            } else
                                problems.add(new TokenProblem("Value of constant too big!", Type.ERROR, file, op2));
                            break;
                        }
                        case NAME:
                            if (op2.getValue().equals("a")) {
                                result = new byte[]{opc7, (byte) value};
                                break;
                            }
                        default:
                            problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op2));
                    }
                else
                    problems.add(new TokenProblem("Value of direct address too big!", Type.ERROR, file, op1));
                break;
            }
            case NAME:
                switch (op2.getOperandType()) {
                    case NEGATED_ADDRESS: {
                        if (!cOp)
                            break outer;
                        int val = Integer.parseInt(op2.getValue());
                        if (val > 0xff)
                            problems.add(new TokenProblem("Value of negated direct address too big!",
                                    Type.ERROR, file, op2));
                        else
                            result = new byte[]{opc5, (byte) val};
                        if (!(op1.getOperandType() == OperandType8051.NAME &&
                                op1.getValue().equals("c")))
                            problems.add(new TokenProblem("Missing 'c' as first operand!", Type.ERROR,
                                    file, op1));
                        break outer;
                    }
                    case INDIRECT:
                    case NAME: {
                        if (op2.getValue().startsWith("r")) {
                            OperandType8051 type = op2.getOperandType();
                            if (!(op1.getOperandType() == OperandType8051.NAME &&
                                    op1.getValue().equals("a")))
                                problems.add(new TokenProblem("Missing 'a' as first operand!", Type.ERROR, file,
                                        op1));

                            int ordinal = Integer.parseInt(op2.getValue().substring(1));
                            if (ordinal > (type == OperandType8051.NAME ? 7 : 1))
                                problems.add(new TokenProblem("Register ordinal too high!", Type.ERROR, file, op2));
                            else
                                result = new byte[]{(byte) (ordinal | (type == OperandType8051.NAME ? opc4 : opc2))};
                        } else
                            problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op2));
                        break outer;
                    }
                    case CONSTANT:
                    case ADDRESS:
                    {
                        int value = Integer.parseInt(op2.getValue());
                        OperandType8051 type = op2.getOperandType();
                        if (value > 0xff) {
                            problems.add(new TokenProblem("Value of " +
                                    (type == OperandType8051.CONSTANT ? "constant" : "direct address") +
                                    " too big!", Type.ERROR, file, op2));
                            break outer;
                        } else if (op1.getValue().equals("a")) {
                            if (type == OperandType8051.CONSTANT
                                    || type == OperandType8051.ADDRESS) {

                                result = new byte[]{type == OperandType8051.CONSTANT ? opc1 : opc3,
                                        (byte) value};
                            } else
                                problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op2));
                            break outer;
                        } else if (op1.getValue().equals("c") && cOp) {
                            if (type == OperandType8051.ADDRESS)
                                result = new byte[]{opc6, (byte) value};
                            else
                                problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op2));
                            break outer;
                        }
                    }

                }

            default:
                problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op1));
        }


        handleUnnecessaryOperands(mnemonic.getName(), 3, operands, file, problems);
        return result;

    }

    /**
     * Generates instruction codes for all bit operations (~ =0 =1) because they are all generated the same way with
     * different opcodes.<br>
     * This function is used by the <code>CLR</code>, <code>SETB</code> and <code>CPL</code> mnemonics.
     *
     * @param opc1 the opcode for <code>A</code> as operand.
     * @param opc2 the opcode for <code>C</code> as operand.
     * @param opc3 the opcode for <code>direct address</code> as operand.
     *
     * @param acc whether <code>A</code> can be used as an operand or not.
     * @param mnemonic the mnemonic that uses this method.
     * @param operands the operands of the mnemonic.
     *
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     *
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] bitOperation(final byte opc1, final byte opc2, final byte opc3,
                                       boolean acc, Mnemonic8051 mnemonic, OperandToken8051[] operands,
                                       Path file, List<Problem<?>> problems) {
        byte[] result = new byte[0];

        OperandToken8051 op = operands[0];
        OperandType8051 type = op.getOperandType();

        switch (type) {
            case ADDRESS:
                int value = Integer.parseInt(op.getValue());
                if (value > 0xff)
                    problems.add(new TokenProblem("Value of direct address too big!", Type.ERROR, file, op));
                else
                    return new byte[] {opc3, (byte) value};
                break;
            case NAME:
                if (op.getValue().equals("a") && acc)
                    return new byte[]{opc1};
                else if (op.getValue().equals("c"))
                    return new byte[]{opc2};
            default:
                problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op));
        }

        handleUnnecessaryOperands(mnemonic.getName(), 1, operands, file, problems);

        return result;
    }

    /**
     * Generates instruction codes for all operations that are influencing the accumulator because they are all
     * generated the same way with different opcodes.<br>
     * This function is used by the <code>DA</code>, <code>RR</code>, <code>RRC</code>, <code>RL</code>, <code>RLC</code>,
     * <code>RR</code> and <code>SWAP</code> mnemonics.
     *
     * @param opc1 the opcode for the instruction.
     *
     * @param mnemonic the mnemonic that uses this method.
     * @param operands the operands of the mnemonic.
     *
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     *
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] accumulatorOperation(final byte opc1,
                                               Mnemonic8051 mnemonic,
                                               OperandToken8051[] operands,
                                               Path file, List<Problem<?>> problems) {
        if (operands.length > 0 && !operands[0].getValue().equals("a")) {
            problems.add(new TokenProblem("Incompatible operand! (Expected \"a\")", Type.WARNING, file, operands[0]));
        }
        handleUnnecessaryOperands(mnemonic.getName(), 1, operands, file, problems);

        return new byte[]{opc1};
    }

    /**
     * Generates instruction codes for increment and decrement operations because they are all generated the same way
     * with different opcodes.<br>
     * This function is used by the <code>INC</code> and <code>DEC</code> mnemonics.
     *
     * @param opc1 the opcode for <code>A</code> as operand.
     * @param opc2 the opcode mask for <code>@Ri</code> as operand.
     * @param opc3 the opcode mask for <code>Rn</code> as operand.
     * @param opc4 the opcode for <code>direct address<code> as operand.
     *
     * @param mnemonic the mnemonic that uses this method.
     * @param operands the operands of the mnemonic.
     *
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     *
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] incDecOperation(final byte opc1, final byte opc2, final byte opc3, final byte opc4,
                                          Mnemonic8051 mnemonic, OperandToken8051[] operands,
                                          Path file, List<Problem<?>> problems) {
        byte[] result = new byte[0];

        OperandToken8051 op = operands[0];
        OperandType8051 type = op.getOperandType();

        switch (type) {
            case ADDRESS: {
                int value = Integer.parseInt(op.getValue());

                if (value > 0xff)
                    problems.add(new TokenProblem("Value of direct address too big!", Type.ERROR, file, op));
                else
                    return new byte[] {opc4, (byte) value};
                break;
            }
            case INDIRECT:
            case NAME:
                if (op.getValue().equals("a")) {
                    result = new byte[]{opc1};
                    break;
                }
                else if (op.getValue().startsWith("r")) {
                    int ordinal = Integer.parseInt(op.getValue().substring(1));
                    if (ordinal > (type == OperandType8051.NAME? 7 : 1))
                        problems.add(new TokenProblem("Register ordinal too high!", Type.ERROR, file, op));
                    else {
                        result = new  byte[] {(byte)(ordinal
                                | (type==OperandType8051.NAME? opc3 : opc2))}; // Set desired bits to ordinal
                    }
                    break;
                }
            default:
                problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op));
        }

        handleUnnecessaryOperands(mnemonic.getName(), 1, operands, file, problems);

        return result;
    }
    /**
     * Generates instruction codes for all tier 2 arithmetic operations (* /) because they are all generated the same
     * way with different opcodes.<br>
     * This function is used by the <code>MUL</code> and <code>DIV</code> mnemonics.
     *
     * @param opc1 the opcode for the instruction.
     *
     * @param mnemonic the mnemonic that uses this method.
     * @param name the name token of the mnemonic to use it as cause in potential errors.
     * @param operands the operands of the mnemonic.
     *
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     *
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] multiplicativeOperation(final byte opc1,
                                                  Mnemonic8051 mnemonic, Tokens.MnemonicNameToken name,
                                                  OperandToken8051[] operands,
                                                  Path file, List<Problem<?>> problems) {
        if (operands.length >= 1) {
            if (!(operands[0].getOperandType() == OperandType8051.NAME && operands[0].getValue().equals("ab")))
                problems.add(new TokenProblem("Incompatible operand! (Expected \"ab\")", Type.WARNING, file, operands[0]));
        }

        handleUnnecessaryOperands(mnemonic.getName(), 1, operands, file, problems);

        return new byte[]{opc1};
    }

    /**
     * Generates instruction codes for all bit relevant jump operations because they are all generated the same way
     * with different opcodes.<br>
     * This function is used by the <code>JB</code>, <code>JNB</code> and <code>JBC</code> mnemonics.
     *
     * @param opc1 the opcode of the instruction.
     *
     * @param mnemonic the mnemonic that uses this method.
     * @param codePoint the position of the instruction in the code memory.
     * @param operands the operands of the mnemonic.
     *
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     *
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] jumpBitRelevant(final byte opc1,
                                          Mnemonic8051 mnemonic, long codePoint, OperandToken8051[] operands,
                                          Path file, List<Problem<?>> problems) {
        byte[] result = new byte[0];

        OperandToken8051 op1 = operands[0], op2 = operands[1];
        Byte offset = handleShortJump(codePoint, op2, 3, file, problems);
        if (offset == null)
            return result;

        if (op1.getOperandType() == OperandType8051.ADDRESS) {
            int value = Integer.parseInt(op1.getValue());
            if (value > 0xff)
                problems.add(new TokenProblem("Value of direct address too big!", Type.ERROR, file, op1));
            else
                result = new byte[]{opc1, (byte) value, offset};
        } else
            problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op1));

        handleUnnecessaryOperands(mnemonic.getName(), 2, operands, file, problems);
        return result;
    }

    /**
     * Generates instruction codes for all name relevant jump operations because they are all generated the same way
     * with different opcodes.<br>
     * This function is used by the <code>JC</code>, <code>JNC</code>, <code>JZ</code> and <code>JNZ</code> mnemonics.
     *
     * @param opc1 the opcode of the instruction.
     *
     * @param mnemonic the mnemonic that uses this method.
     * @param codePoint the position of the instruction in the code memory.
     * @param operands the operands of the mnemonic.
     *
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     *
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] jumpNameRelevant(final byte opc1,
                                           Mnemonic8051 mnemonic, long codePoint, OperandToken8051[] operands,
                                           Path file, List<Problem<?>> problems) {
        Byte offset = handleShortJump(codePoint, operands[0], 2, file, problems);
        if (offset == null)
            return new byte[0];
        handleUnnecessaryOperands(mnemonic.getName(), 1, operands, file, problems);

        return new byte[]{opc1, offset};
    }

    /**
     * Generates instruction codes for all long jump operations because they are all generated the same way
     * with different opcodes.<br>
     * This function is used by the <code>JC</code>, <code>JNC</code>, <code>JZ</code> and <code>JNZ</code> mnemonics.
     *
     * @param opc1 the opcode of the instruction.
     *
     * @param mnemonic the mnemonic that uses this method.
     * @param codePoint the position of the instruction in the code memory.
     * @param operands the operands of the mnemonic.
     *
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     *
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] longJump(final byte opc1,
                                   Mnemonic8051 mnemonic, long codePoint, OperandToken8051[] operands,
                                   Path file, List<Problem<?>> problems) {
        byte[] result = new byte[0];
        OperandType8051 type = operands[0].getOperandType();

        if (type == OperandType8051.ADDRESS ||
            type == OperandType8051.ADDRESS_OFFSET) {
            long jump = Integer.parseInt(operands[0].getValue());

            if (type == OperandType8051.ADDRESS_OFFSET) {
                jump = getFromOffset(codePoint, jump, 3, operands[0], file, problems);
                getErrorSetting(operands[0], AssemblerSettings.ADDRESS_OFFSET, "Using of address offsets" +
                        "is deactivated!", "Address offset has been used! This can result in non-opcode jump targets.",
                        "Address offset used.", file, problems);
            }

            result = new byte[] { opc1,
                                 (byte)(jump >>> 8 & 0xffL),// Shift 8 to the right get the high byte and clear the rest
                                                            // ( ... x x 15 14 13 12 11 10 9 8)
                                 (byte)(jump & 0xffL)};     // Extract low byte ( ... x x 7 6 5 4 3 2 1 0)
        } else
            problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, operands[0]));

        handleUnnecessaryOperands(mnemonic.getName(), 1, operands, file, problems);

        return result;
    }


    /**
     * Generates instruction codes for all operations that have no operands. It basically just handles unnecessary
     * operands and returns the opcode.<br>
     * This function is used by the <code>NOP</code>, <code>RET</code> and <code>RETI</code> mnemonics.
     *
     * @param opc1 the opcode of the instruction.
     *
     * @param mnemonic the mnemonic that uses this method.
     * @param operands the operands of the mnemonic.
     *
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     *
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] noOperandOperation(final byte opc1,
                                             Mnemonic8051 mnemonic, OperandToken8051[] operands,
                                             Path file, List<Problem<?>> problems) {
        handleUnnecessaryOperands(mnemonic.getName(), 0, operands, file, problems);
        return new byte[] {opc1};
    }

    /**
     * Generates instruction codes for all operations that work with raw byte addresses.<br>
     * This function is used by the <code>POP</code> and <code>PUSH</code> mnemonics.
     *
     * @param opc1 the opcode of the instruction.
     *
     * @param mnemonic the mnemonic that uses this method.
     * @param operands the operands of the mnemonic.
     *
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     *
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] addressOperation(final byte opc1,
                                           Mnemonic8051 mnemonic, OperandToken8051[] operands,
                                           Path file, List<Problem<?>> problems) {
        Byte result = handleAddress(operands[0], file, problems);
        handleUnnecessaryOperands(mnemonic.getName(), 1, operands, file, problems);
        return (result == null ? new byte[0] : new byte[]{opc1, result});

    }

    /**
     * Handles a short jump by calculating the needed offset.<br>
     *
     * @param codePoint the position of the target instruction in the code memory.
     * @param op
     *      the operand that should be used to calculate the offset.<br>
     *      It has to be an <code>ADDRESS</code> or this method will return an error.
     * @param offset
     *      the offset from the actual code point. Normally refers to the code point
     *      after the last operand of the instruction.
     *
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     *
     * @return
     *      the needed offset.<br>
     *      <code>null</code> if an error occurred. The error will reported with the
     *      operand as cause. An error occurs if either the operand has an illegal type
     *      (not <code>ADDRESS</code>) or the resulting jump is too far for a short
     *      jump (out of the <code>-128</code> - <code>+128</code> range).
     */
    private static Byte handleShortJump(long codePoint, OperandToken8051 op, int offset,
                                        Path file, List<Problem<?>> problems) {
        final OperandType8051 type = op.getOperandType();
        if (type == OperandType8051.ADDRESS || type == OperandType8051.ADDRESS_OFFSET) {
            long i;
            if (type == OperandType8051.ADDRESS)
                i = getOffset(codePoint, Integer.parseInt(op.getValue()), offset);
            else {
                i = Long.parseLong(op.getValue());
                getErrorSetting(op, AssemblerSettings.ADDRESS_OFFSET, "Using of address offsets is " +
                        "deactivated!", "Address offset has been used! This can result in non-opcode jump targets.",
                        "Address offset used.", file, problems);
            }

            if (i >= -128 && i <= 127)
                return (byte) i;
            else {
                problems.add(new TokenProblem("Jump is too far for a short jump!", Type.ERROR, file, op));
            }
        } else
            problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op));

        return null;

    }

    /**
     * @param codePoint the position in code memory
     * @param targetCodePoint the code memory point if the jump target
     * @param offset
     *      the offset from the <code>codePoint</code> to the <code>targetCodePoint</code>.
     *      Can only be positive.
     * @return
     *      the calculated offset from the <code>codePoint</code> the <code>targetCodePoint</code>.
     */
    private static long getOffset(long codePoint, long targetCodePoint, int offset) {
        if (offset < 0)
            throw new IllegalArgumentException("Offset 'offset' cannot be negative!");
        return targetCodePoint - (codePoint + offset);

    }

    /**
     * @param codePoint the position in code memory
     * @param codeOffset the offset that will be used to generate the target code point
     * @param offset
     *      the offset from the <code>codePoint</code> to the code offset source.
     *      Can only be positive.
     * @param op the involved operand.
     *
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     *
     * @return
     *      the calculated target code point from the <code>codePoint</code> and the <code>offset</code>.
     */
    private static long getFromOffset(long codePoint, long codeOffset, int offset, OperandToken8051 op,
                                      Path file, List<Problem<?>> problems) {
        if (offset < 0)
            throw new IllegalArgumentException("Offset 'offset' cannot be negative!");
        long result =  codePoint+offset + codeOffset;

        if (result >= 0 && result <= 0xffffL)
            return result;
        else {
            problems.add(new TokenProblem("Resulting address out of bounds. (" + result + ")", Type.ERROR, file, op));
            return offset;
        }
    }

    /**
     * Adds an ERROR or WARNING to every unnecessary operand of a mnemonic.
     *  @param mnemonicName the name of the mnemonic.
     * @param firstOperand
     *      the first operand that should be handled. If some operands are ignored their value will be subtracted from
     *      this value.
     * @param operands the operands that will be handled.
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     */
    private static void handleUnnecessaryOperands(String mnemonicName,
                                                  int firstOperand, OperandToken8051[] operands,
                                                  Path file, List<Problem<?>> problems) {
        final String err = "Too many operands! " + mnemonicName.toUpperCase() + " must have exactly " + firstOperand +
                " operand" + (firstOperand == 1 ? "" : "s") + ".";
        for (int i = firstOperand; i < operands.length; ++i)
            getErrorSetting(operands[i], AssemblerSettings.UNNECESSARY_OPERANDS,
                    err, "Unnecessary operand should not be written.", "Unnecessary operand.", file, problems);

    }

    /**
     * Processes a potential address.<br>
     * This method can convert the SFR names for <code>A</code> and <code>C</code> and the
     * RN registers.<br>
     * If the address is invalid a new error with the operand at its cause will be created.
     * Also a new warning will be created if the resulting address is in the SFR-area of the
     * internal RAM but no valid SFR.
     *
     * @param op the address to process.
     *
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     *
     * @return
     *      the value of the address as a byte.<br>
     *      <code>null</code> if the value is out
     *      of bounds or no address.
     */
    private static Byte handleAddress(OperandToken8051 op,
                                      Path file, List<Problem<?>> problems) {
        Byte result = null;
        if (op.getOperandType() == OperandType8051.ADDRESS) {
            int val = Integer.parseInt(op.getValue());
            if (val > 0xff)
                problems.add(new TokenProblem("Value of address too big!", Type.ERROR, file, op));
            else
                result = (byte) val;
        } else if (op.getOperandType() == OperandType8051.NAME &&
                op.getValue().equals("a"))
            result = A;
        else if (op.getOperandType() == OperandType8051.NAME &&
                op.getValue().startsWith("r")) {
            problems.add(new TokenProblem("Illegal operand!", Type.ERROR, file, op));
        }
        else
            problems.add(new TokenProblem("Incompatible operand!", Type.ERROR, file, op));

        if (result == null)
            return null;
        else if (!isValidByte(result))
            problems.add(new TokenProblem("Address in invalid SFR memory area.", Type.WARNING, file, op));

        return result;
    }

    /**
     * Check whether a byte address is valid.<br>
     * A byte address is valid if it is in the lower part of the internal RAM or a SFR.
     *
     * @param address the byte address to check.
     */
    public static boolean isValidByte(byte address) {
        return (address >= 0 && address <= (byte) 0x7F) ||
                address == (byte) 0xF0 || address == (byte) 0xE0 || address == (byte) 0xD0 || address == (byte) 0xB8 ||
                address == (byte) 0xB0 || address == (byte) 0xA8 || address == (byte) 0xA0 || address == (byte) 0x98 ||
                address == (byte) 0x99 || address == (byte) 0x90 || address == (byte) 0x88 || address == (byte) 0x89 ||
                address == (byte) 0x8A || address == (byte) 0x8B || address == (byte) 0x8C || address == (byte) 0x8D ||
                address == (byte) 0x80 || address == (byte) 0x81 || address == (byte) 0x82 || address == (byte) 0x83 ||
                                          address == (byte) 0x84 || address == (byte) 0x87;
    }

    /**
     * Check whether a bit address is valid.<br>
     * A bit address is valid if it is in the bit addressable part of the internal RAM or in a SFR.
     *
     * @param bitAddress the byte address to check.
     */
    public static boolean isValidBit(byte bitAddress) {
        return (bitAddress >= 0 && bitAddress <= (byte) 0x7F) || isValidByte((byte)(bitAddress - bitAddress % 8));
    }


    /**
     * Generates a Problem for a given error Setting. (The Setting must be verifiable with
     * {@link AssemblerSettings#VALID_ERROR})<br>
     * The messages will be taken from 2 given Strings and the
     * generated problem added to the Problems List.
     *  @param cause the cause of the Problem.
     * @param settingName the setting (by name) that should be used for generating the Problem.
     * @param errorMessage the message String for the case of a generated error
     * @param warningMessage the message String for the case of an generated warning
     * @param infoMessage the message String for the case of an generated information
     *
     * @param file the file of the mnemonic.
     * @param problems a List to that occurring Problems can be added to.
     */
    public static void getErrorSetting(Token cause, String settingName,
                                       String errorMessage, String warningMessage, String infoMessage,
                                       Path file, List<Problem<?>> problems) {
        String setting = Settings.INSTANCE.getProperty(settingName, AssemblerSettings.VALID_ERROR);
        switch (setting) {
            case "error":
                problems.add(new TokenProblem(errorMessage, Type.ERROR, file, cause));
                break;
            case "warn":
                problems.add(new TokenProblem(warningMessage, Type.WARNING, file, cause));
                break;
            case "info":
                problems.add(new TokenProblem(warningMessage, Type.INFORMATION, file, cause));
                break;
            case "ignore":
                break;
            default:
                throw new IllegalArgumentException("Illegal value for error setting! (\""+setting+"\")");
        }
    }
    /**
     * Modifies a given Problem for a given error Setting. (The Setting must be verifiable
     * with {@link AssemblerSettings#VALID_ERROR}.)<br>
     * The messages will be taken from 2 given Strings and
     * added to a specified List.
     *
     * @param p the Problem to be modified and added
     * @param settingName the setting (by name) that should be used for generating the Problem.
     * @param errorMessage the message String for the case of a generated Problem
     * @param warningMessage the message String for the case of an generated warning
     * @param infoMessage the message String for the case of an generated information
     * @param problems the List the newly modified Problem will be added to
     */
    public static void getGeneralErrorSetting(Problem<?> p, String settingName,
                                              String errorMessage, String warningMessage, String infoMessage,
                                              List<Problem<?>> problems) {
        String setting = Settings.INSTANCE.getProperty(settingName, AssemblerSettings.VALID_ERROR);
        switch (setting) {
            case "error":
                p.setType(Type.ERROR);
                p.setMessage(errorMessage);
                break;
            case "warn":
                p.setType(Type.WARNING);
                p.setMessage(warningMessage);
                break;
            case "info":
                p.setType(Type.INFORMATION);
                p.setMessage(infoMessage);
            case "ignore":
                break;
            default:
                throw new IllegalArgumentException("Illegal value for error setting! (\""+setting+"\")");
        }

        problems.add(p);
    }

}
