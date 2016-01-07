package assembler;

/**
 * @author Noxgrim
 */
public interface LabelConsumer {
    int getLength(long codePoint, OperandToken... operands);
}
