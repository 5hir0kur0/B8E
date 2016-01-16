package assembler;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Objects;

/**
 * Writes a given input of Assembled in Intel HEX format into a Writer.<br>
 * <a href="http://www.keil.com/support/docs/1584/">More information</a>
 *
 * @author Jannik
 */
public class HexWriter implements AutoCloseable {
    /**
     * The Writer that is used for the output.
     */
    private Writer out;
    /**
     * The used buffer.
     * The record will be written if it is full.
     */
    private byte[] buffer;

    /** The current length of the buffer. Cannot be bigger that the buffer size. */
    private int bufferLength;

    /** The current address (code point) of the minimal byte in the buffer. */
    private long address;

    /**
     * Constructs a new HexWriter.
     *
     * @param out
     *      the used output writer.
     * @param buffer
     *      the maximum buffer size.
     */
    public HexWriter(Writer out, int buffer) {
        this.out = Objects.requireNonNull(out, "'Writer' cannot be 'null'!");

        if (buffer <= 0 || buffer > 0xFF)
            throw new IllegalArgumentException("'Buffer size' cannot be negative, zero or bigger than 0xff!");
        this.buffer = new byte[buffer];

        bufferLength = 0;
    }

    /**
     * Constructs a new HexWriter with a buffer size of 16.
     *
     * @param out
     *      the used output writer.
     */
    public HexWriter(Writer out) {
        this(out, 16);
    }

    /**
     * Writes a Assembled into the buffer.<br>
     * The buffer wraps around instructions instead of bytes.
     *
     * @param assembled
     *      the used Assembled.
     */
    public void write(Assembled assembled) throws IOException {
        write(assembled, true);
    }

    /**
     * Writes a Assembled into the buffer.<br>
     *
     * @param assembled
     *      the used Assembled.
     * @param instructionWrap
     *      whether the buffer wraps around instructions ({@link Assembled#getCodes()})
     *      or bytes.
     */
    public void write(Assembled assembled, boolean instructionWrap) throws IOException {
        byte[] codes = assembled.getCodes();

        if (codes.length > buffer.length && instructionWrap)
            throw new IndexOutOfBoundsException("Cannot write instruction codes into the buffer because the " +
                    "buffer is too small");

        if ((address+bufferLength) != assembled.getCodePoint()) {
            flushBuffer();
            address = assembled.getCodePoint();
        }

        if (instructionWrap) {
            if (bufferLength + codes.length <= buffer.length)
                for (byte b : codes)
                    buffer[bufferLength++] = b;
            else {
                flushBuffer();
                address = assembled.getCodePoint();
                for (byte b : codes)
                    buffer[bufferLength++] = b;
            }
        } else {
            for (byte c : codes) {
                if (buffer.length == bufferLength) {
                    flushBuffer();
                    address += buffer.length;
                }
                buffer[bufferLength++] = c;
            }
        }
    }

    /**
     * Writes every Assembled into the buffer.<br>
     * The buffer wraps around instructions instead of bytes.
     *
     * @param assembled
     *      the used Assembled List.
     */
    public void writeAll(List<Assembled> assembled) throws IOException {
        for (Assembled a : assembled)
            write(a, true);
    }
    /**
     * Writes every Assembled into the buffer.
     *
     * @param assembled
     *      the used Assembled List.
     * @param instructionWrap
     *      whether the buffer wraps around instructions ({@link Assembled#getCodes()})
     *      or bytes.
     */
    public void writeAll(List<Assembled> assembled, boolean instructionWrap) throws IOException {
        for (Assembled a : assembled)
            write(a, instructionWrap);
    }

    /**
     * Writes the current buffer into the writer and resets the buffer.
     */
    private void flushBuffer() throws IOException {
        StringBuffer data = new StringBuffer();

        final int recordType = 0;
        long buffersum = 0;

        data.append(String.format("%02x", bufferLength));
        data.append(String.format("%04x", address));
        data.append(String.format("%02x", recordType));

        for (int i = 0; i < bufferLength; ++i) {
            data.append(String.format("%02x", 0xFF & (int) buffer[i]));
            buffersum+= 0xFF & (int) buffer[i];
        }

        out.write(":");

        // Calculate the checksum by adding all prior bytes, extracting the least significant byte
        // and calculating two's complement on it.
        final int checksum = 0xFF & (~(byte)(0xFF & (bufferLength + (address & 0xFF) + (0xFF & address >>> 8) +
                recordType + buffersum))+1);

        data.append(String.format("%02x", checksum));

        out.write(data.toString().toUpperCase()+"\n");

        buffer = new byte[buffer.length];
        bufferLength = 0;
    }


    @Override
    public void close() throws Exception {
        if (bufferLength != 0)
            flushBuffer();
        out.write(":00000001FF"); // Writes default EOF (Record Type 01)

        out.close();
    }
}
