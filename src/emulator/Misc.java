package emulator;

import java.util.Arrays;

/**
 * Miscellaneous utilities for the emulator package.
 *
 * @author 5hir0kur0
 */
public class Misc {
    /**
     * Pads a string with '0's from the left.
     * @param length
     *     the new length
     * @param source
     *     the {@code String} to be padded with '0'
     * @return
     *     the resulting {@code String}
     */
    public static String zeroFill(String source, int length) {
        if (null == source) throw new IllegalArgumentException("source must not be null");
        if (length <= source.length()) return source;
        int numZeros = length - source.length();
        char[] zeros = new char[numZeros];
        Arrays.fill(zeros, '0');
        return new String(zeros)+source;
    }

    public static String getByteDisplayValue(NumeralSystem target, int value) {
        switch (target) {
            case BINARY: return target.toString(value & 0xFF, 8);
            case DECIMAL: return target.toString(value & 0xFF, 3);
            case HEXADECIMAL: return target.toString(value & 0xFF, 2);
            default: throw new IllegalArgumentException("Invalid numeral system.");
        }
    }
}
