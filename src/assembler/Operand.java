package assembler;

/**
 * Represents an operand of a mnemonic.
 *
 * @author Jannik
 */
public class Operand {

    /** Type of the operand. Should be named via <code>final</code> variables. */
    private int type;

    /** The value of the operand as a String. */
    private String value;

    /**
     * Constructs a new Operand.
     *
     * @param type
     *      the type of the Operand.<br>
     *      To remember the type better its value should be
     *      saved in a named final variable or an enum's ordinal
     *      and it should be used instead.<br>
     * @param value
     *      the value of the Operand as a String.
     */
    public Operand(int type, String value) {
        this.type = type;

        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("Value cannot be 'null' or empty.");
        this.value = value;
    }

    /**
     * @return
     *      the type of the operand as an integer.
     */
    public int getType() {
        return type;
    }

    /**
     * @return
     *      whether the given type equals the type of the operand.
     */
    public boolean isType(int type) {
        return this.type == type;
    }

    /**
     * @return
     *      the value of this operand as a String.
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value
     *      the value of this operand as a String.
     * @return
     *      the Operand itself.
     */
    public Operand setValue(String value) {
        if (!(value == null || value.isEmpty()))
            this.value = value;
        return this;
    }

    /**
     * @param type
     *      the type to be set.<br>
     *      To remember the type better its value should be
     *      saved in a named final variable or an enum's ordinal
     *      and it should be used instead.<br>
     * @return
     *      the Operand itself.
     */
    public Operand setType(int type) {
        this.type = type;
        return this;
    }
}
