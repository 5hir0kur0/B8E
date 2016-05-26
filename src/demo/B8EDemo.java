package demo;

import assembler.Assembler;
import assembler.util.problems.Problem;
import emulator.*;
import emulator.arc8051.MC8051;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * Execute a specified (binary) file using the MC8051 class.
 * This class provides a (very) basic text-based interface for demonstrating the 8051 emulator's basic functionality.
 * @author Gordian, (Jannik)
 */
public class B8EDemo {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java B8EDemo file");
            System.exit(42);
        }
        Path p = Paths.get(args[0]);

        Assembler assembler;
        RAM ram = new RAM(0xffff+1);
        Emulator emulator;
        if (!Files.exists(p)) {
            System.err.println("ERROR: The specified file does not exist.");
            System.exit(21);
        } else if (Files.isDirectory(p)) {
            System.err.println("ERROR: The specified path must be a file!");
            System.exit(21);
        }

        // Assembling
        try {
            Path directory = p.toAbsolutePath().getParent();
            assembler = Assembler.of("8051");

            String rawFileName = p.getFileName().toString().substring(0, p.getFileName().toString().lastIndexOf('.'));
            Path output = Paths.get(directory.toString(), rawFileName + ".bin");

            System.out.print("Assembling \"" + p + "\"...");
            try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(
                    Paths.get(rawFileName + ".bin")))){
                List<Problem<?>> problems = new LinkedList<>();
                byte[] outputs = assembler.assemble(p, directory, problems);
                System.out.println("Done.");
                if (problems.size() == 0)
                    System.out.println("No problems occurred.");
                else {
                    problems.forEach(System.out::println);
                    System.out.println("Total Problems: " + problems.size());
                    if (problems.stream().anyMatch(Problem::isError)) {
                        System.out.flush();
                        System.err.println("Assembling FAILED!");
                        System.exit(210);
                    }
                }
                p = output;
            } catch (IOException e) {
                System.out.println();
                System.err.println("Error: Couldn't write or read to specified file.");
                System.exit(126);
            }

        } catch (Exception e) {
            System.out.println();
            System.err.println("ERROR: Unspecified error!");
            e.printStackTrace();
            System.exit(168);
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
            try {
                if ("run".equals(s) || "r".equals(s)) emulator.next();
                else if (s.startsWith("run ") || s.startsWith("r ")) {
                    for (int i = 0; i < Integer.parseInt(s.split(" ")[1]); ++i) emulator.next();
                } else if ("runv".equals(s) || "rv".equals(s)) {
                    emulator.next();
                    System.out.println(emulator);
                } else if (s.startsWith("runv ") || s.startsWith("rv ")) {
                    for (int i = 0; i < Integer.parseInt(s.split(" ")[1]); ++i) {
                        emulator.next();
                        System.out.println(emulator);
                    }
                } else if ("printstate".equals(s) || "ps".equals(s)) {
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
                } else if (s.startsWith("print ") || s.startsWith("p ")) {
                    String regName = s.split(" ")[1].toUpperCase();
                    for (Register r : emulator.getRegisters()) {
                        if (regName.equals(r.getName())) {
                            System.out.printf("%5s = %4s%n", r.getName(), r.getHexadecimalDisplayValue());
                            break;
                        }
                    }
                } else if ("exit".equals(s) || "x".equals(s)) System.exit(0);
            } catch (EmulatorException ee) {
                ee.printStackTrace();
            }
        }
    }
}
