package assembler.util;

import assembler.tokens.LabelToken;
import assembler.tokens.OperandToken;
import assembler.tokens.Token;
import assembler.util.assembling.Assembled;
import emulator.NumeralSystem;
import misc.Settings;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.*;

/**
 * @author Noxgrim
 */
public class Listing {

    private List<ListingElement> elements;

    public Listing(List<? extends Assembled> assembled, int addressLength) {
        this.elements = new ArrayList<>();

        Settings s = Settings.INSTANCE;
        NumeralSystem addressNS = NumeralSystem.valueOf(s.getProperty(AssemblerSettings.OUTPUT_LST_ADDR_NR_SYSTEM,
                AssemblerSettings.VALID_NUMERICAL_SYSTEM));
        NumeralSystem codeNS = NumeralSystem.valueOf(s.getProperty(AssemblerSettings.OUTPUT_LST_CODES_NR_SYSTEM,
                AssemblerSettings.VALID_NUMERICAL_SYSTEM));
        for (Assembled a : assembled)
            elements.add(new ListingElement(a, addressLength, addressNS, codeNS));
    }

    public List<ListingElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public ListingElement getFromAddress(long address) {
        for (ListingElement e : elements)
            if (e.isInBounds(address))
                return e;
        return null;
    }

    public void writeAll(Writer w) throws IOException {
        int maxLabelLength = 0, maxCodesLength = 0, maxLineLNLength = 0, addressLength = 0;

        for (ListingElement le : elements) {
            addressLength = le.getAddress().length();
            int lineNumberLength = Integer.toString(le.getLine()).length();
            if (maxLineLNLength < lineNumberLength)
                maxLineLNLength = lineNumberLength;
            int labelLength = le.getLabels().length();
            if (maxLabelLength < labelLength)
                maxLabelLength = labelLength;
            int codesLength = le.getCodes().length();
            if (maxCodesLength < codesLength)
                maxCodesLength = codesLength;
        }

        String file = "";

        w.write("LISTING\n" +
                "=======\n");

        if (Settings.INSTANCE.getBoolProperty(AssemblerSettings.OUTPUT_LST_LABELS_LB)) {
            String normalFormat = "%" + maxLineLNLength + "d: %s  %-" + maxCodesLength + "s      %s%n";
            String labelFormat  = "%" + maxLineLNLength + "d: %s  %-" + maxCodesLength + "s    %s%n" +
                    String.format("%" + maxLineLNLength + "s%" + (addressLength+maxCodesLength+6+4) +"s", "*", "") +
                    "%s%n";

            for (ListingElement le : elements) {
                if (!le.getPath().equals(file)) {
                    w.write("\nFile: " + (file = le.getPath()) + '\n');
                    char[] line = new char[6 + file.length()];
                    Arrays.fill(line, '-');
                    w.write(line);
                    w.write('\n');
                }

                if (le.getLabels().isEmpty())
                    w.write(String.format(normalFormat,
                            le.getLine(), le.getAddress(), le.getCodes(), le.getLineString()));
                else
                w.write(String.format(labelFormat,
                        le.getLine(), le.getAddress(), le.getCodes(), le.getLabels(), le.getLineString()));
            }
        } else {
            final String format = "%" + maxLineLNLength + "d: %s  %-" + maxCodesLength + "s    %-" + maxLabelLength +
                    "s %s%n";
            for (ListingElement le : elements) {
                if (!le.getPath().equals(file)) {
                    w.write("\nFile: " + (file = le.getPath()) + '\n');
                    char[] line = new char[6 + file.length()];
                    Arrays.fill(line, '-');
                    w.write(line);
                    w.write('\n');
                }

                w.write(String.format(format,
                        le.getLine(), le.getAddress(), le.getCodes(), le.getLabels(), le.getLineString()));
            }
        }
    }

    public static class ListingElement implements Comparable<ListingElement> {
        private final String codes;
        private final long address;
        private final String addressStr;

        private final int line;
        private final String labels;
        private final String lineString;
        private final String path;

        private final int length;

        public ListingElement(Assembled a, int addressLength, NumeralSystem addressNS, NumeralSystem codeNS) {
            Token[] tokens = a.getTokens();
            byte[] codes = a.getCodes();

            this.address = a.getAddress();

            switch (addressNS) {
                case BINARY:
                    addressStr = addressNS.toString(this.address, addressLength);
                    break;
                case OCTAL:
                    addressStr = addressNS.toString(this.address, (int) Math.ceil(addressLength/3F));
                    break;
                case DECIMAL: {
                    int length = 0;
                    for (int i = (int) Math.pow(2, addressLength); i > 0; ++length, i /= 10);
                    addressStr = addressNS.toString(this.address, length);
                    break;
                }
                case HEXADECIMAL:
                default:
                    addressStr = addressNS.toString(this.address, (int) Math.ceil(addressLength/4F));
                    break;
            }
            this.length = codes.length;
            this.path = a.getFile().toAbsolutePath().toString();

            this.line = tokens[0].getLine();

            StringBuilder sb = new StringBuilder();

            // Tokens
            for (Token t : tokens) {
                if (t.getType() == Token.TokenType.OPERAND)
                    sb.append(((OperandToken) t).getFullValue()).append(", ");
                else
                    sb.append(t.getValue()).append(" ");
            }
            if (tokens[tokens.length-1].getType() == Token.TokenType.OPERAND)
                sb.setLength(sb.length()-2);
            else
                sb.setLength(sb.length()-1);

            this.lineString =  sb.toString();

            sb.setLength(0);
            // Codes
            int length = 0;
            switch (codeNS) {
                case BINARY:
                    length = 8;
                    break;
                case OCTAL:
                case DECIMAL:
                    length = 3;
                    break;
                case HEXADECIMAL:
                    length = 2;
                    break;
            }
            for (byte b : codes)
                sb.append(codeNS.toString(b & 0xFF, length)).append(" ");

            if (codes.length > 0)
                sb.setLength(sb.length()-1);

            this.codes =  sb.toString();

            sb.setLength(0);
            // Labels
            for (LabelToken lt : a.getLabels())
                sb.append(lt.getValue()).append(": ");

            if (a.getLabels().length > 0)
                sb.setLength(sb.length()-1);

            this.labels = sb.toString();
        }

        public String getCodes() {
            return codes;
        }

        public String getAddress() {
            return addressStr;
        }

        public String getLabels() {
            return labels;
        }

        public int getLine() {
            return line;
        }

        public String getLineString() {
            return lineString;
        }

        public String getPath() {
            return path;
        }

        public int getLength() {
            return length;
        }

        public boolean isInBounds(long address) {
            return this.address <= address && this.address + length > address;
        }

        @Override
        public int compareTo(ListingElement o) {
            if (o.address != address)
                return (int) (address - o.address);
            else if (o.line != line)
                return line - o.line;
            else
                return lineString.compareTo(o.lineString);
        }

        @Override
        public String toString() {
            return Long.toHexString(address) + "  " + codes + "    " + line + " " + lineString;
        }
    }

}
