package emulator;

import emulator.arc8051.MC8051;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Execute a specified (binary) file using the MC8051 class.
 * This class provides a (very) basic text-based interface for demonstrating the 8051 emulator's basic functionality.
 * @author Gordian
 */
public class EmulatorMC8051Demo {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java EmulatorMC8051Demo file");
            System.exit(42);
        }
        Path p = Paths.get(args[0]);
        RAM ram = new RAM(0xffff+1);
        Emulator emulator;
        if (!Files.exists(p)) {
            System.err.println("ERROR: The specified file does not exist.");
            System.exit(21);
        }
        try {
            byte[] codeMemory = Files.readAllBytes(p);
            int index = 0;
            for (byte b : codeMemory) {
                ram.set(index++, b);
            }
        } catch (IOException e) {
            System.err.println("ERROR: The specified file could not be read.");
            System.exit(84);
        }
        emulator = new MC8051((ROM)ram, new RAM(0xffff));
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.print(">>> ");
            String s = in.nextLine();
            s = s.toLowerCase();
            if ("run".equals(s) || "r".equals(s)) emulator.next();
            else if (s.startsWith("run ") || s.startsWith("r ")) {
                for (int i = 0; i < Integer.parseInt(s.split(" ")[1]); ++i) emulator.next();
            }
            else if ("runv".equals(s) || "rv".equals(s)) {
                emulator.next();
                System.out.println(emulator);
            }
            else if (s.startsWith("runv ") || s.startsWith("rv ")) {
                for (int i = 0; i < Integer.parseInt(s.split(" ")[1]); ++i) {
                    emulator.next();
                    System.out.println(emulator);
                }
            }
            else if ("printstate".equals(s) || "ps".equals(s)) {
                boolean first = true;
                System.out.println("Registers_______________________________________________");
                for (Register r : emulator.getRegisters()) {
                    if (first) first = false;
                    else System.out.println("________________________________________________________");
                    System.out.printf("|%19s          |%10s              |%n",
                            r.getName(), r.getHexadecimalDisplayValue());
                }
                System.out.println("Internal_RAM_____________________________________________________________________________________");
                for (int i = 0; i < emulator.getMainMemory().getSize(); ++i) {
                    if (i % 32 == 0 && i != 0) System.out.println("|");
                    System.out.printf("|%02X", emulator.getMainMemory().get(i) & 0xFF);
                }
                System.out.println("|\n-------------------------------------------------------------------------------------------------");
            }
            else if (s.startsWith("print ") || s.startsWith("p ")) {
                String regName = s.split(" ")[1].toUpperCase();
                for (Register r : emulator.getRegisters()) {
                    if (regName.equals(r.getName())) {
                        System.out.printf("%5s = %4s%n", r.getName(), r.getHexadecimalDisplayValue());
                        break;
                    }
                }
            }
            else if ("exit".equals(s) || "x".equals(s)) System.exit(0);
        }
    }
}
