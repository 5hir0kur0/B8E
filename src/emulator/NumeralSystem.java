package emulator;

import org.omg.PortableInterceptor.LOCATION_FORWARD;

/**
 * This class represents a numeral system (binary, decimal or hexadecimal) and provides means of converting between
 * different numeral systems.<br>
 *
 * Signed numbers are not supported.<br>
 *
 * The class uses the standard library to perform all the conversions.
 *
 * @author 5hir0kur0
 * @see Long#parseUnsignedLong(String, int)
 */
public enum NumeralSystem {
    BINARY() {
        @Override
        public long getValue(String value) {
            if (value.charAt(0) == '+') throw new NumberFormatException("No signs allowed in numbers.");
            return Long.parseUnsignedLong(value, 2);
        }
    }, DECIMAL() {
        @Override
        public long getValue(String value) {
            if (value.charAt(0) == '+') throw new NumberFormatException("No signs allowed in numbers.");
            return Long.parseUnsignedLong(value, 10);
        }
    }, HEXADECIMAL() {
        @Override
        public long getValue(String value) {
            if (value.charAt(0) == '+') throw new NumberFormatException("No signs allowed in numbers.");
            return Long.parseUnsignedLong(value, 16);
        }
    };

    /**
     * Create a string representing the number stored in {@code value} in the numeral system specified by {@code target}
     * @param target
     *     the {@code NumeralSystem} the value will be converted into
     * @param value
     *     the value to be converted into a {@code String}
     * @return
     *     a {@code String} representing the number stored in {@code value} in the numeral system specified
     *     by {@code target}
     */
    public static String toString(NumeralSystem target, long value) {
        switch (target) {
            case BINARY: return Long.toUnsignedString(value, 2);
            case DECIMAL: return Long.toUnsignedString(value, 10);
            case HEXADECIMAL: return Long.toUnsignedString(value, 16).toUpperCase();
            default: throw new UnsupportedOperationException("Illegal numeral system: "+target);
        }
    }

    /**
     * Create a string representing the number stored in {@code value} in the numeral system specified by {@code target}
     * <br>
     * The string will be padded with zeros from the left to match the given length.
     * @param target
     *     the {@code NumeralSystem} the value will be converted into
     * @param value
     *     the value to be converted into a {@code String}
     * @param length
     *     the desired length; must be bigger or equal to the string resulting from
     *     {@code Long.toUnsignedString(value))
     * @return
     *     a {@code String} representing the number stored in {@code value} in the numeral system specified
     *     by {@code target}
     */
    public static String toString(NumeralSystem target, long value, int length) {
        switch (target) {
            case BINARY: return Misc.zeroFill(Long.toUnsignedString(value, 2), length);
            case DECIMAL: return Misc.zeroFill(Long.toUnsignedString(value, 10), length);
            case HEXADECIMAL: return Misc.zeroFill(Long.toUnsignedString(value, 16).toUpperCase(), length);
            default: throw new UnsupportedOperationException("Illegal numeral system: "+target);
        }
    }

    /**
     * Convert a string representing a number in the numeral system represented by the current instance to a long
     * using {@code Long.parseUnsignedLong(...)}
     * @param
     *     value string representing a number in this instance's numeral system
     * @throws NumberFormatException
     *     whenever {@code Long.parseUnsignedLong(...)} throws it and if the expression
     *     starts with a '+'.
     * @return
     *     the result of the conversion
     * @see Long#parseUnsignedLong(String, int)
     */
    public abstract long getValue(String value) throws NumberFormatException;

    /**
     * Create a string representing the number stored in {@code value} in the numeral system represented by
     * this instance.
     * @param value
     *     the value to be converted into a {@code String}
     * @return
     *     a {@code String} representing the number stored in {@code value} in the numeral system represented by
     *     this instance
     */
    public String toString(long value) {
        return NumeralSystem.toString(this, value);
    }

    /**
     * Create a string representing the number stored in {@code value} in the numeral system represented by
     * this instance.
     * <br>
     * The string will be padded with zeros from the left to match the given length.
     * @param value
     *     the value to be converted into a {@code String}
     * @param length
     *     the desired length; must be bigger or equal to the string resulting from
     *     {@code Long.toUnsignedString(...))}
     * @return
     *     a {@code String} representing the number stored in {@code value} in the numeral system specified
     *     by {@code target}
     * @see Long#toUnsignedString(long, int)
     */
    public String toString(long value, int length) {
        return NumeralSystem.toString(this, value, length);
    }
}
