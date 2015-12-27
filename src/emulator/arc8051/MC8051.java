package emulator.arc8051;

import com.sun.javaws.exceptions.InvalidArgumentException;
import emulator.*;

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
 * @author Gordian
 */
public class MC8051 implements Emulator {

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
        this.state = new State8051(codeMemory, externalRAM);
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
        byte previousA = this.state.sfrs.A.getValue();
        switch (currentInstruction) {
            case       0x00: retValue = nop(); break;
            case       0x01: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case       0x02: retValue = ljmp(getCodeByte(), getCodeByte()); break;
            case       0x03: retValue = rr_a(); break;
            case       0x04: retValue = inc(this.state.sfrs.A); break;
            case       0x05: retValue = inc(getCodeByte()); break;
            case       0x06: retValue = inc_r_indirect(true); break;
            case       0x07: retValue = inc_r_indirect(false); break;
            case       0x08: retValue = inc_r(0); break;
            case       0x09: retValue = inc_r(1); break;
            case       0x0a: retValue = inc_r(2); break;
            case       0x0b: retValue = inc_r(3); break;
            case       0x0c: retValue = inc_r(4); break;
            case       0x0d: retValue = inc_r(5); break;
            case       0x0e: retValue = inc_r(6); break;
            case       0x0f: retValue = inc_r(7); break;
            case       0x10: break;
            case       0x11: break;
            case       0x12: break;
            case       0x13: break;
            case       0x14: break;
            case       0x15: break;
            case       0x16: break;
            case       0x17: break;
            case       0x18: break;
            case       0x19: break;
            case       0x1a: break;
            case       0x1b: break;
            case       0x1c: break;
            case       0x1d: break;
            case       0x1e: break;
            case       0x1f: break;
            case       0x20: break;
            case       0x21: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case       0x22: break;
            case       0x23: break;
            case       0x24: break;
            case       0x25: break;
            case       0x26: break;
            case       0x27: break;
            case       0x28: break;
            case       0x29: break;
            case       0x2a: break;
            case       0x2b: break;
            case       0x2c: break;
            case       0x2d: break;
            case       0x2e: break;
            case       0x2f: break;
            case       0x30: break;
            case       0x31: break;
            case       0x32: break;
            case       0x33: break;
            case       0x34: break;
            case       0x35: break;
            case       0x36: break;
            case       0x37: break;
            case       0x38: break;
            case       0x39: break;
            case       0x3a: break;
            case       0x3b: break;
            case       0x3c: break;
            case       0x3d: break;
            case       0x3e: break;
            case       0x3f: break;
            case       0x40: break;
            case       0x41: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case       0x42: break;
            case       0x43: break;
            case       0x44: break;
            case       0x45: break;
            case       0x46: break;
            case       0x47: break;
            case       0x48: break;
            case       0x49: break;
            case       0x4a: break;
            case       0x4b: break;
            case       0x4c: break;
            case       0x4d: break;
            case       0x4e: break;
            case       0x4f: break;
            case       0x50: break;
            case       0x51: break;
            case       0x52: break;
            case       0x53: break;
            case       0x54: break;
            case       0x55: break;
            case       0x56: break;
            case       0x57: break;
            case       0x58: break;
            case       0x59: break;
            case       0x5a: break;
            case       0x5b: break;
            case       0x5c: break;
            case       0x5d: break;
            case       0x5e: break;
            case       0x5f: break;
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
            case       0x6a: break;
            case       0x6b: break;
            case       0x6c: break;
            case       0x6d: break;
            case       0x6e: break;
            case       0x6f: break;
            case       0x70: break;
            case       0x71: break;
            case       0x72: break;
            case       0x73: break;
            case       0x74: break;
            case       0x75: break;
            case       0x76: break;
            case       0x77: break;
            case       0x78: break;
            case       0x79: break;
            case       0x7a: break;
            case       0x7b: break;
            case       0x7c: break;
            case       0x7d: break;
            case       0x7e: break;
            case       0x7f: break;
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
            case (byte)0x8a: break;
            case (byte)0x8b: break;
            case (byte)0x8c: break;
            case (byte)0x8d: break;
            case (byte)0x8e: break;
            case (byte)0x8f: break;
            case (byte)0x90: break;
            case (byte)0x91: break;
            case (byte)0x92: break;
            case (byte)0x93: break;
            case (byte)0x94: break;
            case (byte)0x95: break;
            case (byte)0x96: break;
            case (byte)0x97: break;
            case (byte)0x98: break;
            case (byte)0x99: break;
            case (byte)0x9a: break;
            case (byte)0x9b: break;
            case (byte)0x9c: break;
            case (byte)0x9d: break;
            case (byte)0x9e: break;
            case (byte)0x9f: break;
            case (byte)0xa0: break;
            case (byte)0xa1: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case (byte)0xa2: break;
            case (byte)0xa3: retValue = inc_dptr(); break;
            case (byte)0xa4: break;
            case (byte)0xa5: break;
            case (byte)0xa6: break;
            case (byte)0xa7: break;
            case (byte)0xa8: break;
            case (byte)0xa9: break;
            case (byte)0xaa: break;
            case (byte)0xab: break;
            case (byte)0xac: break;
            case (byte)0xad: break;
            case (byte)0xae: break;
            case (byte)0xaf: break;
            case (byte)0xb0: break;
            case (byte)0xb1: break;
            case (byte)0xb2: break;
            case (byte)0xb3: break;
            case (byte)0xb4: break;
            case (byte)0xb5: break;
            case (byte)0xb6: break;
            case (byte)0xb7: break;
            case (byte)0xb8: break;
            case (byte)0xb9: break;
            case (byte)0xba: break;
            case (byte)0xbb: break;
            case (byte)0xbc: break;
            case (byte)0xbd: break;
            case (byte)0xbe: break;
            case (byte)0xbf: break;
            case (byte)0xc0: break;
            case (byte)0xc1: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case (byte)0xc2: break;
            case (byte)0xc3: break;
            case (byte)0xc4: break;
            case (byte)0xc5: break;
            case (byte)0xc6: break;
            case (byte)0xc7: break;
            case (byte)0xc8: break;
            case (byte)0xc9: break;
            case (byte)0xca: break;
            case (byte)0xcb: break;
            case (byte)0xcc: break;
            case (byte)0xcd: break;
            case (byte)0xce: break;
            case (byte)0xcf: break;
            case (byte)0xd0: break;
            case (byte)0xd1: break;
            case (byte)0xd2: break;
            case (byte)0xd3: break;
            case (byte)0xd4: break;
            case (byte)0xd5: break;
            case (byte)0xd6: break;
            case (byte)0xd7: break;
            case (byte)0xd8: break;
            case (byte)0xd9: break;
            case (byte)0xda: break;
            case (byte)0xdb: break;
            case (byte)0xdc: break;
            case (byte)0xdd: break;
            case (byte)0xde: break;
            case (byte)0xdf: break;
            case (byte)0xe0: break;
            case (byte)0xe1: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case (byte)0xe2: break;
            case (byte)0xe3: break;
            case (byte)0xe4: break;
            case (byte)0xe5: break;
            case (byte)0xe6: break;
            case (byte)0xe7: break;
            case (byte)0xe8: break;
            case (byte)0xe9: break;
            case (byte)0xea: break;
            case (byte)0xeb: break;
            case (byte)0xec: break;
            case (byte)0xed: break;
            case (byte)0xee: break;
            case (byte)0xef: break;
            case (byte)0xf0: break;
            case (byte)0xf1: break;
            case (byte)0xf2: break;
            case (byte)0xf3: break;
            case (byte)0xf4: break;
            case (byte)0xf5: break;
            case (byte)0xf6: break;
            case (byte)0xf7: break;
            case (byte)0xf8: break;
            case (byte)0xf9: break;
            case (byte)0xfa: break;
            case (byte)0xfb: break;
            case (byte)0xfc: break;
            case (byte)0xfd: break;
            case (byte)0xfe: break;
            case (byte)0xff: break;
        }
        //TODO put the following in a finally-block and add exception handling
        //TODO add updateTimers()-Method
        updateRRegisters();
        if (previousA != this.state.sfrs.A.getValue()) updateParityFlag();
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
     * Update the R registers according to the internal RAM.
     */
    private void updateRRegisters() {
        for (int i = 0; i < 8; ++i) {
            //I inserted the if to prevent unnecessary property change events
            if (this.state.getR(i).getValue() != getR(i)) this.state.getR(i).setValue(getR(i));
        }
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
        result |= last8bits;
        result |= (currentOp & 0xE0) << 3; //copy the three bits of the address encoded in the opcode
        this.state.PCH.setValue((byte)(result >>> 8));
        this.state.PCL.setValue((byte)result);
        return 2;
    }

    /**
     * <b>Long Jump</b><br>
     * The address is encoded in the following way:<br>
     * opcode | A<sub>15<sub>-A<sub>8</sub> | A<sub>7</sub>-A<sub>0</sub>
     * @param arg1 A<sub>15<sub>-A<sub>8</sub>
     * @param arg2 A<sub>7</sub>-A<sub>0</sub>
     * @return the number of cycles (2)
     */
    private int ljmp(byte arg1, byte arg2) {
        this.state.PCH.setValue(arg1);
        this.state.PCL.setValue(arg2);
        return 2;
    }

    /**
     * <b>Rotate Right</b> (rotates the accumulator one bit to the right)<br>
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
        if ((directAddress & 0xFF) < 0x80) //if the address in in the directly addresable part of the internal RAM
            this.state.internalRAM.set(directAddress & 0xFF,
                    (byte) (this.state.internalRAM.get(directAddress & 0xFF) + 1));
        else {
            int pcOfThisInstruction = this.state.PCH.getValue() << 8 & 0xFF00 | this.state.PCL.getValue() & 0xFF;
            if (!this.state.sfrs.hasAddress(directAddress))
                throw new IndexOutOfBoundsException("Illegal address used at "+pcOfThisInstruction+": "
                        +(directAddress & 0xFF));
            inc(this.state.sfrs.get(directAddress & 0xFF));
        }
        return 1;
    }

    /**
     * <b>Increment (@Ri)</b><br>
     * Increment the byte at the address contained in R0 or R1.
     * @param rZero
     *     {@code false} -> use R1
     *     {@code true}  -> use R0
     * @return the number of cycles (1)
     */
    private int inc_r_indirect(boolean rZero) {
        return inc(getR(rZero ? 0 : 1));
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
     * <b>Increment (RX)</b>
     * <br>
     * Increment the value of the specified R register.
     * @param ordinal
     *     specifies the register (->R&lt;ordinal&gt;); 0 <= ordinal <= 7
     * @return the number of cycles (1)
     * @throws IllegalArgumentException when given an illegal ordinal
     */
    private int inc_r(int ordinal) {
        return inc((byte)getRAddress(ordinal));
    }
}
