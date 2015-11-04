package emulator;

import java.util.Arrays;

/**
 * Miscellaneous utilities for the emulator package.
 *
 * @author 5hir0kur0
 */
class Misc {
    /**
     * Pads a string with '0's from the left.
     * @param length The new length.
     * @param source The {@code String} to be padded with '0'
     * @return The resulting {@code String}
     */
    public static String zeroFill(String source, int length) {
        if (null == source) throw new IllegalArgumentException("source must not be null");
        if (length < source.length()) throw new IllegalArgumentException("length must be >= source.length");
        if (length == source.length()) return source;
        int numZeros = length - source.length();
        char[] zeros = new char[numZeros];
        Arrays.fill(zeros, '0');
        return new String(zeros)+source;
    }
}