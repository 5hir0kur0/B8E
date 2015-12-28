package emulator.arc8051;

import emulator.FlagRegister;
import emulator.RAM;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.*;

/**
 * @author Gordian
 */
public class MC8051Test {

    MC8051 testController;

    @Before
    public void setUp() throws Exception {
        testController = new MC8051(new RAM(65536), new RAM(65536));
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
    public void testNext() throws Exception {
        assertTrue(testController.next() == 1);
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
        final Random r = new Random();
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
            final char resultingAddress = (char)((pch << 8) & 0xF800 | arg | (i << 8));
            assertTrue(testController.next() == 2);
            char actualResult = (char)(testController.state.PCH.getValue() << 8 | testController.state.PCL.getValue());
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
        final Random r = new Random();
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
    public void testRRA() {
        System.out.println("__________Testing RR A...");
        final RAM ram = (RAM) testController.getCodeMemory();
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