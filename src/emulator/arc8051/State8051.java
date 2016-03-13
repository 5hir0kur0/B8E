package emulator.arc8051;

import emulator.*;
import misc.Settings;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.*;
import java.util.function.Predicate;

/**
 * This class represents the internal state of the 8051 micro controller.
 * This includes all the registers and the internal memory.
 */
@XmlRootElement(namespace = "https://github.com/5hir0kur0/B8E")
@XmlAccessorType(XmlAccessType.FIELD)
public class State8051 {

    byte TMOD_OLD; //used by updateTimers() to keep track of the previous state TMOD when in mode 3
    boolean TR1_OLD; //used by updateTimers() to keep track of the previous state of TR1 when in mode 3

    //variables used by the updateInterruptRequestFlags() method to keep track of transitions of the
    //external interrupts' pins
    boolean prevP3_3;
    boolean prevP3_2;

    //variable used by handleInterrupts() method to keep track of the running interrupt's priority
    //can either be 1 for high, 0 for low or -1 to indicate that no interrupt is being executed at the moment
    int runningInterruptPriority;

    //If this is true, the currently running interrupt is interrupting another interrupt of lower priority
    //the only case when this can happen is when an interrupt of 'low' priority is interrupted by an interrupt of 'high'
    //priority. Consequently this can only happen once, as the interrupt running when this flag is true is of 'high'
    //priority and thus cannot be interrupted.
    boolean runningInterruptInterruptedOtherInterrupt = false;

    //settings:
    final static String IGNORE_SO_SU = "emulator.ignore-stack-overflow-and-stack-underflow";
    final static String IGNORE_SO_SU_DEFAULT = "false";
    @XmlAttribute final boolean ignoreSOSU; //ignore stack overflow/underflow

    final static String IGNORE_UNDEFINED_MNEMONIC = "emulator.ignore-undefined-mnemonic";
    final static String IGNORE_UNDEFINED_MNEMONIC_DEFAULT = "false";
    @XmlAttribute final boolean ignoreUndefined;

    final static String IGNORE_ALL_EXCEPTIONS = "emulator.ignore-all-exceptions";
    final static String IGNORE_ALL_EXCEPTIONS_DEFAULT = "false";
    @XmlAttribute final boolean ignoreExceptions;

    final static String IGNORE_UNDEFINED_SFRS = "emulator.ignore-undefined-sfrs";
    final static String IGNORE_UNDEFINED_SFRS_DEFAULT = "false";
    @XmlAttribute final boolean ignoreUndefinedSfrs;

     //initialize default values for settings
    static {
        Settings.INSTANCE.setDefault(IGNORE_SO_SU, IGNORE_SO_SU_DEFAULT);
        Settings.INSTANCE.setDefault(IGNORE_UNDEFINED_MNEMONIC, IGNORE_UNDEFINED_MNEMONIC_DEFAULT);
        Settings.INSTANCE.setDefault(IGNORE_ALL_EXCEPTIONS, IGNORE_ALL_EXCEPTIONS_DEFAULT);
        Settings.INSTANCE.setDefault(IGNORE_UNDEFINED_SFRS, IGNORE_UNDEFINED_SFRS_DEFAULT);
    }

    /**
     * Get the current address of a R register.
     * @param ordinal
     *     the returned register will be R&lt;ordinal&gt;; 0 <= ordinal <= 7
     * @return
     *     the register's address
     * @throws IllegalArgumentException
     *     when given an invalid ordinal
     */
    int getRAddress(int ordinal) throws IllegalArgumentException {
        if (ordinal < 0 || ordinal > 7)
            throw new IllegalArgumentException("Invalid R register ordinal: "+ordinal);
        //PSW: C | AC | F0 | RS1 | RS0 | OV | UD | P
        //==> RS1 and RS0 control the register bank and are conveniently already located at the correct position
        return this.sfrs.PSW.getValue() & 0b00011000 | ordinal;
    }

    /**
     * This class represents the SFR area of the 8051 micro controller.
     * There are two references to each register as every register is an attribute of the class, but is also
     * contained in an internal hash map (the hash map is needed to quickly get the register corresponding to a
     * given memory address). This improved performance and readability as you can type {@code instance.A} instead
     * of {@code instance.get((byte)0xE0)}. It should not be a problem as the set of registers is not intended to
     * be changed during the execution of the program.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    static class SpecialFunctionRegisters implements ROM {

        private final boolean ignoreUndefinedSfrs;

        //TODO add flag names for other flag registers

        @XmlTransient private HashMap<Byte, ByteRegister> specialFunctionRegisters;

        final BitAddressableByteRegister B = new BitAddressableByteRegister("B");
        final BitAddressableByteRegister A = new BitAddressableByteRegister("A");
        final ByteFlagRegister PSW = new ByteFlagRegister("PSW",
                Flag.fromNames("P", "UD", "OV", "RS0", "RS1", "F0", "AC", "C")
        );
        final ByteFlagRegister IP = new ByteFlagRegister("IP",
                Flag.fromNames("EX0", "ET0", "EX1", "ET1", "ES", "UD0", "UD1", "EA")
        );
        // the initial value of all ports is specified to be 0xFF
        final BitAddressableByteRegister P3 = new BitAddressableByteRegister("P3", (byte)0xFF);
        final ByteFlagRegister IE = new ByteFlagRegister("IE",
                Flag.fromNames("EX0", "ET0", "EX1", "ET1", "ES", "UD0", "UD1", "EA")
        );
        // the initial value of all ports is specified to be 0xFF
        final BitAddressableByteRegister P2 = new BitAddressableByteRegister("P2", (byte)0xFF);
        final BitAddressableByteRegister SCON = new BitAddressableByteRegister("SCON");
        final ByteRegister SBUF = new ByteRegister("SBUF");
        // the initial value of all ports is specified to be 0xFF
        final BitAddressableByteRegister P1 = new BitAddressableByteRegister("P1", (byte)0xFF);
        final ByteFlagRegister TCON = new ByteFlagRegister("TCON",
                Flag.fromNames("IT0", "IE0", "IT1", "IE1", "TR0", "TF0", "TR1", "TF1")
        );
        final ByteRegister TMOD = new ByteRegister("TMOD");
        final ByteRegister TL0 = new ByteRegister("TL0");
        final ByteRegister TL1 = new ByteRegister("TL1");
        final ByteRegister TH0 = new ByteRegister("TH0");
        final ByteRegister TH1 = new ByteRegister("TH1");
        // the initial value of all ports is specified to be 0xFF
        final BitAddressableByteRegister P0 = new BitAddressableByteRegister("P0", (byte)0xFF);
        final ByteRegister SP = new ByteRegister("SP", (byte)7); // the stack pointer's initial value must be 7
        final ByteRegister DPL = new ByteRegister("DPL");
        final ByteRegister DPH = new ByteRegister("DPH");
        final ByteRegister PCON = new ByteRegister("PCON");

        @SuppressWarnings("unused")
        private SpecialFunctionRegisters() { // no-arg constructor for JAXB
            ignoreUndefinedSfrs = false;
        }

        private SpecialFunctionRegisters(boolean ignoreUndefinedSfrs) {
            updateSfrMap();
            this.ignoreUndefinedSfrs = ignoreUndefinedSfrs;
        }

        void updateSfrMap() {
            this.specialFunctionRegisters = new HashMap<>(21);
            this.specialFunctionRegisters.put((byte)0xF0, B);
            this.specialFunctionRegisters.put((byte)0xE0, A);
            this.specialFunctionRegisters.put((byte)0xD0, PSW);
            this.specialFunctionRegisters.put((byte)0xB8, IP);
            this.specialFunctionRegisters.put((byte)0xB0, P3);
            this.specialFunctionRegisters.put((byte)0xA8, IE);
            this.specialFunctionRegisters.put((byte)0xA0, P2);
            this.specialFunctionRegisters.put((byte)0x98, SCON);
            this.specialFunctionRegisters.put((byte)0x99, SBUF);
            this.specialFunctionRegisters.put((byte)0x90, P1);
            this.specialFunctionRegisters.put((byte)0x88, TCON);
            this.specialFunctionRegisters.put((byte)0x89, TMOD);
            this.specialFunctionRegisters.put((byte)0x8A, TL0);
            this.specialFunctionRegisters.put((byte)0x8B, TL1);
            this.specialFunctionRegisters.put((byte)0x8C, TH0);
            this.specialFunctionRegisters.put((byte)0x8D, TH1);
            this.specialFunctionRegisters.put((byte)0x80, P0);
            this.specialFunctionRegisters.put((byte)0x81, SP);
            this.specialFunctionRegisters.put((byte)0x82, DPL);
            this.specialFunctionRegisters.put((byte)0x83, DPH);
            this.specialFunctionRegisters.put((byte)0x87, PCON);
        }

        @Override
        public byte get(int index) {
            if (index < 0x80 || index > 255)
                throw new IndexOutOfBoundsException("SFR index too big or too small: "+index);
            if (specialFunctionRegisters.containsKey((byte)index))
                return specialFunctionRegisters.get((byte)index).getValue();
            else if (!this.ignoreUndefinedSfrs) throw new IndexOutOfBoundsException("SFR index out of range: "+index);
            else return 0;
        }

        @Override
        @Deprecated
        public byte[] get(int index, int length) {
            if (index < 0x80 || index > 255)
                throw new IndexOutOfBoundsException("SFR index too big or too small: "+index);
            if (length < 1) throw new IllegalArgumentException("length must be bigger than or equal to 1");
            byte[] ret = new byte[length];
            for (int i = 0; i < ret.length; ++i) {
                int tmpIndex = i + index;
                if (tmpIndex > 255) ret[i] = 0;
                else if (specialFunctionRegisters.containsKey((byte)i))
                    ret[i] = specialFunctionRegisters.get((byte)i).getValue();
                else ret[i] = 0;
            }
            return ret;
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
        public Iterator<Byte> iterator() {
            return new Iterator<Byte>() {
                int index = 0;
                @Override
                public boolean hasNext() {
                    return index <= 0xFF;
                }

                @Override
                public Byte next() {
                    return SpecialFunctionRegisters.this.specialFunctionRegisters.containsKey((byte)index)
                            ? SpecialFunctionRegisters.this.specialFunctionRegisters.get((byte)index).getValue()
                            : null;
                }
            };
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (null == other) return false;
            if (!(other instanceof SpecialFunctionRegisters)) return false;
            SpecialFunctionRegisters tmp = (SpecialFunctionRegisters)other;
            for (byte b : this.specialFunctionRegisters.keySet()) {
                try {
                    if (tmp.getRegister(b) == null || !tmp.getRegister(b).equals(this.getRegister(b))) return false;
                } catch (IndexOutOfBoundsException e) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Get the direct address of the specified register.
         * @param r
         *     the register whose address will be returned; must be one of the registers contained in this object
         * @return
         *     the direct address
         * @throws IllegalArgumentException
         *     when a register that is not contained in this object is given as a parameter
         */
        public byte getAddress(ByteRegister r) throws IllegalArgumentException {
            for (Byte b : this.specialFunctionRegisters.keySet()) {
                if (specialFunctionRegisters.get(b) == r) return b;
            }
            throw new IllegalArgumentException("Invalid byte register; cannot get address: "+r);
        }

        /**
         * Get register by address.
         * @param address
         *     the {@code ByteRegister}'s address; must be >= 0x80
         * @return
         *     the specified register or {@code null} it it is not present
         */
        ByteRegister getRegister(byte address) {
            if ((address & 0xFF) < 0x80) throw new IllegalArgumentException("Invalid address for SFR: "+address);
            if (this.specialFunctionRegisters.containsKey(address)) return this.specialFunctionRegisters.get(address);
            //If the program attempts to use a value in the SFR area which does not hold a register,
            //create a new register and throw an exception (because the program would exhibit undefined behaviour
            //on real hardware
            ByteRegister tmp = new ByteRegister(String.format("TMP_SFR#%02X", address & 0xFF));
            this.addRegister(address, tmp);
            if (!this.ignoreUndefinedSfrs)
                throw new IndexOutOfBoundsException("Illegal SFR address: " + (address & 0xFF));
            else return tmp;
        }

        /**
         * Add a new SFR at the specified address.
         * @param address
         *     the address; must be >= 0x80
         * @param register
         *     the register to be added; must not be {@code null}
         */
        void addRegister(byte address, ByteRegister register) {
            if ((address & 0xFF) < 0x80) throw new IllegalArgumentException("Invalid address for SFR: "+address);
            this.specialFunctionRegisters.put(address, Objects.requireNonNull(register));
        }
    }

    SpecialFunctionRegisters sfrs;

    final RAM internalRAM;

    final RAM externalRAM;

    @XmlJavaTypeAdapter(RAM.RomAdapter.class)
    final ROM codeMemory;

    ByteRegister PCH, PCL;

    @XmlTransient RRegister R7, R6, R5, R4, R3, R2, R1, R0;

    @SuppressWarnings("unused")
    private State8051() { // no-arg constructor for JAXB
        this.ignoreSOSU = false;
        this.ignoreExceptions = false;
        this.ignoreUndefined = false;
        this.ignoreUndefinedSfrs = false;
        // prevent NullPointerException during deserialization caused by the implementation in RRegister
        // which extends ByteRegister whose constructor calls setValue() which results in an attempt to modify the
        // internal RAM that would not be initialized until later if the following statements weren't there
        this.internalRAM = new RAM(32);
        this.sfrs = new SpecialFunctionRegisters(false);
        this.externalRAM = null;
        this.codeMemory = null;
        this.setRRegisters(this.generateRRegisters());
    }

    /**
     * @param codeMemory
     *     The instructions will be read from this object. (must not be {@code null})
     *     The size must be 65536 bytes.
     * @param externalRAM
     *     external {@code RAM} that can be accessed with the {@code MOVX} command;
     *     {@code null} is a valid value and implies that there is no external RAM
     */
    public State8051(ROM codeMemory, RAM externalRAM) {
        this.codeMemory = Objects.requireNonNull(codeMemory, "trying to create MC8051 object without code memory");
        if (this.codeMemory.getSize() != 65536)
            throw new IllegalArgumentException("code memory has to be 2^16 bytes long");

        //settings
        final Predicate<String> isValidBoolean = s -> "true".equals(s) || "false".equals(s);
        this.ignoreExceptions = Boolean.parseBoolean(Settings.INSTANCE.getProperty(
                IGNORE_ALL_EXCEPTIONS, IGNORE_ALL_EXCEPTIONS_DEFAULT, isValidBoolean));
        this.ignoreSOSU = Boolean.parseBoolean(Settings.INSTANCE.getProperty(
                IGNORE_SO_SU, IGNORE_SO_SU_DEFAULT, isValidBoolean)) || this.ignoreExceptions;
        this.ignoreUndefined = Boolean.parseBoolean(Settings.INSTANCE.getProperty(
                IGNORE_UNDEFINED_MNEMONIC, IGNORE_UNDEFINED_MNEMONIC_DEFAULT, isValidBoolean)) || this.ignoreExceptions;
        this.ignoreUndefinedSfrs = Boolean.parseBoolean(Settings.INSTANCE.getProperty(
                IGNORE_UNDEFINED_SFRS, IGNORE_UNDEFINED_SFRS_DEFAULT, isValidBoolean
        )) || this.ignoreExceptions;

        this.sfrs = new SpecialFunctionRegisters(this.ignoreUndefinedSfrs);
        this.internalRAM = new RAM(256);
        this.externalRAM = externalRAM;
        this.PCH = new ByteRegister("PCH");
        this.PCL = new ByteRegister("PCL");
        this.setRRegisters(this.generateRRegisters());
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
        if (!this.PCL.equals(tmp.PCL) || !this.PCH.equals(tmp.PCH)) return false;
        if (!(this.TMOD_OLD == tmp.TMOD_OLD && this.TR1_OLD == tmp.TR1_OLD && this.prevP3_2 == tmp.prevP3_2 &&
                this.prevP3_3 == tmp.prevP3_3)) return false;
        return this.sfrs.equals(tmp.sfrs);
    }

    /**
     * Get a R register by its ordinal.
     * @param ordinal
     *     specifies the register (-> R&lt;ordinal&gt;); 0 <= ordinal <= 7
     * @return
     *     the specified register
     * @throws IllegalArgumentException
     *     when given an illegal ordinal
     */
    RRegister getR(int ordinal) throws IllegalArgumentException {
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

    private void setRRegisters(RRegister[] rRegisters) {
        this.R7 = Objects.requireNonNull(rRegisters[7]);
        this.R6 = Objects.requireNonNull(rRegisters[6]);
        this.R5 = Objects.requireNonNull(rRegisters[5]);
        this.R4 = Objects.requireNonNull(rRegisters[4]);
        this.R3 = Objects.requireNonNull(rRegisters[3]);
        this.R2 = Objects.requireNonNull(rRegisters[2]);
        this.R1 = Objects.requireNonNull(rRegisters[1]);
        this.R0 = Objects.requireNonNull(rRegisters[0]);
    }

    class RRegister extends ByteRegister {
        final int ordinal;

        RRegister(int ordinal) {
            super("R"+ordinal, (byte)0);
            this.ordinal = ordinal;
        }

        void firePropertyChangeIfUpdated() {
            // super.getValue() reads from the attribute and this.getValue() reads from RAM
            // if they are not equal, the attribute in the super class needs to be updated in order for it to fire
            // a property change event to the GUI
            if (this.getValue() != super.getValue()) this.setValue(this.getValue());
        }

        @Override
        public void setValue(byte newValue) {
            State8051.this.internalRAM.set(State8051.this.getRAddress(this.ordinal), newValue);
            super.setValue(newValue); //super.setValue() is being called here to fire a property change
        }

        @Override
        public byte getValue() {
            //the byte from the internal RAM needs to be returned here as the R registers may also be modified
            //through it
            return State8051.this.internalRAM.get(State8051.this.getRAddress(this.ordinal));
        }
    }

    private RRegister[] generateRRegisters() {
        RRegister[] rRegisters = new RRegister[8];
        for (int i = 0; i < rRegisters.length; ++i) rRegisters[i] = new RRegister(i);
        return rRegisters;
    }
}
