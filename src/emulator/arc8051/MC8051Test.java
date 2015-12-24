package emulator.arc8051;

import emulator.ByteRegister;
import emulator.FlagRegister;
import emulator.RAM;
import emulator.arc8051.MC8051;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author 5hir0kur0
 */
public class MC8051Test {

    MC8051 testController;

    @Before
    public void setUp() throws Exception {
        testController = new MC8051(new RAM(65536), new RAM(65536));
    }

    @Test
    public void testGetRegisters() throws Exception {
        assertTrue(testController.getRegisters() != null && testController.getRegisters().size() == 23);
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
        assertTrue(testController.next() == 1);
        assertTrue(testController.state.PCL.getValue() == 1);
    }

    @Test
    public void testAjmp() {
        System.out.println("Test AJMP...");
        final RAM tmp = (RAM)testController.getCodeMemory();
        final byte STATIC_AJMP = 0x01; //AJMP: 0bXXX00001
        final Random r = new Random();
        for (byte i = 0; i <= 0b111; ++i) {
            final byte instruction = (byte)(STATIC_AJMP | i << 5);
            System.out.printf("INSTRUCTION = %02X%n", instruction);
            //if this was a byte the result of the left-shift might become negative
            final int pch = r.nextInt(256);
            System.out.println("PCH = "+pch);
            testController.state.PCH.setValue((byte)pch);
            testController.state.PCL.setValue((byte)0);
            final byte arg = (byte) r.nextInt(256);
            tmp.set(pch << 8, instruction);
            tmp.set((pch << 8) + 1, arg);
            final char resultingAddress = (char)(pch << 8 & 0xF800 | arg | i << 8);
            assertTrue(testController.next() == 2);
            char actualResult = (char)(testController.state.PCH.getValue() << 8 | testController.state.PCL.getValue());
            System.out.printf("ACTUAL = %04X%nRESULT = %04X%n", (int)actualResult, (int)resultingAddress);
            assertTrue(actualResult == resultingAddress);
        }
    }
}