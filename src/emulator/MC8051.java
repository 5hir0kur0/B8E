package emulator;

import java.util.HashMap;

/**
 * This class represents the 8051 microcontroller.
 *
 * @author 5hir0kur0
 */
public class MC8051 implements Emulator {
    /**
     * This class represents the internal state of the 8051 microcontroller.
     * This includes all the registers and the internal memory.
     */
    public static class State8051 {

        /**
         * This class represents the SFR area of the 8051 microcontroller.
         * There are two references to each register as every register is an attribute of the class, but is also
         * contained in an internal hash map (the hash map is needed to quickly get the register corresponding to a
         * given memory address). This improved performance and readability as you can type {@code instance.A} instead
         * of {@code instance.get((byte)0xE0)}. It should not be a problem as the set of registers is not intended to
         * be changed during the execution of the program.
         */
        private static class SpecialFunctionRegisters implements ROM {

            private enum PSWFlags {
                P, UD, OV, RS0, RS1, F0, AC, C;
            }

            private HashMap<Byte, ByteRegister> sfrs;

            final BitAddressableByteRegister B = new BitAddressableByteRegister("B");
            final BitAddressableByteRegister A = new BitAddressableByteRegister("A");
            final ByteFlagRegister PSW = new ByteFlagRegister("PSW") {
                @Override
                public Enum[] getFlags() {
                    return PSWFlags.values();
                }
            };
            final BitAddressableByteRegister IP = new BitAddressableByteRegister("IP");
            final BitAddressableByteRegister P3 = new BitAddressableByteRegister("P3");
            final BitAddressableByteRegister IE = new BitAddressableByteRegister("IE");
            final BitAddressableByteRegister P2 = new BitAddressableByteRegister("P2");
            final BitAddressableByteRegister SCON = new BitAddressableByteRegister("SCON");
            final ByteRegister SBUF = new ByteRegister("SBUF");
            final BitAddressableByteRegister P1 = new BitAddressableByteRegister("P1");
            final BitAddressableByteRegister TCON = new BitAddressableByteRegister("TCON")
            final ByteRegister TMOD = new ByteRegister("TMOD");
            final ByteRegister TL0 = new ByteRegister("TL0");
            final ByteRegister TL1 = new ByteRegister("TL1");
            final ByteRegister TH0 = new ByteRegister("TH0");
            final ByteRegister TH1 = new ByteRegister("TH1");
            final BitAddressableByteRegister P0 = new BitAddressableByteRegister("P0");
            final ByteRegister SP = new ByteRegister("SP");
            final ByteRegister DPL = new ByteRegister("DPL");
            final ByteRegister DPH = new ByteRegister("DPH");
            final ByteRegister PCON = new ByteRegister("PCON");

            private SpecialFunctionRegisters() {
                this.sfrs = new HashMap<>(21);
                sfrs.put((byte)0xF0, B);
                sfrs.put((byte)0xE0, A);
                sfrs.put((byte)0xD0, PSW);
                sfrs.put((byte)0xB8, IP);
                sfrs.put((byte)0xB0, P3);
                sfrs.put((byte)0xA8, IE);
                sfrs.put((byte)0xA0, P2);
                sfrs.put((byte)0x98, SCON);
                sfrs.put((byte)0x99, SBUF);
                sfrs.put((byte)0x90, P1);
                sfrs.put((byte)0x88, TCON);
                sfrs.put((byte)0x89, TMOD);
                sfrs.put((byte)0x8A, TL0);
                sfrs.put((byte)0x8B, TL1);
                sfrs.put((byte)0x8C, TH0);
                sfrs.put((byte)0x8D, TH1);
                sfrs.put((byte)0x80, P0);
                sfrs.put((byte)0x81, SP);
                sfrs.put((byte)0x82, DPL);
                sfrs.put((byte)0x83, DPH);
                sfrs.put((byte)0x87, PCON);
            }

            @Override
            public byte get(int index) {
                if (index < 0 || index > 255) throw new IndexOutOfBoundsException("SFR index too big or too small.");
                if (sfrs.containsKey(index)) return sfrs.get(index).getValue();
                else throw new IndexOutOfBoundsException("SFR index out of range");
            }

            @Override
            public Byte[] get(int index, int length) {
                if (index < 0 || index > 255) throw new IndexOutOfBoundsException("SFR index too big or too small.");
                if (length < 1) throw new IllegalArgumentException("length must be bigger than or equal to 1");
                Byte[] ret = new Byte[length];
                for (int i = 0; i < ret.length; ++i) {
                    if (sfrs.containsKey(i)) ret[i] = sfrs.get(i).getValue();
                    else ret[i] = null;
                }
                return ret;
            }

            @Override
            public int getMinAddress() {
                return 0x80; //the SFR area in the 8051 controller starts at 80h
            }

            @Override
            public int getMaxAddress() {
                return 0xFF; //the SFR area in the 8051 controller goes up to FFh
            }

            @Override
            public int getSize() {
                return 128;
            }

            @Override
            public void setMinAddress(int address) {
                throw new UnsupportedOperationException("attempting to set minimal address of SFR area");
            }

            @Override
            public void setMaxAddress(int address) {
                throw new UnsupportedOperationException("attempting to set maximal address of SFR area");
            }
        }

        private SpecialFunctionRegisters specialFunctionRegisters;

        public State8051() {
            this.specialFunctionRegisters = new SpecialFunctionRegisters();
       }
    }
}
