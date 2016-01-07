package assembler;

/**
 * @author Jannik
 */
public interface LabelConsumer {
    int getLength(long codePoint, OperandToken... operands);
}
