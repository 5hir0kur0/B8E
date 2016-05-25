package assembler.tokens;

import assembler.util.assembling.Assembled;

/**
 * Represents a token of type 'label'.
 * Can hold the code point it is referring to.
 *
 * @author Noxgrim
 */
public class LabelToken extends Token {

    private long origin;

    /**
     * The position in the code memory this label points
     * to.
     */
    private long originOffset;

    private Assembled attachedTo;

    /**
     * Constructs a new LabelToken.
     *
     * @param value the value of the token.
     * @param line the line of the token.
     * @param originOffset
     *      the position in code memory this label
     *      points to.<br>
     *      Can be <code>-1</code> if not set.
     */
    public LabelToken(String value, int line, long origin, long originOffset) {
        super(value, TokenType.LABEL, line);
        if ((this.origin = origin) < 0)
            throw new IllegalArgumentException("Origin address cannot be negative");
        if ((this.originOffset = originOffset) < 0)
            throw new IllegalArgumentException("Origin address offset cannot be negative");
    }

    /**
     * Constructs a new LabelToken with <code>-1</code>
     * as origin and origin offset.
     *
     * @param value the value of the token.
     * @param line the line of the token.
     */
    public LabelToken(String value, int line) {
        super(value, TokenType.LABEL, line);
        this.origin = -1;
        this.originOffset = 0;
    }

    /**
     * Returns the position in code memory this label
     * is referring to.
     */
    public long getAddress() {
        return origin + originOffset;
    }

    /**
     * @param origin
     *      the origin address the address is based
     *      of to be set.<br>
     *      Cannot be set if it is already set.
     */
    public void setOrigin(long origin) {
        if (this.origin == -1)
            this.origin = origin;
        else
            throw new IllegalStateException("Origin address already set!");
    }

    /**
     * @return
     *      the {@link Assembled} associated with this label
     *      (the instructions that are before the label).
     */
    public Assembled getAttachedTo() {
        return attachedTo;
    }

    /**
     * @param attachedTo
     *      the {@link Assembled} associated with this label
     *      (the instructions that are before the label) to be
     *      set.
     */
    public void setAttachedTo(Assembled attachedTo) {
        this.attachedTo = attachedTo;
    }

    public void moveAddress(long amount) {
        this.originOffset += amount;
        if (this.originOffset < 0) {
            originOffset = 0;
            throw new IllegalArgumentException("Resulting address cannot be smaller than the origin address!");
        }
    }

    @Override
    public String toString() {
        return String.format("%s(%d)[%s, %s"+(originOffset != -1?", %02x":"")+"]",
                getClass().getSimpleName(), line, type, value, originOffset);
    }
}
