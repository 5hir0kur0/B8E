package assembler.arc8051;

import assembler.LabelConsumer;

/**
 * @author Noxgrim
 */
public abstract class Mnemonic8051LabelConsumer extends Mnemonic8051 implements LabelConsumer {
    protected Mnemonic8051LabelConsumer(String name, int minOp, boolean positionSensitive) {
        super(name, minOp, positionSensitive);
    }

    protected Mnemonic8051LabelConsumer(String name, int minOp) {
        super(name, minOp);
    }
}
