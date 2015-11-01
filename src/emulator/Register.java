package emulator;

/**
 * This interface represents a CPU register. It has setters and getters for binary, hex and decimal strings that
 * represent a register value.<br>
 * A register has a name (e.g. "A") and a numerical value (e.g. 42).
 *
 * @author 5hir0kur0
 */
public interface Register {
    /**
     * @return the {@code Register}'s name (cannot be {@code null})
     */
    String getName();

    /**
     * @param target {@code NumeralSystem} used to perform the conversion
     * @return {@code String} representing the {@code Register}'s (unsigned) value in the numeral system specified by
     *     {@code target}
     */
    String getDisplayValue(NumeralSystem target);

    /**
     * @return {@code String} representing the {@code Register}'s (unsigned) value in binary.
     */
    default String getBinaryDisplayValue() {
        return getDisplayValue(NumeralSystem.BINARY);
    }

    /**
     * @return {@code String} representing the {@code Register}'s (unsigned) value in decimal.
     */
    default String getDecimalDisplayValue() {
        return getDisplayValue(NumeralSystem.DECIMAL);
    }

    /**
     * @return {@code String} representing the {@code Register}'s (unsigned) value in hexadecimal.
     */
    default String getHexadecimalDisplayValue() {
        return getDisplayValue(NumeralSystem.HEXADECIMAL);
    }

    /**
     * Set the {@code Register}'s value to the value represented by {@code newValue} in the specified numeral system.
     * <br>
     * This operation is expected to fail quite often (as it relies on user input), hence it returns a {@code boolean}
     * instead of throwing an exception.
     * @param numeralSystem {@code NumeralSystem} used for the conversion
     * @param newValue {@code String} that holds the value. Must not be {@code null} or empty.
     * @return {@code boolean} indicating whether the operation was successful.
     */
    boolean setValueFromString(NumeralSystem numeralSystem, String newValue);

    /**
     * Set the {@code Register}'s value to the value represented by {@code newValue} in binary.
     * <br>
     * This operation is expected to fail quite often (as it relies on user input), hence it returns a {@code boolean}
     * instead of throwing an exception.
     * @param newValue {@code String} that holds the value. Must not be {@code null} or empty.
     * @return {@code boolean} indicating whether the operation was successful.
     */
    default boolean setValueFromBinaryString(String newValue) {
        return setValueFromString(NumeralSystem.BINARY, newValue);
    }

    /**
     * Set the {@code Register}'s value to the value represented by {@code newValue} in decimal.
     * <br>
     * This operation is expected to fail quite often (as it relies on user input), hence it returns a {@code boolean}
     * instead of throwing an exception.
     * @param newValue {@code String} that holds the value. Must not be {@code null} or empty.
     * @return {@code boolean} indicating whether the operation was successful.
     */
    default boolean setValueFromDecimalString(String newValue) {
        return setValueFromString(NumeralSystem.DECIMAL, newValue);
    }


    /**
     * Set the {@code Register}'s value to the value represented by {@code newValue} in hexadecimal.
     * <br>
     * This operation is expected to fail quite often (as it relies on user input), hence it returns a {@code boolean}
     * instead of throwing an exception.
     * @param newValue {@code String} that holds the value. Must not be {@code null} or empty.
     * @return {@code boolean} indicating whether the operation was successful.
     */
    default boolean setValueFromHexadecimalString(String newValue) {
        return setValueFromString(NumeralSystem.HEXADECIMAL, newValue);
    }
}
