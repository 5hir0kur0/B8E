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
 *   all arithmetic operations, like e.g. bit shifting):<br>
 *   <code>(int)(byte)0x80 = 0xFFFFFF80</code><br>
 *   In order to get the correct result, we have to do <code>&lt;result&gt; & 0xFF</code> (in this case)
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

    /**
     * Create a new 8051 micro controller object.<br>
     * @param externalRAM
     *     the external RAM accessible through the {@code MOVX} command; {@code null} is a valid value and implies
     *     that there is no external RAM (and thus, all {@code MOVX}-instructions should fail)
     * @param codeMemory
     *     the 8051's "code memory"; The instructions will be read from this object (must not be {@code null})
     *     The size must be 65536 bytes.
     */
    public MC8051(ROM codeMemory, RAM externalRAM) {
        this(new State8051(codeMemory, externalRAM));
    }

    /**
     * Start with a specific state.
     * @param state
     *     the state of the new {@code Emulator}; must not be {@code null}
     */
    public MC8051(State8051 state) {
        this.state = Objects.requireNonNull(state, "trying to initialize MC8051 with empty state");
        this.state.setRRegisters(generateRRegisters());
        this.state.prevP3_2 = this.state.sfrs.P3.getBit(2);
        this.state.prevP3_3 = this.state.sfrs.P3.getBit(3);
        this.state.runningInterruptPriority = -1;
    }

    /**
     * Return a list of all the {@code Register}s.
     *
     * This method is not intended to be used frequently. To observe the {@code Register} you should register a change
     * listener.
     * @return
     *     a {@code List} of all the {@code Register}s
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
     * @return
     *     the number of cycles this instruction takes
     * @throws EmulatorException
     */
    @Override
    public int next() throws EmulatorException {
        byte currentInstruction = getCodeByte();
        int retValue = -1;
        try {
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
                case       0x32: retValue = reti(); break;
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
                case (byte)0x80: retValue = sjmp(getCodeByte()); break;
                case (byte)0x81: retValue = ajmp(currentInstruction, getCodeByte()); break;
                case (byte)0x82: retValue = anl_c(getCodeByte(), false); break;
                case (byte)0x83: retValue = movc_a(this.state.PCH, this.state.PCL); break;
                case (byte)0x84: retValue = div_ab(); break;
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
                case (byte)0x93: retValue = movc_a(this.state.sfrs.DPH, this.state.sfrs.DPL); break;
                case (byte)0x94: retValue = subb_immediate(getCodeByte()); break;
                case (byte)0x95: retValue = subb_direct(getCodeByte()); break;
                case (byte)0x96: retValue = subb_indirect(getR(0)); break;
                case (byte)0x97: retValue = subb_indirect(getR(1)); break;
                case (byte)0x98: retValue = subb_r(0); break;
                case (byte)0x99: retValue = subb_r(1); break;
                case (byte)0x9A: retValue = subb_r(2); break;
                case (byte)0x9B: retValue = subb_r(3); break;
                case (byte)0x9C: retValue = subb_r(4); break;
                case (byte)0x9D: retValue = subb_r(5); break;
                case (byte)0x9E: retValue = subb_r(6); break;
                case (byte)0x9F: retValue = subb_r(7); break;
                case (byte)0xA0: retValue = orl_c(getCodeByte(), true); break;
                case (byte)0xA1: retValue = ajmp(currentInstruction, getCodeByte()); break;
                case (byte)0xA2: retValue = mov_c_bit(getCodeByte()); break;
                case (byte)0xA3: retValue = inc_dptr(); break;
                case (byte)0xA4: retValue = mul_ab(); break;
                case (byte)0xA5: retValue = reserved();
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
                case (byte)0xB2: retValue = cpl(getCodeByte()); break;
                case (byte)0xB3: retValue = cpl_c(); break;
                case (byte)0xB4: retValue = cjne_a_immediate(getCodeByte(), getCodeByte()); break;
                case (byte)0xB5: retValue = cjne_a_direct(getCodeByte(), getCodeByte()); break;
                case (byte)0xB6: retValue = cjne_indirect_immediate(getR(0), getCodeByte(), getCodeByte()); break;
                case (byte)0xB7: retValue = cjne_indirect_immediate(getR(1), getCodeByte(), getCodeByte()); break;
                case (byte)0xB8: retValue = cjne_r_immediate(0, getCodeByte(), getCodeByte()); break;
                case (byte)0xB9: retValue = cjne_r_immediate(1, getCodeByte(), getCodeByte()); break;
                case (byte)0xBA: retValue = cjne_r_immediate(2, getCodeByte(), getCodeByte()); break;
                case (byte)0xBB: retValue = cjne_r_immediate(3, getCodeByte(), getCodeByte()); break;
                case (byte)0xBC: retValue = cjne_r_immediate(4, getCodeByte(), getCodeByte()); break;
                case (byte)0xBD: retValue = cjne_r_immediate(5, getCodeByte(), getCodeByte()); break;
                case (byte)0xBE: retValue = cjne_r_immediate(6, getCodeByte(), getCodeByte()); break;
                case (byte)0xBF: retValue = cjne_r_immediate(7, getCodeByte(), getCodeByte()); break;
                case (byte)0xC0: retValue = pop(getCodeByte()); break;
                case (byte)0xC1: retValue = ajmp(currentInstruction, getCodeByte()); break;
                case (byte)0xC2: retValue = clr(getCodeByte()); break;
                case (byte)0xC3: retValue = clr_c(); break;
                case (byte)0xC4: retValue = swap_a(); break;
                case (byte)0xC5: retValue = xch_a_direct(getCodeByte()); break;
                case (byte)0xC6: retValue = xch_a_indirect(getR(0)); break;
                case (byte)0xC7: retValue = xch_a_indirect(getR(1)); break;
                case (byte)0xC8: retValue = xch_a_r(0); break;
                case (byte)0xC9: retValue = xch_a_r(1); break;
                case (byte)0xCA: retValue = xch_a_r(2); break;
                case (byte)0xCB: retValue = xch_a_r(3); break;
                case (byte)0xCC: retValue = xch_a_r(4); break;
                case (byte)0xCD: retValue = xch_a_r(5); break;
                case (byte)0xCE: retValue = xch_a_r(6); break;
                case (byte)0xCF: retValue = xch_a_r(7); break;
                case (byte)0xD0: retValue = push(getCodeByte()); break;
                case (byte)0xD1: retValue = acall(currentInstruction, getCodeByte()); break;
                case (byte)0xD2: retValue = setb(getCodeByte()); break;
                case (byte)0xD3: retValue = setb_c(); break;
                case (byte)0xD4: retValue = da_a(); break;
                case (byte)0xD5: retValue = djnz(getCodeByte(), getCodeByte()); break;
                case (byte)0xD6: retValue = xchd_a(getR(0)); break;
                case (byte)0xD7: retValue = xchd_a(getR(1)); break;
                case (byte)0xD8: retValue = djnz_r(0, getCodeByte()); break;
                case (byte)0xD9: retValue = djnz_r(1, getCodeByte()); break;
                case (byte)0xDA: retValue = djnz_r(2, getCodeByte()); break;
                case (byte)0xDB: retValue = djnz_r(3, getCodeByte()); break;
                case (byte)0xDC: retValue = djnz_r(4, getCodeByte()); break;
                case (byte)0xDD: retValue = djnz_r(5, getCodeByte()); break;
                case (byte)0xDE: retValue = djnz_r(6, getCodeByte()); break;
                case (byte)0xDF: retValue = djnz_r(7, getCodeByte()); break;
                case (byte)0xE0: retValue = movx_a_dptr(); break;
                case (byte)0xE1: retValue = ajmp(currentInstruction, getCodeByte()); break;
                case (byte)0xE2: retValue = movx_a_indirect(getR(0)); break;
                case (byte)0xE3: retValue = movx_a_indirect(getR(1)); break;
                case (byte)0xE4: retValue = clr_a(); break;
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
                case (byte)0xF0: retValue = movx_dptr_a(); break;
                case (byte)0xF1: retValue = acall(currentInstruction, getCodeByte()); break;
                case (byte)0xF2: retValue = movx_indirect_a(getR(0)); break;
                case (byte)0xF3: retValue = movx_indirect_a(getR(1)); break;
                case (byte)0xF4: retValue = cpl_a(); break;
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
        } catch (Exception e) {
             //TODO: Log exception
             throw new EmulatorException(e);
        } finally {
            updateParityFlag();
            updateTimers(retValue);
            handleInterrupts();
            //The value of the R registers can be changed through memory.
            //In order to ensure that the GUI displays the correct values, the setter in each R register is called every
            //time, so that it fires property-change-events
            for (int i = 0; i < 8; ++i) this.state.getR(i).setValue(this.state.getR(i).getValue());
        }
        return retValue;
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
     * @return
     *     the retrieved byte
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
     * @return
     *     the register's address
     * @throws IllegalArgumentException
     *     when given an invalid ordinal
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
     *     the R register to be used (E.g. '5' implies R5)
     * @return
     *     R<sup>ordinal</sup>'s value
     * @throws IllegalArgumentException
     *     when given an illegal ordinal
     */
    private byte getR(int ordinal) {
        return this.state.internalRAM.get(getRAddress(ordinal));
    }

    /**
     * Update the parity flag in the PSW.
     * It is set so that the number of bits set to one plus the parity flag is even.
     * NOTE: This algorithm was inspired by
     * <a href="http://www.geeksforgeeks.org/write-a-c-program-to-find-the-parity-of-an-unsigned-integer/">
     *     http://www.geeksforgeeks.org/write-a-c-program-to-find-the-parity-of-an-unsigned-integer/
     * </a>
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
     * Update the values of the SFR TH0, TL0, TH1, TL1.
     * NOTE: The implementation of split mode is a bit ugly and untested, so I'm not sure if it works...
     * 13-bit mode is also untested.
     * Those modes are basically deprecated and not used most of the time.
     * @param cycles
     *     the number of cycles (used when the timers are used as "timers" [as opposed to "counters"])
     */
    private void updateTimers(int cycles) {
        if (cycles < 0) cycles = 1;
        //TMOD bits
        final byte tmod     = this.state.sfrs.TMOD.getValue();
        final boolean GATE1 = (tmod & 0x80) != 0;
        final boolean CT1   = (tmod & 0x40) != 0;
        //final boolean T1M1  = (tmod & 0x20) != 0;
        //final boolean T1M0  = (tmod & 0x10) != 0;
        final boolean GATE0 = (tmod & 0x08) != 0;
        final boolean CT0   = (tmod & 0x04) != 0;
        //final boolean T0M1  = (tmod & 0x02) != 0;
        //final boolean T0M0  = (tmod & 0x01) != 0;
        final int MODE0     =  tmod & 0x03;
        final int MODE1     =  (tmod & 0x30) >> 4 ;
        //TCON flags
        final boolean TR1   = this.state.sfrs.TCON.getBit(6);
        final boolean TR0   = this.state.sfrs.TCON.getBit(4);

        final int howMuch0;
        final int howMuch1;
        if (MODE0 != 3) {
            if (MODE1 == 3) throw new IllegalStateException("Timer 1 cannot be put in mode 3 when timer 0 isn't");
            this.state.TMOD_OLD = tmod;
            this.state.TR1_OLD = TR1;
            if (CT0) //timer 0 counts events
                howMuch0 = this.state.sfrs.P3.getBit(4) ? 1 : 0; //P3.4 is T0
            else //timer 0 counts cycles
                howMuch0 = cycles;

            if (CT1) //timer 1 counts events
                howMuch1 = this.state.sfrs.P3.getBit(5) ? 1 : 0; //P3.5 is T1
            else //timer 1 counts cycles
                howMuch1 = cycles;

            if ((!GATE0 || this.state.sfrs.P3.getBit(2)) && TR0) //P3.2 is INT0
                incrementTimer(this.state.sfrs.TH0, this.state.sfrs.TL0, 5, MODE0, howMuch0); //bit 5 in TCON is TF0
            if ((!GATE1 || this.state.sfrs.P3.getBit(3)) && TR1) //P3.3 is INT1
                incrementTimer(this.state.sfrs.TH1, this.state.sfrs.TL1, 7, MODE1, howMuch1); //bit 7 in TCON is TF1
        } else { //mode 3 (split mode)
            if (MODE1 != 3) throw new IllegalStateException("When timer 0 is in mode 3, timer 1 must be too");
            final boolean GATE1_OLD = (this.state.TMOD_OLD & 0x80) != 0;
            final boolean CT1_OLD   = (this.state.TMOD_OLD & 0x40) != 0;
            final int MODE1_OLD     = (this.state.TMOD_OLD & 0x30) >> 4;
            if (CT1_OLD)
                throw new IllegalStateException("Timer 1 cannot count events when timer 0 is in mode 3");
            if (GATE1_OLD)
                throw new IllegalStateException("Timer 1 cannot be used with GATE when timer 0 is in mode 3");
            if (this.state.TR1_OLD)
                //-1 is given, because timer 1 does not have an OV flag when timer 0 is in mode 3
                incrementTimer(this.state.sfrs.TH1, this.state.sfrs.TL1, -1, MODE1_OLD, cycles);

            if (CT0) //timer 0 (low) counts events
                howMuch0 = this.state.sfrs.P3.getBit(4) ? 1 : 0; //P3.4 is T0
            else //timer 0 (low) counts cycles
                howMuch0 = cycles;

            if (CT1) //timer 1 (high) counts events
                howMuch1 = this.state.sfrs.P3.getBit(5) ? 1 : 0; //P3.5 is T1
            else //timer 1 (high) counts cycles
                howMuch1 = cycles;

            if ((!GATE0 || this.state.sfrs.P3.getBit(2)) && TR0) //P3.2 is INT0
                incrementTimer(null, this.state.sfrs.TL0, 5, MODE0, howMuch0); //bit 5 in TCON is TF0
            if ((!GATE1 || this.state.sfrs.P3.getBit(3)) && TR1) //P3.3 is INT1
                incrementTimer(this.state.sfrs.TH1, null, 7, MODE1, howMuch1); //bit 7 in TCON is TF1
        }
    }

    /**
     * Increment a timer register.
     * @param high
     *     the register's high byte (should be {@code null} for mode 3 when low is not {@code null})
     * @param low
     *     the registers's low byte (should be {@code null} for mode 3 when high is not {@code null})
     * @param ovflag
     *     the timer's overflow flag; must either be < 0 (when the timer has no overflow flag [this cannot
     *     happen in mode 3]) or >= 0 and <= 7
     * @param mode
     *     the timer mode; must be >= 0 and <= 3
     * @param howMuch
     *     specifies by how much the timer should be incremented; must be > 0
     */
    private void incrementTimer(ByteRegister high, ByteRegister low, int ovflag, int mode, int howMuch) {
        if (howMuch < 1) throw new IllegalArgumentException("Invalid timer increment value: "+howMuch);
        switch (mode) {
            case 0: //13-bit mode
                final byte oldHigh = high.getValue();
                byte h = oldHigh;
                byte l = low.getValue();
                for (int i = 0; i < howMuch; ++i) {
                    if (++l > 0x1F) { //only the last 5 bits of the timer are used
                        l = 0;
                        ++h;
                    }
                }
                if ((oldHigh & 0xFF) > (h & 0xFF) && ovflag > -1) this.state.sfrs.TCON.setBit(true, ovflag);
                high.setValue(h);
                low.setValue(l);
                break;
            case 1: //16-bit mode
                int timer = high.getValue() << 8 & 0xFF00 | low.getValue() & 0xFF;
                timer += howMuch;
                if (timer > 0xFFFF && ovflag > -1) this.state.sfrs.TCON.setBit(true, ovflag);
                high.setValue((byte)(timer >>> 8));
                low.setValue((byte)timer);
                break;
            case 2: //8-bit mode
                int timerLow = (low.getValue() & 0xFF) + howMuch;
                if (timerLow > 0xFF) {
                    if (ovflag > -1) this.state.sfrs.TCON.setBit(true, ovflag);
                    timerLow = high.getValue();
                }
                low.setValue((byte)timerLow);
                break;
            case 3: //split mode
                ByteRegister tmp = low;
                if (null == tmp) tmp = high;
                int newVal = (tmp.getValue() & 0xFF) + howMuch;
                if (newVal > 0xFF) {
                    this.state.sfrs.TCON.setBit(true, ovflag);
                }
                tmp.setValue((byte)newVal);
                break;
            default:
                throw new IllegalStateException("Invalid timer mode: "+mode); //this can basically never happen
        }
    }

    /**
     * This method is called after every instruction and evaluates whether an interrupt should occur.
     * The order in which interrupts are evaluated is the following:<br>
     * <ol>
     *     <li>External 0 Interrupt</li>
     *     <li>Timer 0 Interrupt</li>
     *     <li>External 1 Interrupt</li>
     *     <li>Timer 1 Interrupt</li>
     *     <li>Serial Interrupt (triggered by either the TI or the RI flag)</li>
     * </ol>
     * <br>
     * This order can be changed by changing the bits of the IP (interrupt priority) registers.
     * Note that the original 8051 only has two priorities: low and high
     * (as opposed to four priorities in newer models).
     * More info at: <a href="http://8052.com/tutint.phtml">http://8052.com/tutint.phtml</a><br>
     */
    private void handleInterrupts() {
        updateInterruptRequestFlags();

        final boolean EA = this.state.sfrs.IE.getBit(7); //global interrupt enable/disable
        if (!EA) return; //if interrupts are disables, there is nothing to do
        if (this.state.runningInterruptPriority == 1) return; // a interrupt of high priority cannot be cancelled
        final boolean ES  = this.state.sfrs.IE.getBit(4); //enable serial interrupt
        final boolean ET1 = this.state.sfrs.IE.getBit(3); //enable timer 1 interrupt
        final boolean EX1 = this.state.sfrs.IE.getBit(2); //enable external 1 interrupt
        final boolean ET0 = this.state.sfrs.IE.getBit(1); //enable timer 0 interrupt
        final boolean EX0 = this.state.sfrs.IE.getBit(0); //enable external 0 interrupt
        final BitAddressableByteRegister TCON = this.state.sfrs.TCON;
        final BitAddressableByteRegister SCON = this.state.sfrs.SCON;
        final BitAddressableByteRegister IP   = this.state.sfrs.IP;

        final boolean[] interruptRequests = {
                EX0 && TCON.getBit(1), // external interrupt 0 (IE0)
                ET0 && TCON.getBit(5), // timer 0 overflow (TF0)
                EX1 && TCON.getBit(3), // external interrupt 1 (IE1)
                ET1 && TCON.getBit(7), // timer 1 overflow (TF1)
                ES  && (SCON.getBit(0) || SCON.getBit(1)) // SCON.1 is TI (set when a byte has been transmitted
                                                          // through the serial port)
                                                          // SCON.0 is RI (set when a byte has been received through the
                                                          // serial port)
        };

        // the SCON-bits aren't cleared (that's why those indexes are set to -1)
        final int[] tconClear = { 1 /* IE0 */, 5 /* TF0 */, 3 /* IE1 */, 7 /* TF1 */, -1, -1 };

        final boolean[] priorities = {
                IP.getBit(0), // external interrupt 0 priority
                IP.getBit(1), // timer 0 interrupt priority
                IP.getBit(2), // external interrupt 1 priority
                IP.getBit(3), // timer 1 interrupt priority
                IP.getBit(4)  // serial interrupt priority
        };

        boolean priority = true; // check high priority interrupts first
        for (int helper = 0, i = 0; helper < 8; ++helper, i = helper % 4, priority = helper < 4) {
            if (priority != priorities[i]) continue;
            if (interruptRequests[i] && (priority || this.state.runningInterruptPriority == -1)) {
                // if we made it this far, we can execute our interrupt
                if (tconClear[i] >= 0) TCON.setBit(false, tconClear[i]); // clear request flag if necessary
                this.state.runningInterruptPriority = priority ? 1 : 0;
                if (this.state.runningInterruptPriority == 0)
                    this.state.runningInterruptInterruptedOtherInterrupt = true;
                interruptJump((char)(3 + i * 8));
                break; // we can only execute one interrupt at a time
            }
        }
    }

    /**
     * Update the interrupt request flags in {@code TCON} (except for the timer overflow flags; they are maintained by
     * {@code updateTimers()} and {@code incrementTimer()}.
     * The flags are updated regardless of whether their respective interrupts are enabled (because that's how MCU8051
     * IDE behaves). Furthermore, an interrupt request flag may be reset even though its interrupt service routine
     * has not been executed yet (for the same reason as above).
     * NOTE: The interrupt request flags  of the serial interface and the analog/digital converter
     * are NOT updated by this method as those features are not supported.
     */
    private void updateInterruptRequestFlags() {
        final boolean IT0  = this.state.sfrs.TCON.getBit(0);
        final boolean IT1  = this.state.sfrs.TCON.getBit(2);
        final boolean P3_2 = this.state.sfrs.P3.getBit(2);
        final boolean P3_3 = this.state.sfrs.P3.getBit(3);
        boolean IE0; // interrupt request flag of external interrupt 0; located at TCON.1
        boolean IE1; // interrupt request flag of external interrupt 1; located at TCON.3
        if (!IT0) { // external interrupt 0 (P3.2) is triggered by level (instead of transition)
            IE0 = !P3_2;
        } else {
            IE0 = this.state.prevP3_2 && !P3_2; //this expression checks for a falling transition at P3.2
        }
        if (!IT1) { // external interrupt 1 (P3.3) is triggered by level (instead of transition)
            IE1 = !P3_3;
        } else {
            IE1 = this.state.prevP3_3 && !P3_3; //this expression checks for a falling transition at P3.3
        }
        this.state.sfrs.TCON.setBit(IE0, 1);
        this.state.sfrs.TCON.setBit(IE1, 3);
        this.state.prevP3_2 = P3_2;
        this.state.prevP3_3 = P3_3;
    }

    /**
     * Jump to a specific interrupt.
     * @param newPC
     *     the new value of the program counter
     */
    private void interruptJump(char newPC) {
        lcall((byte)(newPC >> 8), (byte)newPC);
    }

    /**
     * Get the value of an address from internal RAM (or the SFR area)
     * @param address
     *     the address to be used
     * @return
     *     the byte at this address
     * @throws IndexOutOfBoundsException
     *     when the address is in the SFR area but is not a valid SFR
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
     * @param address
     *     the address to be used
     * @param value
     *     the value the byte at this address will be set to
     * @throws IndexOutOfBoundsException
     *     when the address is in the SFR area, but is not a valid SFR (the operation is
     *     still performed, though; a new temporary SFR is created in this case)
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
     * @return
     *     a {@code BitAddress} object containing the direct address of the byte containing the specified bit and a
     *     bit mask for the bit
     * @throws IllegalArgumentException
     *     when given an invalid bit address
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
     * @param bitAddress
     *     the bit's bit address
     * @return
     *     {@code true} if the bit is 1; {@code false} if it is 0
     * @see #decodeBitAddress(byte)
     */
    boolean getBit(byte bitAddress) {
        BitAddress tmp = decodeBitAddress(bitAddress);
        return (getDirectAddress(tmp.DIRECT_ADDRESS) & tmp.BIT_MASK) == tmp.BIT_MASK;
    }

    /**
     * Set the value of a single bit.
     * @param bitAddress
     *     the bit's bit address
     * @param bit
     *     the bit's new value; {@code true} -> 1; {@code false} -> 0
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
     * @param exceptionOnUnderflow
     *     if this is {@code true}, an exception is thrown, when the stack pointer is 0
     * @return
     *     the byte from the stack
     * @see #pop(byte)
     * @throws IllegalArgumentException
     *     on stack underflow
     */
    private byte _pop(boolean exceptionOnUnderflow) throws IllegalStateException {
        int resultingAddress = this.state.sfrs.SP.getValue() & 0xFF;
        byte result = this.state.internalRAM.get(resultingAddress);
        --resultingAddress;
        this.state.sfrs.SP.setValue((byte)resultingAddress);
        if (resultingAddress < 0 && exceptionOnUnderflow && !this.state.ignoreSOSU)
            throw new IllegalStateException("Stack underflow.");
        return result;
    }

    /**
     * <b>No Operation</b><br>
     * @return
     *     the number of cycles (1)
     */
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
     * <pre><br>
     *
     * instruction =         11100001<br>
     * pc          = 1011011100000010<br>
     * argument    =         00100010<br>
     * <br>
     * A<sub>10</sub> - A<sub>8</sub> (from instruction) = 111<br>
     * A<sub>7</sub> - A<sub>0</sub> (from argument      =    00100010<br>
     * ==> resulting address = 11100100010<br>
     * <br>
     * replace last 11 bits in pc:<br>
     * pc       = 1011011100000010<br>
     * result   =      11100100010<br>
     * replaced = 1011011100100010<br>
     * </pre>
     * @param currentOp
     *     the current opcode
     * @param last8bits
     *     the argument (next byte in code memory)
     * @return
     *     the number of cycles (2)
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
     * @param highByte
     *     A<sub>15<sub>-A<sub>8</sub>
     * @param lowByte
     *     A<sub>7</sub>-A<sub>0</sub>
     * @return
     *     the number of cycles (2)
     */
    private int ljmp(byte highByte, byte lowByte) {
        this.state.PCH.setValue(highByte);
        this.state.PCL.setValue(lowByte);
        return 2;
    }

    /**
     * <b>SJMP (by offset)</b>
     * Jump by a certain offset. The base address is the address of the byte after the instruction and the argument.
     * @param offset
     *     the offset to jump by
     * @return
     *     the number of cycles (2)
     */
    private int sjmp(byte offset) {
        jumpToOffset(offset);
        return 2;
    }

    /**
     * <b>Rotate Right</b><br>
     * This instruction rotates the accumulator one bit to the right.
     * Bit 0 of the accumulator is rotated to bit 7.<br>
     * @return
     *     the number of cycles (1)
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
     * @return
     *     the number of cycles (1)
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
     * @return
     *     the number of cycles (1)
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
     * @return
     *     the number of cycles (1)
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
     * @return
     *     the number of cycles (1)
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
     * @return
     *     the number of cycles (1)
     * @throws IndexOutOfBoundsException
     *     when an address in the SFR memory is used that does not contain an SFR
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
     * @return
     *     the number of cycles (1)
     */
    private int inc_indirect(byte indirectAddress) {
        this.state.internalRAM.set(indirectAddress & 0xFF,
                (byte)(this.state.internalRAM.get(indirectAddress & 0xFF) + 1));
        return 1;
    }

    /**
     * <b>Increment (DPTR)</b><br>
     * Increment the data pointer.
     * @return
     *     the number of cycles (2)
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
     * @return
     *     the number of cycles (1)
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
     * @return
     *     the number of cycles (1)
     * @throws IndexOutOfBoundsException
     *     when an address in the SFR memory is used that does not contain an SFR
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
     * @return
     *     the number of cycles (1)
     */
    private int dec_indirect(byte indirectAddress) {
        this.state.internalRAM.set(indirectAddress & 0xFF,
                (byte)(this.state.internalRAM.get(indirectAddress & 0xFF) - 1));
        return 1;
    }

    /**
     * <b>Jump (if) Bit (is set)</b>
     * Jump by a certain offset if the specified bit is set.
     * @param bitAddress
     *     the bit's address
     * @param offset
     *     the offset to jump by if the bit is set
     * @return
     *     the number of cycles (2)
     * @see #decodeBitAddress(byte)
     */
    private int jb(byte bitAddress, byte offset) {
        if (getBit(bitAddress)) jumpToOffset(offset);
        return 2;
    }

    /**
     * <b>Jump (if) Bit (is set and) Clear (it)</b>
     * Jump by a certain offset if the specified bit is set and clear the bit afterwards.
     * @param bitAddress
     *     the bit's address
     * @param offset
     *     the offset to jump by if the bit is set
     * @return
     *     the number of cycles (2)
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
     * @param bitAddress
     *     the bit's address
     * @param offset
     *     the offset to jump by if the bit is set
     * @return
     *     the number of cycles (2)
     * @see #decodeBitAddress(byte)
     */
    private int jnb(byte bitAddress, byte offset) {
        if (!getBit(bitAddress)) jumpToOffset(offset);
        return 2;
    }

    /**
     * <b>Jump (if) C (is set)</b>
     * Jump by a certain offset if the C flag is set.
     * @param offset
     *     the offset to jump by if the C flag is set
     * @return
     *     the number of cycles (2)
     */
    private int jc(byte offset) {
        if (this.state.sfrs.PSW.getBit(7)) jumpToOffset(offset);
        return 2;
    }

    /**
     * <b>Jump (if) C (is not set)</b>
     * Jump by a certain offset if the C flag is not set.
     * @param offset
     *     the offset to jump by if the C flag is not set
     * @return
     *     the number of cycles (2)
     */
    private int jnc(byte offset) {
        if (!this.state.sfrs.PSW.getBit(7)) jumpToOffset(offset);
        return 2;
    }

    /**
     * <b>Jump (if A == 0)</b><br>
     * Jump by a certain offset if the accumulator equals zero.
     * @param offset
     *     the offset to jump by if the accumulator is 0
     * @return
     *     the number of cycles (2)
     */
    private int jz(byte offset) {
        if (this.state.sfrs.A.getValue() == (byte)0) jumpToOffset(offset);
        return 2;
    }

    /**
     * <b>Jump (if A != 0)</b><br>
     * Jump by a certain offset if the accumulator does not equal zero.
     * @param offset
     *     the offset to jump by if the accumulator is not 0
     * @return
     *     the number of cycles (2)
     */
    private int jnz(byte offset) {
        if (this.state.sfrs.A.getValue() != (byte)0) jumpToOffset(offset);
        return 2;
    }

    /**
     * <b>JMP (@A+DPTR)</b><br>
     * Add the content of the accumulator to the content of the DPTR (data pointer) and jump to the resulting address.
     * @return
     *     the number of cycles (2)
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
     * @param currentOpcode
     *     the current opcode
     * @param last8bits
     *     the argument (which contains the destination address' low-byte)
     * @return
     *     the number of cycles (2)
     * @throws IllegalStateException
     *     on stack overflow
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
     * @param highByte
     *     the destination address' high byte
     * @param lowByte
     *     the destination address' low byte
     * @return
     *     the number of cycles (2)
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
     * @return
     *     the number of cycles (2)
     * @throws IllegalStateException
     *     on stack underflow
     */
    private int ret() throws IllegalStateException {
        ljmp(_pop(!this.state.ignoreSOSU), _pop(!this.state.ignoreSOSU));
        return 2;
    }

    /**
     * <b>Return from Interrupt</b>
     * @return
     *     the number of cycles (2)
     * @throws IllegalStateException
     *     on stack underflow
     */
    private int reti() {
        if (this.state.runningInterruptPriority < 0)
            throw new IllegalStateException("RETI called even though no interrupt is running");
        if (this.state.runningInterruptInterruptedOtherInterrupt) {
            this.state.runningInterruptInterruptedOtherInterrupt = false;
            this.state.runningInterruptPriority = 0;
        } else this.state.runningInterruptPriority = -1;
        return ret();
    }

    /**
     * <b>Push</b><br>
     * Increment the stack pointer and push a byte onto the stack.
     * @param value
     *     the value to be stored on the stack
     * @return
     *     the number of cycles (2)
     * @throws IllegalStateException
     *     on stack overflow
     */
    private int push(byte value) throws IllegalStateException {
        int resultingAddress = (this.state.sfrs.SP.getValue() & 0xFF) + 1;
        this.state.internalRAM.set(resultingAddress & 0xFF, value);
        this.state.sfrs.SP.setValue((byte)resultingAddress);
        if (resultingAddress > 0xFF && !this.state.ignoreSOSU)
            throw new IllegalStateException("Stack overflow.");
        return 2;
    }

    /**
     * <b>Pop</b> a byte from the stack and decrement the stack pointer
     * @param direct
     *     address at which the result is stored
     * @return
     *     the number of cycles (2)
     * @throws IllegalStateException
     *     on stack underflow
     * @see #setDirectAddress(byte, byte)
     */
    private int pop(byte direct) throws IllegalStateException {
        setDirectAddress(direct, _pop(!this.state.ignoreSOSU));
        return 2;
    }

    /**
     * <b>ADD (#immediateValue)</b><br>
     * Add an immediate value to the accumulator and store the result in the accumulator.
     * @param immediate
     *     the value to be added to the accumulator
     * @return
     *     the number of cycles (1)
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
     * @param indirectAddress
     *     the address (usually the content of either R0 or R1)
     * @return
     *     the number of cycles (1)
     * @see #add_immediate(byte)
     */
    private int add_indirect(byte indirectAddress) {
        return add_immediate(this.state.internalRAM.get(indirectAddress & 0xFF));
    }

    /**
     * <b>ADD (direct address)</b><br>
     * Add a directly addressed value to the accumulator and store the result in the accumulator.
     * @param directAddress
     *     the address
     * @return
     *     the number of cycles (1)
     * @see #getDirectAddress(byte)
     * @see #add_immediate(byte)
     */
    private int add_direct(byte directAddress) {
        return add_immediate(getDirectAddress(directAddress));
    }

    /**
     * <b>Add (RX)</b><br>
     * Add the value of a R register to the accumulator and store the result in the accumulator.
     * @param ordinal
     *     the R register's number; must be >= 0 and <= 7
     * @return
     *     the number of cycles (1)
     * @see #add_immediate(byte)
     */
    private int add_r(int ordinal) {
        return add_immediate(getR(ordinal));
    }

    /**
     * <b>Add (#immediateValue and C flag)</b><br>
     * Add an immediate value and the carry flag to the accumulator and store the result in the accumulator.
     * @param immediateValue
     *     the value to be added to the accumulator
     * @return
     *     the number of cycles (1)
     * @see #add_immediate(byte)
     */
    private int addc_immediate(byte immediateValue) {
        return add_immediate((byte)(immediateValue + (byte)(this.state.sfrs.PSW.getBit(7) ? 1 : 0)));
    }

    /**
     * <b>ADDC (@Ri)</b><br>
     * Add an indirectly addressed value and the carry flag to the accumulator and store the result in the accumulator.
     * @param indirectAddress
     *     the address (usually the content of either R0 or R1)
     * @return
     *     the number of cycles (1)
     * @see #addc_immediate(byte)
     */
    private int addc_indirect(byte indirectAddress) {
        return addc_immediate(this.state.internalRAM.get(indirectAddress & 0xFF));
    }

    /**
     * <b>ADDC (direct address)</b><br>
     * Add a directly addressed value and the carry flag to the accumulator and store the result in the accumulator.
     * @param directAddress
     *     the address
     * @return
     *     the number of cycles (1)
     * @see #getDirectAddress(byte)
     * @see #addc_immediate(byte)
     */
    private int addc_direct(byte directAddress) {
        return addc_immediate(getDirectAddress(directAddress));
    }

    /**
     * <b>Add (RX)</b><br>
     * Add the value of a R register and the carry flag to the accumulator and store the result in the accumulator.
     * @param ordinal
     *     the R register's number; must be >= 0 and <= 7
     * @return
     *     the number of cycles (1)
     * @see #addc_immediate(byte)
     */
    private int addc_r(int ordinal) {
        return addc_immediate(getR(ordinal));
    }

    /**
     * <b>SUBB (A, #immediate)</b><br>
     * Subtract a value and the carry flag from the accumulator.
     * @param immediateValue
     *     the value to be subtracted from A
     * @return
     *     the number of cycles (1)
     * @see #add_immediate(byte)
     */
    private int subb_immediate(byte immediateValue) {
        //NOTE: I did a lot of experimentation with this method as I could not find very clear documentation
        //      It *seems* to work now, but I wouldn't be surprised to see that it fails in some edge cases. :-)
        byte operand = immediateValue;
        byte a = this.state.sfrs.A.getValue();
        final boolean current_carry = this.state.sfrs.PSW.getBit(7);
        if (this.state.sfrs.PSW.getBit(7)) operand++;
        final boolean carry = (((immediateValue & 0xFF)+(current_carry ? 1 : 0)) & 0x1FF) > (a & 0xFF);
        final boolean auxiliary_carry = (operand & 0xF) > (a & 0xF);
        final int result = a - operand;
        final boolean ov = result > 127 || result < -128;
        this.state.sfrs.A.setValue((byte)result);
        this.state.sfrs.PSW.setBit(carry, 7);
        this.state.sfrs.PSW.setBit(auxiliary_carry, 6);
        this.state.sfrs.PSW.setBit(ov, 2);
        return 1;
    }

    /**
     * <b>Subb (A, @Ri)</b>
     * @param indirectAddress
     *     the indirect address that determines the byte to be subtracted from A (normally the content of R0 or R1)
     * @return
     *     the number of cycles (1)
     * @see #subb_immediate(byte)
     */
    private int subb_indirect(byte indirectAddress) {
        return subb_immediate(this.state.internalRAM.get(indirectAddress & 0xFF));
    }

    /**
     * <b>SUBB (A, directAddress)</b>
     * @param directAddress
     *     the direct address that determines the byte to be subtracted from A
     * @return
     *     the number of cycles (1)
     * @see #subb_immediate(byte)
     */
    private int subb_direct(byte directAddress) {
        return subb_immediate(getDirectAddress(directAddress));
    }

    /**
     * <b>SUBB (A, Rn)</b>
     * @param ordinal
     *     determines the R register whose value is subtracted from A; must be >= 0 and <= 7
     * @return
     *     the number of cycles (1)
     * @see #subb_immediate(byte)
     */
    private int subb_r(int ordinal) {
        return subb_immediate(getR(ordinal));
    }

    /**
     * <b>ORL (direct, #immediate)</b>
     * <br>
     * Perform a logical OR on the byte at the direct address and the immediate byte; store the result at the direct
     * address.
     * @param directAddress
     *     the direct address
     * @param immediateValue
     *     the immediate value
     * @return
     *     the number of cycles (2)
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
     * @param directAddress
     *     the direct address
     * @return
     *     the number of cycles (1)
     * @see #orl(byte, byte)
     */
    private int orl_direct_a(byte directAddress) {
        orl(directAddress, this.state.sfrs.A.getValue());
        return 1;
    }

    /**
     * <b>ORL (A, #immediate)</b><br>
     * Perform a logical OR on the accumulator and the immediate value; store the result in the accumulator.
     * @param immediateValue
     *     the immediate value
     * @return
     *     the number of cycles (1)
     * @see #orl(byte, byte)
     */
    private int orl_a_immediate(byte immediateValue) {
        orl(this.state.sfrs.getAddress(this.state.sfrs.A), immediateValue);
        return 1;
    }

    /**
     * <b>ORL (A, direct)</b><br>
     * Perform a logical OR on the accumulator and the byte at the direct address; store the result in the accumulator.
     * @param directAddress
     *     the direct address to be used
     * @return
     *     the number of cycles (1)
     * @see #orl_a_immediate(byte)
     */
    private int orl_a(byte directAddress) {
        return orl_a_immediate(getDirectAddress(directAddress));
    }

    /**
     * <b>ORL (A, @Ri)</b><br>
     * Perform a logical OR on the accumulator and the byte at the direct address; store the result in the accumulator.
     * @param indirectAddress
     *     the indirect address to be used
     * @return
     *     the number of cycles (1)
     * @see #orl_a_immediate(byte)
     */
    private int orl_a_indirect(byte indirectAddress) {
        return orl_a_immediate(this.state.internalRAM.get(indirectAddress & 0xFF));
    }

    /**
     * <b>ORL (C, (/)bit)</b><br>
     * Perform a logical OR on C and the specified bit (or the negated bit); store the result in C.
     * @param bitAddress
     *     the bit to be used
     * @param negateBit
     *     if this is {@code true}, the bit will be negated, before the OR is performed
     * @return
     *     the number of cycles (2)
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
     * @param directAddress
     *     the direct address
     * @param immediateValue
     *     the immediate value
     * @return
     *     the number of cycles (2)
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
     * @param directAddress
     *     the direct address
     * @return
     *     the number of cycles (1)
     * @see #xrl(byte, byte)
     */
    private int xrl_direct_a(byte directAddress) {
        xrl(directAddress, this.state.sfrs.A.getValue());
        return 1;
    }

    /**
     * <b>XRL (A, #immediate)</b><br>
     * Perform a logical XOR on the accumulator and the immediate value; store the result in the accumulator.
     * @param immediateValue
     *     the immediate value
     * @return
     *     the number of cycles (1)
     * @see #xrl(byte, byte)
     */
    private int xrl_a_immediate(byte immediateValue) {
        xrl(this.state.sfrs.getAddress(this.state.sfrs.A), immediateValue);
        return 1;
    }

    /**
     * <b>XRL (A, direct)</b><br>
     * Perform a logical XOR on the accumulator and the byte at the direct address; store the result in the accumulator.
     * @param directAddress
     *     the direct address to be used
     * @return
     *     the number of cycles (1)
     * @see #xrl_a_immediate(byte)
     */
    private int xrl_a(byte directAddress) {
        return xrl_a_immediate(getDirectAddress(directAddress));
    }

    /**
     * <b>XRL (A, @Ri)</b><br>
     * Perform a logical XOR on the accumulator and the byte at the direct address; store the result in the accumulator.
     * @param indirectAddress
     *     the indirect address to be used
     * @return
     *     the number of cycles (1)
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
     * @param directAddress
     *     the direct address
     * @param immediateValue
     *     the immediate value
     * @return
     *     the number of cycles (2)
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
     * @param directAddress
     *     the direct address
     * @return
     *     the number of cycles (1)
     * @see #anl(byte, byte)
     */
    private int anl_direct_a(byte directAddress) {
        anl(directAddress, this.state.sfrs.A.getValue());
        return 1;
    }

    /**
     * <b>ANL (A, #immediate)</b><br>
     * Perform a logical AND on the accumulator and the immediate value; store the result in the accumulator.
     * @param immediateValue
     *     the immediate value
     * @return
     *     the number of cycles (1)
     * @see #anl(byte, byte)
     */
    private int anl_a_immediate(byte immediateValue) {
        anl(this.state.sfrs.getAddress(this.state.sfrs.A), immediateValue);
        return 1;
    }

    /**
     * <b>ANL (A, direct)</b><br>
     * Perform a logical AND on the accumulator and the byte at the direct address; store the result in the accumulator.
     * @param directAddress
     *     the direct address to be used
     * @return
     *     the number of cycles (1)
     * @see #anl_a_immediate(byte)
     */
    private int anl_a(byte directAddress) {
        return anl_a_immediate(getDirectAddress(directAddress));
    }

    /**
     * <b>ANL (A, @Ri)</b><br>
     * Perform a logical AND on the accumulator and the byte at the direct address; store the result in the accumulator.
     * @param indirectAddress
     *     the indirect address to be used
     * @return
     *     the number of cycles (1)
     * @see #anl_a_immediate(byte)
     */
    private int anl_a_indirect(byte indirectAddress) {
        return anl_a_immediate(this.state.internalRAM.get(indirectAddress & 0xFF));
    }

    /**
     * <b>ANL (C, (/)bit)</b><br>
     * Perform a logical AND on C and the specified bit (or the negated bit); store the result in C.
     * @param bitAddress
     *     the bit to be used
     * @param negateBit
     *     if this is {@code true}, the bit will be negated, before the OR is performed
     * @return
     *     the number of cycles (2)
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
     * @param indirectAddress
     *     the indirect address to be used (typically the content of R0/R1)
     * @param immediateValue
     *     the value to be stored at this address
     * @return
     *     the number of cycles (1)
     */
    private int mov_indirect_immediate(byte indirectAddress, byte immediateValue) {
        this.state.internalRAM.set(indirectAddress & 0xFF, immediateValue);
        return 1;
    }

    /**
     * <b>MOV (@Ri, direct)</b><br>
     * @param indirectAddress
     *     the indirect address to be used (typically the content of R0/R1)
     * @param directAddress
     *     the direct address from which the value that is stored at the indirect address is read
     * @return
     *     the number of cycles (2)
     */
    private int mov_indirect_direct(byte indirectAddress, byte directAddress) {
        mov_indirect_immediate(indirectAddress, getDirectAddress(directAddress));
        return 2;
    }

    /**
     * <b>MOV (direct, #immediate)</b>
     * @param directAddress
     *     the direct address at which the immediate value is stored
     * @param immediateValue
     *     the immediate value to be stored at said address
     * @return
     *     the number of cycles (2)
     */
    private int mov_direct_immediate(byte directAddress, byte immediateValue) {
        setDirectAddress(directAddress, immediateValue);
        return 2;
    }

    /**
     * <b>MOV (A, #immediate)</b>
     * @param immediateValue
     *     the immediate value to be moved to the accumulator
     * @return
     *     the number of cycles (1)
     */
    private int mov_a_immediate(byte immediateValue) {
        mov_direct_immediate(this.state.sfrs.getAddress(this.state.sfrs.A), immediateValue);
        return 1;
    }

    /**
     * <b>MOV (A, @Ri)</b>
     * @param indirectAddress
     *     the indirect address to be used (typically the content of R0/R1)
     * @return
     *     the number of cycles (1)
     */
    private int mov_a_indirect(byte indirectAddress) {
        return mov_a_immediate(this.state.internalRAM.get(indirectAddress & 0xFF));
    }

    /**
     * <b>MOV (A, direct)</b>
     * @param directAddress
     *     the direct address whose value is stored in the accumulator
     * @return
     *     the number of cycles (1)
     */
    private int mov_a_direct(byte directAddress) {
        return mov_a_immediate(getDirectAddress(directAddress));
    }

    /**
     * <b>MOV (bit, C)</b>
     * @param bitAddress
     *     the bit address to which the value of the carry flag is copied
     * @return
     *     the number of cycles (2)
     */
    private int mov_bit_c(byte bitAddress) {
        setBit(this.state.sfrs.PSW.getBit(7), bitAddress);
        return 2;
    }

    /**
     * <b>MOV (C, bit)</b>
     * @param bitAddress
     *     the bit address whose value is copied to the carry flag
     * @return
     *     the number of cycles (2)
     */
    private int mov_c_bit(byte bitAddress) {
        this.state.sfrs.PSW.setBit(getBit(bitAddress), 7);
        return 2;
    }

    /**
     * <b>MOV (direct, direct)</b><br>
     * NOTE: The reason the parameters are flipped is that they appear in that order in machine code. In assembly they
     * are written differently:<br>
     * <pre>
     *     Machine code: sourceAddress     , destinationAddress<br>
     *     Assembly    : destinationAddress, sourceAddress
     * </pre>
     * @param srcDirect
     *     the direct source address
     * @param destDirect
     *     the indirect source address
     * @return
     *     the number of cycles (2)
     */
    private int mov_direct_direct(byte srcDirect, byte destDirect) {
        mov_direct_immediate(destDirect, getDirectAddress(srcDirect));
        return 2;
    }

    /**
     * <b>MOV (direct, @Ri)</b>
     * @param directAddress
     *     the direct address to be used
     * @param indirectAddress
     *     the indirect address to be used (typically the content of R0/R1)
     * @return
     *     the number of cycles (2)
     */
    private int mov_direct_indirect(byte directAddress, byte indirectAddress) {
        mov_direct_immediate(directAddress, this.state.internalRAM.get(indirectAddress & 0xFF));
        return 2;
    }

    /**
     * <b>MOV (DPTR, #immediate)</b>
     * @param immediateHigh
     *     the new value of DPH
     * @param immediateLow
     *     the new value of DPL
     * @return
     *     the number of cycles (2)
     */
    private int mov_dptr(byte immediateHigh, byte immediateLow) {
        this.state.sfrs.DPH.setValue(immediateHigh);
        this.state.sfrs.DPL.setValue(immediateLow);
        return 2;
    }

    /**
     * <b>MOV (Rn, #immediate)</b>
     * @param ordinal
     *     specifies the R register (R<sub>ordinal</sub>); must be >= 0 and <= 7
     * @param immediateValue
     *     the immediate value to be stored at the specified R register
     * @return
     *     the number of cycles (1)
     */
    private int mov_r_immediate(int ordinal, byte immediateValue) {
        this.state.internalRAM.set(getRAddress(ordinal), immediateValue);
        return 1;
    }

    /**
     * <b>MOV (Rn, direct)</b>
     * @param ordinal
     *     specifies the R register (R<sub>ordinal</sub>); must be >= 0 and <= 7
     * @param directAddress
     *     the direct address whose content is copied to the specified R register
     * @return
     *     the number of cycles (2)
     */
    private int mov_r_direct(int ordinal, byte directAddress) {
        mov_r_immediate(ordinal, getDirectAddress(directAddress));
        return 2;
    }

    /**
     * <b>MOV (direct, A)</b>
     * @param directAddress
     *     the direct address whose content is moved to the accumulator
     * @return
     *     the number of cycles (1)
     */
    private int mov_direct_a(byte directAddress) {
        mov_direct_immediate(directAddress, this.state.sfrs.A.getValue());
        return 1;
    }

    /**
     * <b>MOVC (A, @A+DPTR/PC)</b><br>
     * Copy code memory to the accumulator.
     * @param high
     *     high byte of the DPTR/PC register
     * @param low
     *     byte of the DPTR/PC register
     * @return
     *     the number of cycles (2)
     */
    private int movc_a(ByteRegister high, ByteRegister low) {
        final char address = (char)((this.state.sfrs.A.getValue() & 0xFF)
                + (high.getValue() << 8 & 0xFF00 | low.getValue() & 0xFF));
        this.state.sfrs.A.setValue(this.state.codeMemory.get(address));
        return 2;
    }

    /**
     * <b>MOVX (@Ri, A)</b><br>
     * @param indirectAddress
     *     indirect address in external RAM the value of A is moved to
     * @return
     *     the number of cycles (2)
     */
    private int movx_indirect_a(byte indirectAddress) {
        if (null != this.state.externalRAM)
            this.state.externalRAM.set(indirectAddress & 0xFF, this.state.sfrs.A.getValue());
        else if (!this.state.ignoreExceptions) throw new IllegalStateException("no external RAM, but MOVX was used");
        return 2;
    }

    /**
     * <b>MOVX (@DPTR, A)</b>
     * @return
     *     the number of cycles (2)
     */
    private int movx_dptr_a() {
        final int address = this.state.sfrs.DPH.getValue() << 8 & 0xFF00 | this.state.sfrs.DPL.getValue() & 0xFF;
        if (null != this.state.externalRAM)
            this.state.externalRAM.set(address, this.state.sfrs.A.getValue());
        else if (!this.state.ignoreExceptions) throw new IllegalStateException("no external RAM, but MOVX was used");
        return 2;
    }

    /**
     * <b>MOVX (A, @Ri)</b>
     * @param indirectAddress
     *     the indirect address in external RAM to move to A
     * @return
     *     the number of cycles (2)
     */
    private int movx_a_indirect(byte indirectAddress) {
        if (null != this.state.externalRAM)
            this.state.sfrs.A.setValue(this.state.externalRAM.get(indirectAddress & 0xFF));
        else if (!this.state.ignoreExceptions) throw new IllegalStateException("no external RAM, but MOVX was used");
        return 2;
    }

    /**
     * <b>MOVX (A, @DPTR)</b>
     * @return
     *     the number of cycles (2)
     */
    private int movx_a_dptr() {
        final int address = this.state.sfrs.DPH.getValue() << 8 & 0xFF00 | this.state.sfrs.DPL.getValue() & 0xFF;
        if (null != this.state.externalRAM)
            this.state.sfrs.A.setValue(this.state.externalRAM.get(address));
        else if (!this.state.ignoreExceptions) throw new IllegalStateException("no external RAM, but MOVX was used");
        return 2;
    }

    /**
     * <b>DIV AB</b><br>
     * Divide the A register by the B register and store the result in A and the remainder in B.
     * The carry flag is always cleared.
     * If a division by zero is attempted, the OV flag is set, otherwise it is cleared.
     * (The states of A and B after a division by zero are undefined.)
     * @return
     *     the number of cycles (4)
     */
    private int div_ab() {
        final ByteRegister A = this.state.sfrs.A;
        final ByteRegister B = this.state.sfrs.B;
        final FlagRegister PSW = this.state.sfrs.PSW;
        PSW.setBit(false, 7); //DIV always clears the carry flag
        if (B.getValue() == (byte)0) {
            PSW.setBit(true, 2); //the OV flag is set if the program tries to divide by 0
        } else {
            //when the operands are valid, the OV flag is cleared
            PSW.setBit(false, 2);
            final byte oldA = A.getValue();
            //the division is unsigned; the result is stored in A
            A.setValue((byte)((A.getValue() & 0xFF) / (B.getValue() & 0xFF)));
            //the remainder is stored in B
            B.setValue((byte)((oldA & 0xFF) % (B.getValue() & 0xFF)));
        }
        return 4;
    }

    /**
     * <b>MUL (AB)</b><br>
     * Perform an unsigned multiplication of A and B. The low byte of the result is stored in A, the high byte in B.
     * @return
     *     the number of cycles (4)
     */
    private int mul_ab() {
        this.state.sfrs.PSW.setBit(false, 7); //MUL always clears the carry flag
        final byte a = this.state.sfrs.A.getValue();
        final byte b = this.state.sfrs.B.getValue();
        final int result = (a & 0xFF) * (b & 0xFF);
        this.state.sfrs.PSW.setBit(result > 0xFF, 2); //if the result does not fit, set OV; otherwise clear it
        this.state.sfrs.A.setValue((byte)result);
        this.state.sfrs.B.setValue((byte)(result >>> 8));
        return 4;
    }

    /**
     * <b>0xA5 (this opcode is reserved/not defined)</b>
     * @return
     *     1
     * @throws UnsupportedOperationException
     *     when it is called (except when {@code ignoreUndefined} is set to {@code true})
     */
    private int reserved() {
        if (!this.state.ignoreUndefined) throw new UnsupportedOperationException("0xA5 used.");
        return 1;
    }

    /**
     * <b>CPL (bit)</b><br>
     * Complement the specified bit.
     * @param bitAddress address of the bit that should be complemented
     * @return
     *     the number of cycles (1)
     */
    private int cpl(byte bitAddress) {
        setBit(!getBit(bitAddress), bitAddress);
        return 1;
    }

    /**
     * <b>CPL (A)</b><br>
     * Complement the accumulator.
     * @return
     *     the number of cycles (1)
     */
    private int cpl_a() {
        this.state.sfrs.A.setValue((byte)~this.state.sfrs.A.getValue());
        return 1;
    }

    /**
     * <b>CPL (C)</b><br>
     * Complement the C flag.
     * @return
     *     the number of cycles (1)
     */
    private int cpl_c() {
        final int C = 7;
        this.state.sfrs.PSW.setBit(!this.state.sfrs.PSW.getBit(C), C);
        return 1;
    }

    /**
     * Helper function for the CJNE mnemonics.
     * @param immediate1
     *     byte 1 for the comparison
     * @param immediate2
     *     byte 2 for the comparison
     * @param offset
     *     the offset to jump by if the bytes aren't equal
     * @return
     *     the number of cycles (2)
     */
    private int _cjne(byte immediate1, byte immediate2, byte offset) {
        final int C = 7;
        if (immediate1 != immediate2) jumpToOffset(offset);
        this.state.sfrs.PSW.setBit((immediate1 & 0xFF) < (immediate2 & 0xFF), C);
        return 2;
    }

    /**
     * <b>CJNE (@Ri, #immediate, LABEL)</b>
     * @param indirectAddress
     *     the indirect address used to get byte 1 for the comparison (usually the content of R0/R1)
     * @param immediateValue
     *     byte 2 for the comparison
     * @param offset
     *     the offset to jump by if the bytes aren't equal
     * @return
     *     the number of cycles (2)
     */
    private int cjne_indirect_immediate(byte indirectAddress, byte immediateValue, byte offset) {
        return _cjne(this.state.internalRAM.get(indirectAddress & 0xFF), immediateValue, offset);
    }

    /**
     * <b>CJNE (A, #immediate, LABEL)</b>
     * @param immediateValue
     *     byte 2 for the comparison
     * @param offset
     *     the offset to jump by if byte 2 isn't equal to A
     * @return
     *     the number of cycles (2)
     */
    private int cjne_a_immediate(byte immediateValue, byte offset) {
        return _cjne(this.state.sfrs.A.getValue(), immediateValue, offset);
    }

    /**
     * <b>CJNE (A, directAddress, LABEL)</b>
     * @param directAddress
     *     the direct address used to get byte 2 of the comparison
     * @param offset
     *     the offset to jump by if A and byte 2 aren't equal
     * @return
     *     the number of cycles (2)
     * @see #getDirectAddress(byte)
     */
    private int cjne_a_direct(byte directAddress, byte offset) {
        return _cjne(this.state.sfrs.A.getValue(), getDirectAddress(directAddress), offset);
    }

    /**
     * <b>CJNE (Rn, #immediate, LABEL)</b>
     * @param ordinal
     *     the R register's number; must be >= 0 and <= 7
     * @param immediateValue
     *     byte 2 for the comparison
     * @param offset
     *     the offset to jump by if R&lt;ordinal&gt; and byte 2 aren't equal
     * @return
     *     the number of cycles (2)
     */
    private int cjne_r_immediate(int ordinal, byte immediateValue, byte offset) {
        return _cjne(getR(ordinal), immediateValue, offset);
    }

    /**
     * <b>CLR (A)</b>
     * @return
     *     the number of cycles (1)
     */
    private int clr_a() {
        this.state.sfrs.A.setValue((byte)0);
        return 1;
    }

    /**
     * <b>CLR (bitAddress)</b>
     * @param bitAddress
     *     the bit to be cleared
     * @return
     *     the number of cycles (1)
     */
    private int clr(byte bitAddress) {
        setBit(false, bitAddress);
        return 1;
    }

    /**
     * <b>CLR (C)</b>
     * @return
     *     the number of cycles (1)
     */
    private int clr_c() {
        this.state.sfrs.PSW.setBit(false, 7); //bit 7 is the carry flag
        return 1;
    }

    /**
     * <b>SWAP (A)</b>
     * <br>
     * Swap the high and the low nibble of the accumulator.
     * @return
     *     the number of cycles (1)
     */
    private int swap_a() {
        final byte a = this.state.sfrs.A.getValue();
        //swap high and low nibble of the accumulator
        this.state.sfrs.A.setValue((byte)((a << 4) & 0xF0 | (a >> 4) & 0xF));
        return 1;
    }

    /**
     * <b>XCH (A, @Ri)</b>
     * @param indirectAddress
     *     the indirect address to be exchanged with A
     * @return
     *     the number of cycles (1)
     */
    private int xch_a_indirect(byte indirectAddress) {
        final byte a = this.state.sfrs.A.getValue();
        this.state.sfrs.A.setValue(this.state.internalRAM.get(indirectAddress & 0xFF));
        this.state.internalRAM.set(indirectAddress & 0xFF, a);
        return 1;
    }

    /**
     * <b>XCH (A, direct)</b>
     * @param directAddress
     *     the direct address to be exchanged with A
     * @return
     *     the number of cycles (1)
     */
    private int xch_a_direct(byte directAddress) {
        final byte a = this.state.sfrs.A.getValue();
        this.state.sfrs.A.setValue(getDirectAddress(directAddress));
        setDirectAddress(directAddress, a);
        return 1;
    }

    /**
     * <b>XCH (A, Rn)</b>
     * @param ordinal
     *     specifies the R register to use; must be >= 0 and <= 7
     * @return
     *     the number of cycles (1)
     */
    private int xch_a_r(int ordinal) {
        return xch_a_indirect((byte)getRAddress(ordinal));
    }

    /**
     * <b>SETB (C)</b><br>
     * Set the carry flag to 1.
     * @return
     *     the number of cycles (1)
     */
    private int setb_c() {
        this.state.sfrs.PSW.setBit(true, 7); //bit 7 is the carry flag
        return 1;
    }

    /**
     * <b>SETB (bitAddress)</b><br>
     * Set a bit to 1.
     * @param bitAddress
     *     specifies the bit to be set to 1
     * @return
     *     the number fo cycles (1)
     */
    private int setb(byte bitAddress) {
        setBit(true, bitAddress);
        return 1;
    }

    /**
     * <b>DA (A)</b>
     * <br>
     * This instruction is used after two BCD numbers have been added using {@code ADD} or {@code ADDC}.
     * For an in-depth explanation see http://www.keil.com/support/man/docs/is51/is51_da.htm
     * @return
     *     the number of cycles (1)
     */
    private int da_a() {
        final byte a = this.state.sfrs.A.getValue();
        byte result = a;

        //if bits 0-3 are > 9 or the AC flag is set, 6 is added to the accumulator
        if ((a & 0xF) > 9 || this.state.sfrs.PSW.getBit(6)) {
            int tmp = (result & 0xFF) + 6;
            //if the addition causes an overflow, the C bit is set; it is not cleared otherwise
            if ((tmp & 0x100) == 0x100) this.state.sfrs.PSW.setBit(true, 7);
            result = (byte)tmp;
        }

        //if the carry flag (bit 7) is set or the four bits of the high-nibble exceed nine, these bits are incremented
        //by 6
        if (((a >> 4) & 0xF) > 9 || this.state.sfrs.PSW.getBit(7)) {
            int tmp = (result & 0xFF) + 0x60;
            //if the sum exceeds nine, the carry flag is set; it is not cleared otherwise
            if ((tmp & 0x100) == 0x100) this.state.sfrs.PSW.setBit(true, 7);
            result = (byte)tmp;
        }

        this.state.sfrs.A.setValue(result);

        return 1;
    }

    /**
     * <b>DJNZ (direct, offset)</b><br>
     * Decrement the byte at the specified direct address and jump to the specified address if it is not {@code 0}.
     * @param directAddress
     *     the direct address to decrement
     * @param offset
     *     the offset to jump by if the byte at {@code directAddress} isn't {@code 0}
     * @return
     *     the number of cycles (2)
     * @see #dec(byte)
     * @see #getDirectAddress(byte)
     */
    private int djnz(byte directAddress, byte offset) {
        dec(directAddress);
        if (getDirectAddress(directAddress) != (byte)0) jumpToOffset(offset);
        return 2;
    }

    /**
     * <b>DJNZ (Rn, offset)</b><br>
     * Decrement the specified R register and jump by the specified offset it is not {@code 0}.
     * @param ordinal
     *     specifies the R register to use; must be >= 0 and <= 7
     * @param offset
     *     the offset to jump by if the R register isn't {@code 0}
     * @return
     *     the number of cycles (2)
     */
    private int djnz_r(int ordinal, byte offset) {
        return djnz((byte)getRAddress(ordinal), offset);
    }

    /**
     * <b>XCHD (A, @Ri)</b><br>
     * Swap the low-nibble of the accumulator and the indirectly addressed byte.
     * @param indirectAddress
     *     the byte whose low-nibble is swapped with the accumulator's
     * @return
     *     the number of cycles (1)
     */
    private int xchd_a(byte indirectAddress) {
        final byte a = this.state.sfrs.A.getValue();
        final byte b = this.state.internalRAM.get(indirectAddress & 0xFF);
        this.state.sfrs.A.setValue((byte)(a & 0xF0 | b & 0xF));
        this.state.internalRAM.set(indirectAddress & 0xFF, (byte)(b & 0xF0 | a & 0xF));
        return 1;
    }

    @Override
    public String toString() {
        final int pc = this.state.PCH.getValue() << 8 & 0xFF00 | this.state.PCL.getValue() & 0xFF;
        return "MC8051[A="+(this.state.sfrs.A.getValue() & 0xFF)+"; PC="+pc+"; SP="
                +(this.state.sfrs.SP.getValue() & 0xFF)+"; PSW="+this.state.sfrs.PSW.getBinaryDisplayValue()+"]";
    }
}
