package assembler.util;

import assembler.tokens.OperandToken;
import assembler.tokens.Token;
import assembler.util.assembling.Assembled;

import java.util.*;

/**
 * @author Jannik
 */
public class Listing {

    private List<ListingElement> elements;

    public Listing(List<? extends Assembled> assembled) {
        this.elements = new ArrayList<>();
        for (Assembled a : assembled)
            elements.add(new ListingElement(a));
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

    public static class ListingElement implements Comparable<ListingElement> {
        private final String codes;
        private final long address;

        private final  int line;
        private final String lineString;
        private final String path;

        private final int length;

        public ListingElement(Assembled a) {
            Token[] tokens = a.getTokens();
            byte[] codes = a.getCodes();

            this.address = a.getAddress();
            this.length = codes.length;
            this.path = a.getFile().toAbsolutePath().toString();

            this.line = tokens[0].getLine();

            StringBuilder sb = new StringBuilder();

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

            for (byte b : codes) {
                sb.append(String.format("%02x ", b & 0xFF).toUpperCase());
            }
            sb.setLength(sb.length()-1);

            this.codes =  sb.toString();
        }

        public String getCodes() {
            return codes;
        }

        public long getAddress() {
            return address;
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
