package assembler.arc8051;

import assembler.util.assembling.LabelConsumer;

/**
 * Describes a Mnemonic8051 that has at least one operand that
 * can be label at first.
 *
 * @author Noxgrim
 */
public abstract class Mnemonic8051LabelConsumer extends Mnemonic8051 implements LabelConsumer {
    /**
     * Constructs a new Mnemonic8051LabelConsumer.
     *
     * @param name
     *      The name of the mnemonic.
     *      It is used to differentiate this mnemonic
     *      from other ones with other functions.
     *      The name will be converted to lower case.
     * @param minOp
     *      The minimum number of operands this
     *      mnemonic needs to work properly.
     * @param positionSensitive
     *      Whether this mnemonic's value changes
     *      with its position in code memory, e.g jumps
     *      or calls.
     */
    protected Mnemonic8051LabelConsumer(String name, int minOp, boolean positionSensitive) {
        super(name, minOp, positionSensitive);
    }

    /**
     * Constructs a new non position sensitive Mnemonic8051LabelConsumer.
     *
     * @param name
     *      The name of the mnemonic.
     *      It is used to differentiate this mnemonic
     *      from other ones with other functions.
     *      The name will be converted to lower case.
     * @param minOp
     *      The minimum number of operands this
     *      mnemonic needs to work properly.
     */
    protected Mnemonic8051LabelConsumer(String name, int minOp) {
        super(name, minOp);
    }
}
