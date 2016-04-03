package emulator.arc8051;

import emulator.ByteRegister;
import emulator.EmulatorException;
import emulator.FlagRegister;
import emulator.RAM;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.*;

/**
 * @author Gordian
 */
public class MC8051Test {

    MC8051 testController;
    Random r;
    ByteRegister A;
    ByteRegister B;
    FlagRegister PSW;
    RAM ram;
    ByteRegister PCH;
    ByteRegister PCL;

    @Before
    public void setUp() throws Exception {
        testController = new MC8051(new RAM(65536), new RAM(65536));
        r = new Random();
        A = testController.state.sfrs.A;
        B = testController.state.sfrs.B;
        PSW = testController.state.sfrs.PSW;
        ram = testController.state.internalRAM;
        PCH = testController.state.PCH;
        PCL = testController.state.PCL;
    }

    @Test
    public void testGetRegisters() throws Exception {
        assertTrue(testController.getRegisters() != null && testController.getRegisters().size() == 31);
    }

    @Test
    public void testGetPSW() throws Exception {
        FlagRegister psw = testController.getPSW();
        assertTrue(psw != null && psw.getFlags().get(7).name.equals("C")
                && psw.getHexadecimalDisplayValue().equals("00"));
    }

    @Test
    public void testGetMainMemory() throws Exception {
        assertTrue(testController.getMainMemory().get(255) == 0);
    }

    @Test
    public void testGetSecondaryMemory() throws Exception {
        assertTrue(testController.getSecondaryMemory().get(65535) == 0);
    }

    @Test
    public void testHasSecondaryMemory() throws Exception {
        assertTrue(testController.hasSecondaryMemory());
        testController = new MC8051(new RAM(65536), null);
        assertFalse(testController.hasSecondaryMemory());
    }

    @Test
    public void testNop() {
        System.out.println("__________Test NOP...");
        testOpcode((byte)0, 0, 1, () -> testController.state.PCL.getValue() == 1);
    }

    @Test
    public void testAjmp() {
        System.out.println("__________Test AJMP...");
        final RAM ram = (RAM)testController.getCodeMemory();
        final byte STATIC_AJMP = 0x01; //AJMP: 0bXXX00001
        for (byte i = 0; i <= 0b111; ++i) {
            final byte instruction = (byte)(STATIC_AJMP | i << 5);
            System.out.printf("Opcode: %02X%n", instruction & 0xFF);
            //if this was a byte the result of the left-shift might become negative
            final int pch = r.nextInt(256);
            testController.state.PCH.setValue((byte)pch);
            testController.state.PCL.setValue((byte)0);
            final byte arg = (byte) r.nextInt(256);
            ram.set(pch << 8, instruction);
            ram.set((pch << 8) + 1, arg);
            final char resultingAddress = (char)((pch << 8) & 0xF800 | arg & 0xFF | (i << 8));
            try {
                assertTrue(testController.next() == 2);
            } catch (EmulatorException e) {
                e.printStackTrace();
            }
            char actualResult = (char)(testController.state.PCH.getValue() << 8 & 0xff00
                    | testController.state.PCL.getValue() & 0xff);
            System.out.println("actualResult = "+actualResult+" resultingAddress = "+resultingAddress);
            assertTrue(actualResult == resultingAddress);
        }
        testController.state.PCH.setValue((byte)0x0F);
        testController.state.PCL.setValue((byte)0xFF);
        ram.set(0xFFF, STATIC_AJMP);
        ram.set(0xFFF+1, (byte)0);
        System.out.println("Testing the base address...");
        System.out.printf("Opcode: %02X%n", STATIC_AJMP & 0xFF);
        try {
            testController.next();
        } catch (EmulatorException e) {
            e.printStackTrace();
        }
        assertTrue(testController.state.PCH.getValue() == (byte)0x10);
        assertTrue(testController.state.PCL.getValue() == (byte)0x00);
    }

    @Test
    public void testLjmp() {
        System.out.println("__________Testing LJMP...");
        final RAM ram = (RAM) testController.getCodeMemory();
        final byte LJMP = 0x02;
        System.out.printf("Opcode: %02X%n", LJMP & 0xFF);
        byte arg1 = (byte)r.nextInt(256);
        byte arg2 = (byte)r.nextInt(256);
        ram.set(0, LJMP);
        ram.set(1, arg1);
        ram.set(2, arg2);
        try {
            assertTrue(testController.next() == 2);
        } catch (EmulatorException e) {
            e.printStackTrace();
        }
        assertTrue(testController.state.PCH.getValue() == arg1);
        assertTrue(testController.state.PCL.getValue() == arg2);
    }

    @Test
    public void testRet() {
        System.out.println("__________Testing RET...");
        final byte LCALL = 0x12;
        final byte RET = 0x22;
        char addr = (char)(r.nextInt(0xfff) + 42);
        RAM tmp = (RAM)testController.state.codeMemory;
        tmp.set(addr, LCALL);
        testController.state.PCH.setValue((byte) (addr >>> 8));
        testController.state.PCL.setValue((byte) addr);
        try {
            testController.next();
        } catch (EmulatorException e) {
            e.printStackTrace();
        }
        testOpcode(RET, 0, 2, () -> {
            int pc = testController.state.PCH.getValue() << 8 & 0xff00 | testController.state.PCL.getValue() & 0xff;
            return pc == addr + 3 && testController.state.sfrs.SP.getValue() == (byte)7;
        });
    }

    @Test
    public void testRRA() {
        System.out.println("__________Testing RR A...");
        final byte RR = 0x3;
        final byte[] testA          = {(byte)0b10001001, 0b00011000, (byte)0b101010};
        final byte[] correctResults = {(byte)0b11000100, 0b00001100,       0b010101};
        for (int i = 0; i < testA.length; ++i) {
            testController.state.sfrs.A.setValue(testA[i]);
            final byte correctResult = correctResults[i];
            testOpcode(RR, 0, 1, () -> testController.state.sfrs.A.getValue() == correctResult);
        }
    }

    @Test
    public void testRLA() {
        System.out.println("__________Testing RL A...");
        final byte RL = 0x23;
        final byte[] testA          = {(byte)0b10001001, 0b00011000, (byte)0b00101010};
        final byte[] correctResults = {(byte)0b00010011, 0b00110000,       0b01010100};
        for (int i = 0; i < testA.length; ++i) {
            testController.state.sfrs.A.setValue(testA[i]);
            final byte correctResult = correctResults[i];
            testOpcode(RL, 0, 1, () -> testController.state.sfrs.A.getValue() == correctResult);
        }
    }

    @Test
    public void testInc() {
        System.out.println("__________Testing INC...");
        byte[] opcodes = {
                0x04, // INC A
                0x05, // INC direct
                0x06, // INC @R0
                0x07, // INC @R1
                0x08, // INC R0
                0x09, // INC R1
                0x0A, // INC R2
                0x0B, // INC R3
                0x0C, // INC R4
                0x0D, // INC R5
                0x0E, // INC R6
                0x0F, // INC R7
                (byte) 0xA3 // INC DPTR
        };
        BooleanSupplier[] checks = {
                () -> testController.state.sfrs.A.getValue() == 1 && testController.state.sfrs.PSW.getBit(0),
                () -> testController.state.R0.getValue() == 1,
                () -> testController.state.R1.getValue() == 1,
                () -> testController.state.R1.getValue() == 2,
                () -> testController.state.R0.getValue() == 2,
                () -> testController.state.R1.getValue() == 3,
                () -> testController.state.R2.getValue() == 1,
                () -> testController.state.R3.getValue() == 1,
                () -> testController.state.R4.getValue() == 1,
                () -> testController.state.R5.getValue() == 1,
                () -> testController.state.R6.getValue() == 1,
                () -> testController.state.R7.getValue() == 1,
                () -> testController.state.sfrs.DPL.getValue() == 1
        };

        for (int i = 0; i < opcodes.length; ++i) {
            //testController.state.internalRAM.set(0, (byte)0);
            testOpcode(opcodes[i], 0, i == 12 ? 2 : 1, checks[i]);
        }
    }

    @Test
    public void testDec() {
        System.out.println("__________Testing DEC...");
        byte[] opcodes = {
                0x14, //DEC A
                0x15, //DEC direct
                0x16, //DEC @R0
                0x17, //DEC @R1
                0x18, //DEC R0
                0x19, //DEC R1
                0x1a, //DEC R2
                0x1b, //DEC R3
                0x1c, //DEC R4
                0x1d, //DEC R5
                0x1e, //DEC R6
                0x1f  //DEC R7
        };
        BooleanSupplier[] checks = {
                () -> testController.state.sfrs.A.getValue() == (byte)0xFF && !testController.state.sfrs.PSW.getBit(0),
                () -> testController.state.R0.getValue() == (byte)0xFF,
                () -> testController.state.internalRAM.get(0xFF) == (byte)0xFF,
                () -> testController.state.R0.getValue() == (byte)0xFE,
                () -> testController.state.R0.getValue() == (byte)0xFD,
                () -> testController.state.R1.getValue() == (byte)0xFF,
                () -> testController.state.R2.getValue() == (byte)0xFF,
                () -> testController.state.R3.getValue() == (byte)0xFF,
                () -> testController.state.R4.getValue() == (byte)0xFF,
                () -> testController.state.R5.getValue() == (byte)0xFF,
                () -> testController.state.R6.getValue() == (byte)0xFF,
                () -> testController.state.R7.getValue() == (byte)0xFF,
                () -> testController.state.sfrs.DPL.getValue() == 1
        };
        for (int i = 0; i < opcodes.length; ++i) {
            testOpcode(opcodes[i], 0, 1, checks[i]);
        }
    }

    @Test
    public void testParity() {
        final byte[] testA = {0b01110000, 0b01111111, 0b00101010, (byte)0b11111111, 0b00000000, 0b00000001};
        final boolean [] results = {true, true,       true,        false,           false,      true};
        assertTrue(!testController.state.sfrs.PSW.getBit(0));
        for (int i = 0; i < testA.length; ++i) {
            final boolean desiredRes = results[i];
            A.setValue(testA[i]);
            testOpcode((byte)0, 0, 1, () -> PSW.getBit(0) == desiredRes);
        }
    }

    @Test
    public void testBitJumps() {
        System.out.println("__________Testing JBC, JB, JNB, JC, JNC...");
        final byte JBC = 0x10;
        final byte JB  = 0x20;
        final byte JNB = 0x30;
        final byte JC  = 0x40;
        final byte JNC = 0x50;
        final byte[] opcodes = {JBC, JB, JNB, JC, JNC};
        for (byte opcode : opcodes) {
            final int address = 0x80 + r.nextInt(0xffff - 0xff);
            byte offset = (byte)r.nextInt(256);
            byte[] args = null;
            if (opcode < JC) {
                final byte bitAddr = (byte)r.nextInt(80);
                args = new byte[]{bitAddr, offset};
                testController.setBit(true, bitAddr);
                testOpcode(opcode, address, args, 2, () -> {
                    char result = (char)address;
                    result += 3;
                    if (opcode != JNB) result += offset;
                    boolean res = testController.state.PCH.getValue() == (byte)(result >>> 8 & 0xff)
                            && testController.state.PCL.getValue() == (byte)result;
                    if (opcode == JBC) res &= !testController.getBit(bitAddr);
                    return res;
                });
            }
            else {
                args = new byte[]{offset};
                testController.state.sfrs.PSW.setBit(true, 7);
                assertTrue(testController.getBit((byte)0xD7)); //test if C is really set
                testOpcode(opcode, address, args, 2, () -> {
                    char result = (char)address;
                    result += 2;
                    if (opcode != JNC) result += offset;
                    boolean res = testController.state.PCH.getValue() == (byte)(result >>> 8 & 0xff)
                            && testController.state.PCL.getValue() == (byte)result;
                    return res;
                });
            }
        }
    }

    @Test
    public void testJZJNZ() {
        System.out.println("__________Testing JZ, JNZ...");
        final byte JZ = 0x60;
        final byte JNZ = 0x70;
        final byte offset = (byte)r.nextInt(256);
        final byte[] args = new byte[]{offset};
        final char pc = (char)(130 + r.nextInt(1000));
        A.setValue((byte)0);
        testOpcode(JZ, pc, args, 2, () -> {
            char result = (char)pc;
            result += 2;
            result += offset;
            boolean res = testController.state.PCH.getValue() == (byte)(result >>> 8 & 0xff)
                    && testController.state.PCL.getValue() == (byte)result;
            return res;
        });
        A.setValue((byte)42);
        testOpcode(JZ, pc, args, 2, () -> {
            char result = (char)pc;
            result += 2;
            boolean res = testController.state.PCH.getValue() == (byte)(result >>> 8 & 0xff)
                    && testController.state.PCL.getValue() == (byte)result;
            return res;
        });
        A.setValue((byte)0);
        testOpcode(JNZ, pc, args, 2, () -> {
            char result = (char)pc;
            result += 2;
            boolean res = testController.state.PCH.getValue() == (byte)(result >>> 8 & 0xff)
                    && testController.state.PCL.getValue() == (byte)result;
            return res;
        });
        A.setValue((byte)42);
        testOpcode(JNZ, pc, args, 2, () -> {
            char result = (char)pc;
            result += 2;
            result += offset;
            boolean res = testController.state.PCH.getValue() == (byte)(result >>> 8 & 0xff)
                    && testController.state.PCL.getValue() == (byte)result;
            return res;
        });
    }

    @Test
    public void testJMP() {
        System.out.println("__________Testing JMP...");
        final byte JMP = 0x73;
        final byte a = (byte)r.nextInt(256);
        final char dptr = (char)r.nextInt(0xfff);
        final ByteRegister DPL = testController.state.sfrs.DPL;
        final ByteRegister DPH = testController.state.sfrs.DPH;
        A.setValue(a);
        DPL.setValue((byte)dptr);
        DPH.setValue((byte)(dptr>>>8));
        testOpcode(JMP, 0, 2, () -> {
            final int pc = testController.state.PCH.getValue() << 8 & 0xFF00 | testController.state.PCL.getValue() & 0xff;
            final int res = dptr + (a&0xff);
            return  res == pc;
        });
    }

    @Test
    public void testAcall() {
        System.out.println("__________Testing ACALL...");
        final byte ACALL_STATIC = 0b00010001;
        for (int i = 0; i < 8; ++i) {
            final byte opcode = (byte) (ACALL_STATIC | i << 5);
            final byte arg    = (byte) r.nextInt(0xff);
            final char result = (char) (i << 8 | arg & 0xff);
            testController.state.sfrs.SP.setValue((byte)7);
            testOpcode(opcode, 0, new byte[]{arg}, 2, () -> {
                final char pc = (char) (testController.state.PCH.getValue() << 8 & 0xff00
                        | testController.state.PCL.getValue() & 0xff);
                System.out.println("result = "+result+"; pc = "+pc+"; sp = "+testController.state.sfrs.SP.getValue());
                return pc == result && testController.state.sfrs.SP.getValue() == (byte) 9;
            });
        }
    }

    @Test
    public void testLcall() {
        System.out.println("__________Testing LCALL...");
        final byte LCALL = 0x12;
        final byte high  = (byte)r.nextInt(256);
        final byte low   = (byte)r.nextInt(256);
        testOpcode(LCALL, 42, new byte[]{high, low}, 2, () ->
            testController.state.PCH.getValue() == high && testController.state.PCL.getValue() == low &&
            testController.state.sfrs.SP.getValue() == (byte)9
        );
    }

    @Test
    public void testPush() {
        System.out.println("__________Testing PUSH...");
        final byte PUSH = (byte)0xD0;
        final byte arg = (byte)r.nextInt(255);
        final int stack = r.nextInt(100);
        testController.state.sfrs.SP.setValue((byte)stack);
        testOpcode(PUSH, 111, new byte[]{arg}, 2, () -> testController.state.internalRAM.get(stack + 1) == arg
                && testController.state.sfrs.SP.getValue() == (byte)((stack & 0xFF) + 1));
    }

    @Test
    public void testPop() {
        System.out.println("__________Testing POP...");
        final byte POP = (byte)0xC0;
        final byte stack = (byte)(1 + r.nextInt(255));
        final byte arg = (byte)0x42;
        final byte addr = (byte)r.nextInt(0x80);
        testController.state.sfrs.SP.setValue(stack);
        testController.state.internalRAM.set(stack & 0xFF, arg);
        testOpcode(POP, 2134, new byte[]{addr}, 2, () -> testController.state.internalRAM.get(addr) == arg
                && testController.state.sfrs.SP.getValue() == (byte)((stack & 0xFF)-1));
    }

    @Test
    public void testRRCA() {
        System.out.println("__________Testing RRC A...");
        final byte RRC_A = 0x13;
        final byte[] testA          = {(byte)0b10001001,       0b00011000, (byte)0b10101010};
        final byte[] correctResC0   = {      0b01000100,       0b00001100,       0b01010101};
        final byte[] correctResC1   = {(byte)0b11000100, (byte)0b10001100, (byte)0b11010101};
        for (int i = 0; i < testA.length; ++i) {
            testController.state.sfrs.PSW.setBit(false, 7);
            final byte res1 = correctResC0[i];
            final byte res2 = correctResC1[i];
            testController.state.sfrs.A.setValue(testA[i]);
            testOpcode(RRC_A, 11, 1, () -> testController.state.sfrs.A.getValue() == res1);
            testController.state.sfrs.PSW.setBit(true, 7);
            testController.state.sfrs.A.setValue(testA[i]);
            testOpcode(RRC_A, 11, 1, () -> testController.state.sfrs.A.getValue() == res2);
        }
    }

    @Test
    public void testRLCA() {
        System.out.println("__________Testing RLC A...");
        final byte RLC_A = 0x33;
        final byte[] testA          = {(byte)0b10001001,       0b00011000, (byte)0b10101010};
        final byte[] correctResC0   = {      0b00010010,       0b00110000,       0b01010100};
        final byte[] correctResC1   = {(byte)0b00010011, (byte)0b00110001, (byte)0b01010101};
        for (int i = 0; i < testA.length; ++i) {
            testController.state.sfrs.PSW.setBit(false, 7);
            final byte res1 = correctResC0[i];
            final byte res2 = correctResC1[i];
            testController.state.sfrs.A.setValue(testA[i]);
            testOpcode(RLC_A, 11, 1, () -> testController.state.sfrs.A.getValue() == res1);
            testController.state.sfrs.PSW.setBit(true, 7);
            testController.state.sfrs.A.setValue(testA[i]);
            testOpcode(RLC_A, 11, 1, () -> testController.state.sfrs.A.getValue() == res2);
            if (i != 1) assertTrue(testController.state.sfrs.PSW.getBit(7));
            else assertTrue(!testController.state.sfrs.PSW.getBit(7));
        }
    }

    @Test
    public void testAdd() {
        System.out.println("__________Testing ADD...");
        final byte ADD_IMM  = 0x24;
        final byte ADD_DIR  = 0x25;
        final byte ADD_IND0 = 0x26;
        final byte ADD_IND1 = 0x27;
        final byte ADD_R0   = 0x28;
        final byte ADD_R1   = 0x29;
        final byte ADD_R2   = 0x2a;
        final byte ADD_R3   = 0x2b;
        final byte ADD_R4   = 0x2c;
        final byte ADD_R5   = 0x2d;
        final byte ADD_R6   = 0x2e;
        final byte ADD_R7   = 0x2f;
        final ByteRegister A = testController.state.sfrs.A;
        final FlagRegister PSW = testController.state.sfrs.PSW;
        final ByteRegister R0 = testController.state.R0;
        A.setValue((byte)42);
        testOpcode(ADD_IMM, 0, new byte[]{42}, 1, () -> A.getValue() == (byte)(84) && !PSW.getBit(7) && !PSW.getBit(2)
                && PSW.getBit(6) && PSW.getBit(0));
        R0.setValue((byte)1);
        testOpcode(ADD_DIR, 0, new byte[]{0}, 1, () -> A.getValue() == (byte) (85));
        testOpcode(ADD_IND0, 0, 1, () -> A.getValue() == (byte)85);
        testOpcode(ADD_IND1, 0, 1, () -> A.getValue() == (byte)86);
        testOpcode(ADD_R0, 0, 1, () -> A.getValue() == (byte)87);
        byte op = ADD_R1;
        for (int i = 1; i < 8; ++i) {
            testController.state.getR(i).setValue((byte)1);
            final int ordinal = i;
            testOpcode(op++, 0, 1, () -> A.getValue() == (byte)(87+ordinal));
        }
        //test C, OV and AC
        A.setValue((byte)0x7F);
        testController.state.getR(2).setValue((byte)0x7F);
        testOpcode(ADD_R2, 0, 1, () -> A.getValue() == (byte)-2 && !PSW.getBit(7) && PSW.getBit(2) && PSW.getBit(6)
        && PSW.getBit(0));
        A.setValue((byte)-127);
        testController.state.getR(3).setValue((byte)-10);
        testOpcode(ADD_R3, 0, 1, () -> A.getValue() == (byte)119 && PSW.getBit(2) && PSW.getBit(7) && !PSW.getBit(6));
        testController.state.getR(4).setValue((byte)-128);
        testOpcode(ADD_R4, 0, 1, () -> A.getValue() == (byte)-9 && !PSW.getBit(2) && !PSW.getBit(7) && !PSW.getBit(6));
        A.setValue((byte)127);
        testController.state.getR(5).setValue((byte)-1);
        testOpcode(ADD_R5, 0, 1, () -> A.getValue() == (byte)126 && !PSW.getBit(2) && PSW.getBit(7) && PSW.getBit(6));
    }

    @Test
    public void testAddc() {
        System.out.println("__________Testing ADDC...");
        final byte ADDC_IMM  = 0x34;
        final byte ADDC_DIR  = 0x35;
        final byte ADDC_IND0 = 0x36;
        final byte ADDC_IND1 = 0x37;
        final byte ADDC_R0   = 0x38;
        final ByteRegister A = testController.state.sfrs.A;
        final FlagRegister PSW = testController.state.sfrs.PSW;
        final ByteRegister R0 = testController.state.R0;
        final RAM ram = testController.state.internalRAM;

        PSW.setBit(true, 7);
        testOpcode(ADDC_IMM, 0, new byte[]{42}, 1, () -> A.getValue() == (byte)43);
        ram.set(0, (byte)12);
        PSW.setBit(false, 7);
        testOpcode(ADDC_DIR, 0, new byte[]{0}, 1, () -> A.getValue() == (byte)55);
        PSW.setBit(true, 7);
        testOpcode(ADDC_IND0, 0, 1, () -> A.getValue() == (byte)56);
        testOpcode(ADDC_IND1, 0, 1, () -> A.getValue() == (byte)68);
        for (int i = 0; i < 8; ++i) {
            A.setValue((byte)0);
            PSW.setBit(true, 7);
            testController.state.getR(i).setValue((byte)i);
            final int finI = i;
            testOpcode((byte)(ADDC_R0+i), 0,1, () -> A.getValue() == (byte)(finI+1));
            PSW.setBit(false, 7);
            testOpcode((byte)(ADDC_R0+i), 0, 1, () -> A.getValue() == (byte)(finI + 1 + finI));
        }
    }

    @Test
    public void testSUBB() {
        System.out.println("__________Testing SUBB...");
        final byte SUBB_A_immed = (byte)0x94;
        final byte SUBB_A_dir   = (byte)0x95;
        final byte SUBB_A_ind0  = (byte)0x96;
        final byte SUBB_A_ind1  = (byte)0x97;
        final byte SUBB_A_R0    = (byte)0x98;
        final byte SUBB_A_R1    = (byte)0x99;
        final byte SUBB_A_R2    = (byte)0x9A;
        final byte SUBB_A_R3    = (byte)0x9B;
        final byte SUBB_A_R4    = (byte)0x9C;
        final byte SUBB_A_R5    = (byte)0x9D;
        final byte SUBB_A_R6    = (byte)0x9E;
        final byte SUBB_A_R7    = (byte)0x9F;
        Runnable printPsw = () -> {
            System.out.println("C: "+PSW.getBit(7)+" AC: "+PSW.getBit(6)+" OV: "+PSW.getBit(2));
            System.out.printf("A = %02X%n", A.getValue() & 0xFF);
        };
        A.setValue((byte)0x42);
        testOpcode(SUBB_A_immed, 0, new byte[]{0x5}, 1, () -> {
            printPsw.run();
            return A.getValue() == (byte) 0x3D && PSW.getBit(6)
                    && !PSW.getBit(2) && !PSW.getBit(7);
        });
        A.setValue((byte)1);
        PSW.setBit(true, 7);
        testOpcode(SUBB_A_immed, 0, new byte[]{(byte)0xFF}, 1, () -> {
            printPsw.run();
            return A.getValue() == (byte)0x01 && PSW.getBit(7) && !PSW.getBit(6)
                    && !PSW.getBit(2);
        });
        byte directAddr = (byte)r.nextInt(0x80);
        ram.set(directAddr, (byte) 0x84);
        A.setValue((byte) 0x22);
        PSW.setBit(false, 7);
        testOpcode(SUBB_A_dir, 0, new byte[]{directAddr}, 1, () -> A.getValue() == (byte)0x9E && PSW.getBit(7)
                && PSW.getBit(6) && PSW.getBit(2) && PSW.getBit(0));
        directAddr = directAddr == 0 ? 42 : directAddr;
        testController.state.R0.setValue(directAddr);
        ram.set(directAddr, (byte) 0xFF);
        A.setValue((byte) 0xFF);
        PSW.setBit(false, 7);
        //TODO: This failed a test; FIX
        testOpcode(SUBB_A_ind0, 0, 1, () -> A.getValue() == 0 && !PSW.getBit(7) && !PSW.getBit(6) && !PSW.getBit(2));
        testController.state.R1.setValue(directAddr);
        ram.set(directAddr, (byte) 0xFF);
        A.setValue((byte) 1);
        PSW.setBit(false, 7);
        testOpcode(SUBB_A_ind1, 0, 1, () -> A.getValue() == (byte)0x02 && PSW.getBit(7) && PSW.getBit(6)
                && !PSW.getBit(2));
        A.setValue((byte)0x80);
        testController.state.R0.setValue((byte)1);
        PSW.setBit(false, 7);
        testOpcode(SUBB_A_R0, 0, 1, () -> {
                printPsw.run();
                return A.getValue() == (byte)0x7F && !PSW.getBit(7) && PSW.getBit(6) && PSW.getBit(2);
        });
        A.setValue((byte)0x80);
        testController.state.R1.setValue((byte)1);
        PSW.setBit(true, 7);
        testOpcode(SUBB_A_R1, 0, 1, () -> {
            printPsw.run();
            return A.getValue() == (byte)0x7E && !PSW.getBit(7) && PSW.getBit(6) && PSW.getBit(2);
        });
        A.setValue((byte)0x80);
        testController.state.R2.setValue((byte)0x80);
        PSW.setBit(true, 7);
        testOpcode(SUBB_A_R2, 0, 1, () -> {
            printPsw.run();
            return A.getValue() == (byte)0xFF && PSW.getBit(7) && PSW.getBit(6) && !PSW.getBit(2);
        });
        A.setValue((byte)1);
        testController.state.R3.setValue((byte)0x80);
        PSW.setBit(true, 7);
        testOpcode(SUBB_A_R3, 0, 1, () -> {
            printPsw.run();
            return A.getValue() == (byte)0x80 && PSW.getBit(7) && !PSW.getBit(6) && PSW.getBit(2);
        });
        A.setValue((byte)0);
        testController.state.R4.setValue((byte)0);
        PSW.setBit(false, 7);
        testOpcode(SUBB_A_R4, 0, 1, () -> {
            printPsw.run();
            return A.getValue() == (byte)0 && !PSW.getBit(7) && !PSW.getBit(6) && !PSW.getBit(2);
        });
        A.setValue((byte)0);
        testController.state.R5.setValue((byte)0);
        PSW.setBit(true, 7);
        testOpcode(SUBB_A_R5, 0, 1, () -> {
            printPsw.run();
            return A.getValue() == (byte)0xFF && PSW.getBit(7) && PSW.getBit(6) && !PSW.getBit(2);
        });
        A.setValue((byte)0x80);
        PSW.setBit(false, 7);
        testController.state.R6.setValue((byte)1);
        testOpcode(SUBB_A_R6, 0, 1, () -> {
            printPsw.run();
            return A.getValue() == (byte)0x7F && !PSW.getBit(7) && PSW.getBit(6) && PSW.getBit(2);
        });
        A.setValue((byte)0x7F);
        PSW.setBit(true, 7);
        testController.state.R7.setValue((byte)0xF0);
        testOpcode(SUBB_A_R7, 0, 1, () -> {
            printPsw.run();
            return A.getValue() == (byte)0x8E && PSW.getBit(7) && !PSW.getBit(6) && PSW.getBit(2);
        });
    }

    @Test
    public void testORL() {
        System.out.println("__________Testing ORL...");
        final byte ORL_direct_A = 0x42;
        final byte ORL_direct_immed = 0x43;
        final byte ORL_A_immed = 0x44;
        final byte ORL_A_direct = 0x45;
        final byte ORL_A_ind0 = 0x46;
        final byte ORL_A_ind1 = 0x47;
        final byte ORL_A_R0 = 0x48;
        final byte directAddr1 = (byte)r.nextInt(0x80);
        final byte random = (byte)r.nextInt(256);
        ram.set(directAddr1 & 0xFF, random);
        final byte a1 = (byte)r.nextInt(256);
        A.setValue(a1);
        testOpcode(ORL_direct_A, 0, new byte[]{directAddr1}, 1, () -> ram.get(directAddr1 & 0xff) == (random | a1));
        ram.set(directAddr1, (byte)0x2A);
        testOpcode(ORL_direct_immed, 0, new byte[]{directAddr1, 0x42}, 2, () -> ram.get(directAddr1) == (0x2A|0x42));
        A.setValue((byte)-1);
        testOpcode(ORL_A_immed, 0, new byte[]{(byte)r.nextInt(256)}, 1, () -> A.getValue() == (byte)-1);
        ram.set(directAddr1, (byte)0x22);
        final byte a = A.getValue();
        testOpcode(ORL_A_direct, 0, new byte[]{directAddr1}, 1, () -> A.getValue() == (a|0x22));
        testController.state.R0.setValue((byte)0x42);
        testController.state.R1.setValue((byte)0x42);
        ram.set(0x42, (byte)0xFF);
        testOpcode(Math.random() > 0.5 ? ORL_A_ind0 : ORL_A_ind1, 0, 1, () -> A.getValue() == (byte)0xFF);
        for (int i = 0; i < 8; ++i) {
            testController.state.getR(i).setValue(random);
            A.setValue((byte)0x88);
            testOpcode((byte)(ORL_A_R0+i), 0, 1, () -> A.getValue() == (byte)(random & 0xFF | 0x88));
        }
        final byte ORL_C_bit = 0x72;
        final byte ORL_C_not_bit = (byte)0xA0;
        final boolean bit = r.nextBoolean();
        final boolean cBit = r.nextBoolean();
        testController.setBit(bit, (byte)0x10);
        PSW.setBit(cBit, 7);
        testOpcode(ORL_C_bit, 0, new byte[]{0x10}, 2, () -> PSW.getBit(7) == (bit | cBit));
        testOpcode(ORL_C_not_bit, 0, new byte[]{0x10}, 2, () -> PSW.getBit(7));
    }

    @Test
    public void testANL() {
        System.out.println("__________Testing ANL...");
        final byte ANL_direct_A = 0x52;
        final byte ANL_direct_immed = 0x53;
        final byte ANL_A_immed = 0x54;
        final byte ANL_A_direct = 0x55;
        final byte ANL_A_ind0 = 0x56;
        final byte ANL_A_ind1 = 0x57;
        final byte ANL_A_R0 = 0x58;
        final byte directAddr1 = (byte)r.nextInt(0x80);
        final byte random = (byte)r.nextInt(256);
        ram.set(directAddr1 & 0xFF, random);
        final byte a1 = (byte)r.nextInt(256);
        A.setValue(a1);
        testOpcode(ANL_direct_A, 0, new byte[]{directAddr1}, 1, () -> ram.get(directAddr1&0xff) == (random&a1));
        ram.set(directAddr1, (byte)0x2A);
        testOpcode(ANL_direct_immed, 0, new byte[]{directAddr1, 0x42}, 2, () -> ram.get(directAddr1) == (0x2A&0x42));
        A.setValue((byte)0);
        testOpcode(ANL_A_immed, 0, new byte[]{(byte)r.nextInt(256)}, 1, () -> A.getValue() == (byte)0);
        ram.set(directAddr1, (byte)0x22);
        final byte a = A.getValue();
        testOpcode(ANL_A_direct, 0, new byte[]{directAddr1}, 1, () -> A.getValue() == (a&0x22));
        testController.state.R0.setValue((byte)0x42);
        testController.state.R1.setValue((byte)0x42);
        ram.set(0x42, (byte)0);
        testOpcode(Math.random() > 0.5 ? ANL_A_ind0 : ANL_A_ind1, 0, 1, () -> A.getValue() == (byte)0);
        for (int i = 0; i < 8; ++i) {
            testController.state.getR(i).setValue(random);
            A.setValue((byte)0x88);
            testOpcode((byte)(ANL_A_R0+i), 0, 1, () -> A.getValue() == (byte)(random & 0x88));
        }
        final byte ANL_C_bit = (byte) 0x82;
        final byte ANL_C_not_bit = (byte)0xB0;
        final boolean bit = r.nextBoolean();
        final boolean cBit = r.nextBoolean();
        testController.setBit(bit, (byte)0x10);
        PSW.setBit(cBit, 7);
        testOpcode(ANL_C_bit, 0, new byte[]{0x10}, 2, () -> PSW.getBit(7) == (bit && cBit));
        PSW.setBit(cBit, 7);
        testOpcode(ANL_C_not_bit, 0, new byte[]{0x10}, 2, () -> PSW.getBit(7) == (cBit & !bit));
    }

    @Test
    public void testXRL() {
        System.out.println("__________Testing XRL...");
        final byte XRL_direct_A = 0x62;
        final byte XRL_direct_immed = 0x63;
        final byte XRL_A_immed = 0x64;
        final byte XRL_A_direct = 0x65;
        final byte XRL_A_ind0 = 0x66;
        final byte XRL_A_ind1 = 0x67;
        final byte XRL_A_R0 = 0x68;
        final byte directAddr1 = (byte)r.nextInt(0x80);
        final byte random = (byte)r.nextInt(256);
        ram.set(directAddr1 & 0xFF, random);
        final byte a1 = (byte)r.nextInt(256);
        A.setValue(a1);
        testOpcode(XRL_direct_A, 0, new byte[]{directAddr1}, 1, () -> ram.get(directAddr1&0xff) == (random^a1));
        ram.set(directAddr1, (byte)0x2A);
        testOpcode(XRL_direct_immed, 0, new byte[]{directAddr1, 0x42}, 2, () -> ram.get(directAddr1) == (0x2A^0x42));
        A.setValue((byte)0);
        testOpcode(XRL_A_immed, 0, new byte[]{random}, 1, () -> A.getValue() == random);
        ram.set(directAddr1, (byte)0x22);
        final byte a = A.getValue();
        testOpcode(XRL_A_direct, 0, new byte[]{directAddr1}, 1, () -> A.getValue() == (a^0x22));
        testController.state.R0.setValue((byte)0x42);
        testController.state.R1.setValue((byte)0x42);
        ram.set(0x42, (byte)0);
        final byte a2 = A.getValue();
        testOpcode(Math.random() > 0.5 ? XRL_A_ind0 : XRL_A_ind1, 0, 1, () -> A.getValue() == a2);
        for (int i = 0; i < 8; ++i) {
            testController.state.getR(i).setValue(random);
            A.setValue((byte)0x88);
            testOpcode((byte)(XRL_A_R0+i), 0, 1, () -> A.getValue() == (byte)(random ^ 0x88));
        }
    }

    @Test
    public void testMov() {
        System.out.println("__________Testing MOV...");
        final byte MOV_A_immed = 0x74;
        final byte MOV_direct_immed = 0x75;
        final byte MOV_ind_immed0 = 0x76;
        final byte MOV_ind_immed1 = 0x77;
        final byte MOV_R0_immed = 0x78;
        final byte MOV_direct_direct = (byte)0x85;
        final byte MOV_direct_ind0 = (byte)0x86;
        final byte MOV_direct_ind1 = (byte)0x87;
        final byte MOV_direct_R0 = (byte)0x88;
        final byte MOV_DPTR_immed = (byte)0x90;
        final byte MOV_bit_C = (byte)0x92;
        final byte MOV_C_bit = (byte)0xA2;
        final byte MOV_ind_dir0 = (byte)0xA6;
        final byte MOV_ind_dir1 = (byte)0xA7;
        final byte MOV_R0_dir = (byte)0xA8;
        final byte MOV_A_dir = (byte)0xE5;
        final byte MOV_A_ind0 = (byte)0xE6;
        final byte MOV_A_ind1 = (byte)0xE7;
        final byte MOV_A_R0 = (byte)0xE8;
        final byte MOV_dir_A = (byte)0xF5;
        final byte MOV_ind_A0 = (byte)0xF6;
        final byte MOV_ind_A1 = (byte)0xF7;
        final byte MOV_R0_A = (byte)0xF8;
        testOpcode(MOV_A_immed, 0, new byte[]{0x42}, 1, () -> A.getValue() == (byte)0x42);
        testOpcode(MOV_direct_immed, 0, new byte[]{10, 42}, 2, () -> ram.get(10) == (byte)42);
        testOpcode(MOV_ind_immed0, 0, new byte[]{22}, 1, () -> ram.get(0) == (byte)22);
        testController.state.R1.setValue((byte)33);
        testOpcode(MOV_ind_immed1, 0, new byte[]{22}, 1, () -> ram.get(33) == (byte) 22);
        for (int i = 0; i < 8; ++i) {
            final int fI = i;
            testOpcode((byte)(MOV_R0_immed+i), 0, new byte[]{90}, 1,
                    () -> testController.state.getR(fI).getValue() == (byte)90);
        }
        final byte direct1 = 0x22;
        final byte direct2 = 0x42;
        ram.set(direct2 & 0xff, (byte)44);
        testOpcode(MOV_direct_direct, 0, new byte[]{direct2, direct1}, 2, () -> ram.get(direct1 & 0xff) == (byte)44);
        testController.state.R0.setValue((byte)33);
        ram.set(33, (byte)21);
        testOpcode(MOV_direct_ind0, 0, new byte[]{50}, 2, () -> ram.get(50) == (byte)21);
        ram.set(1, (byte)33);
        ram.set(33, (byte)22);
        testOpcode(MOV_direct_ind1, 0, new byte[]{50}, 2, () -> ram.get(50) == (byte) 22);
        for (int i = 0; i < 8; ++i) {
            final int fI = i;
            testController.state.getR(fI).setValue((byte)0x22);
            testOpcode((byte) (MOV_direct_R0 + i), 0, new byte[]{10}, 2, () -> ram.get(10) == (byte) 0x22);
        }
        testOpcode(MOV_DPTR_immed, 0, new byte[]{22,23}, 2, () -> testController.state.sfrs.DPH.getValue() == (byte)22
                && testController.state.sfrs.DPL.getValue() == (byte)23);
        PSW.setBit(true, 7);
        testOpcode(MOV_bit_C, 0, new byte[]{1}, 2, () -> testController.getBit((byte) 1));
        testController.setBit(true, (byte)42);
        PSW.setBit(false, 7);
        testOpcode(MOV_C_bit, 0, new byte[]{42}, 2, () -> PSW.getBit(7));
        testController.state.R0.setValue((byte)18);
        ram.set(42, (byte)33);
        testOpcode(MOV_ind_dir0, 0, new byte[]{42}, 2, () -> ram.get(18) == (byte)33);
        testController.state.R1.setValue((byte)19);
        ram.set(42, (byte)33);
        testOpcode(MOV_ind_dir1, 0, new byte[]{42}, 2, () -> ram.get(19) == (byte)33);
        for (int i = 0; i < 8; ++i) {
            final byte direct = (byte)r.nextInt(0x80);
            ram.set(direct & 0xff, (byte)111);
            final int fI = i;
            testOpcode((byte)(MOV_R0_dir+i), 0, new byte[]{direct},2,
                    () -> testController.state.getR(fI).getValue() == (byte)111);
        }
        ram.set(1, (byte)88);
        testOpcode(MOV_A_dir, 0, new byte[]{1}, 1, () -> A.getValue() == (byte)88);
        ram.set(2, (byte)0xFF);
        testController.state.R0.setValue((byte)2);
        testController.state.R1.setValue((byte)2);
        testOpcode(MOV_A_ind0, 0, 1, () -> A.getValue() == (byte)0xFF);
        testOpcode(MOV_A_ind1, 0, 1, () -> A.getValue() == (byte)0xFF);
        for (int i = 0; i < 8; ++i) {
            final byte random = (byte)r.nextInt(0x100);
            testController.state.getR(i).setValue(random);
            testOpcode((byte)(MOV_A_R0+i), 0, 1, () -> A.getValue() == random);
        }
        byte randomA = (byte)r.nextInt(0x100);
        A.setValue(randomA);
        testOpcode(MOV_dir_A, 0, new byte[]{22}, 1, () -> ram.get(22) == randomA);
        testController.state.R0.setValue((byte)0x88);
        testController.state.R1.setValue((byte)0x89);
        testOpcode(MOV_ind_A0, 0, 1, () -> ram.get(0x88) == randomA);
        testOpcode(MOV_ind_A1, 0, 1, () -> ram.get(0x89) == randomA);
        for (int i = 0; i < 8; ++i) {
            final int fI = i;
            testOpcode((byte)(MOV_R0_A+i), 0, 1, () -> testController.state.getR(fI).getValue() == randomA);
        }
    }

    @Test
    public void testSJMP() {
        System.out.println("___________Testing SJMP...");
        final byte SJMP = (byte)0x80;
        final int address = r.nextInt(0xfff)+128;
        final byte offset = (byte)r.nextInt(0x100);
        final int result = address + offset + 2;
        testOpcode(SJMP, address, new byte[]{offset}, 2, ()->((PCH.getValue()&0xff)<<8|PCL.getValue()&0xff) == result);
    }

    @Test
    public void testMOVC() {
        System.out.println("__________Testing MOVC...");
        final byte MOVC_A_PC = (byte)0x83;
        final byte MOVC_A_DPTR = (byte)0x93;
        final RAM code = (RAM)testController.state.codeMemory;
        final byte a = (byte)r.nextInt(0x80);
        final char dptr = (char)(128+r.nextInt(0xfff));
        code.set(dptr+(a&0xff), (byte)0x42);
        testController.state.sfrs.DPL.setValue((byte)dptr);
        testController.state.sfrs.DPH.setValue((byte) (dptr >> 8));
        A.setValue(a);
        testOpcode(MOVC_A_DPTR, 0, 2, () -> A.getValue() == (byte) 0x42);
        final char pc = dptr;
        A.setValue(a);
        code.set(pc + (a&0xff) + 1, (byte)0x55);
        testOpcode(MOVC_A_PC, pc, 2, () -> A.getValue() == (byte)0x55);
    }

    @Test
    public void testDIV() {
        System.out.println("__________Testing DIV...");
        final byte DIV_AB = (byte)0x84;
        final byte a = (byte)(r.nextInt(0x100));
        final byte b = (byte)(r.nextInt(0xff)+1);
        A.setValue(a);
        B.setValue(b);
        testOpcode(DIV_AB, 0, 4, () -> A.getValue() == (byte)((a&0xff)/(b&0xff))
                && B.getValue() == (byte)((a&0xff)%(b&0xff))
                && !PSW.getBit(7) && !PSW.getBit(2));
        B.setValue((byte)0);
        testOpcode(DIV_AB, 0, 4, () -> !PSW.getBit(7) && PSW.getBit(2));
    }

    @Test
    public void testMUL() {
        System.out.println("__________Testing MUL...");
        final byte MUL = (byte)0xA4;
        A.setValue((byte)6);
        B.setValue((byte)7);
        testOpcode(MUL, 0, 4, () -> A.getValue() == (byte)42 && B.getValue() == 0 && !PSW.getBit(2) && !PSW.getBit(7));
        A.setValue((byte)0xFF);
        B.setValue((byte)0xFf);
        testOpcode(MUL, 0, 4, () -> A.getValue() == (byte)0x01 && B.getValue() == (byte)0xFE && PSW.getBit(2)
                && !PSW.getBit(7) && PSW.getBit(0));
    }

    @Test(expected=EmulatorException.class)
    public void testReserved() throws EmulatorException {
        final byte RESERVED = (byte)0xA5;
        System.out.println("__________Testing <reserved>...");
        RAM rom = (RAM) testController.getCodeMemory();
        rom.set(0, RESERVED);
        testController.next();
    }

    @Test
    public void testCPL() {
        System.out.println("__________Testing CPL...");
        final byte CPL_bit = (byte)0xB2;
        final byte CPL_C = (byte)0xB3;
        final byte CPL_A = (byte)0xF4;
        testOpcode(CPL_bit, 0, new byte[]{22}, 1, () -> testController.getBit((byte)22));
        testOpcode(CPL_bit, 0, new byte[]{22}, 1, () -> !testController.getBit((byte)22));
        testOpcode(CPL_C, 0, 1, () -> testController.state.sfrs.PSW.getBit(7));
        testOpcode(CPL_C, 0, 1, () -> !testController.state.sfrs.PSW.getBit(7));
        final byte a = (byte)r.nextInt(256);
        A.setValue(a);
        testOpcode(CPL_A, 0, 1, () -> A.getValue() == (byte) ~a);
    }

    @Test
    public void testCJNE() {
        System.out.println("__________Testing CJNE...");
        final byte CJNE_A_immed  = (byte)0xB4;
        final byte CJNE_A_dir    = (byte)0xB5;
        final byte CJNE_ind_imm0 = (byte)0xB6;
        final byte CJNE_ind_imm1 = (byte)0xB7;
        final byte CJNE_R0_imm   = (byte)0xB8;
        final int address = 128 + r.nextInt(0xfff);
        A.setValue((byte)1);
        testOpcode(CJNE_A_immed, address, new byte[]{42, -5}, 2, () -> {
            int pc = PCH.getValue() << 8  & 0xFF00 | PCL.getValue() & 0xFF;
            int result = address + 3 - 5;
            return pc == result && PSW.getBit(7);
        });
        ram.set(22, (byte)21);
        A.setValue((byte)21);
        testOpcode(CJNE_A_dir, address, new byte[]{22, 0}, 2, () -> {
            int pc = PCH.getValue() << 8 & 0xFF00 | PCL.getValue() & 0xFF;
            int result = address + 3;
            return pc == result && !PSW.getBit(7);
        });
        testController.state.R0.setValue((byte)10);
        ram.set(10, (byte)100);
        testOpcode(CJNE_ind_imm0, address, new byte[]{50, -40}, 2, () -> {
            int pc = PCH.getValue() << 8  & 0xFF00 | PCL.getValue() & 0xFF;
            int result = address + 3 - 40;
            return pc == result && !PSW.getBit(7);
        });
        testController.state.R1.setValue((byte)10);
        ram.set(10, (byte)50);
        testOpcode(CJNE_ind_imm1, address, new byte[]{60, -40}, 2, () -> {
            int pc = PCH.getValue() << 8  & 0xFF00 | PCL.getValue() & 0xFF;
            int result = address + 3 - 40;
            return pc == result && PSW.getBit(7);
        });
        for (int i = 0; i < 8; ++i) {
            testController.state.getR(i).setValue((byte)0x2A);
            testOpcode((byte)(CJNE_R0_imm+i), address, new byte[]{0x2A, 22}, 2, () -> {
                int pc = PCH.getValue() << 8  & 0xFF00 | PCL.getValue() & 0xFF;
                return pc == address + 3 && !PSW.getBit(7);
            });
        }
    }

    @Test
    public void testCLR() {
        System.out.println("__________Testing CLR...");
        final byte CLR_bit = (byte)0xC2;
        final byte CLR_C = (byte)0xC3;
        final byte CLR_A = (byte)0xE4;
        final byte bitaddr = 10;
        testController.setBit(true, bitaddr);
        testOpcode(CLR_bit, 0, new byte[]{bitaddr}, 1, () -> !testController.getBit(bitaddr));
        PSW.setBit(true, 7);
        testOpcode(CLR_C, 0, 1, () -> !PSW.getBit(7));
        A.setValue((byte)3);
        testOpcode(CLR_A, 0, 1, () -> A.getValue() == 0);
    }

    @Test
    public void testSWAP() {
        System.out.println("__________Testing SWAP...");
        final byte SWAP_A = (byte)0xC4;
        A.setValue((byte)0xFE);
        testOpcode(SWAP_A, 0, 1, () -> A.getValue() == (byte)0xEF);
    }

    @Test
    public void testXCH() {
        System.out.println("__________Testing XCH...");
        final byte XCH_A_direct = (byte)0xC5;
        final byte XCH_A_ind0   = (byte)0xC6;
        final byte XCH_A_ind1   = (byte)0xC7;
        final byte XCH_A_R0     = (byte)0xC8;
        final byte val1 = 0x11;
        final byte val2 = 0x22;
        A.setValue(val1);
        final byte direct = 120;
        ram.set(direct, val2);
        testOpcode(XCH_A_direct, 0, new byte[]{direct}, 1, () -> A.getValue() == val2 && ram.get(direct) == val1);
        final byte indirect = 100;
        testController.state.R0.setValue(indirect);
        ram.set(indirect, val2);
        A.setValue(val1);
        testOpcode(XCH_A_ind0, 0, new byte[]{indirect}, 1, () -> A.getValue() == val2 && ram.get(indirect) == val1);
        testController.state.R1.setValue(indirect);
        ram.set(indirect, val2);
        A.setValue(val1);
        testOpcode(XCH_A_ind1, 0, new byte[]{indirect}, 1, () -> A.getValue() == val2 && ram.get(indirect) == val1);
        for (int i = 0; i < 8; ++i) {
            testController.state.getR(i).setValue(val2);
            A.setValue(val1);
            final int fI = i;
            testOpcode((byte)(XCH_A_R0+i), 0, 1, () -> A.getValue() == val2
                    && testController.state.getR(fI).getValue() == val1);
        }
    }

    @Test
    public void testSETB() {
        System.out.println("__________Testing SETB...");
        final byte SETB_bit = (byte)0xD2;
        final byte SETB_C = (byte)0xD3;
        final byte bitaddr = 12;
        testController.setBit(false, bitaddr);
        testOpcode(SETB_bit, 0, new byte[]{bitaddr}, 1, () -> testController.getBit(bitaddr));
        PSW.setBit(false, 7);
        testOpcode(SETB_C, 0, 1, () -> PSW.getBit(7));
    }

    @Test
    public void testDA() {
        System.out.println("__________Testing DA...");
        final byte DA_A = (byte)0xD4;
        final byte ADD_A_imm = 0x24;
        A.setValue((byte)0x42);
        testOpcode(DA_A, 0, 1, () -> !PSW.getBit(7) && A.getValue() == (byte)0x42);
        A.setValue((byte)0x42);
        testOpcode(ADD_A_imm, 0, new byte[]{0x42}, 1, () -> A.getValue() == (byte)(0x42+0x42));
        testOpcode(DA_A, 0, 1, () -> !PSW.getBit(7) && A.getValue() == (byte)0x84);
        A.setValue((byte)0x51);
        testOpcode(ADD_A_imm, 0, new byte[]{0x51}, 1, () -> A.getValue() == (byte)(0x51+0x51));
        testOpcode(DA_A, 0, 1, () -> PSW.getBit(7) && A.getValue() == (byte)0x02);
        A.setValue((byte)0x99);
        PSW.setBit(false, 7);
        testOpcode(DA_A, 0, 1, () -> !PSW.getBit(7) && A.getValue() == (byte)0x99);
        A.setValue((byte)(0x21 + 0x75));
        testOpcode(DA_A, 0, 1, () -> A.getValue() == (byte)0x96 && !PSW.getBit(7));
        A.setValue((byte)0);
        testOpcode(DA_A, 0, 1, () -> A.getValue() == 0 && !PSW.getBit(7));
        A.setValue((byte)0x4A);
        testOpcode(DA_A, 0, 1, () -> A.getValue() == (byte)0x50 && !PSW.getBit(7));
    }

    @Test
    public void testDJNZ() {
        System.out.println("__________Testing DJNZ...");
        final byte DJNZ_dir = (byte)0xD5;
        final byte DJNZ_R0  = (byte)0xD8;
        final byte DJNZ_R1  = (byte)0xD9;
        final byte randomGtOne = (byte)(1 + r.nextInt(0xff));
        final byte directAddr  = (byte)r.nextInt(0x80);
        final int address = 128 + r.nextInt(0xfff);
        final byte offset = (byte)r.nextInt(256);
        ram.set(directAddr, randomGtOne);
        testOpcode(DJNZ_dir, address, new byte[]{directAddr, offset}, 2, () -> {
            final int pc = PCH.getValue() << 8 & 0xFF00 | PCL.getValue() & 0xFF;
            final int result = address + 3 + offset;
            return pc == result;
        });
        testController.state.R0.setValue((byte)1);
        testOpcode(DJNZ_R0, address, new byte[]{55}, 2, () -> {
            final int pc = PCH.getValue() << 8 & 0xFF00 | PCL.getValue() & 0xFF;
            return pc == address + 2;
        });
        for (int i = 0; i < 7; ++i) {
            testController.state.getR(i+1).setValue((byte)42);
            final int fI = i;
            testOpcode((byte)(DJNZ_R1+i), address, new byte[]{offset}, 2, () -> {
                final int pc = PCH.getValue() << 8 & 0xFF00 | PCL.getValue() & 0xFF;
                final int result = address + 2 + offset;
                return pc == result && testController.state.getR(fI+1).getValue() == (byte)41;
            });
        }
    }

    @Test
    public void testXCHD() {
        System.out.println("__________Testing XCHD...");
        final byte XCHD_R0 = (byte)0xD6;
        final byte XCHD_R1 = (byte)0xD7;
        testController.state.R0.setValue((byte)0x13);
        ram.set(0x13, (byte)0x13);
        A.setValue((byte)0xF2);
        testOpcode(XCHD_R0, 0, 1, () -> A.getValue() == (byte)0xF3 && ram.get(0x13) == (byte)0x12);
        testController.state.R1.setValue((byte)0x42);
        ram.set(0x42, (byte)0x42);
        A.setValue((byte)0xF5);
        testOpcode(XCHD_R1, 0, 1, () -> A.getValue() == (byte)0xF2 && ram.get(0x42) == (byte)0x45);
    }

    @Test
    public void testMOVX() {
        System.out.println("__________Testing MOVX...");
        final byte MOVX_A_DPTR = (byte)0xE0;
        final byte MOVX_A_IND0 = (byte)0xE2;
        final byte MOVX_A_IND1 = (byte)0xE3;
        final byte MOVX_DPTR_A = (byte)0xF0;
        final byte MOVX_IND0_A = (byte)0xF2;
        final byte MOVX_IND1_A = (byte)0xF3;
        final ByteRegister DPH = testController.state.sfrs.DPH;
        final ByteRegister DPL = testController.state.sfrs.DPL;
        final RAM extRAM       = testController.state.externalRAM;
        int dptr = r.nextInt(0xFFFF);  DPH.setValue((byte)(dptr >> 8)); DPL.setValue((byte)dptr);
        extRAM.set(dptr, (byte)42);
        testOpcode(MOVX_A_DPTR, 0, 2, () -> A.getValue() == (byte)42);
        testController.state.R0.setValue((byte)dptr);
        extRAM.set(dptr & 0xFF, (byte)0xAA);
        testOpcode(MOVX_A_IND0, 0, 2, () -> A.getValue() == (byte)0xAA);
        testController.state.R1.setValue((byte)dptr);
        extRAM.set(dptr & 0xFF, (byte)0xBB);
        testOpcode(MOVX_A_IND1, 0, 2, () -> A.getValue() == (byte)0xBB);
        A.setValue((byte)0x33);
        testOpcode(MOVX_DPTR_A, 0, 2, () -> extRAM.get(dptr) == (byte)0x33);
        testController.state.R0.setValue((byte)dptr);
        A.setValue((byte)0x88);
        testOpcode(MOVX_IND0_A, 0, 2, () -> extRAM.get(dptr & 0xFF) == (byte)0x88);
        testController.state.R1.setValue((byte)dptr);
        A.setValue((byte)0x88);
        testOpcode(MOVX_IND1_A, 0, 2, () -> extRAM.get(dptr & 0xFF) == (byte)0x88);
    }

    @Test
    public void testTimers() {
        //TODO: Add tests for mode 0 and 3 (maybe) and test event counting
        System.out.println("__________Testing Timers....");
        final byte TMOD = 0x11; //both timers should counts cycles as 16-bit counters
        testController.state.sfrs.TMOD.setValue(TMOD);
        //set TR1 and TR0
        testController.state.sfrs.TCON.setBit(true, 6);
        testController.state.sfrs.TCON.setBit(true, 4);
        final int num = r.nextInt(0xff)+12;
        for (int i = 0; i < num; ++i) {
            try {
                testController.next();
            } catch (EmulatorException e) {
                e.printStackTrace();
            }
        }
        final int timer1_16 = testController.state.sfrs.TH0.getValue() << 8 & 0xFF00
                | testController.state.sfrs.TL0.getValue() & 0xFF;
        final int timer2_16 = testController.state.sfrs.TH1.getValue() << 8 & 0xFF00
                | testController.state.sfrs.TL1.getValue() & 0xFF;
        assertTrue(timer1_16 == num && timer2_16 == num);
        final byte TMOD2 = 0x22; //both timers should be 8-bit auto-reload timer and count cycles
        testController.state.sfrs.TMOD.setValue(TMOD2);
        testController.state.sfrs.TL0.setValue((byte)0);
        testController.state.sfrs.TL1.setValue((byte)0);
        testController.state.sfrs.TH0.setValue((byte)0x42);
        testController.state.sfrs.TH1.setValue((byte)0x2A);
        //set TR1 and TR0
        testController.state.sfrs.TCON.setBit(true, 6);
        testController.state.sfrs.TCON.setBit(true, 4);
        for (int i = 0; i < 256; ++i) {
            try {
                testController.next();
            } catch (EmulatorException e) {
                e.printStackTrace();
            }
        }
        assertTrue(testController.state.sfrs.TH0.getValue() == (byte)0x42
                && testController.state.sfrs.TL0.getValue() == (byte)0x42
                && testController.state.sfrs.TCON.getBit(5)); // bit 5 is TF0
        assertTrue(testController.state.sfrs.TH1.getValue() == (byte)0x2A
                && testController.state.sfrs.TL1.getValue() == (byte)0x2A
                && testController.state.sfrs.TCON.getBit(7)); // bit 7 is TF1
    }

    @Test
    public void testUpdateInterruptRequestFlags() {
        System.out.println("__________Testing interrupt flag updates...");
        testController.state.sfrs.TMOD.setValue((byte)0x11);
        testController.state.sfrs.TH0.setValue((byte)0xFF);
        testController.state.sfrs.TL0.setValue((byte)0xFF);
        testController.state.sfrs.TH1.setValue((byte)0xFF);
        testController.state.sfrs.TL1.setValue((byte)0xFF);
        testController.state.sfrs.TCON.setBit(true, 0); //IT0
        testController.state.sfrs.TCON.setBit(true, 2); //IT1
        testController.state.sfrs.TCON.setBit(true, 4); //TR0
        testController.state.sfrs.TCON.setBit(true, 6); //TR1
        testOpcode((byte) 0, 0, 1, () ->
                        testController.state.sfrs.TCON.getValue() == (byte) 0b11110101
        );
        testController.state.sfrs.P3.setBit(false, 2);
        testController.state.sfrs.P3.setBit(false, 3);
        testOpcode((byte)0, 0, 1, () ->
                        testController.state.sfrs.TCON.getValue() == (byte)0xFF
        );
        testController.state.sfrs.P3.setBit(true, 2);
        testController.state.sfrs.P3.setBit(true, 3);
        testController.state.sfrs.TCON.setBit(false, 0); //IT0
        testController.state.sfrs.TCON.setBit(false, 2); //IT1
        testOpcode((byte)0, 0, 1, () ->
                        testController.state.sfrs.TCON.getValue() == (byte)0b11110000
        );
        testController.state.sfrs.P3.setBit(false, 2);
        testController.state.sfrs.P3.setBit(false, 3);
        testOpcode((byte)0, 0, 1, () ->
                        testController.state.sfrs.TCON.getValue() == (byte)0b11111010
        );
    }

    @Test
    public void testInterrupts() throws Exception {
        System.out.println("__________Testing interrupts...");
        byte[] program = {
                /* 0x0 */    (byte)0x80,       0x03,            // sjmp beginning
                /* 0x2 */          0,                           // <padding>
                /* 0x3 */    (byte)0x80, (byte)0xFE,            // loop: sjmp loop
                /* 0x5 */    (byte)0xD2, (byte)0xAF,            // beginning: setb ea
                /* 0x7 */    (byte)0xD2, (byte)0xA8,            // setb ex0
                /* 0x9 */    (byte)0xD2, (byte)0xB4,            // setb t0
                /* 0xB */    (byte)0xC2, (byte)0xB2,            // clr p3.2
                /* 0xD */          0x02,       0x00,       0x0D // endlabel: ljmp endlabel
        };
        RAM rom = (RAM)this.testController.getCodeMemory();
        for (int address = 0; address < program.length; ++address) rom.set(address, program[address]);
        for (int i = 0; i < 5; ++i) testController.next();
        assertTrue((PCH.getValue() << 8 & 0xFF00 | PCL.getValue() & 0xFF) == 0x3);
        byte[] program2 = {
                /* 0x0  */   (byte)0x80,       0x16,            // sjmp beginning
                /* 0x2  */            0,                        // <padding>
                /* 0x3  */   (byte)0xC2, (byte)0xB3,            // clr p3.3 ; (org 03h)
                /* 0x5  */   (byte)0xD2, (byte)0xB2,            // setb p3.2
                /* 0x7  */            0,          0,            // nop nop
                /* 0x9  */   (byte)0x32,                        // reti
                /* 0xA  */         0,0,0,0,0,0,0,0,0,           // <padding>
                /* 0x13 */   (byte)0xD2, (byte)0xB3,            // setb p3.3 ; (org 13h)
                /* 0x15 */            0,          0,            // nop nop
                /* 0x17 */         0x32,                        // reti
                /* 0x18 */   (byte)0xD2, (byte)0xAF,            // beginning: setb ea ; (org 18h)
                /* 0x1A */   (byte)0xD2, (byte)0xA8,            // setb ex0
                /* 0x1C */   (byte)0xD2, (byte)0xAA,            // setb ex1
                /* 0x1E */   (byte)0xD2, (byte)0xBA,            // setb px1
                /* 0x20 */   (byte)0xC2, (byte)0xB2,            // clr p3.2
                /* 0x22 */         0x02,          0,       0,   // endlabel: ljmp endlabel

        };
        this.setUp();
        rom = (RAM)this.testController.getCodeMemory();
        for (int address = 0; address < program2.length; ++address) rom.set(address, program2[address]);
        for (int i = 0; i < 6; ++i) testController.next();
        assertTrue((PCH.getValue() << 8 & 0xFF00 | PCL.getValue() & 0xFF) == 0x3);
        testController.next();
        assertTrue((PCH.getValue() << 8 & 0xFF00 | PCL.getValue() & 0xFF) == 0x13);
        for (int i = 0; i < 4; ++i) testController.next();
        assertTrue((PCH.getValue() << 8 & 0xFF00 | PCL.getValue() & 0xFF) == 0x5);
        for (int i = 0; i < 4; ++i) testController.next();
        assertTrue((PCH.getValue() << 8 & 0xFF00 | PCL.getValue() & 0xFF) == 0x22);
    }

    @Test
    public void testXmlSerialization() throws IOException {
        System.out.println("__________Testing serialization to XML...");
        // generate some state
        testController.state.PCH.setValue((byte)r.nextInt(256));
        testController.state.PCL.setValue((byte)r.nextInt(256));
        testController.state.R7.setValue((byte)r.nextInt(256));
        testController.state.R6.setValue((byte)r.nextInt(256));
        testController.state.R5.setValue((byte)r.nextInt(256));
        testController.state.R4.setValue((byte)r.nextInt(256));
        testController.state.R3.setValue((byte)r.nextInt(256));
        testController.state.R2.setValue((byte)r.nextInt(256));
        testController.state.R1.setValue((byte)r.nextInt(256));
        testController.state.R0.setValue((byte)r.nextInt(256));
        ((RAM)testController.state.codeMemory).set(r.nextInt(65536), (byte)r.nextInt(256));
        testController.state.internalRAM.set(r.nextInt(256), (byte)r.nextInt(256));
        testController.state.externalRAM.set(r.nextInt(65536), (byte)r.nextInt(256));
        testController.state.sfrs.PSW.setBit(true, 0);
        testController.state.sfrs.A.setValue((byte)0x2a);
        testController.state.sfrs.DPH.setValue((byte)r.nextInt(256));
        testController.state.sfrs.P2.setBit(true, r.nextInt(8));
        testController.state.sfrs.TMOD.setValue((byte)r.nextInt(256));
        Path p = Paths.get("/tmp/" + r.nextInt(9000) + ".b8eSerializationTest");
        System.out.println("test path: "+p);
        testController.saveStateTo(p);
        State8051 restoredState = new MC8051(p).state;
        assertTrue("failed serialization test", testController.state.equals(restoredState));
        assertTrue("sfr map not updated after deserialization",
                restoredState.sfrs.B == restoredState.sfrs.getRegister((byte)0xf0)
                && restoredState.sfrs.PSW == restoredState.sfrs.getRegister((byte)0xd0)
        );
    }

    private void testOpcode(byte opcode, int address, int desiredReturn, BooleanSupplier resultCorrect) {
        testOpcode(opcode, address, new byte[0], desiredReturn, resultCorrect);
    }

    private void testOpcode(byte opcode, int address, byte[] args, int desiredReturn, BooleanSupplier resultCorrect) {
        System.out.printf("Opcode: %02X%n", opcode & 0xFF);
        final RAM ram = (RAM) testController.getCodeMemory();
        ram.set(address, opcode);
        for (int i = 0; i < args.length; ++i) ram.set(address+i+1, args[i]);
        testController.state.PCH.setValue((byte) (address >>> 8));
        testController.state.PCL.setValue((byte) address);
        try {
            assertTrue(testController.next() == desiredReturn);
        } catch (EmulatorException e) {
            e.printStackTrace();
        }
        assertTrue(resultCorrect.getAsBoolean());
    }
}