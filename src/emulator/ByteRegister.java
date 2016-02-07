package emulator;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * This class represents a CPU register containing an 8-bit-value (byte).
 *
 * NOTE: The reason {@code this.value} is almost never used directly in the internal code is that there are
 *       (anonymous) subclasses which overwrite {@code getValue()} and {@code setValue(byte)} in order to synchronize
 *       registers like e.g. R0-R7 with other data structures.
 *
 * @author Gordian
 */
public class ByteRegister implements Register {
    private final String name;
    private byte value;

    //fires whenever value is changed
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    /**
     * @param name
     *     the {@code ByteRegister}'s name; must not be {@code null} or empty
     * @param initialValue
     *     the {@code ByteRegister}'s initial value
     */
    public ByteRegister(String name, byte initialValue) {
       if (null == name || name.isEmpty())
           throw new IllegalArgumentException("Register names must not be null or empty.");
        this.name = name;
        setValue(initialValue);
    }

    /**
     * @param name the {@code ByteRegister}'s name; must not be {@code null} or empty
     */
    public ByteRegister(String name) {
        this(name, (byte)0);
    }

    public void setValue(byte newValue) {
        byte oldValue = getValue();
        this.value = newValue;
        changeSupport.firePropertyChange("value", oldValue, newValue);
    }

    public byte getValue() {
        return this.value;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDisplayValue(NumeralSystem target) {
        switch (target) {
            case BINARY: return target.toString(Byte.toUnsignedLong(getValue()), 8);
            case DECIMAL: return target.toString(Byte.toUnsignedLong(getValue()), 3);
            case HEXADECIMAL: return target.toString(Byte.toUnsignedLong(getValue()), 2);
            default: throw new IllegalArgumentException("Invalid numeral system.");
        }
    }

    @Override
    public boolean setValueFromString(NumeralSystem numeralSystem, String newValue) {
        if (newValue.isEmpty()) return false;
        try {
            long tmp = numeralSystem.getValue(newValue);
            if (tmp < 0 || tmp > 255) return false;
            setValue((byte)tmp);
        } catch (NumberFormatException ignored) { return false; }
        return true;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.changeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public String toString() {
        return "ByteRegister[name=\""+this.name+"\";value="+Byte.toUnsignedInt(getValue())+"]";
    }

    @Override
    public boolean equals(Object other) {
        if (null == other) return false;
        if (this == other) return true;
        if (!(other instanceof ByteRegister)) return false;
        ByteRegister tmp = (ByteRegister) other;
        return this.getValue() == tmp.getValue() && this.name.equals(tmp.name);
    }
}
