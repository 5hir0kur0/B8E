package emulator.arc8051;

import emulator.ByteRegister;
import emulator.FlagRegister;
import emulator.RAM;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.*;

/**
 * @author 5hir0kur0
 */
public class MC8051Test {

    MC8051 testController;
    Random r;

    @Before
    public void setUp() throws Exception {
        testController = new MC8051(new RAM(65536), new RAM(65536));
        r = new Random();
    }

    @Test
    public void testGetRegisters() throws Exception {
        assertTrue(testController.getRegisters() != null && testController.getRegisters().size() == 31);
    }

    @Test
    public void testGetPSW() throws Exception {
        FlagRegister psw = testController.getPSW();
        assertTrue(psw != null && psw.getName(7).equals("C") && psw.getHexadecimalDisplayValue().equals("00"));
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
            assertTrue(testController.next() == 2);
            char actualResult = (char)(testController.state.PCH.getValue() << 8 & 0xff00
                    | testController.state.PCL.getValue() & 0xff);
            System.out.println("actualResult = "+actualResult+" resultingAddress = "+resultingAddress);
            assertTrue(actualResult == resultingAddress);
        }
        testController.state.PCH.setValue((byte)0x0F);
        testController.state.PCL.setValue((byte)0xFF);
        ram.set(0xFFF, STATIC_AJMP);
        System.out.println("Testing the base address...");
        System.out.printf("Opcode: %02X%n", STATIC_AJMP & 0xFF);
        testController.next();
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
        assertTrue(testController.next() == 2);
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
        testController.next();
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
        final byte MOV_A = 0x74;
        for (int i = 0; i < testA.length; ++i) {
            final boolean desiredRes = results[i];
            //TODO uncomment when MOV is implemented
            //testOpcode(MOV_A, 0, new byte[]{testA[i]}, 1, () -> testController.state.sfrs.PSW.getBit(0) == desiredRes);
        }
        assertTrue(!testController.state.sfrs.PSW.getBit(0));
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
        final byte arg = (byte)r.nextInt(256);
        final int stack = r.nextInt(100);
        testController.state.sfrs.SP.setValue((byte)stack);
        testOpcode(PUSH, 111, new byte[]{arg}, 2, () -> testController.state.internalRAM.get(stack + 1) == arg
                && testController.state.sfrs.SP.getValue() == (byte)((stack & 0xFF) + 1));
    }

    @Test
    public void testPop() {
        System.out.println("__________Testing POP...");
        final byte POP = (byte)0xC0;
        final byte stack = (byte)r.nextInt(256);
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
        final byte ADDC_R1   = 0x39;
        final byte ADDC_R2   = 0x2a;
        final byte ADDC_R3   = 0x3b;
        final byte ADDC_R4   = 0x3c;
        final byte ADDC_R5   = 0x3d;
        final byte ADDC_R6   = 0x3e;
        final byte ADDC_R7   = 0x3f;
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
        assertTrue(testController.next() == desiredReturn);
        assertTrue(resultCorrect.getAsBoolean());
    }
}