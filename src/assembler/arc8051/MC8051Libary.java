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
                    byte[] result = new byte[0];
                    if (operands[0].getOperandType() == OperandType8051.ADDRESS) {
                        long jump = Long.parseLong(operands[0].getValue()) + 2;

                        if ((jump >>> 11L & 0x1fL) == (codePoint >>> 11L & 0x1fL))
                            result = new byte[]{((byte)(jump >>> 3L & 0xE00L | 0x11L)), (byte)(jump & 0xffL)};
                        else
                            operands[0].setError(new SimpleAssemblyError(Type.ERROR,
                                    "Call address too far absolute 11 bit addressing!"));
                    } else
                        operands[0].setError(new SimpleAssemblyError(Type.ERROR,
                                "Operand needs to be an address!"));

                    for (int i = 1; i < operands.length; ++i)
                        operands[i].setError(getErrorFromErrorHandlingSetting(Settings.Errors.ADDITIONAL_OPERANDS,
                                "Too many operands! ACALL must have exactly 1 operand.", "Unnecessary operand."));
                    return result;
                }
            },

            new Mnemonic8051("add", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
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
                                result = new byte[]{(byte) (type == OperandType8051.ADDRESS ? 0x25:0x24), (byte) value};
                            break;
                        }
                        case INDIRECT_NAME:
                        case NAME:
                            if (op.getValue().startsWith("r")) {
                                int ordinal = Integer.parseInt(op.getValue().substring(1));
                                if (ordinal > (type == OperandType8051.NAME? 7 : 1))
                                    op.setError(new SimpleAssemblyError(Type.ERROR, "Register ordinal too high!"));
                                else {
                                    result = new  byte[] {(byte)(ordinal|(type==OperandType8051.NAME?0x28:0x26))};
                                    break;
                                }
                            }
                        default:
                            op.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                    } //TODO Multiple error handling.

                    final String err = Settings.Errors.IGNORE_OBVIOUS_OPERANDS == ErrorHandling.ERROR ?
                            "Too many operands! ADD must have exactly 2 operands." :
                            "Too many operands! ADD must have 1 or 2 operands.";
                    for (int i = firstIsA?2:1; i < operands.length; ++i)
                        operands[i].setError(getErrorFromErrorHandlingSetting(Settings.Errors.ADDITIONAL_OPERANDS,
                                err, "Unnecessary operand."));

                    return result;
                }
            },

            new Mnemonic8051("addc", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    byte[] result = new byte[0];

                    boolean firstIsA = true;

                    if (!(operands[0].getOperandType() == OperandType8051.NAME &&
                            operands[0].getValue().equalsIgnoreCase("a"))) {
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
                                result = new byte[]{(byte) (type == OperandType8051.ADDRESS ? 0x35:0x34), (byte) value};
                            break;
                        }
                        case INDIRECT_NAME:
                        case NAME:
                            if (op.getValue().startsWith("r")) {
                                int ordinal = Integer.parseInt(op.getValue().substring(1));
                                if (ordinal > (type == OperandType8051.NAME? 7 : 1))
                                    op.setError(new SimpleAssemblyError(Type.ERROR, "Register ordinal too high!"));
                                else {
                                    result = new  byte[] {(byte)(ordinal|(type==OperandType8051.NAME?0x38:0x36))};
                                    break;
                                }
                            }
                        default:
                            op.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                    } //TODO Multiple error handling.

                    final String err = Settings.Errors.IGNORE_OBVIOUS_OPERANDS == ErrorHandling.ERROR ?
                            "Too many operands! ADD must have exactly 2 operands." :
                            "Too many operands! ADD must have 1 or 2 operands.";
                    for (int i = firstIsA?2:1; i < operands.length; ++i)
                        operands[i].setError(getErrorFromErrorHandlingSetting(Settings.Errors.ADDITIONAL_OPERANDS,
                                err, "Unnecessary operand."));

                    return result;
                }
            },

            new Mnemonic8051("ajmp", 1, true) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    byte[] result = new byte[0];
                    if (operands[0].getOperandType() == OperandType8051.ADDRESS) {
                        long jump = Long.parseLong(operands[0].getValue()) + 2;

                        if ((jump >>> 11L & 0x1fL) == (codePoint >>> 11L & 0x1fL))
                            result = new byte[]{((byte)(jump >>> 3L & 0xE00L | 0x01L)), (byte)(jump & 0xffL)};
                        else
                            operands[0].setError(new SimpleAssemblyError(Type.ERROR,
                                    "Jump address too far absolute 11 bit addressing!"));
                    } else
                        operands[0].setError(new SimpleAssemblyError(Type.ERROR,
                                "Operand needs to be an address!"));

                    for (int i = 1; i < operands.length; ++i)
                        operands[i].setError(getErrorFromErrorHandlingSetting(Settings.Errors.ADDITIONAL_OPERANDS,
                                "Too many operands! AJMP must have exactly 1 operand.", "Unnecessary operand."));
                    return result;
                }
            },

            new Mnemonic8051("anl", 1) {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    byte[] result = new  byte[0];
                    OperandToken8051 op1 = operands[0];
                    boolean firstIgnored = false;

                    switch (op1.getOperandType()) {
                        case NEGATED_ADDRESS: {
                            firstIgnored = true;
                            op1.setError(getErrorFromErrorHandlingSetting(Settings.Errors.IGNORE_OBVIOUS_OPERANDS,
                                    "Missing 'c' as first operand!", "Operand 'c' should be written as first operand."));
                            result = new byte[]{(byte)0xb0};
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
                                    result = new  byte[] {(byte)(ordinal|(type==OperandType8051.NAME?0x58:0x56))};
                                    break;
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
                                                    result = new byte[]{0x53, (byte) value, (byte) constVal};
                                                    break;
                                                } else
                                                    op2.setError(new SimpleAssemblyError(Type.ERROR,
                                                            "Value of constant too big!"));
                                                break;
                                            }
                                            case NAME:
                                                if (op2.getValue().equals("a")) {
                                                    result = new byte[]{(byte) 0x52, (byte) value};
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
                                    if (op1.getValue().startsWith("a")) {
                                        if (op2.getOperandType() == OperandType8051.CONSTANT
                                                || op2.getOperandType() == OperandType8051.ADDRESS) {
                                            int value = Integer.parseInt(op2.getValue());
                                            OperandType8051 type = op2.getOperandType();
                                            if (value <= 0xff)
                                                result = new byte[]{(byte) (type == OperandType8051.CONSTANT ? 0x54 : 0x55),
                                                        (byte) value};
                                            else
                                                op2.setError(new SimpleAssemblyError(Type.ERROR, "Value of " +
                                                        (type == OperandType8051.CONSTANT ? "constant" : "direct address") +
                                                        " too big!"));
                                        } else
                                            op2.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));
                                        break;
                                    }
                                default:
                                    op1.setError(new SimpleAssemblyError(Type.ERROR, "Incompatible operand!"));

                            }
                        }

                    }


                    final String err = Settings.Errors.IGNORE_OBVIOUS_OPERANDS == ErrorHandling.ERROR ?
                            "Too many operands! ANL must have exactly 2 operands." :
                            "Too many operands! ANL must have 1 or 2 operands.";
                    for (int i = firstIgnored?2:1; i < operands.length; ++i)
                        operands[i].setError(getErrorFromErrorHandlingSetting(Settings.Errors.ADDITIONAL_OPERANDS,
                                err, "Unnecessary operand."));

                    return result;
                }
            }
    };

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
