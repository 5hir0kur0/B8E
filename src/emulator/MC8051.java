package emulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * This class represents the 8051 microcontroller.
 *
 * @author Gordian
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

            private HashMap<Byte, ByteRegister> specialFunctionRegisters;

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
            final BitAddressableByteRegister TCON = new BitAddressableByteRegister("TCON");
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
                this.specialFunctionRegisters = new HashMap<>(21);
                specialFunctionRegisters.put((byte)0xF0, B);
                specialFunctionRegisters.put((byte)0xE0, A);
                specialFunctionRegisters.put((byte)0xD0, PSW);
                specialFunctionRegisters.put((byte)0xB8, IP);
                specialFunctionRegisters.put((byte)0xB0, P3);
                specialFunctionRegisters.put((byte)0xA8, IE);
                specialFunctionRegisters.put((byte)0xA0, P2);
                specialFunctionRegisters.put((byte)0x98, SCON);
                specialFunctionRegisters.put((byte)0x99, SBUF);
                specialFunctionRegisters.put((byte)0x90, P1);
                specialFunctionRegisters.put((byte)0x88, TCON);
                specialFunctionRegisters.put((byte)0x89, TMOD);
                specialFunctionRegisters.put((byte)0x8A, TL0);
                specialFunctionRegisters.put((byte)0x8B, TL1);
                specialFunctionRegisters.put((byte)0x8C, TH0);
                specialFunctionRegisters.put((byte)0x8D, TH1);
                specialFunctionRegisters.put((byte)0x80, P0);
                specialFunctionRegisters.put((byte)0x81, SP);
                specialFunctionRegisters.put((byte)0x82, DPL);
                specialFunctionRegisters.put((byte)0x83, DPH);
                specialFunctionRegisters.put((byte)0x87, PCON);
            }

            @Override
            public byte get(int index) {
                if (index < 0 || index > 255) throw new IndexOutOfBoundsException("SFR index too big or too small.");
                if (specialFunctionRegisters.containsKey((byte)index))
                    return specialFunctionRegisters.get((byte)index).getValue();
                else throw new IndexOutOfBoundsException("SFR index out of range");
            }

            @Override
            public Byte[] get(int index, int length) {
                if (index < 0 || index > 255) throw new IndexOutOfBoundsException("SFR index too big or too small.");
                if (length < 1) throw new IllegalArgumentException("length must be bigger than or equal to 1");
                Byte[] ret = new Byte[length];
                for (int i = 0; i < ret.length; ++i) {
                    int tmpIndex = i + index;
                    if (tmpIndex > 255) ret[i] = null;
                    else if (specialFunctionRegisters.containsKey((byte)i))
                        ret[i] = specialFunctionRegisters.get((byte)i).getValue();
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

            List<Register> getRegisters() {
                return new ArrayList<>(this.specialFunctionRegisters.values());
            }

            boolean hasAddress(byte address) {
                return this.specialFunctionRegisters.containsKey(address);
            }
        }

        private SpecialFunctionRegisters sfrs;

        private RAM internalRAM;

        private RAM externalRAM;

        private ROM codeMemory;

        /**
         * @param externalRAM
         *        external RAM that can be accessed with the {@code MOVX} command; {@code null} is a valid value and implies,
         *        that there is no external RAM
         */
        public State8051(RAM externalRAM, ROM codeMemory) {
            this.codeMemory = Objects.requireNonNull(codeMemory, "trying to create MC8051 object without code memory");
            this.sfrs = new SpecialFunctionRegisters();
            this.internalRAM = new RAM(256);
            this.externalRAM = externalRAM;
        }

    }

    private State8051 state;

    /**
     * Create a new 8051 microcontroller object.
     * @param externalRAM
     *        The external RAM accessible through the {@code MOVX} command. {@code null} is a valid value and implies,
     *        that there is no external RAM (and thus, all {@code MOVX}-instructions should fail).
     * @param codeMemory
     *        The 8051's "code memory".
     */
    public MC8051(RAM externalRAM, ROM codeMemory) {
        this.state = new State8051(externalRAM, codeMemory);
    }

    /**
     * Start with a specific state.
     * @param state
     *        The state. Must not be {@code null}.
     */
    public MC8051(State8051 state) {
        this.state = Objects.requireNonNull(state, "trying to initialize MC8051 with empty state");
    }

    /**
     * Return a list of all the {@code Register}s.
     *
     * This method is not intended to be used frequently. To observe the {@code Register} you should register a change
     * listener.
     * @return a list of all the {@code Register}s
     */
    @Override
    public List<Register> getRegisters() {
        return this.state.sfrs.getRegisters();
    }

    @Override
    public FlagRegister getPSW() {
        return this.state.sfrs.PSW;
    }

    @Override
    public int next() {
        return 0;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public RAM getMainMemory() {
        return this.state.internalRAM;
    }

    @Override
    public RAM getSecondaryMemory() throws UnsupportedOperationException {
        if (null == this.state.externalRAM) throw new UnsupportedOperationException("no external RAM");
        return this.state.externalRAM;
    }

    @Override
    public boolean hasSecondaryMemory() {
        return this.state.externalRAM != null;
    }
}
