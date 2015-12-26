package assembler.arc8051;

import assembler.util.Settings;
import assembler.util.Settings.Errors.ErrorHandling;
import assembler.util.SimpleAssemblyError;
import assembler.util.SimpleAssemblyError.Type;



/**
 * @author Jannik
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
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051 ... operands) {
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
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051 ... operands) {
                    return tier1ArithmeticOperation((byte) 0x34, (byte) 0x36, (byte) 0x35, (byte) 0x38, this, operands);
                }
            },

            new Mnemonic8051("ajmp", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051 ... operands) {
                    return absoluteCodeJump(0x01, this, codePoint, operands);
                }
            },

            new Mnemonic8051("anl", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051 ... operands) {
                    return bitwiseLogicalOperation((byte) 0x54, (byte) 0x56, (byte) 0x55, (byte) 0x58,
                            (byte) 0xb0, (byte) 0x82, (byte) 0x52, (byte) 0x53, this, operands);
                }
            },

            new Mnemonic8051("cjne", 3, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051 ... operands) {
                    byte[] result = new byte[0];

                    OperandToken8051 op1 = operands[0], op2 = operands[1], op3 = operands[2];
                    OperandType8051 type1 = op1.getOperandType(), type2 = op2.getOperandType();

                    Byte offset = handleShortJump(codePoint, op3, 3);

                    int value;

                    if (type2 == OperandType8051.ADDRESS || type2 == OperandType8051.CONSTANT) {
                        value = Integer.parseInt(op2.getValue());
                        if (value <= 0xff) {
                            if (type1 == OperandType8051.NAME || type1 == OperandType8051.INDIRECT_NAME) {
                                if (op1.getValue().equals("a"))
                                    result = new byte[]{(byte) (type2 == OperandType8051.CONSTANT ? 0xB4 : 0xB5),
                                            (byte) value, offset == null ? -3 : offset};
                                else if (op1.getValue().startsWith("r")) {
                                    int ordinal = Integer.parseInt(op1.getValue().substring(1));
                                    if (ordinal > (type1 == OperandType8051.NAME ? 7 : 1))
                                        op1.setError(new SimpleAssemblyError(Type.ERROR, "Register ordinal too high!"));
                                    else {
                                        result = new byte[]{(byte) (ordinal
                                                | (type1 == OperandType8051.NAME ? 0xB8 : 0xB6)), // Set desired bits to ordinal
                                                (byte) value, offset == null ? -3 : offset};
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
            },

            new Mnemonic8051("clr", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051 ... operands) {
                    return bitOperation((byte) 0xE4, (byte) 0xC3, (byte) 0xC2, this, operands);
                }
            },

            new Mnemonic8051("cpl", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051 ... operands) {
                    return bitOperation((byte) 0xF4, (byte) 0xB3, (byte) 0xB2, this, operands);
                }
            },

            new Mnemonic8051("da", 0) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051 ... operands) {
                    return accumulatorOperation((byte) 0xD4, this, operands);
                }
            },

            new Mnemonic8051("dec", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051 ... operands) {
                    return incDecOperation((byte) 0x14, (byte) 0x16, (byte) 0x18, (byte) 0x15, this, operands);
                }
            },

            new Mnemonic8051("div", 0) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051 ... operands) {
                    return tier2ArithmeticOperation((byte) 0x84, this, operands);
                }
            },

            new Mnemonic8051("djnz", 2, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051 ... operands) {
                    byte[] result = new byte[0];
                    OperandToken8051 op1 = operands[0], op2 = operands[1];
                    OperandType8051 type1 = op1.getOperandType();

                    Byte offset = handleShortJump(codePoint,op2, 2);

                    switch (type1) {
                        case ADDRESS: {
                            int value = Integer.parseInt(op1.getValue());
                            if (value <= 0xff)
                                result = new byte[]{(byte) 0xD5, offset == null ? -2 : offset};
                            else
                                op1.setError(new SimpleAssemblyError(Type.ERROR, "Value of direct address too big!"));
                            break;
                        }
                        case NAME:
                            if (op1.getValue().startsWith("r")) {
                                int ordinal = Integer.parseInt(op1.getValue().substring(1));
                                if (ordinal > 7)
                                    op1.setError(new SimpleAssemblyError(Type.ERROR, "Register ordinal too big!"));
                                else
                                    return new byte[]{(byte)(ordinal | 0xD8), offset == null ? -2 : offset};
                                break;
                            }
                        default:
                            op1.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                    }

                    return result;
                }
            },

            new Mnemonic8051("inc", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051 ... operands) {
                    if (operands[0].getOperandType() == OperandType8051.NAME && operands[0].getValue().equals("dptr")) {
                        handleUnnecessaryOperands(this.getName(), false, 0, 1, operands);
                        return new byte[]{(byte) 0xA3};
                    } else
                        return incDecOperation((byte) 0x04, (byte) 0x06, (byte) 0x08, (byte) 0x05, this, operands);
                }
            },

            new Mnemonic8051("jb", 2, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    return jumpBitRelevant((byte) 0x20, this, codePoint, operands);
                }
            },

            new Mnemonic8051("jbc", 2, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    return jumpBitRelevant((byte) 0x10, this, codePoint, operands);
                }
            },

            new Mnemonic8051("jc", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    return jumpNameRelevant((byte) 0x40, this, codePoint, operands);
                }
            },

            new Mnemonic8051("jmp", 1, false) { // TODO add substitution support
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    boolean firstIgnored = true;
                    if (operands.length > 0) {
                        firstIgnored = false;
                        if (!(operands[0].getOperandType() == OperandType8051.INDIRECT_NAME &&
                              operands[0].getValue().equals("a+dptr")))
                            operands[0].setError(new SimpleAssemblyError(Type.WARNING, "Incompatible operand! " +
                                    "(Expected \"@a+dptr\")"));
                    } else {
                        // TODO handle ignored operands
                    }
                    handleUnnecessaryOperands(this.getName(), true, firstIgnored?1:0, 1, operands);

                    return new byte[]{(byte) 0x73};
                }
            },

            new Mnemonic8051("jnb", 2, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    return jumpBitRelevant((byte) 0x30, this, codePoint, operands);
                }
            },

            new Mnemonic8051("jnc", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    return jumpNameRelevant((byte) 0x50, this, codePoint, operands);
                }
            },

            new Mnemonic8051("jnz", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    return jumpNameRelevant((byte) 0x70, this, codePoint, operands);
                }
            },

            new Mnemonic8051("jz", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    return jumpNameRelevant((byte) 0x60, this, codePoint, operands);
                }
            },

            new Mnemonic8051("lcall", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    return longJump((byte) 0x12, this, codePoint, operands);
                }
            },

            new Mnemonic8051("ljmp", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    return longJump((byte) 0x02, this, codePoint, operands);
                }
            },

            new Mnemonic8051("mov", 2) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    byte[] result = new byte[0];

                    OperandToken8051 op1 = operands[0], op2 = operands[1];
                    OperandType8051 type1 = op1.getOperandType(), type2 = op2.getOperandType();
                    int ord1 = -1, ord2 = -1; // Ordinals for Rn and @Ri operands

                    for (int i = 0; i < 2; ++i) {
                        OperandToken8051 op = operands[i];  OperandType8051 type = op.getOperandType();
                        if ((type == OperandType8051.NAME || type == OperandType8051.INDIRECT_NAME) &&
                                op.getValue().startsWith("r")) {
                            int ordinal = Integer.parseInt(op.getValue().substring(1));

                            if (ordinal < (type == OperandType8051.NAME ? 8 : 2))
                                if (i == 0) ord1 = ordinal;
                                else ord2 = ordinal;
                            else
                                op.setError(new SimpleAssemblyError(Type.ERROR, "Register ordinal too big!"));
                        }
                    }


                    // Begin assembling
                    switch (type1) {
                        case ADDRESS: {
                            int dest = Integer.parseInt(op1.getValue());
                            if (dest > 0xff)
                                op1.setError(new SimpleAssemblyError(Type.ERROR, "Value of direct address too big!"));
                            else
                                switch (type2) {
                                    case ADDRESS: {
                                        int src = Integer.parseInt(op2.getValue());
                                        if (src > 0xff)
                                            op2.setError(new SimpleAssemblyError(Type.ERROR, "Value of direct address" +
                                                    " too big!"));
                                        else
                                            result = new byte[]{(byte) 0x85, (byte) src, (byte) dest}; //address, address
                                        break;
                                    }
                                    case CONSTANT: {
                                        int val = Integer.parseInt(op2.getValue());
                                        if (val > 0xff)
                                            op2.setError(new SimpleAssemblyError(Type.ERROR, "Value of constant too" +
                                                    " big!"));
                                        else
                                            result = new byte[]{(byte) 0x75, (byte) dest, (byte) val}; //address, #constant
                                        break;
                                    }
                                    case INDIRECT_NAME:
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
                                        }
                                    }
                                    default:
                                        op2.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                                }
                            break;
                        }
                        case INDIRECT_NAME:
                        case NAME: {
                            String val = op1.getValue();
                            if (val.equals("c")) {
                                if (type2 == OperandType8051.ADDRESS) {
                                   int valAd = Integer.parseInt(op2.getValue());
                                    if (valAd > 0xff)
                                        op2.setError(new SimpleAssemblyError(Type.ERROR, "Value of direct address too" +
                                                " big!"));
                                    else
                                        result = new byte[] {(byte) 0xA2, (byte) valAd};      // c, address
                                } else
                                    op2.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                                break;
                            } else if (val.equals("dptr")) {
                                if (type2 == OperandType8051.CONSTANT) {
                                    int val2 = Integer.parseInt(op2.getValue());
                                    result = new byte[] {(byte) 0x90,                         // dptr, #constant16
                                                         (byte)(val2 >>> 8 & 0xff), // Extract high byte
                                                         (byte)(val2 & 0xff)};      // Extract low byte
                                } else
                                    op2.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                                break;
                            } else if (val.equals("a")) {
                                switch (type2) {
                                    case CONSTANT:
                                    case ADDRESS: {
                                        int val2 = Integer.parseInt(op2.getValue());
                                        if (val2 > 0xff)
                                            op2.setError(new SimpleAssemblyError(Type.ERROR, "Value of " +
                                                    (type2==OperandType8051.ADDRESS?"direct address":"constant") +
                                                    " too big!"));
                                        else
                                            return new byte[]{(byte)(type2 == OperandType8051.ADDRESS ? 0xE5 ://a,direct
                                            0x74), (byte) val2};                              // a, #constant
                                        break;
                                    }
                                    case INDIRECT_NAME:
                                    case NAME: {
                                        if (val.startsWith("r") && ord2 != -1) {
                                            result = new byte[]{(byte) (ord2
                                                    | (type2 == OperandType8051.NAME ? 0xE8 : // a,  Rn
                                                    0xE6))};                                  // a, @Ri
                                            break;
                                        }
                                    }
                                    default:
                                        op2.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                                }
                                break;
                            } else if (val.startsWith("r")) {
                                switch (type2) {
                                    case CONSTANT:
                                    case ADDRESS: {
                                        int valCon = Integer.parseInt(op2.getValue());
                                        if (valCon > 0xff)
                                            op2.setError(new SimpleAssemblyError(Type.ERROR, "Value of " +
                                                    (type2 == OperandType8051.CONSTANT ? "constant":"direct address") +
                                                     " too big!"));
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
                                        op2.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                                }
                                break;
                            }
                        }
                        default:
                            op1.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                    }

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
     * Generates instruction codes for all bit operations (~ =0 =1) because they are all generated the same way with
     * different opcodes.<br>
     * This function is used by the <code>CLR</code>, <code>SETB</code> and <code>CPL</code> mnemonics.
     *
     * @param opc1 the opcode for <code>A</code> as operand.
     * @param opc2 the opcode for <code>C</code> as operand.
     * @param opc3 the opcode for <code>direct address</code> as operand.
     *
     * @param mnemonic the mnemonic that uses this method.
     * @param operands the operands of the mnemonic.
     *
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] bitOperation(final byte opc1, final byte opc2, final byte opc3,
                                       Mnemonic8051 mnemonic, OperandToken8051... operands) {
        byte[] result = new byte[0];

        OperandToken8051 op = operands[0];
        OperandType8051 type = op.getOperandType();

        switch (type) {
            case ADDRESS:
                int value = Integer.parseInt(op.getValue());
                if (value > 0xff)
                    op.setError(new SimpleAssemblyError(Type.ERROR, "Value of direct address too big!"));
                else
                    return new byte[] {opc3, (byte) value};
                break;
            case NAME:
                if (op.getValue().equals("a"))
                    return new byte[]{opc1};
                else if (op.getValue().equals("c"))
                    return new byte[]{opc2};
            default:
                op.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
        }

        handleUnnecessaryOperands(mnemonic.getName(), false, 0, 1, operands);

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
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] accumulatorOperation(final byte opc1,
                                               Mnemonic8051 mnemonic, OperandToken8051 ... operands) {
        boolean opIsA = false;

        if (operands.length > 0) {
            if (!operands[0].getValue().equals("a"))
                operands[0].setError(new SimpleAssemblyError(Type.WARNING, "Incompatible operand! (Expected \"a\")"));
            opIsA = true;
        } else {
            // TODO: Correct Error handling (for no operands as well)
        }

        handleUnnecessaryOperands(mnemonic.getName(), true, opIsA?0:1, 1, operands);

        return new byte[] {opc1};
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
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] incDecOperation(final byte opc1, final byte opc2, final byte opc3, final byte opc4,
                                          Mnemonic8051 mnemonic, OperandToken8051... operands) {
        byte[] result = new byte[0];

        OperandToken8051 op = operands[0];
        OperandType8051 type = op.getOperandType();

        switch (type) {
            case ADDRESS: {
                int value = Integer.parseInt(op.getValue());

                if (value > 0xff)
                    op.setError(new SimpleAssemblyError(Type.ERROR, "Value of direct address too big!"));
                else
                    return new byte[] {opc4, (byte) value};
                break;
            }
            case INDIRECT_NAME:
            case NAME:
                if (op.getValue().equals("a")) {
                    result = new byte[]{opc1};
                    break;
                }
                else if (op.getValue().startsWith("r")) {
                    int ordinal = Integer.parseInt(op.getValue().substring(1));
                    if (ordinal > (type == OperandType8051.NAME? 7 : 1))
                        op.setError(new SimpleAssemblyError(Type.ERROR, "Register ordinal too high!"));
                    else {
                        result = new  byte[] {(byte)(ordinal
                                | (type==OperandType8051.NAME? opc3 : opc2))}; // Set desired bits to ordinal
                    }
                    break;
                }
            default:
                op.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
        }

        handleUnnecessaryOperands(mnemonic.getName(), false, 0, 1, operands);

        return result;
    }
    /**
     * Generates instruction codes for all tier 2 arithmetic operations (* /) because they are all generated the same way with
     * different opcodes.<br>
     * This function is used by the <code>ADD</code>, <code>ADDC</code> and <code>SUBB</code> mnemonics.
     *
     * @param opc1 the opcode for the instruction.
     *
     * @param mnemonic the mnemonic that uses this method.
     * @param operands the operands of the mnemonic.
     *
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] tier2ArithmeticOperation(final byte opc1,
                                                   Mnemonic8051 mnemonic, OperandToken8051 ... operands) {
        int ignored = 0;

        if (operands.length >= 2) {
            ignored = -1;

            if (!(operands[0].getOperandType() == OperandType8051.NAME && operands[0].getValue().equals("a")))
                operands[0].setError(new SimpleAssemblyError(Type.WARNING, "Incompatible operand! (Expected \"a\")"));

            if (!(operands[1].getOperandType() == OperandType8051.NAME && operands[1].getValue().equals("b")))
                operands[1].setError(new SimpleAssemblyError(Type.WARNING, "Incompatible operand! (Expected \"b\")"));

        } else if (operands.length >= 1) {
            ignored = 0;

            if (!(operands[0].getOperandType() == OperandType8051.NAME && operands[0].getValue().equals("ab")))
                operands[0].setError(new SimpleAssemblyError(Type.WARNING, "Incompatible operand! (Expected \"ab\")"));

        } else {
            // TODO Handle ignored operands
        }


        handleUnnecessaryOperands(mnemonic.getName(), true, ignored, 1, operands);

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
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] jumpBitRelevant(final byte opc1,
                                          Mnemonic8051 mnemonic, long codePoint, OperandToken8051 ... operands) {
        byte[] result = new byte[0];

        OperandToken8051 op1 = operands[0], op2 = operands[1];
        Byte offset = handleShortJump(codePoint, op2, 3);

        if (op1.getOperandType() == OperandType8051.ADDRESS) {
            int value = Integer.parseInt(op1.getValue());
            if (value > 0xff)
                op1.setError(new SimpleAssemblyError(Type.ERROR, "Value of direct address too big!"));
            else
                result = new byte[]{opc1, offset == null ? -3 : offset};
        } else
            op1.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));

        handleUnnecessaryOperands(mnemonic.getName(), false, 0, 2, operands);
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
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] jumpNameRelevant(final byte opc1,
                                           Mnemonic8051 mnemonic, long codePoint, OperandToken8051 ... operands) {
        Byte offset = handleShortJump(codePoint, operands[0], 2);
        handleUnnecessaryOperands(mnemonic.getName(), false, 0, 1, operands);

        return new byte[]{opc1, offset == null ? -2 : offset};
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
     * @return
     *      an assembled representation of the mnemonic.
     *      It consists of the opcode and the assembled
     *      operands.
     */
    private static byte[] longJump(final byte opc1,
                                   Mnemonic8051 mnemonic, long codePoint, OperandToken8051 ... operands) {
        byte[] result = new byte[0];
        OperandType8051 type = operands[0].getOperandType();

        if (type == OperandType8051.ADDRESS ||
            type == OperandType8051.ADDRESS_OFFSET) {
            long jump = Integer.parseInt(operands[0].getValue());

            if (type == OperandType8051.ADDRESS_OFFSET)
                jump = getFromOffset(codePoint, jump, 3);

            result = new byte[] {(byte)(jump >>> 8 & 0xffL),// Shift 8 to the right get the high byte and clear the rest
                                                            // ( ... x x 15 14 13 12 11 10 9 8)
                                 (byte)(jump & 0xffL)};     // Extract low byte ( ... x x 7 6 5 4 3 2 1 0)
        } else
            operands[0].setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));

        handleUnnecessaryOperands(mnemonic.getName(), false, 0, 1, operands);

        return result;
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
     * @return
     *      the needed offset.<br>
     *      <code>null</code> if an error occurred. The error will reported with the
     *      operand as cause. An error occurs if either the operand has an illegal type
     *      (not <code>ADDRESS</code>) or the resulting jump is too far for a short
     *      jump (out of the <code>-128</code> - <code>+128</code> range).
     */
    private static Byte handleShortJump(long codePoint, OperandToken8051 op, int offset) {
        if (op.getOperandType() == OperandType8051.ADDRESS) {
            long i = getOffset(codePoint, Integer.parseInt(op.getValue()), offset);
            if (i >= -128 && i <= 127)
                return (byte) i;
            else {
                op.setError(new SimpleAssemblyError(Type.ERROR, "Jump is too far for a short jump!"));
            }
        } else
            op.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));

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
     * @return
     *      the calculated target code point from the <code>codePoint</code> and the <code>offset</code>.
     */
    private static long getFromOffset(long codePoint, long codeOffset, int offset) {
        if (offset < 0)
            throw new IllegalArgumentException("Offset 'offset' cannot be negative!");
        long result =  codePoint+offset + codeOffset;

        if (result >= 0 && result <= 0xffffL)
            return result;
        else
            throw new IllegalArgumentException("Address is out of range! ('" + result + "')");
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

    /**
     * Generates an error for a given ErrorHandling.<br>
     * The messages will be taken from 2 given Strings.
     *
     * @param setting the ErrorHandling that should be used for generating.
     * @param errorMessage the message String for the case of a generated error
     * @param warningMessage the message String for the case of an generated warning
     *
     * @return
     *      an error with the given messages and error type.<br>
     *      If the error is set to be ignored, <code>null</code> will be returned.
     */
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
