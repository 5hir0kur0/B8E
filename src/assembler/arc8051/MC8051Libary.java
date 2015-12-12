package assembler.arc8051;

import assembler.util.SimpleAssemblyError;
import assembler.util.Settings;
import assembler.util.Settings.ErrorHandling;
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
         * system (10) form.
         * The value has to be the raw value
         * without the '#'.
         */
        CONSTANT,
        /**
         * An address.<br> In 8051 assembly
         * language addresses have no prefix.<br>
         * All addresses must be present in decimal
         * system (10) form.
         */
        ADDRESS,
        /**
         * An 'negated' address. In 8051 assembly
         * language all negated addresses have a
         * solidus '/' as prefix.<br>
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

            new Mnemonic8051("acall") {
                @Override
                public byte[] getInstructionFromOperands(long codePoint, OperandToken8051... operands) {
                    byte[] result;
                    if (operands[0].getOperandType() == OperandType8051.ADDRESS) {
                        long jump = Long.parseLong(operands[0].getValue()) + 2;

                        if ((jump >>> 0xbL & 0x1fL) == (codePoint >>> 0xbL & 0x1fL))
                            result = new byte[]{((byte)(jump >>>0x5L & 0xf00L | 0x11)), (byte)(jump & 0xff)};
                        else {
                            operands[0].setError(new SimpleAssemblyError(Type.ERROR,
                                    "Call address too far far absolute 11 bit addressing!"));
                            result = new byte[0];
                        }
                    } else {
                        operands[0].setError(new SimpleAssemblyError(Type.ERROR,
                                "Operand needs to be an address!"));
                        result = new byte[0];
                    }

                    for (int i = 1; i < operands.length; ++i)
                        operands[i].setError(getErrorFromErrorHandlingSetting(Settings.Errors.ADDITIONAL_OPERANDS,
                                "Too many operands! ACALL must have exactly 1 operand.", "Unnecessary operand."));
                    return result;
                }
            },
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
