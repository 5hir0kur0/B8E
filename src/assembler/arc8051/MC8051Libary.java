package assembler.arc8051;

import assembler.util.SimpleAssemblyError;
import assembler.util.Settings;
import assembler.util.Settings.Errors.ErrorHandling;
import assembler.util.SimpleAssemblyError.Type;



/**
 * @author Noxgrim
 */
public class MC8051Libary {

    /**
     * Contains all the possible types of a 8051 operand
     * as an enum.
     */
    public enum OperandType8051 {
        /**
         * A constant value.<br> In 8051 assembly
         * language all constant values have
         * a number sign '#' as prefix.<br>
         * All numbers must be present in decimal
         * system (10) form and have to be <code>8</code>
         * or <code>16</code> bit long.
         * The value has to be the raw value
         * without the '#'.
         */
        CONSTANT,
        /**
         * An address.<br> In 8051 assembly
         * language addresses have no prefix.<br>
         * All addresses must be present in decimal
         * system (10) form and have to be <code>8</code>
         * or <code>16</code> bit long.
         */
        ADDRESS,
        /**
         * A 'negated' address.<br> In 8051 assembly
         * language all negated addresses have a
         * solidus '/' as prefix.<br>
         * All addresses must be present in decimal
         * system (10) form and have to be <code>8</code>
         * bit long.<br>
         * Negated addresses are used in bit operations
         * to indicate that the  microcomputer first
         * has to negate the value at the value at the
         * address before perform the operation itself.
         *
         */
        NEGATED_ADDRESS,

        /**
         * An offset for an address.<br>
         * All address offsets are labeled by either a '+' or
         * a '-' prefix.<br>
         * An address offset is used for relative addressing.
         * Instead of the absolute address a address generated
         * form the code point and the address offset is used as
         * the jump target.
         */
        ADDRESS_OFFSET,
        /**
         * A name that the 8051 mnemonic knows.<br>
         * Named addresses like "P0.7" don't count because
         * these names don't have specific mnemonics attached
         * to them.<br>
         * E.g.:
         * <pre>
         *     MOV A, B
         *     ; is in reality
         *     MOV A, F0h
         * </pre>
         */
        NAME,
        /**
         * A name, that suggests an indirect addressing process
         * with the name.<br>
         * An indirect addressed name has '@' prefix.<br>
         * Only R0 and R1 can be used to address indirect.
         */
        INDIRECT_NAME;
    }

    public static final Mnemonic8051[] mnemonics = {

            new Mnemonic8051("acall", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    return absoluteCodeJump(0x11, this, codePoint, operands);
                }
            },

            new Mnemonic8051("add", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051 ... operands) {
                    return tier1ArithmeticOperation((byte) 0x24, (byte) 0x26, (byte) 0x25, (byte) 0x28, this, operands);
                }
            },

            new Mnemonic8051("addc", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    return tier1ArithmeticOperation((byte) 0x34, (byte) 0x36, (byte) 0x35, (byte) 0x38, this, operands);
                }
            },

            new Mnemonic8051("ajmp", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    return absoluteCodeJump(0x01, this, codePoint, operands);
                }
            },

            new Mnemonic8051("anl", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    return bitwiseLogicalOperation((byte) 0x54, (byte) 0x56, (byte) 0x55, (byte) 0x58,
                            (byte) 0xb0, (byte) 0x82, (byte) 0x52, (byte) 0x53, this, operands);
                }
            },

            new Mnemonic8051("cjne", 3) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    byte[] result = new byte[0];

                    OperandToken8051 op1 = operands[0], op2 = operands[1], op3 = operands[2];
                    OperandType8051 type1 = op1.getOperandType(), type2 = op2.getOperandType(),
                                    type3 = op3.getOperandType();

                    byte offset;
                    if (type3 == OperandType8051.ADDRESS) {
                        long i = getOffset(codePoint, Integer.parseInt(op3.getValue()), 4);
                        if (i >= -127 && i <= 128)
                            offset = (byte) i;
                        else {
                            op3.setError(new SimpleAssemblyError(Type.ERROR, "Jump is too far for a short jump!"));
                            return result;
                        }
                    } else {
                        op3.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                        return result;
                    }

                    int  value;

                    if (type2 == OperandType8051.ADDRESS || type2 == OperandType8051.CONSTANT) {
                        value = Integer.parseInt(op2.getValue());
                        if (value <= 0xff) {
                            if (type1 == OperandType8051.NAME || type1 == OperandType8051.INDIRECT_NAME) {
                                if (op1.getValue().equals("a"))
                                    result = new byte[]{(byte)(type2 == OperandType8051.CONSTANT ? 0xB4 : 0xB5),
                                            (byte) value, offset};
                                else if (op1.getValue().startsWith("r")) {
                                    int ordinal = Integer.parseInt(op1.getValue().substring(1));
                                    if (ordinal > (type1 == OperandType8051.NAME? 7 : 1))
                                        op1.setError(new SimpleAssemblyError(Type.ERROR, "Register ordinal too high!"));
                                    else {
                                        result = new  byte[] {(byte)(ordinal
                                                | (type1==OperandType8051.NAME? 0xB8 : 0xB6)), // Set desired bits to ordinal
                                                (byte) value, offset};
                                    }
                                }
                            } else
                                op1.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                        } else
                            op2.setError(new SimpleAssemblyError(Type.ERROR, "Value of " +
                                    (type2 == OperandType8051.ADDRESS ? "direct address" : "constant") + " too big!"));
                    } else
                        op2.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));

                    handleUnnecessaryOperands(this.getName(), false, 0, 3, operands);

                    return result;
                }
            }
    };

    /**
     * Generates instruction codes for all tier 1 arithmetic operations (+ -) because they are all generated the same way with
     * different opcodes.<br>
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
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] tier1ArithmeticOperation(final byte opc1, final byte opc2, final byte opc3, final byte opc4,
            Mnemonic8051 mnemonic, OperandToken8051 ... operands) {
        byte[] result = new byte[0];

        boolean firstIsA = true;

        if (!(operands[0].getOperandType() == OperandType8051.NAME &&
                operands[0].getValue().equals("a"))) {
            firstIsA = false;
            operands[0].setError(getErrorFromErrorHandlingSetting(Settings.Errors.IGNORE_OBVIOUS_OPERANDS,
                    "Missing 'a' as first operand!", "Operand 'a' should be written as first operand."));
        } else if (operands.length < 2) {
            operands[0].setError(new SimpleAssemblyError(Type.ERROR, "Expected 2 operands!"));
            return new byte[0];
        }

        final OperandToken8051 op = operands[firstIsA?1:0];
        final OperandType8051 type = op.getOperandType();
        switch (op.getOperandType()) {
            case CONSTANT:
            case ADDRESS: {
                int value = Integer.parseInt(op.getValue());
                if (value > 0xFF)
                    op.setError(new SimpleAssemblyError(Type.ERROR, "Value of " +
                            (type == OperandType8051.ADDRESS ? "direct address" : "constant" ) + " too big!"));
                else
                    result = new byte[]{(type == OperandType8051.ADDRESS ? opc3 : opc1), (byte) value};
                break;
            }
            case INDIRECT_NAME:
            case NAME:
                if (op.getValue().startsWith("r")) {
                    int ordinal = Integer.parseInt(op.getValue().substring(1));
                    if (ordinal > (type == OperandType8051.NAME? 7 : 1))
                        op.setError(new SimpleAssemblyError(Type.ERROR, "Register ordinal too high!"));
                    else {
                        result = new  byte[] {(byte)(ordinal
                                | (type==OperandType8051.NAME? opc4 : opc2))}; // Set desired bits to ordinalgg
                        break;
                    }
                }
            default:
                op.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
        } //TODO Multiple error handling.

        handleUnnecessaryOperands(mnemonic.getName(), true, firstIsA ? 0 : 1, 2, operands);

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
                                           Mnemonic8051 mnemonic, long codePoint, OperandToken8051 ... operands) {
        byte[] result = new byte[0];
        if (operands[0].getOperandType() == OperandType8051.ADDRESS ||
            operands[0].getOperandType() == OperandType8051.ADDRESS_OFFSET) {
            codePoint+=2;
            long jump = Long.parseLong(operands[0].getValue());

            if (operands[0].getOperandType() == OperandType8051.ADDRESS_OFFSET) {
                jump = getFromOffset(codePoint, jump, 0);
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
                operands[0].setError(new SimpleAssemblyError(Type.ERROR,
                        "Call address too far absolute 11 bit addressing!"));
        } else
            operands[0].setError(new SimpleAssemblyError(Type.ERROR,
                    "Operand needs to be an address!"));

        handleUnnecessaryOperands(mnemonic.getName(), false, 0, 1, operands);
        return result;

    }
    /**
     * Generates instruction codes for all bitwise logical operations (& | ^) because they are all generated the same way with
     * different opcodes.<br>
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
     * @param mnemonic the mnemonic that uses this method.
     * @param operands the operands of the mnemonic.
     *
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] bitwiseLogicalOperation(final byte opc1, final byte opc2, final byte opc3, final byte opc4,
                                                  final byte opc5, final byte opc6, final byte opc7, final byte opc8,
            Mnemonic8051 mnemonic, OperandToken8051 ... operands)  {
        byte[] result = new  byte[0];
        OperandToken8051 op1 = operands[0];
        boolean firstIgnored = false;

        switch (op1.getOperandType()) {
            case NEGATED_ADDRESS: {
                firstIgnored = true;
                op1.setError(getErrorFromErrorHandlingSetting(Settings.Errors.IGNORE_OBVIOUS_OPERANDS,
                        "Missing 'c' as first operand!", "Operand 'c' should be written as first operand."));
                result = new byte[]{opc5};
            }
            case INDIRECT_NAME:
            case NAME:
                if (op1.getValue().startsWith("r")) {
                    OperandType8051 type = op1.getOperandType();
                    firstIgnored = true;
                    op1.setError(getErrorFromErrorHandlingSetting(Settings.Errors.IGNORE_OBVIOUS_OPERANDS,
                            "Missing 'a' as first operand!", "Operand 'a' should be written as first operand."));

                    int ordinal = Integer.parseInt(op1.getValue().substring(1));
                    if (ordinal > (type == OperandType8051.NAME? 7 : 1))
                        op1.setError(new SimpleAssemblyError(Type.ERROR, "Register ordinal too high!"));
                    else {
                        result = new  byte[] {(byte)(ordinal|(type==OperandType8051.NAME? opc4: opc2))};
                        break;
                    }
                }
        }

        if (!firstIgnored) {
            if (operands.length < 2) {
                op1.setError(new SimpleAssemblyError(Type.ERROR, "Expected 2 operands!"));
                return new byte[0];
            }
            OperandToken8051 op2 = operands[1];

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
                                    op2.setError(new SimpleAssemblyError(Type.ERROR,
                                            "Value of constant too big!"));
                                break;
                            }
                            case NAME:
                                if (op2.getValue().equals("a")) {
                                    result = new byte[]{opc7, (byte) value};
                                    break;
                                }
                            default:
                                op2.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                        }
                    else
                        op1.setError(new SimpleAssemblyError(Type.ERROR, "Value of direct address too big!"));
                    break;
                }
                case NAME:
                    int value = Integer.parseInt(op2.getValue());
                    OperandType8051 type = op2.getOperandType();
                    if (value > 0xff)
                        op2.setError(new SimpleAssemblyError(Type.ERROR, "Value of " +
                                (type == OperandType8051.CONSTANT ? "constant" : "direct address") +
                                " too big!"));

                    else if (op1.getValue().equals("a")) {
                        if (op2.getOperandType() == OperandType8051.CONSTANT
                                || type == OperandType8051.ADDRESS) {

                            result = new byte[]{type == OperandType8051.CONSTANT ? opc1 : opc3,
                                    (byte) value};
                        } else
                            op2.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                        break;
                    } else if (op1.getValue().equals("c")) {
                        if (type == OperandType8051.ADDRESS)
                            return new byte[]{opc6};
                        else
                            op2.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                        break;
                    }
                default:
                    op1.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));

            }
        }


        handleUnnecessaryOperands(mnemonic.getName(), true, firstIgnored ? 1 : 0, 3, operands);
        return result;

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
     * @return
     *      the calculated target code point from the <code>codePoint</code> and the <code>offset</code>.
     */
    private static long getFromOffset(long codePoint, long codeOffset, int offset) {
        if (offset < 0)
            throw new IllegalArgumentException("Offset 'offset' cannot be negative!");
        return codePoint+offset + codeOffset;
    }

    /**
     * Adds an ERROR or WARNING to every unnecessary operand of a mnemonic.
     *
     * @param mnemonicName the name of the mnemonic.
     * @param hasObviousOperands whether the mnemonic has unnecessary operands.
     * @param ignored
     *      the number of operands that were ignored. If <code>hasObviousOperands</code> is <code>true</code>
     *      0 is assumed.
     * @param firstOperand
     *      the first operand that should be handled. If some operands are ignored their value will be subtracted from
     *      this value.
     * @param operands the operands that will be handled.
     */
    private static void handleUnnecessaryOperands(String mnemonicName, boolean hasObviousOperands, int ignored,
                                           int firstOperand, OperandToken8051 ... operands) {
        if (!hasObviousOperands)
            ignored = 0;
        final String err = Settings.Errors.IGNORE_OBVIOUS_OPERANDS != ErrorHandling.ERROR && hasObviousOperands?
                "Too many operands! " + mnemonicName.toUpperCase() + " must have "+(firstOperand-1)+" or "+firstOperand+
                        " operands." :
                "Too many operands! " + mnemonicName.toUpperCase() + " must have exactly "+firstOperand+" operand"+
                        (firstOperand == 0 ? "":"s")+".";
        for (int i = firstOperand - ignored; i < operands.length; ++i)
            operands[i].setError(getErrorFromErrorHandlingSetting(Settings.Errors.ADDITIONAL_OPERANDS,
                    err, "Unnecessary operand."));

    }

    private static SimpleAssemblyError getErrorFromErrorHandlingSetting(ErrorHandling setting, String errorMessage,
                                                                        String warningMessage) {
        switch (setting) {
            case ERROR:
                return new SimpleAssemblyError(Type.ERROR, errorMessage);
            case WARN:
                return new SimpleAssemblyError(Type.WARNING, warningMessage);
            default:
                return null;
        }
    }
}
