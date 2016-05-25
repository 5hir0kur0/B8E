package assembler.arc8051;

import assembler.tokens.Token;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author Jannik
 */
public class DirectiveTokens {

    /**
     * Represents a token that indicates that the current file should be
     * changed.
     *
     * @author Jannik
     */
    public static class FileChangeToken extends Token {

        /** The path the file should be changed to.*/
        private Path file;

        /**
         * Constructs a new <code>FileChangeToken</code> that indicates that the
         * current file should be changed to a new file.<br>
         *
         * @param file
         *      the new file.
         * @param line
         *      the line of the token.
         */
        public FileChangeToken(Path file, int line) {
            super(Objects.requireNonNull(file, "'file' cannot be 'null'!").toString(), TokenType.DIRECTIVE, line);
            this.file = file;
        }

        /**
         * Returns the file the current file should be
         * changed to.
         */
        public Path getFile() {
            return file;
        }
    }

    /**
     * Represents a token that contains data in form of a byte array that should be
     * written at the space in the code memory.
     *
     * @author Jannik
     */
    public static class DataToken extends Token {

        /** The associated data. */
        private byte[] data;

        /**
         * Constructs a new <code>DataToken</code> that contains a set
         * of data in form of a byte array.<br>
         * The data should be written at the corresponding space in the
         * code.
         *
         * @param data
         *      the associated data.
         * @param line
         *      the line of the token.
         */
        public DataToken(final byte[] data, int line) {
            super(Arrays.toString(Objects.requireNonNull(data, "'data' cannot be 'null'!")), TokenType.DIRECTIVE, line);
        }

        /**
         * Returns the data associated with this token
         * as a byte array.
         */
        public byte[] getData() {
            return Arrays.copyOf(data, data.length);
        }
    }

    /**
     * Represents a token that indicates that the internal address should be
     * changed.
     *
     * @author Jannik
     */
    public static class OriginChangeToken extends Token {

        /** The code point the <code>OriginChangeToken</code> is referring to. */
        private long codePoint;

        /**
         * Constructs a new <code>OriginChangeToken</code> that contains an
         * address.<br>
         * The the internal address should be changed to the given one.
         *
         * @param codePoint
         *      the address the internal address should be set to.
         * @param line
         *      the line of the token.
         */
        public OriginChangeToken(final long codePoint, int line) {
            super(Long.toString(codePoint), TokenType.DIRECTIVE, line);
            if ((this.codePoint = codePoint) < 0) throw new IllegalArgumentException("'code point' cannot be 'null'!");
        }

        /**
         * Returns the <code>code point</code> the internal <code>code point</code>
         * should be set to.
         */
        public long getAddress() {
            return codePoint;
        }
    }
}
