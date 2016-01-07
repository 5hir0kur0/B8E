package assembler;

/**
 * @author Polymehr
 */
public interface LabelConsumer {
    int getLength(long codePoint, OperandToken... operands);
}
