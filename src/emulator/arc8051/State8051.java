package emulator.arc8051;

import emulator.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * This class represents the internal state of the 8051 microcontroller.
 * This includes all the registers and the internal memory.
 */
public class State8051 {

    /**
     * This class represents the SFR area of the 8051 microcontroller.
     * There are two references to each register as every register is an attribute of the class, but is also
     * contained in an internal hash map (the hash map is needed to quickly get the register corresponding to a
     * given memory address). This improved performance and readability as you can type {@code instance.A} instead
     * of {@code instance.get((byte)0xE0)}. It should not be a problem as the set of registers is not intended to
     * be changed during the execution of the program.
     */
    static class SpecialFunctionRegisters implements ROM {

        private enum PSWFlags {
            P, UD, OV, RS0, RS1, F0, AC, C;
        }
        //TODO add flag names for other flag registers

        private HashMap<Byte, ByteRegister> specialFunctionRegisters;

        final BitAddressableByteRegister B = new BitAddressableByteRegister("B");
        final BitAddressableByteRegister A = new BitAddressableByteRegister("A");
        final ByteFlagRegister PSW = new ByteFlagRegister("PSW") {
            @Override
            public Enum[] getFlags() {
                return SpecialFunctionRegisters.PSWFlags.values();
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
        final ByteRegister SP = new ByteRegister("SP", (byte)7);
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

        public int getMinAddress() {
            return 0x80; //the SFR area in the 8051 controller starts at 80h
        }

        public int getMaxAddress() {
            return 0xFF; //the SFR area in the 8051 controller goes up to FFh
        }

        @Override
        public int getSize() {
            return 128;
        }

        List<Register> getRegisters() {
            return new ArrayList<>(this.specialFunctionRegisters.values());
        }

        public boolean hasAddress(byte address) {
            return this.specialFunctionRegisters.containsKey(address);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (null == other) return false;
            if (!(other instanceof SpecialFunctionRegisters)) return false;
            SpecialFunctionRegisters tmp = (SpecialFunctionRegisters)other;
            for (byte b : this.specialFunctionRegisters.keySet()) {
                try {
                    if (tmp.get(b) == this.get(b)) return false;
                } catch (IndexOutOfBoundsException e) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Get the direct address of the specified register.
         * @param r
         *     The register whose address will be returned; must be one of the registers contained in this object
         * @return the direct address
         * @throws IllegalArgumentException when a register that is not contained in this object is given as a parameter
         */
        public byte getAddress(ByteRegister r) throws IllegalArgumentException {
            for (Byte b : this.specialFunctionRegisters.keySet()) {
                if (specialFunctionRegisters.get(b) == r) return b;
            }
            throw new IllegalArgumentException("Invalid byte register; cannot get address: "+r);
        }

        /**
         * Get register by address.
         * @param address the address; must be >= 0x80
         * @return the specified register or {@code null} it it is not present
         */
        ByteRegister getRegister(byte address) {
            if ((address & 0xFF) < 0x80) throw new IllegalArgumentException("Invalid address for SFR: "+address);
            return this.specialFunctionRegisters.get(address);
        }

        /**
         * Add a new SFR at the specified address.
         * @param address the address; must be >= 0x80
         * @param register the register to be added; must not be {@code null}
         */
        void addRegister(byte address, ByteRegister register) {
            if ((address & 0xFF) < 0x80) throw new IllegalArgumentException("Invalid address for SFR: "+address);
            this.specialFunctionRegisters.put(address, Objects.requireNonNull(register));
        }
    }

    SpecialFunctionRegisters sfrs;

    RAM internalRAM;

    RAM externalRAM;

    ROM codeMemory;

    ByteRegister PCH, PCL;

    ByteRegister R7, R6, R5, R4, R3, R2, R1, R0;

    /**
     * @param codeMemory
     *        The instructions will be read from this object. Must not be {@code null}.
     *        The size must be 65536 bytes.
     * @param externalRAM
     *        external RAM that can be accessed with the {@code MOVX} command; {@code null} is a valid value and implies,
     *        that there is no external RAM
     */
    public State8051(ROM codeMemory, RAM externalRAM) {
        this.codeMemory = Objects.requireNonNull(codeMemory, "trying to create MC8051 object without code memory");
        if (this.codeMemory.getSize() != 65536)
            throw new IllegalArgumentException("code memory has to be 2^16 bytes long");
        this.sfrs = new SpecialFunctionRegisters();
        this.internalRAM = new RAM(256);
        this.externalRAM = externalRAM;
        this.PCH = new ByteRegister("PCH");
        this.PCL = new ByteRegister("PCL");
    }

    @Override
    public boolean equals(Object other) {
        if (null == other) return false;
        if (this == other) return true;
        if (!(other instanceof State8051)) return false;
        State8051 tmp = (State8051) other;
        if (!this.codeMemory.equals(tmp.codeMemory)) return false;
        if (!this.internalRAM.equals(tmp.internalRAM)) return false;
        if (this.externalRAM != null) {
            if (!this.externalRAM.equals(tmp.externalRAM)) return false;
        }
        if (!this.PCL.equals(tmp.PCL)) return false;
        if (!this.PCH.equals(tmp.PCH)) return false;
        return true;
    }

    /**
     * Get a R register by its ordinal.
     * @param ordinal
     *     specifies the register (-> R&lt;ordinal&gt;); 0 <= ordinal <= 7
     * @return the specified register
     * @throws IllegalArgumentException when given an illegal ordinal
     */
    ByteRegister getR(int ordinal) throws IllegalArgumentException {
        switch (ordinal) {
            case 7: return this.R7;
            case 6: return this.R6;
            case 5: return this.R5;
            case 4: return this.R4;
            case 3: return this.R3;
            case 2: return this.R2;
            case 1: return this.R1;
            case 0: return this.R0;
            default: throw new IllegalArgumentException("Invalid R ordinal: "+ordinal);
        }
    }

    List<Register> getRegisters() {
        List<Register> ret = this.sfrs.getRegisters();
        //the PC and the R registers are not accessible through the special function register area in the 8051's memory,
        //thus they are represented as a member of the state class and need to be added manually to the list
        ret.add(PCH);
        ret.add(PCL);
        ret.add(R7);
        ret.add(R6);
        ret.add(R5);
        ret.add(R4);
        ret.add(R3);
        ret.add(R2);
        ret.add(R1);
        ret.add(R0);
        return ret;
    }

    void setRRegisters(ByteRegister[] rRegisters) {
        this.R7 = Objects.requireNonNull(rRegisters[7]);
        this.R6 = Objects.requireNonNull(rRegisters[6]);
        this.R5 = Objects.requireNonNull(rRegisters[5]);
        this.R4 = Objects.requireNonNull(rRegisters[4]);
        this.R3 = Objects.requireNonNull(rRegisters[3]);
        this.R2 = Objects.requireNonNull(rRegisters[2]);
        this.R1 = Objects.requireNonNull(rRegisters[1]);
        this.R0 = Objects.requireNonNull(rRegisters[0]);
    }
}
