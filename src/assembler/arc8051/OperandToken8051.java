package assembler.arc8051;

import assembler.tokens.OperandToken;

/**
 * Represents an operand for an 8051 mnemonic.<br>
 * All types of this are specified by the nested
 * <code>OperandType8051</code> class.
 *
 * @author Jannik
 */
public class OperandToken8051 extends OperandToken {
    /**
     * Constructs a new OperandToken.
     *
     * @param type  the operandType of the OperandToken.<br>
     * @param value the value of the token as a string.
     * @param line the line of the token.
     */
    public OperandToken8051(OperandType8051 type, String value, int line) {
        super(type, value, line);
    }

    @Override
    public OperandType8051 getOperandType() {
        return (OperandType8051) operandType;
    }

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
         *     MOV A, 0F0h
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

        /** Whether the type is a <code>CONSTANT</code>. */
        public boolean isConstant() { return this == CONSTANT; }
        /** Whether the type is a <code>ADDRESS</code>. */
        public boolean isAddress() { return this == ADDRESS; }
        /** Whether the type is a <code>NEGATED_ADDRESS</code>. */
        public boolean isNegatedAddress() { return this == NEGATED_ADDRESS; }
        /** Whether the type is a <code>ADDRESS_OFFSET</code>. */
        public boolean isAddressOffset() { return this == ADDRESS_OFFSET; }
        /** Whether the type is a <code>NAME</code>. */
        public boolean isName() { return this == NAME; }
        /** Whether the type is a <code>INDIRECT_NAME</code>. */
        public boolean isIndirectName() {return  this == INDIRECT_NAME; }
    }
}
