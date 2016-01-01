package emulator.arc8051;

import emulator.*;

import java.util.List;
import java.util.Objects;

/**
 * This class represents the 8051 micro controller.
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

    private static class BitAddress {
        public final byte DIRECT_ADDRESS;
        public final byte BIT_MASK;
        public BitAddress(byte directAddress, byte bitMask) {
            this.BIT_MASK = bitMask;
            this.DIRECT_ADDRESS = directAddress;
        }
    }

    State8051 state;
    private final boolean ignoreSOSU; //ignore stack overflow/underflow


    /**
     * Create a new 8051 micro controller object.<br>
     * @param externalRAM
     *        The external RAM accessible through the {@code MOVX} command. {@code null} is a valid value and implies,
     *        that there is no external RAM (and thus, all {@code MOVX}-instructions should fail).
     * @param codeMemory
     *        The 8051's "code memory". The instructions will be read from this object. Must not be {@code null}.
     *        The size must be 65536 bytes.
     */
    public MC8051(ROM codeMemory, RAM externalRAM) {
        this(new State8051(codeMemory, externalRAM), false);
    }

    /**
     * Create a new 8051 microcontroller object.<br>
     * @param externalRAM
     *        The external RAM accessible through the {@code MOVX} command. {@code null} is a valid value and implies,
     *        that there is no external RAM (and thus, all {@code MOVX}-instructions should fail).
     * @param codeMemory
     *        The 8051's "code memory". The instructions will be read from this object. Must not be {@code null}.
     *        The size must be 65536 bytes.
     * @param ignoreSOSU
     *        Do not throw an exception on stack underflow or overflow
     */
    public MC8051(ROM codeMemory, RAM externalRAM, boolean ignoreSOSU) {
        this(new State8051(codeMemory, externalRAM), ignoreSOSU);
    }

    /**
     * Start with a specific state.
     * @param state
     *        The state. Must not be {@code null}.
     * @param ignoreSOSU
     *        Do not throw an exception on stack underflow or overflow
     */
    public MC8051(State8051 state, boolean ignoreSOSU) {
        this.state = Objects.requireNonNull(state, "trying to initialize MC8051 with empty state");
        this.state.setRRegisters(generateRRegisters());
        this.ignoreSOSU = ignoreSOSU;
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
            case       0x06: retValue = inc_indirect(getR(0)); break;
            case       0x07: retValue = inc_indirect(getR(1)); break;
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
            case       0x14: retValue = dec(this.state.sfrs.A); break;
            case       0x15: retValue = dec(getCodeByte()); break;
            case       0x16: retValue = dec_indirect(getR(0)); break;
            case       0x17: retValue = dec_indirect(getR(1)); break;
            case       0x18: retValue = dec(this.state.R0); break;
            case       0x19: retValue = dec(this.state.R1); break;
            case       0x1A: retValue = dec(this.state.R2); break;
            case       0x1B: retValue = dec(this.state.R3); break;
            case       0x1C: retValue = dec(this.state.R4); break;
            case       0x1D: retValue = dec(this.state.R5); break;
            case       0x1E: retValue = dec(this.state.R6); break;
            case       0x1F: retValue = dec(this.state.R7); break;
            case       0x20: retValue = jb(getCodeByte(), getCodeByte()); break;
            case       0x21: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case       0x22: retValue = ret(); break;
            case       0x23: retValue = rl_a(); break;
            case       0x24: retValue = add_immediate(getCodeByte()); break;
            case       0x25: retValue = add_direct(getCodeByte()); break;
            case       0x26: retValue = add_indirect(getR(0)); break;
            case       0x27: retValue = add_indirect(getR(1)); break;
            case       0x28: retValue = add_r(0); break;
            case       0x29: retValue = add_r(1); break;
            case       0x2A: retValue = add_r(2); break;
            case       0x2B: retValue = add_r(3); break;
            case       0x2C: retValue = add_r(4); break;
            case       0x2D: retValue = add_r(5); break;
            case       0x2E: retValue = add_r(6); break;
            case       0x2F: retValue = add_r(7); break;
            case       0x30: retValue = jnb(getCodeByte(), getCodeByte()); break;
            case       0x31: retValue = acall(currentInstruction, getCodeByte()); break;
            case       0x32: break;
            case       0x33: retValue = rlc_a(); break;
            case       0x34: retValue = addc_immediate(getCodeByte()); break;
            case       0x35: retValue = addc_direct(getCodeByte()); break;
            case       0x36: retValue = addc_indirect(getR(0)); break;
            case       0x37: retValue = addc_indirect(getR(1)); break;
            case       0x38: retValue = addc_r(0); break;
            case       0x39: retValue = addc_r(1); break;
            case       0x3A: retValue = addc_r(2); break;
            case       0x3B: retValue = addc_r(3); break;
            case       0x3C: retValue = addc_r(4); break;
            case       0x3D: retValue = addc_r(5); break;
            case       0x3E: retValue = addc_r(6); break;
            case       0x3F: retValue = addc_r(7); break;
            case       0x40: retValue = jc(getCodeByte()); break;
            case       0x41: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case       0x42: retValue = orl_direct_a(getCodeByte()); break;
            case       0x43: retValue = orl(getCodeByte(), getCodeByte()); break;
            case       0x44: retValue = orl_a_immediate(getCodeByte()); break;
            case       0x45: retValue = orl_a(getCodeByte()); break;
            case       0x46: retValue = orl_a_indirect(getR(0)); break;
            case       0x47: retValue = orl_a_indirect(getR(1)); break;
            case       0x48: retValue = orl_a_immediate(getR(0)); break;
            case       0x49: retValue = orl_a_immediate(getR(1)); break;
            case       0x4A: retValue = orl_a_immediate(getR(2)); break;
            case       0x4B: retValue = orl_a_immediate(getR(3)); break;
            case       0x4C: retValue = orl_a_immediate(getR(4)); break;
            case       0x4D: retValue = orl_a_immediate(getR(5)); break;
            case       0x4E: retValue = orl_a_immediate(getR(6)); break;
            case       0x4F: retValue = orl_a_immediate(getR(7)); break;
            case       0x50: retValue = jnc(getCodeByte()); break;
            case       0x51: retValue = acall(currentInstruction, getCodeByte()); break;
            case       0x52: retValue = anl_direct_a(getCodeByte()); break;
            case       0x53: retValue = anl(getCodeByte(), getCodeByte()); break;
            case       0x54: retValue = anl_a_immediate(getCodeByte()); break;
            case       0x55: retValue = anl_a(getCodeByte()); break;
            case       0x56: retValue = anl_a_indirect(getR(0)); break;
            case       0x57: retValue = anl_a_indirect(getR(1)); break;
            case       0x58: retValue = anl_a_immediate(getR(0)); break;
            case       0x59: retValue = anl_a_immediate(getR(1)); break;
            case       0x5A: retValue = anl_a_immediate(getR(2)); break;
            case       0x5B: retValue = anl_a_immediate(getR(3)); break;
            case       0x5C: retValue = anl_a_immediate(getR(4)); break;
            case       0x5D: retValue = anl_a_immediate(getR(5)); break;
            case       0x5E: retValue = anl_a_immediate(getR(6)); break;
            case       0x5F: retValue = anl_a_immediate(getR(7)); break;
            case       0x60: retValue = jz(getCodeByte()); break;
            case       0x61: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case       0x62: retValue = xrl_direct_a(getCodeByte()); break;
            case       0x63: retValue = xrl(getCodeByte(), getCodeByte()); break;
            case       0x64: retValue = xrl_a_immediate(getCodeByte()); break;
            case       0x65: retValue = xrl_a(getCodeByte()); break;
            case       0x66: retValue = xrl_a_indirect(getR(0)); break;
            case       0x67: retValue = xrl_a_indirect(getR(1)); break;
            case       0x68: retValue = xrl_a_immediate(getR(0)); break;
            case       0x69: retValue = xrl_a_immediate(getR(1)); break;
            case       0x6A: retValue = xrl_a_immediate(getR(2)); break;
            case       0x6B: retValue = xrl_a_immediate(getR(3)); break;
            case       0x6C: retValue = xrl_a_immediate(getR(4)); break;
            case       0x6D: retValue = xrl_a_immediate(getR(5)); break;
            case       0x6E: retValue = xrl_a_immediate(getR(6)); break;
            case       0x6F: retValue = xrl_a_immediate(getR(7)); break;
            case       0x70: retValue = jnz(getCodeByte()); break;
            case       0x71: retValue = acall(currentInstruction, getCodeByte()); break;
            case       0x72: retValue = orl_c(getCodeByte(), false); break;
            case       0x73: retValue = jmp_a_dptr(); break;
            case       0x74: retValue = mov_a_immediate(getCodeByte()); break;
            case       0x75: retValue = mov_direct_immediate(getCodeByte(), getCodeByte()); break;
            case       0x76: retValue = mov_indirect_immediate(getR(0), getCodeByte()); break;
            case       0x77: retValue = mov_indirect_immediate(getR(1), getCodeByte()); break;
            case       0x78: retValue = mov_r_immediate(0, getCodeByte()); break;
            case       0x79: retValue = mov_r_immediate(1, getCodeByte()); break;
            case       0x7A: retValue = mov_r_immediate(2, getCodeByte()); break;
            case       0x7B: retValue = mov_r_immediate(3, getCodeByte()); break;
            case       0x7C: retValue = mov_r_immediate(4, getCodeByte()); break;
            case       0x7D: retValue = mov_r_immediate(5, getCodeByte()); break;
            case       0x7E: retValue = mov_r_immediate(6, getCodeByte()); break;
            case       0x7F: retValue = mov_r_immediate(7, getCodeByte()); break;
            case (byte)0x80: break;
            case (byte)0x81: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case (byte)0x82: retValue = anl_c(getCodeByte(), false); break;
            case (byte)0x83: break;
            case (byte)0x84: break;
            case (byte)0x85: retValue = mov_direct_direct(getCodeByte(), getCodeByte()); break;
            case (byte)0x86: retValue = mov_direct_indirect(getCodeByte(), getR(0)); break;
            case (byte)0x87: retValue = mov_direct_indirect(getCodeByte(), getR(1)); break;
            case (byte)0x88: retValue = mov_direct_immediate(getCodeByte(), getR(0)); break;
            case (byte)0x89: retValue = mov_direct_immediate(getCodeByte(), getR(1)); break;
            case (byte)0x8A: retValue = mov_direct_immediate(getCodeByte(), getR(2)); break;
            case (byte)0x8B: retValue = mov_direct_immediate(getCodeByte(), getR(3)); break;
            case (byte)0x8C: retValue = mov_direct_immediate(getCodeByte(), getR(4)); break;
            case (byte)0x8D: retValue = mov_direct_immediate(getCodeByte(), getR(5)); break;
            case (byte)0x8E: retValue = mov_direct_immediate(getCodeByte(), getR(6)); break;
            case (byte)0x8F: retValue = mov_direct_immediate(getCodeByte(), getR(7)); break;
            case (byte)0x90: retValue = mov_dptr(getCodeByte(), getCodeByte()); break;
            case (byte)0x91: retValue = acall(currentInstruction, getCodeByte()); break;
            case (byte)0x92: retValue = mov_bit_c(getCodeByte()); break;
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
            case (byte)0xA0: retValue = orl_c(getCodeByte(), true); break;
            case (byte)0xA1: retValue = ajmp(currentInstruction, getCodeByte()); break;
            case (byte)0xA2: retValue = mov_c_bit(getCodeByte()); break;
            case (byte)0xA3: retValue = inc_dptr(); break;
            case (byte)0xA4: break;
            case (byte)0xA5: break; //reserved
            case (byte)0xA6: retValue = mov_indirect_direct(getR(0), getCodeByte()); break;
            case (byte)0xA7: retValue = mov_indirect_direct(getR(1), getCodeByte()); break;
            case (byte)0xA8: retValue = mov_r_direct(0, getCodeByte()); break;
            case (byte)0xA9: retValue = mov_r_direct(1, getCodeByte()); break;
            case (byte)0xAA: retValue = mov_r_direct(2, getCodeByte()); break;
            case (byte)0xAB: retValue = mov_r_direct(3, getCodeByte()); break;
            case (byte)0xAC: retValue = mov_r_direct(4, getCodeByte()); break;
            case (byte)0xAD: retValue = mov_r_direct(5, getCodeByte()); break;
            case (byte)0xAE: retValue = mov_r_direct(6, getCodeByte()); break;
            case (byte)0xAF: retValue = mov_r_direct(7, getCodeByte()); break;
            case (byte)0xB0: retValue = anl_c(getCodeByte(), true); break;
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
            case (byte)0xE5: retValue = mov_a_direct(getCodeByte()); break;
            case (byte)0xE6: retValue = mov_a_indirect(getR(0)); break;
            case (byte)0xE7: retValue = mov_a_indirect(getR(1)); break;
            case (byte)0xE8: retValue = mov_a_immediate(getR(0)); break;
            case (byte)0xE9: retValue = mov_a_immediate(getR(1)); break;
            case (byte)0xEA: retValue = mov_a_immediate(getR(2)); break;
            case (byte)0xEB: retValue = mov_a_immediate(getR(3)); break;
            case (byte)0xEC: retValue = mov_a_immediate(getR(4)); break;
            case (byte)0xED: retValue = mov_a_immediate(getR(5)); break;
            case (byte)0xEE: retValue = mov_a_immediate(getR(6)); break;
            case (byte)0xEF: retValue = mov_a_immediate(getR(7)); break;
            case (byte)0xF0: break;
            case (byte)0xF1: retValue = acall(currentInstruction, getCodeByte()); break;
            case (byte)0xF2: break;
            case (byte)0xF3: break;
            case (byte)0xF4: break;
            case (byte)0xF5: retValue = mov_direct_a(getCodeByte()); break;
            case (byte)0xF6: retValue = mov_indirect_immediate(getR(0), this.state.sfrs.A.getValue()); break;
            case (byte)0xF7: retValue = mov_indirect_immediate(getR(1), this.state.sfrs.A.getValue()); break;
            case (byte)0xF8: retValue = mov_r_immediate(0, this.state.sfrs.A.getValue()); break;
            case (byte)0xF9: retValue = mov_r_immediate(1, this.state.sfrs.A.getValue()); break;
            case (byte)0xFA: retValue = mov_r_immediate(2, this.state.sfrs.A.getValue()); break;
            case (byte)0xFB: retValue = mov_r_immediate(3, this.state.sfrs.A.getValue()); break;
            case (byte)0xFC: retValue = mov_r_immediate(4, this.state.sfrs.A.getValue()); break;
            case (byte)0xFD: retValue = mov_r_immediate(5, this.state.sfrs.A.getValue()); break;
            case (byte)0xFE: retValue = mov_r_immediate(6, this.state.sfrs.A.getValue()); break;
            case (byte)0xFF: retValue = mov_r_immediate(7, this.state.sfrs.A.getValue()); break;
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
        for (byte b = this.state.sfrs.A.getValue(); b != 0; b = (byte)(b & (b - 1))) parity = !parity;
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
        int retAddress;
        byte retBitMask;

        if (address < 0x80) { //the address is in the lower part of the internal RAM
            final int START_OF_BIT_MEMORY = 0x20;
            retAddress = START_OF_BIT_MEMORY + address / 8;
        } else {
            ByteRegister tmp;
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
        if (resultingAddress < 0 && exceptionOnUnderflow && !ignoreSOSU)
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
     * <b>Rotate Left</b><br>
     * This instruction rotates the accumulator one bit to the left.
     * Bit 7 of the accumulator is rotated to bit 0.<br>
     * @return the number of cycles (1)
     */
    private int rl_a() {
        final int a = this.state.sfrs.A.getValue() & 0xFF;
        int result = a << 1; //rotate a one bit to the left
        if ((a & 0x80) == 0x80) { //if bit 7 is set
            result |= 1; //set bit 0 in result
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
     * <b>Rotate Left (with) Carry</b><br>
     * This instruction rotates the accumulator one bit to the left. Bit 7 is rotated into C and C into bit 0.
     * @return the number of cycles (1)
     */
    private int rlc_a() {
        int a = this.state.sfrs.A.getValue() & 0xFF;
        final boolean oldC = this.state.sfrs.PSW.getBit(7);
        final boolean newC = (a & 0x80) == 0x80;
        a <<= 1;
        if (oldC) a |= 1;
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
     * <b>Increment (Indirect Address)</b><br>
     * Increment the byte at the indirect address by one. <br>
     * NOTE: The difference between a direct and an indirect address is that an indirect address always refers to the
     * "normal" RAM whereas a direct address refers to the SFR area when it is >= 0x80.
     * @param indirectAddress
     *     the direct address; must be < 0x80 or the address of a SFR
     * @return the number of cycles (1)
     */
    private int inc_indirect(byte indirectAddress) {
        this.state.internalRAM.set(indirectAddress & 0xFF,
                (byte)(this.state.internalRAM.get(indirectAddress & 0xFF) + 1));
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
     * <b>Decrement (Register)</b><br>
     * Decrement a register by one.
     * @param r
     *     the register
     * @return the number of cycles (1)
     */
    private int dec(ByteRegister r) {
        r.setValue((byte)(r.getValue() - 1));
        return 1;
    }

    /**
     * <b>Decrement (Direct Address)</b><br>
     * Decrement the byte at the direct address by one.
     * @param directAddress
     *     the direct address; must be < 0x80 or the address of a SFR
     * @return the number of cycles (1)
     * @throws IndexOutOfBoundsException when an address in the SFR memory is used that does not contain an SFR
     */
    private int dec(byte directAddress) throws IndexOutOfBoundsException {
        setDirectAddress(directAddress, (byte)(getDirectAddress(directAddress) - 1));
        return 1;
    }


    /**
     * <b>Decrement (Indirect Address)</b><br>
     * Decrement the byte at the indirect address by one.<br>
     * NOTE: The difference between a direct and an indirect address is that an indirect address always refers to the
     * "normal" RAM whereas a direct address refers to the SFR area when it is >= 0x80.
     * @param indirectAddress
     *     the direct address; must be < 0x80 or the address of a SFR
     * @return the number of cycles (1)
     */
    private int dec_indirect(byte indirectAddress) {
        this.state.internalRAM.set(indirectAddress & 0xFF,
                (byte)(this.state.internalRAM.get(indirectAddress & 0xFF) - 1));
        return 1;
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
     * <b>Jump (if A == 0)</b><br>
     * Jump by a certain offset if the accumulator equals zero.
     * @param offset the offset to jump by if the accumulator is 0
     * @return the number of cycles (2)
     */
    private int jz(byte offset) {
        if (this.state.sfrs.A.getValue() == (byte)0) jumpToOffset(offset);
        return 2;
    }

    /**
     * <b>Jump (if A != 0)</b><br>
     * Jump by a certain offset if the accumulator does not equal zero.
     * @param offset the offset to jump by if the accumulator is not 0
     * @return the number of cycles (2)
     */
    private int jnz(byte offset) {
        if (this.state.sfrs.A.getValue() != (byte)0) jumpToOffset(offset);
        return 2;
    }

    /**
     * <b>JMP (@A+DPTR)</b><br>
     * Add the content of the accumulator to the content of the DPTR (data pointer) and jump to the resulting address.
     * @return the number of cycles (2)
     */
    private int jmp_a_dptr() {
        final int dptr = this.state.sfrs.DPH.getValue() << 8 & 0xFF00 | this.state.sfrs.DPL.getValue() & 0xFF;
        final int pc = dptr + (this.state.sfrs.A.getValue() & 0xFF);
        this.state.PCH.setValue((byte) (pc >>> 8));
        this.state.PCL.setValue((byte)pc);
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
     * <b>Return (from a LCALL/ACALL)</b><br>
     * Get a code memory address (2 bytes) from the stack and jump to that address.
     * @return the number of cycles (2)
     */
    private int ret() {
        ljmp(_pop(!this.ignoreSOSU), _pop(!this.ignoreSOSU));
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
        if (resultingAddress > 0xFF && !ignoreSOSU)
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
        setDirectAddress(direct, _pop(!ignoreSOSU));
        return 2;
    }

    /**
     * <b>ADD (#immediateValue)</b><br>
     * Add an immediate value to the accumulator and store the result in the accumulator.
     * @param immediate the value to be added to the accumulator
     * @return the number of cycles (1)
     */
    private int add_immediate(byte immediate) {
        final byte bA = this.state.sfrs.A.getValue();
        final int a = bA & 0xFF;
        int result = a + (immediate & 0xFF);
        this.state.sfrs.PSW.setBit(result > 0xFF, 7); //bit 7 is the C (carry) flag
        //bit 6 is the AC (auxiliary carry) flag
        //it is set, if there is a carry out of bit 3
        this.state.sfrs.PSW.setBit((a & 0xF) + (immediate & 0xF) > 0xF, 6);
        //bit 2 is the OV (overflow) flag
        //(I think) it is set, when the two addends have the same sign, but the result's sign differs
        //In actual hardware this is implemented using an XOR of the 7th and the 8th bit's carry
        this.state.sfrs.PSW.setBit((bA & 0x80) == (immediate & 0x80) && (result & 0x80) != (bA & 0x80), 2);
        this.state.sfrs.A.setValue((byte)result);
        return 1;
    }

    /**
     * <b>ADD (@Ri)</b><br>
     * Add an indirectly addressed value to the accumulator and store the result in the accumulator.
     * @param indirectAddress the address (usually the content of either R0 or R1)
     * @return the number of cycles (1)
     * @see #add_immediate(byte)
     */
    private int add_indirect(byte indirectAddress) {
        return add_immediate(this.state.internalRAM.get(indirectAddress & 0xFF));
    }

    /**
     * <b>ADD (direct address)</b><br>
     * Add a directly addressed value to the accumulator and store the result in the accumulator.
     * @param directAddress the address
     * @return the number of cycles (1)
     * @see #getDirectAddress(byte)
     * @see #add_immediate(byte)
     */
    private int add_direct(byte directAddress) {
        return add_immediate(getDirectAddress(directAddress));
    }

    /**
     * <b>Add (RX)</b><br>
     * Add the value of a R register to the accumulator and store the result in the accumulator.
     * @param ordinal the R register's number; must be >= 0 and <= 7
     * @return the number of cycles (1)
     * @see #add_immediate(byte)
     */
    private int add_r(int ordinal) {
        return add_immediate(getR(ordinal));
    }

    /**
     * <b>Add (#immediateValue and C flag)</b><br>
     * Add an immediate value and the carry flag to the accumulator and store the result in the accumulator.
     * @param immediateValue the value to be added to the accumulator
     * @return the number of cycles (1)
     * @see #add_immediate(byte)
     */
    private int addc_immediate(byte immediateValue) {
        return add_immediate((byte)(immediateValue + (byte)(this.state.sfrs.PSW.getBit(7) ? 1 : 0)));
    }

    /**
     * <b>ADDC (@Ri)</b><br>
     * Add an indirectly addressed value and the carry flag to the accumulator and store the result in the accumulator.
     * @param indirectAddress the address (usually the content of either R0 or R1)
     * @return the number of cycles (1)
     * @see #addc_immediate(byte)
     */
    private int addc_indirect(byte indirectAddress) {
        return addc_immediate(this.state.internalRAM.get(indirectAddress & 0xFF));
    }

    /**
     * <b>ADDC (direct address)</b><br>
     * Add a directly addressed value and the carry flag to the accumulator and store the result in the accumulator.
     * @param directAddress the address
     * @return the number of cycles (1)
     * @see #getDirectAddress(byte)
     * @see #addc_immediate(byte)
     */
    private int addc_direct(byte directAddress) {
        return addc_immediate(getDirectAddress(directAddress));
    }

    /**
     * <b>Add (RX)</b><br>
     * Add the value of a R register and the carry flag to the accumulator and store the result in the accumulator.
     * @param ordinal the R register's number; must be >= 0 and <= 7
     * @return the number of cycles (1)
     * @see #addc_immediate(byte)
     */
    private int addc_r(int ordinal) {
        return addc_immediate(getR(ordinal));
    }

    /**
     * <b>ORL (direct, #immediate)</b>
     * <br>
     * Perform a logical OR on the byte at the direct address and the immediate byte; store the result at the direct
     * address.
     * @param directAddress the direct address
     * @param immediateValue the immediate value
     * @return the number of cycles (2)
     * @see #setDirectAddress(byte, byte)
     * @see #getDirectAddress(byte)
     */
    private int orl(byte directAddress, byte immediateValue) {
        setDirectAddress(directAddress, (byte)(getDirectAddress(directAddress)|immediateValue));
        return 2;
    }

    /**
     * <b>ORL (direct, A)</b>
     * <br>
     * Perform a logical OR on the byte at the direct address and the accumulator; store the result at the direct
     * address.
     * @param directAddress the direct address
     * @return the number of cycles (1)
     * @see #orl(byte, byte)
     */
    private int orl_direct_a(byte directAddress) {
        orl(directAddress, this.state.sfrs.A.getValue());
        return 1;
    }

    /**
     * <b>ORL (A, #immediate)</b><br>
     * Perform a logical OR on the accumulator and the immediate value; store the result in the accumulator.
     * @param immediateValue the immediate value
     * @return the number of cycles (1)
     * @see #orl(byte, byte)
     */
    private int orl_a_immediate(byte immediateValue) {
        orl(this.state.sfrs.getAddress(this.state.sfrs.A), immediateValue);
        return 1;
    }

    /**
     * <b>ORL (A, direct)</b><br>
     * Perform a logical OR on the accumulator and the byte at the direct address; store the result in the accumulator.
     * @param directAddress the direct address to be used
     * @return the number of cycles (1)
     * @see #orl_a_immediate(byte)
     */
    private int orl_a(byte directAddress) {
        return orl_a_immediate(getDirectAddress(directAddress));
    }

    /**
     * <b>ORL (A, @Ri)</b><br>
     * Perform a logical OR on the accumulator and the byte at the direct address; store the result in the accumulator.
     * @param indirectAddress the indirect address to be used
     * @return the number of cycles (1)
     * @see #orl_a_immediate(byte)
     */
    private int orl_a_indirect(byte indirectAddress) {
        return orl_a_immediate(this.state.internalRAM.get(indirectAddress & 0xFF));
    }

    /**
     * <b>ORL (C, (/)bit)</b><br>
     * Perform a logical OR on C and the specified bit (or the negated bit); store the result in C.
     * @param bitAddress the bit to be used
     * @param negateBit if this is {@code true}, the bit will be negated, before the OR is performed
     * @return the number of cycles (2)
     */
    private int orl_c(byte bitAddress, boolean negateBit) {
        boolean bit = getBit(bitAddress);
        bit = negateBit ? !bit : bit;
        boolean c = this.state.sfrs.PSW.getBit(7);
        this.state.sfrs.PSW.setBit(bit || c, 7);
        return 2;
    }

    /**
     * <b>XRL (direct, #immediate)</b>
     * <br>
     * Perform a logical XOR on the byte at the direct address and the immediate byte; store the result at the direct
     * address.
     * @param directAddress the direct address
     * @param immediateValue the immediate value
     * @return the number of cycles (2)
     * @see #setDirectAddress(byte, byte)
     * @see #getDirectAddress(byte)
     */
    private int xrl(byte directAddress, byte immediateValue) {
        setDirectAddress(directAddress, (byte)(getDirectAddress(directAddress) ^ immediateValue));
        return 2;
    }

    /**
     * <b>XRL (direct, A)</b>
     * <br>
     * Perform a logical XOR on the byte at the direct address and the accumulator; store the result at the direct
     * address.
     * @param directAddress the direct address
     * @return the number of cycles (1)
     * @see #xrl(byte, byte)
     */
    private int xrl_direct_a(byte directAddress) {
        xrl(directAddress, this.state.sfrs.A.getValue());
        return 1;
    }

    /**
     * <b>XRL (A, #immediate)</b><br>
     * Perform a logical XOR on the accumulator and the immediate value; store the result in the accumulator.
     * @param immediateValue the immediate value
     * @return the number of cycles (1)
     * @see #xrl(byte, byte)
     */
    private int xrl_a_immediate(byte immediateValue) {
        xrl(this.state.sfrs.getAddress(this.state.sfrs.A), immediateValue);
        return 1;
    }

    /**
     * <b>XRL (A, direct)</b><br>
     * Perform a logical XOR on the accumulator and the byte at the direct address; store the result in the accumulator.
     * @param directAddress the direct address to be used
     * @return the number of cycles (1)
     * @see #xrl_a_immediate(byte)
     */
    private int xrl_a(byte directAddress) {
        return xrl_a_immediate(getDirectAddress(directAddress));
    }

    /**
     * <b>XRL (A, @Ri)</b><br>
     * Perform a logical XOR on the accumulator and the byte at the direct address; store the result in the accumulator.
     * @param indirectAddress the indirect address to be used
     * @return the number of cycles (1)
     * @see #xrl_a_immediate(byte)
     */
    private int xrl_a_indirect(byte indirectAddress) {
        return xrl_a_immediate(this.state.internalRAM.get(indirectAddress & 0xFF));
    }

    /**
     * <b>ANL (direct, #immediate)</b>
     * <br>
     * Perform a logical AND on the byte at the direct address and the immediate byte; store the result at the direct
     * address.
     * @param directAddress the direct address
     * @param immediateValue the immediate value
     * @return the number of cycles (2)
     * @see #setDirectAddress(byte, byte)
     * @see #getDirectAddress(byte)
     */
    private int anl(byte directAddress, byte immediateValue) {
        setDirectAddress(directAddress, (byte)(getDirectAddress(directAddress) & immediateValue));
        return 2;
    }

    /**
     * <b>ANL (direct, A)</b>
     * <br>
     * Perform a logical AND on the byte at the direct address and the accumulator; store the result at the direct
     * address.
     * @param directAddress the direct address
     * @return the number of cycles (1)
     * @see #anl(byte, byte)
     */
    private int anl_direct_a(byte directAddress) {
        anl(directAddress, this.state.sfrs.A.getValue());
        return 1;
    }

    /**
     * <b>ANL (A, #immediate)</b><br>
     * Perform a logical AND on the accumulator and the immediate value; store the result in the accumulator.
     * @param immediateValue the immediate value
     * @return the number of cycles (1)
     * @see #anl(byte, byte)
     */
    private int anl_a_immediate(byte immediateValue) {
        anl(this.state.sfrs.getAddress(this.state.sfrs.A), immediateValue);
        return 1;
    }

    /**
     * <b>ANL (A, direct)</b><br>
     * Perform a logical AND on the accumulator and the byte at the direct address; store the result in the accumulator.
     * @param directAddress the direct address to be used
     * @return the number of cycles (1)
     * @see #anl_a_immediate(byte)
     */
    private int anl_a(byte directAddress) {
        return anl_a_immediate(getDirectAddress(directAddress));
    }

    /**
     * <b>ANL (A, @Ri)</b><br>
     * Perform a logical AND on the accumulator and the byte at the direct address; store the result in the accumulator.
     * @param indirectAddress the indirect address to be used
     * @return the number of cycles (1)
     * @see #anl_a_immediate(byte)
     */
    private int anl_a_indirect(byte indirectAddress) {
        return anl_a_immediate(this.state.internalRAM.get(indirectAddress & 0xFF));
    }

    /**
     * <b>ANL (C, (/)bit)</b><br>
     * Perform a logical AND on C and the specified bit (or the negated bit); store the result in C.
     * @param bitAddress the bit to be used
     * @param negateBit if this is {@code true}, the bit will be negated, before the OR is performed
     * @return the number of cycles (2)
     */
    private int anl_c(byte bitAddress, boolean negateBit) {
        boolean bit = getBit(bitAddress);
        bit = negateBit ? !bit : bit;
        boolean c = this.state.sfrs.PSW.getBit(7);
        this.state.sfrs.PSW.setBit(bit && c, 7);
        return 2;
    }

    /**
     * <b>MOV (@Ri, #immediate)</b>
     * @param indirectAddress the indirect address to be used (typically the content of R0/R1)
     * @param immediateValue the value to be stored at this address
     * @return the number of cycles (1)
     */
    private int mov_indirect_immediate(byte indirectAddress, byte immediateValue) {
        this.state.internalRAM.set(indirectAddress & 0xFF, immediateValue);
        return 1;
    }

    /**
     * <b>MOV (@Ri, direct)</b><br>
     * @param indirectAddress the indirect address to be used (typically the content of R0/R1)
     * @param directAddress the direct address from which the value that is stored at the indirect address is read
     * @return the number of cycles (2)
     */
    private int mov_indirect_direct(byte indirectAddress, byte directAddress) {
        mov_indirect_immediate(indirectAddress, getDirectAddress(directAddress));
        return 2;
    }

    /**
     * <b>MOV (direct, #immediate)</b>
     * @param directAddress the direct address at which the immediate value is stored
     * @param immediateValue the immediate value to be stored at said address
     * @return the number of cycles (2)
     */
    private int mov_direct_immediate(byte directAddress, byte immediateValue) {
        setDirectAddress(directAddress, immediateValue);
        return 2;
    }

    /**
     * <b>MOV (A, #immediate)</b>
     * @param immediateValue the immediate value to be moved to the accumulator
     * @return the number of cycles (1)
     */
    private int mov_a_immediate(byte immediateValue) {
        mov_direct_immediate(this.state.sfrs.getAddress(this.state.sfrs.A), immediateValue);
        return 1;
    }

    /**
     * <b>MOV (A, @Ri)</b>
     * @param indirectAddress the indirect address to be used (typically the content of R0/R1)
     * @return the number of cycles (1)
     */
    private int mov_a_indirect(byte indirectAddress) {
        return mov_a_immediate(this.state.internalRAM.get(indirectAddress & 0xFF));
    }

    /**
     * <b>MOV (A, direct)</b>
     * @param directAddress the direct address whose value is stored in the accumulator
     * @return the number of cycles (1)
     */
    private int mov_a_direct(byte directAddress) {
        return mov_a_immediate(getDirectAddress(directAddress));
    }

    /**
     * <b>MOV (bit, C)</b>
     * @param bitAddress the bit address to which the value of the carry flag is copied
     * @return the number of cycles (2)
     */
    private int mov_bit_c(byte bitAddress) {
        setBit(this.state.sfrs.PSW.getBit(7), bitAddress);
        return 2;
    }

    /**
     * <b>MOV (C, bit)</b>
     * @param bitAddress the bit address whose value is copied to the carry flag
     * @return the number of cycles (2)
     */
    private int mov_c_bit(byte bitAddress) {
        this.state.sfrs.PSW.setBit(getBit(bitAddress), 7);
        return 2;
    }

    /**
     * <b>MOV (direct, direct)</b><br>
     * NOTE: The reason the parameters are flipped is that they appear in that order in machine code. In assembly they
     * are written differently:<br>
     *     Machine code: sourceAddress     , destinationAddress
     *     Assembly    : destinationAddress, sourceAddress
     * @param srcDirect the direct source address
     * @param destDirect the indirect source address
     * @return the number of cycles (2)
     */
    private int mov_direct_direct(byte srcDirect, byte destDirect) {
        mov_direct_immediate(destDirect, getDirectAddress(srcDirect));
        return 2;
    }

    /**
     * <b>MOV (direct, @Ri)</b>
     * @param directAddress the direct address to be used
     * @param indirectAddress the indirect address to be used (typically the content of R0/R1)
     * @return the number of cycles (2)
     */
    private int mov_direct_indirect(byte directAddress, byte indirectAddress) {
        mov_direct_immediate(directAddress, this.state.internalRAM.get(indirectAddress & 0xFF));
        return 2;
    }

    /**
     * <b>MOV (DPTR, #immediate)</b>
     * @param immediateHigh the new value of DPH
     * @param immediateLow the new value of DPL
     * @return the number of cycles (2)
     */
    private int mov_dptr(byte immediateHigh, byte immediateLow) {
        this.state.sfrs.DPH.setValue(immediateHigh);
        this.state.sfrs.DPL.setValue(immediateLow);
        return 2;
    }

    /**
     * <b>MOV (Rn, #immediate)</b>
     * @param ordinal specifies the R register (R<sub>ordinal</sub>); must be >= 0 and <= 7
     * @param immediateValue the immediate value to be stored at the specified R register
     * @return the number of cycles (1)
     */
    private int mov_r_immediate(int ordinal, byte immediateValue) {
        this.state.internalRAM.set(getRAddress(ordinal), immediateValue);
        return 1;
    }

    /**
     * <b>MOV (Rn, direct)</b>
     * @param ordinal specifies the R register (R<sub>ordinal</sub>); must be >= 0 and <= 7
     * @param directAddress the direct address whose content is copied to the specified R register
     * @return the number of cycles (2)
     */
    private int mov_r_direct(int ordinal, byte directAddress) {
        mov_r_immediate(ordinal, getDirectAddress(directAddress));
        return 2;
    }

    /**
     * <b>MOV (direct, A)</b>
     * @param directAddress the direct address whose content is moved to the accumulator
     * @return the number of cycles (1)
     */
    private int mov_direct_a(byte directAddress) {
        mov_direct_immediate(directAddress, this.state.sfrs.A.getValue());
        return 1;
    }
}