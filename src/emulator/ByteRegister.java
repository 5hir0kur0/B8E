package emulator;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * This class represents a CPU register containing an 8-bit-value (byte).
 *
 * @author Gordian
 */
public class ByteRegister implements Register {
    private final String name;
    private byte value;

    //fires whenever value is changed
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    /**
     * @param name the {@code ByteRegister}'s name. Must not be {@code null} or empty.
     * @param initialValue the {@code ByteRegister}'s initial value. All values are allowed.
     */
    public ByteRegister(String name, byte initialValue) {
       if (null == name || name.isEmpty())
           throw new IllegalArgumentException("Register names must not be null or empty.");
        this.name = name;
        this.value = initialValue;
    }

    protected void setValue(byte newValue) {
        byte oldValue = this.value;
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
            case BINARY: return target.toString(Byte.toUnsignedLong(this.value), 8);
            case DECIMAL: return target.toString(Byte.toUnsignedLong(this.value), 3);
            case HEXADECIMAL: return target.toString(Byte.toUnsignedLong(this.value), 2);
            default: throw new IllegalArgumentException("Invalid numeral system.");
        }
    }

    @Override
    public boolean setValueFromString(NumeralSystem numeralSystem, String newValue) {
        if (newValue.isEmpty()) return false;
        try {
            long tmp = numeralSystem.getValue(newValue);
            if (tmp < 0 || tmp > 255) return false;
            this.value = (byte)tmp;
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
        return "ByteRegister[name=\""+this.name+"\";value="+Byte.toUnsignedInt(this.value)+"]";
    }
}
