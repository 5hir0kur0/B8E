package emulator.arc8051;

import com.sun.javaws.exceptions.InvalidArgumentException;
import emulator.*;

import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.Objects;

/**
 * This class represents the 8051 microcontroller.
 * <br>
 * NOTE: <br>
 *   The reason for all the bitwise ANDs is that Java uses signed arithmetic and thus exhibits weird behaviour
 *   when casting a negative type to a type with a greater bit count (this casting occurs automatically during almost
 *   all arithmetic operations, like e.g. bit shifting):
 *   (int)(byte)0x80 = 0xFFFFFF80
 *   In order to get the correct result, we have to do <result> & 0xFF (in this case)
 *
 * @author 5hir0kur0
 */
public class MC8051 implements Emulator {

    private static class BitAddress {
        public final byte DIRECT_ADDRESS;
        public final byte BIT_MASK;
        public BitAddress(byte directAddress, byte bitMask) {
            this.BIT_MASK = bitMask;
            this.DIRECT_ADDRESS = directAddress;
        }
    }

    State8051 state;

    /**
     * Create a new 8051 microcontroller object.<br>
     * @param externalRAM
     *        The external RAM accessible through the {@code MOVX} command. {@code null} is a valid value and implies,
     *        that there is no external RAM (and thus, all {@code MOVX}-instructions should fail).
     * @param codeMemory
     *        The 8051's "code memory". The instructions will be read from this object. Must not be {@code null}.
     *        The size must be 65536 bytes.
     */
    public MC8051(ROM codeMemory, RAM externalRAM) {
        this(new State8051(codeMemory, externalRAM));
    }

    /**
     * Start with a specific state.
     * @param state
     *        The state. Must not be {@code null}.
     */
    public MC8051(State8051 state) {
        this.state = Objects.requireNonNull(state, "trying to initialize MC8051 with empty state");
        this.state.setRRegisters(generateRRegisters());
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
        return this.state.getRegisters();
    }

    @Override
    public FlagRegister getPSW() {
        return this.state.sfrs.PSW;
    }

    @Override
    public boolean hasCodeMemory() { return true; }

    @Override
    public ROM getCodeMemory() {
        return this.state.codeMemory;
    }

    /**
     * Execute the next instruction.<br>
     * @return the number of cycles this instruction takes.
     */
    @Override
    public int next() {
        byte currentInstruction = getCodeByte();
        int retValue = -1;
        switch (currentInstruction) {
            case       0x00: retValue = nop(); break;
            case       0x01: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case       0x02: retValue = ljmp(getCodeByte(), getCodeByte()); break;
            case       0x03: retValue = rr_a(); break;
            case       0x04: retValue = inc(this.state.sfrs.A); break;
            case       0x05: retValue = inc(getCodeByte()); break;
            case       0x06: retValue = inc(this.state.R0.getValue()); break;
            case       0x07: retValue = inc(this.state.R1.getValue()); break;
            case       0x08: retValue = inc(this.state.R0); break;
            case       0x09: retValue = inc(this.state.R1); break;
            case       0x0A: retValue = inc(this.state.R2); break;
            case       0x0B: retValue = inc(this.state.R3); break;
            case       0x0C: retValue = inc(this.state.R4); break;
            case       0x0D: retValue = inc(this.state.R5); break;
            case       0x0E: retValue = inc(this.state.R6); break;
            case       0x0F: retValue = inc(this.state.R7); break;
            case       0x10: retValue = jbc(getCodeByte(), getCodeByte()); break;
            case       0x11: retValue = acall(currentInstruction, getCodeByte()); break;
            case       0x12: retValue = lcall(getCodeByte(), getCodeByte()); break;
            case       0x13: retValue = rrc_a(); break;
            case       0x14: break;
            case       0x15: break;
            case       0x16: break;
            case       0x17: break;
            case       0x18: break;
            case       0x19: break;
            case       0x1A: break;
            case       0x1B: break;
            case       0x1C: break;
            case       0x1D: break;
            case       0x1E: break;
            case       0x1F: break;
            case       0x20: retValue = jb(getCodeByte(), getCodeByte()); break;
            case       0x21: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case       0x22: break;
            case       0x23: break;
            case       0x24: break;
            case       0x25: break;
            case       0x26: break;
            case       0x27: break;
            case       0x28: break;
            case       0x29: break;
            case       0x2A: break;
            case       0x2B: break;
            case       0x2C: break;
            case       0x2D: break;
            case       0x2E: break;
            case       0x2F: break;
            case       0x30: retValue = jnb(getCodeByte(), getCodeByte()); break;
            case       0x31: retValue = acall(currentInstruction, getCodeByte()); break;
            case       0x32: break;
            case       0x33: break;
            case       0x34: break;
            case       0x35: break;
            case       0x36: break;
            case       0x37: break;
            case       0x38: break;
            case       0x39: break;
            case       0x3A: break;
            case       0x3B: break;
            case       0x3C: break;
            case       0x3D: break;
            case       0x3E: break;
            case       0x3F: break;
            case       0x40: retValue = jc(getCodeByte()); break;
            case       0x41: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case       0x42: break;
            case       0x43: break;
            case       0x44: break;
            case       0x45: break;
            case       0x46: break;
            case       0x47: break;
            case       0x48: break;
            case       0x49: break;
            case       0x4A: break;
            case       0x4B: break;
            case       0x4C: break;
            case       0x4D: break;
            case       0x4E: break;
            case       0x4F: break;
            case       0x50: retValue = jnc(getCodeByte()); break;
            case       0x51: retValue = acall(currentInstruction, getCodeByte()); break;
            case       0x52: break;
            case       0x53: break;
            case       0x54: break;
            case       0x55: break;
            case       0x56: break;
            case       0x57: break;
            case       0x58: break;
            case       0x59: break;
            case       0x5A: break;
            case       0x5B: break;
            case       0x5C: break;
            case       0x5D: break;
            case       0x5E: break;
            case       0x5F: break;
            case       0x60: break;
            case       0x61: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case       0x62: break;
            case       0x63: break;
            case       0x64: break;
            case       0x65: break;
            case       0x66: break;
            case       0x67: break;
            case       0x68: break;
            case       0x69: break;
            case       0x6A: break;
            case       0x6B: break;
            case       0x6C: break;
            case       0x6D: break;
            case       0x6E: break;
            case       0x6F: break;
            case       0x70: break;
            case       0x71: retValue = acall(currentInstruction, getCodeByte()); break;
            case       0x72: break;
            case       0x73: break;
            case       0x74: break;
            case       0x75: break;
            case       0x76: break;
            case       0x77: break;
            case       0x78: break;
            case       0x79: break;
            case       0x7A: break;
            case       0x7B: break;
            case       0x7C: break;
            case       0x7D: break;
            case       0x7E: break;
            case       0x7F: break;
            case (byte)0x80: break;
            case (byte)0x81: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case (byte)0x82: break;
            case (byte)0x83: break;
            case (byte)0x84: break;
            case (byte)0x85: break;
            case (byte)0x86: break;
            case (byte)0x87: break;
            case (byte)0x88: break;
            case (byte)0x89: break;
            case (byte)0x8A: break;
            case (byte)0x8B: break;
            case (byte)0x8C: break;
            case (byte)0x8D: break;
            case (byte)0x8E: break;
            case (byte)0x8F: break;
            case (byte)0x90: break;
            case (byte)0x91: retValue = acall(currentInstruction, getCodeByte());
            case (byte)0x92: break;
            case (byte)0x93: break;
            case (byte)0x94: break;
            case (byte)0x95: break;
            case (byte)0x96: break;
            case (byte)0x97: break;
            case (byte)0x98: break;
            case (byte)0x99: break;
            case (byte)0x9A: break;
            case (byte)0x9B: break;
            case (byte)0x9C: break;
            case (byte)0x9D: break;
            case (byte)0x9E: break;
            case (byte)0x9F: break;
            case (byte)0xA0: break;
            case (byte)0xA1: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case (byte)0xA2: break;
            case (byte)0xA3: retValue = inc_dptr(); break;
            case (byte)0xA4: break;
            case (byte)0xA5: break;
            case (byte)0xA6: break;
            case (byte)0xA7: break;
            case (byte)0xA8: break;
            case (byte)0xA9: break;
            case (byte)0xAA: break;
            case (byte)0xAB: break;
            case (byte)0xAC: break;
            case (byte)0xAD: break;
            case (byte)0xAE: break;
            case (byte)0xAF: break;
            case (byte)0xB0: break;
            case (byte)0xB1: retValue = acall(currentInstruction, getCodeByte()); break;
            case (byte)0xB2: break;
            case (byte)0xB3: break;
            case (byte)0xB4: break;
            case (byte)0xB5: break;
            case (byte)0xB6: break;
            case (byte)0xB7: break;
            case (byte)0xB8: break;
            case (byte)0xB9: break;
            case (byte)0xBA: break;
            case (byte)0xBB: break;
            case (byte)0xBC: break;
            case (byte)0xBD: break;
            case (byte)0xBE: break;
            case (byte)0xBF: break;
            case (byte)0xC0: retValue = pop(getCodeByte()); break;
            case (byte)0xC1: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case (byte)0xC2: break;
            case (byte)0xC3: break;
            case (byte)0xC4: break;
            case (byte)0xC5: break;
            case (byte)0xC6: break;
            case (byte)0xC7: break;
            case (byte)0xC8: break;
            case (byte)0xC9: break;
            case (byte)0xCA: break;
            case (byte)0xCB: break;
            case (byte)0xCC: break;
            case (byte)0xCD: break;
            case (byte)0xCE: break;
            case (byte)0xCF: break;
            case (byte)0xD0: retValue = push(getCodeByte()); break;
            case (byte)0xD1: retValue = acall(currentInstruction, getCodeByte()); break;
            case (byte)0xD2: break;
            case (byte)0xD3: break;
            case (byte)0xD4: break;
            case (byte)0xD5: break;
            case (byte)0xD6: break;
            case (byte)0xD7: break;
            case (byte)0xD8: break;
            case (byte)0xD9: break;
            case (byte)0xDA: break;
            case (byte)0xDB: break;
            case (byte)0xDC: break;
            case (byte)0xDD: break;
            case (byte)0xDE: break;
            case (byte)0xDF: break;
            case (byte)0xE0: break;
            case (byte)0xE1: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case (byte)0xE2: break;
            case (byte)0xE3: break;
            case (byte)0xE4: break;
            case (byte)0xE5: break;
            case (byte)0xE6: break;
            case (byte)0xE7: break;
            case (byte)0xE8: break;
            case (byte)0xE9: break;
            case (byte)0xEA: break;
            case (byte)0xEB: break;
            case (byte)0xEC: break;
            case (byte)0xED: break;
            case (byte)0xEE: break;
            case (byte)0xEF: break;
            case (byte)0xF0: break;
            case (byte)0xF1: retValue = acall(currentInstruction, getCodeByte()); break;
            case (byte)0xF2: break;
            case (byte)0xF3: break;
            case (byte)0xF4: break;
            case (byte)0xF5: break;
            case (byte)0xF6: break;
            case (byte)0xF7: break;
            case (byte)0xF8: break;
            case (byte)0xF9: break;
            case (byte)0xFA: break;
            case (byte)0xFB: break;
            case (byte)0xFC: break;
            case (byte)0xFD: break;
            case (byte)0xFE: break;
            case (byte)0xFF: break;
        }
        //TODO put the following in a finally-block and add exception handling
        //TODO add updateTimers()-Method
        updateParityFlag();
        //The value of the R registers can be changed through memory.
        //In order to ensure that the GUI displays the correct values, the setter in each R register is called every
        //time, so that it fires property-change-events
        for (int i = 0; i < 8; ++i) this.state.getR(i).setValue(this.state.getR(i).getValue());
        return retValue;
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


    /**
     * Get a byte from code memory and increment the PC.<br>
     * @return the retrieved byte
     */
    private byte getCodeByte() {
        char tmp = (char)((this.state.PCH.getValue() << 8) & 0xFF00 | this.state.PCL.getValue() & 0xFF);
        byte ret = this.state.codeMemory.get(tmp);
        ++tmp;
        this.state.PCH.setValue((byte) (tmp >>> 8));
        this.state.PCL.setValue((byte) tmp);
        return ret;
    }

    /**
     * Get the current address of a R register.
     * @param ordinal
     *     the returned register will be R&lt;ordinal&gt;; 0 <= ordinal <= 7
     * @return the register's address
     * @throws IllegalArgumentException when given an invalid ordinal
     */
    private int getRAddress(int ordinal) throws IllegalArgumentException {
        if (ordinal < 0 || ordinal > 7)
            throw new IllegalArgumentException("Invalid R register ordinal: "+ordinal);
        //PSW: C | AC | F0 | RS1 | RS0 | OV | UD | P
        //==> RS1 and RS0 control the register bank and are conveniently already located at the correct position
        return this.state.sfrs.PSW.getValue() & 0b00011000 | ordinal;
    }

    /**
     * Get the value of an R register.<br>
     * @param ordinal
     *     The R register to be used. E.g. '5' implies R5.
     * @return R<sup>ordinal</sup>'s value
     * @throws IllegalArgumentException when given an illegal ordinal
     */
    private byte getR(int ordinal) {
        return this.state.internalRAM.get(getRAddress(ordinal));
    }

    /**
     * Set the value of an R register.<br>
     * @param ordinal
     *     The R register to be used. E.g. '5' implies R5.
     * @param newValue
     *     R<sub>ordinal</sub>'s new value.
     * @throws IllegalArgumentException when given an illegal ordinal
     */
    private void setR(int ordinal, byte newValue) {
        this.state.internalRAM.set(getRAddress(ordinal), newValue);
    }

    /**
     * Update the parity flag in the PSW.
     * It is set so that the number of bits set to one plus the parity flag is even.
     * NOTE: This algorithm was inspired by
     * http://www.geeksforgeeks.org/write-a-c-program-to-find-the-parity-of-an-unsigned-integer/
     */
    private void updateParityFlag() {
        boolean parity = false;
        for (byte b = this.state.sfrs.A.getValue(); b > 0; b = (byte)(b & (b - 1))) parity = !parity;
        this.state.sfrs.PSW.setBit(parity, 0);
    }

    private ByteRegister[] generateRRegisters() {
        ByteRegister[] rRegisters = new ByteRegister[8];
        for (int i = 0; i < rRegisters.length; ++i) {
            final int tmpOrdinal = i;
            rRegisters[i] = new ByteRegister("R"+i) {

                @Override public void setValue(byte newValue) {
                    MC8051.this.state.internalRAM.set(getRAddress(tmpOrdinal), newValue);
                    super.setValue(newValue); //super.setValue() is being called here to fire a property change
                }

                @Override public byte getValue() {
                    //the byte from the internal RAM needs to be returned here as the R registers may also be modified
                    //through it
                    return MC8051.this.state.internalRAM.get(getRAddress(tmpOrdinal));
                }
            };
        }
        return rRegisters;
    }

    /**
     * Get the value of an address from internal RAM (or the SFR area)
     * @param address the address to be used
     * @return the byte at this address
     * @throws IndexOutOfBoundsException when the address is in the SFR area but is not a valid SFR
     */
    private byte getDirectAddress(byte address) throws IndexOutOfBoundsException {
        if ((address & 0xFF) < 0x80) //if the address in in the directly addressable part of the internal RAM
            return this.state.internalRAM.get(address & 0xFF);
        else {
            if (!this.state.sfrs.hasAddress(address)) {
                int pcOfThisInstruction = this.state.PCH.getValue() << 8 & 0xFF00 | this.state.PCL.getValue() & 0xFF;
                throw new IndexOutOfBoundsException("Illegal address used at " + pcOfThisInstruction + ": "
                        + (address & 0xFF));
            }
            return this.state.sfrs.get(address & 0xFF);
        }
    }

    /**
     * Set the value at a direct address from internal RAM (or the SFR area)
     * @param address the address to be used
     * @param value the value the byte at this address will be set to
     * @throws IndexOutOfBoundsException when the address is in the SFR area, but is not a valid SFR (the operation is
     *         still performed, though; a new temporary SFR is created in this case)
     */
    private void setDirectAddress(byte address, byte value) throws IndexOutOfBoundsException {
        if ((address & 0xFF) < 0x80) //if the address in in the directly addressable part of the internal RAM
            this.state.internalRAM.set(address & 0xFF, value);
        else {
            if (!this.state.sfrs.hasAddress(address)) {
                //If the program attempts to use a value in the SFR area which does not hold a register,
                //create a new register and throw an exception (because the program would exhibit undefined behaviour
                //on real hardware
                this.state.sfrs.addRegister(address, new ByteRegister(String.format("TMP_SFR#%02X", address & 0xFF)));
                int pcOfThisInstruction = this.state.PCH.getValue() << 8 & 0xFF00 | this.state.PCL.getValue() & 0xFF;
                throw new IndexOutOfBoundsException("Illegal address used at " + pcOfThisInstruction + ": "
                        + (address & 0xFF));
            }
            this.state.sfrs.getRegister(address).setValue(value);
        }
    }

    /**
     * Decode a bit address into a direct address and a bit mask.
     * @param bitAddress
     *     the bit address to be decoded; must be valid
     * @return a {@code BitAddress} object containing the direct address of the byte containing the specified bit and a
     *         bit mask for the bit
     * @throws IllegalArgumentException given an invalid bit address
     */
    private BitAddress decodeBitAddress(byte bitAddress) throws IllegalArgumentException {
        int address = bitAddress & 0xFF; //trying to prevent strange behaviour with negative bytes...
        int retAddress = -1;
        byte retBitMask = -1;

        if (address < 0x80) { //the address is in the lower part of the internal RAM
            final int START_OF_BIT_MEMORY = 0x20;
            retAddress = START_OF_BIT_MEMORY + address / 8;
        } else {
            ByteRegister tmp = null;
            switch (address - address % 8) {
                case 0x80: tmp = this.state.sfrs.P0; break; // P0
                case 0x88: tmp = this.state.sfrs.TCON; break; // TCON
                case 0x90: tmp = this.state.sfrs.P1; break; // P1
                case 0x98: tmp = this.state.sfrs.SCON; break; // SCON
                case 0xA0: tmp = this.state.sfrs.P2; break; // P2
                case 0xA8: tmp = this.state.sfrs.IE; break; // IE
                case 0xB0: tmp = this.state.sfrs.P3; break; // P3
                case 0xB8: tmp = this.state.sfrs.IP; break; // IP
                case 0xD0: tmp = this.state.sfrs.PSW; break; // PSW
                case 0xE0: tmp = this.state.sfrs.A; break; // ACC
                case 0xF0: tmp = this.state.sfrs.B; break; // B
                default:
                    int tmpAddr = address - address % 8;
                    tmp = this.state.sfrs.getRegister((byte)tmpAddr);
                    if (null == tmp) {
                        //if the program attempts to use an illegal address, a new SFR ist created at this address
                        //and an exception is thrown, so that the user can continue, if he wants
                        //(on real hardware, the behaviour in this case might be undefined)
                        int pc = this.state.PCH.getValue() << 8 & 0xFF00 | this.state.PCL.getValue() & 0xFF;
                        tmp = new ByteRegister(String.format("TMP_SFR#%02X", tmpAddr & 0xFF));
                        this.state.sfrs.addRegister((byte)tmpAddr, tmp);
                        throw new IndexOutOfBoundsException("Invalid bit address: " + address + " at " + pc);
                    }
            }
            retAddress = this.state.sfrs.getAddress(tmp);
        }

        retBitMask = (byte)(1 << (address % 8)); // in the bit mask, only the addressed bit will be set
        return new BitAddress((byte)retAddress, retBitMask);
    }

    /**
     * Get the value of a single bit.
     * @param bitAddress the bit's bit address
     * @return {@code true} if the bit is 1; {@code false} if it is 0
     * @see #decodeBitAddress(byte)
     */
    boolean getBit(byte bitAddress) {
        BitAddress tmp = decodeBitAddress(bitAddress);
        return (getDirectAddress(tmp.DIRECT_ADDRESS) & tmp.BIT_MASK) == tmp.BIT_MASK;
    }

    /**
     * Set the value of a single bit.
     * @param bitAddress the bit's bit address
     * @param bit the bit's new value; {@code true} -> 1; {@code false} -> 0
     * @see #decodeBitAddress(byte)
     */
    void setBit(boolean bit, byte bitAddress) {
        BitAddress tmp = decodeBitAddress(bitAddress);
        byte b = getDirectAddress(tmp.DIRECT_ADDRESS);
        if (bit) b |= tmp.BIT_MASK & 0xFF;
        else b &= ~(tmp.BIT_MASK & 0xFF);
        setDirectAddress(tmp.DIRECT_ADDRESS, b);
    }

    private void jumpToOffset(byte offset) {
        char pc = (char)(this.state.PCH.getValue() << 8 & 0xFF00 | this.state.PCL.getValue() & 0xFF);
        pc += offset;
        this.state.PCH.setValue((byte)(pc >>> 8));
        this.state.PCL.setValue((byte)pc);
    }

    /**
     * Get a byte from stack. This is not the method called upon the opcode for {@code POP}, but an internal helper
     * method.
     * @param exceptionOnUnderflow if this is {@code true}, an exception is thrown, when the stack pointer is 0
     * @return the byte from the stack
     * @see #pop(byte)
     */
    private byte _pop(boolean exceptionOnUnderflow) {
        int resultingAddress = this.state.sfrs.SP.getValue() & 0xFF;
        byte result = this.state.internalRAM.get(resultingAddress);
        --resultingAddress;
        this.state.sfrs.SP.setValue((byte)resultingAddress);
        if (resultingAddress < 0 && exceptionOnUnderflow)
            throw new IllegalArgumentException("Stack underflow.");
        return result;
    }

    /**
     * <b>No Operation</b><br>
     * @return the number of cycles (1)
     * */
    private int nop() {
        return 1;
    }

    /**
     * <b>Absolute Jump</b><br>
     * Set the last 11 bits of the PC.<br>
     * They are encoded in the following way:<br>
     * Opcode: <code>A<sub>10</sub>A<sub>9</sub>A<sub>8</sub>0001</code><br>
     * Operand: </code>A<sub>7</sub> - A<sub>0</sub>
     * <br>
     * Example:
     * <code><br>
     * &nbsp;&nbsp;&nbsp;&nbsp;
     * instruction&nbsp;=&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;11100001<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;
     * pc&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;=&nbsp;1011011100000010<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;
     * argument&nbsp;&nbsp;&nbsp;&nbsp;=&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;00100010<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;
     * A<sub>10</sub> - A<sub>8</sub> (from instruction) = 111<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;
     * A<sub>7</sub> - A<sub>0</sub> (from argument)
     * &nbsp;&nbsp;&nbsp;&nbsp;=&nbsp;&nbsp;&nbsp;&nbsp;00100010<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;
     * ==>&nbsp;resulting&nbsp;address&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;=&nbsp;11100100010<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;
     * replace&nbsp;last&nbsp;11&nbsp;bits&nbsp;in&nbsp;pc:<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;
     * pc&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;=&nbsp;1011011100000010<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;
     * result&nbsp;&nbsp;&nbsp;=&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;11100100010<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;
     * replaced&nbsp;=&nbsp;1011011100100010<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;
     * [in&nbsp;hex]&nbsp;=&nbsp;0xB722&nbsp;<br>
     * </code>
     * @param currentOp The current opcode.
     * @param last8bits The argument (next byte in code memory)
     * @return the number of cycles (2)
     */
    private int ajmp(byte currentOp, byte last8bits) {

        char result = (char)((this.state.PCH.getValue() << 8) & 0xFF00 | this.state.PCL.getValue() & 0xFF);
        //result += 2; not necessary because the current pc at the time of the call will be the address after ajmp
        result &= 0xF800; //delete the last 11 bits
        result |= last8bits & 0xFF;
        result |= (currentOp & 0xE0) << 3; //copy the three bits of the address encoded in the opcode
        this.state.PCH.setValue((byte)(result >>> 8));
        this.state.PCL.setValue((byte)result);
        return 2;
    }

    /**
     * <b>Long Jump</b><br>
     * The address is encoded in the following way:<br>
     * opcode | A<sub>15<sub>-A<sub>8</sub> | A<sub>7</sub>-A<sub>0</sub>
     * @param highByte A<sub>15<sub>-A<sub>8</sub>
     * @param lowByte A<sub>7</sub>-A<sub>0</sub>
     * @return the number of cycles (2)
     */
    private int ljmp(byte highByte, byte lowByte) {
        this.state.PCH.setValue(highByte);
        this.state.PCL.setValue(lowByte);
        return 2;
    }

    /**
     * <b>Rotate Right</b><br>
     * This instruction rotates the accumulator one bit to the right.
     * Bit 0 of the accumulator is rotated to bit 7.<br>
     * @return the number of cycles (1)
     */
    private int rr_a() {
        final int a = this.state.sfrs.A.getValue() & 0xFF;
        int result = a >>> 1; //rotate a one bit to the right
        if ((a & 0x1) == 1) { //if bit 0 is set
           result |= 1 << 7; //set bit 7 in result
        }
        this.state.sfrs.A.setValue((byte)result);
        return 1;
    }

    /**
     * <b>Rotate Right (with) Carry</b><br>
     * This instruction rotates the accumulator one bit to the right. Bit 0 is rotated into C and C into bit 7.
     * @return the number of cycles (1)
     */
    private int rrc_a() {
        int a = this.state.sfrs.A.getValue() & 0xFF;
        final boolean oldC = this.state.sfrs.PSW.getBit(7);
        final boolean newC = (a & 1) == 1;
        a >>>= 1;
        if (oldC) a |= 1 << 7;
        this.state.sfrs.A.setValue((byte)a);
        this.state.sfrs.PSW.setBit(newC, 7);
        return 1;
    }

    /**
     * <b>Increment (Register)</b><br>
     * Increment a register by one.
     * @param r
     *     the register
     * @return the number of cycles (1)
     */
    private int inc(ByteRegister r) {
        r.setValue((byte)(r.getValue() + 1));
        return 1;
    }

    /**
     * <b>Increment (Direct Address)</b><br>
     * Increment the byte at the direct address by one.
     * @param directAddress
     *     the direct address; must be < 0x80 or the address of a SFR
     * @return the number of cycles (1)
     * @throws IndexOutOfBoundsException when an address in the SFR memory is used that does not contain an SFR
     */
    private int inc(byte directAddress) throws IndexOutOfBoundsException {
        setDirectAddress(directAddress, (byte)(getDirectAddress(directAddress) + 1));
        return 1;
    }

    /**
     * <b>Increment (DPTR)</b><br>
     * Increment the data pointer.
     * @return the number of cycles (2)
     */
    private int inc_dptr() {
        char dptr = (char)(this.state.sfrs.DPH.getValue() << 8 & 0xFF00 | this.state.sfrs.DPL.getValue() & 0xFF);
        ++dptr;
        this.state.sfrs.DPH.setValue((byte)(dptr >>> 8));
        this.state.sfrs.DPL.setValue((byte)dptr);
        return 2;
    }

    /**
     * <b>Jump (if) Bit (is set)</b>
     * Jump by a certain offset if the specified bit is set.
     * @param bitAddress the bit's address
     * @param offset the offset to jump by if the bit is set
     * @return the number of cycles (2)
     * @see #decodeBitAddress(byte)
     */
    private int jb(byte bitAddress, byte offset) {
        if (getBit(bitAddress)) jumpToOffset(offset);
        return 2;
    }

    /**
     * <b>Jump (if) Bit (is set and) Clear (it)</b>
     * Jump by a certain offset if the specified bit is set and clear the bit afterwards.
     * @param bitAddress the bit's address
     * @param offset the offset to jump by if the bit is set
     * @return the number of cycles (2)
     * @see #decodeBitAddress(byte)
     * @see #jb(byte, byte)
     */
    private int jbc(byte bitAddress, byte offset) {
        jb(bitAddress, offset);
        setBit(false, bitAddress);
        return 2;
    }

    /**
     * <b>Jump (if) Bit (is not set)</b>
     * Jump by a certain offset if the specified bit is not set.
     * @param bitAddress the bit's address
     * @param offset the offset to jump by if the bit is set
     * @return the number of cycles (2)
     * @see #decodeBitAddress(byte)
     */
    private int jnb(byte bitAddress, byte offset) {
        if (!getBit(bitAddress)) jumpToOffset(offset);
        return 2;
    }

    /**
     * <b>Jump (if) C (is set)</b>
     * Jump by a certain offset if the C flag is set.
     * @param offset the offset to jump by if the C flag is set
     * @return the number of cycles (2)
     */
    private int jc(byte offset) {
        if (this.state.sfrs.PSW.getBit(7)) jumpToOffset(offset);
        return 2;
    }

    /**
     * <b>Jump (if) C (is not set)</b>
     * Jump by a certain offset if the C flag is not set.
     * @param offset the offset to jump by if the C flag is not set
     * @return the number of cycles (2)
     */
    private int jnc(byte offset) {
        if (!this.state.sfrs.PSW.getBit(7)) jumpToOffset(offset);
        return 2;
    }

    /**
     * <b>Absolute Call</b><br>
     *
     * Call a subroutine at the specified address (in the same 2KiB block)<br>
     *
     * NOTE: This instruction internally uses push() and ajmp().
     *
     * @param currentOpcode the current opcode
     * @param last8bits the argument (which contains the destination address' low-byte)
     * @return the number of cycles (2)
     * @throws IllegalStateException on stack overflow
     *
     * @see #ajmp(byte, byte)
     * @see #push(byte)
     */
    private int acall(byte currentOpcode, byte last8bits) {
        push(this.state.PCL.getValue());
        push(this.state.PCH.getValue());
        ajmp(currentOpcode, last8bits);
        return 2;
    }

    /**
     * <b>Long Call</b><br>
     *
     * Call a subroutine at the specified address.<br>
     *
     * NOTE: This instruction internally uses push() and ljmp().
     *
     * @param highByte the destination address' high byte
     * @param lowByte the destination address' low byte
     * @return the number of cycles (2)
     * @see #push(byte)
     * @see #lcall(byte, byte)
     */
    private int lcall(byte highByte, byte lowByte) {
        push(this.state.PCL.getValue());
        push(this.state.PCH.getValue());
        ljmp(highByte, lowByte);
        return 2;
    }

    /**
     * <b>Push</b><br>
     * Increment the stack pointer and push a byte onto the stack.
     * @param value the value to be stored on the stack
     * @return the number of cycles (2)
     * @throws IllegalStateException on stack overflow
     */
    private int push(byte value) throws IllegalStateException {
        int resultingAddress = (this.state.sfrs.SP.getValue() & 0xFF) + 1;
        this.state.internalRAM.set(resultingAddress & 0xFF, value);
        this.state.sfrs.SP.setValue((byte)resultingAddress);
        if (resultingAddress > 0xFF)
            throw new IllegalStateException("Stack overflow.");
        return 2;
    }

    /**
     * <b>Pop</b> a byte from the stack and decrement the stack pointer
     * @return the number of cycles (2)
     * @param direct address at which the result is stored
     * @throws IllegalStateException on stack underflow
     * @see #setDirectAddress(byte, byte)
     */
    private int pop(byte direct) throws IllegalStateException {
        setDirectAddress(direct, _pop(true));
        return 2;
    }
}
