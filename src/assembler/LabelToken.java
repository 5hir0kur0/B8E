package assembler;

/**
 * Represents a token of type 'label'.
 * Can hold the code point it is referring to.
 *
 * @author Jannik
 */
public class LabelToken extends Token {

    /**
     * The position in the code memory this label points
     * to.
     */
    private long codePoint;

    /**
     * Constructs a new LabelToken.
     *
     * @param value the value of the token.
     * @param line the line of the token.
     * @param codePoint
     *      the position in code memory this label
     *      points to.<br>
     *      Can be <code>-1</code> if not set.
     */
    public LabelToken(String value, int line, long codePoint) {
        super(value, TokenType.LABEL, line);

        if ((this.codePoint = codePoint) < -1)
            throw new IllegalArgumentException("Code point cannot be smaller that -1!");
    }

    /**
     * Constructs a new LabelToken and no code point (-1).
     *
     * @param value the value of the token.
     * @param line the line of the token.
     */
    public LabelToken(String value, int line) {
        this(value, line, -1);
    }

    /**
     * Returns the position in code memory this label
     * is referring to.
     */
    public long getCodePoint() {
        return codePoint;
    }

    /**
     * @param codePoint
     *      the position in code memory to set.<br>
     *      This value cannot be negative.
     */
    public void setCodePoint(long codePoint) {
        if (codePoint < 0)
            throw new IllegalArgumentException("Code point cannot be negative!");
        this.codePoint = codePoint;
    }


    @Override
    public String toString() {
        return String.format("%s(%d)[%s, %s"+(codePoint != -1?", %02x":"")+"]",
                getClass().getSimpleName(), line, type, value, codePoint);
    }
}
