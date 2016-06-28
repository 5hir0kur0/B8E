package controller;

import assembler.Assembler;
import assembler.util.problems.Problem;
import emulator.Emulator;
import emulator.RAM;
import emulator.arc8051.MC8051;
import gui.EmulatorWindow;
import gui.MainWindow;
import misc.Pair;
import misc.Settings;

import javax.swing.*;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author 5hir0kur0
 */
public class Main {
    private static List<Pair<String, Consumer<List<String>>>> CL_OPTIONS;
    private static Path PROJECT_PATH;
    private static boolean PROJECT_PERMANENT;
    private static Project PROJECT;
    private static MainWindow MAIN_WINDOW;
    private static boolean exitAfterOption = false;

    private static final String LOOK_AND_FEEL_SETTING = "gui.look-and-feel";
    private static final String LOOK_AND_FEEL_SETTING_DEFAULT;
    static {
        if (System.getProperty("os.name").equalsIgnoreCase("linux"))
            LOOK_AND_FEEL_SETTING_DEFAULT = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
        else
            LOOK_AND_FEEL_SETTING_DEFAULT = UIManager.getSystemLookAndFeelClassName();
        Settings.INSTANCE.setDefault(LOOK_AND_FEEL_SETTING, LOOK_AND_FEEL_SETTING_DEFAULT);
    }

    public static final String[] CLASSES_WITH_SETTINGS = {
            "gui.LineNumberSyntaxPane",
            "gui.SyntaxThemes",
            "gui.SyntaxThemes",
            "gui.EmulatorWindow",
            "gui.MainWindow",
            "emulator.arc8051.State8051",
            "controller.Main",
            "assembler.util.AssemblerSettings"
    };

    static {
        CL_OPTIONS = new LinkedList<>();

        CL_OPTIONS.add(new Pair<>("--help", list -> {
            int exit = 0;
            if (!list.isEmpty()) {
                System.err.println("Invalid syntax for '--help' (no arguments required)");
                exit = 1;
            }
            System.out.println("Syntax:");
            System.out.println(" b8e --project NAME");
            System.out.println("     create new project called NAME");
            System.out.println(" b8e [--not-permanent] DIRECTORY");
            System.out.println("     open project in DIRECTORY");
            System.out.println(" b8e --list-default-settings");
            System.out.println("     print default settings to stdout");
            System.out.println(" b8e --assemble <file> [architecture]");
            System.out.println("     assemble a file without starting the GUI");
            System.out.println("      <file>         the file to assemble");
            System.out.println("      [architecture] the assembler's architecture (defaults to \"8051\")");
            System.out.println(" b8e --settings <setting>...");
            System.out.println("     set settings to specific values on startup; settings don't have to exist");
            System.out.println("      <setting>... setting in <key>=<value> format");
            System.out.println(" b8e --settings-file FILE");
            System.out.println("     load settings from a specified properties-file");
            System.out.println(" b8e --open-state-dump FILE");
            System.out.println("      open specified state dump in a new emulator window");
            System.out.println(" b8e --emulate FILE");
            System.out.println("      open specified binary file in a new emulator window");
            System.exit(exit);
        }));
        CL_OPTIONS.add(new Pair<>("--list-default-settings", list -> {
            if (!list.isEmpty()) {
                System.err.println("Invalid syntax for '--list-default-settings' (no arguments required)");
                System.exit(2);
            }
            try {
                for (String className : CLASSES_WITH_SETTINGS)
                    Class.forName(className);
            } catch (ClassNotFoundException e) {
                System.err.println("An error occurred while listing the settings:");
                e.printStackTrace();
                System.exit(6);
            }
            Settings.INSTANCE.listDefaults(System.out);
            System.exit(0);
        }));
        CL_OPTIONS.add(new Pair<>("--project", list -> {
            if (!(list.size() == 1)) {
                System.err.println("Invalid syntax for '--project' (exactly one argument required)");
                System.exit(3);
            }
            if (list.get(0).trim().isEmpty()) {
                System.err.println("Invalid project name: " + list.get(0));
                System.exit(4);
            }
            PROJECT_PATH = Paths.get(System.getProperty("user.dir"), list.get(0));
            PROJECT_PERMANENT = true;
        }));
        CL_OPTIONS.add(new Pair<>("--not-permanent", list -> {
            if (list.size() != 1) {
                System.err.println("Invalid syntax for '--not-permanent' (exactly one argument required)");
                System.exit(5);
            }
            PROJECT_PATH = Paths.get(list.get(0));
            PROJECT_PERMANENT = false;
        }));
        CL_OPTIONS.add(new Pair<>("--settings", list -> {
            if (list.isEmpty()) {
                System.err.println("Invalid syntax for '--settings': Expected at least 1 argument.");
                System.exit(8);
            } else {
                for (String setting : list) {
                    String[] value = setting.split("=");
                    if (value.length == 2) {
                        Settings.INSTANCE.setProperty(value[0], value[1]);
                    } else {
                        System.err.println("Malformed input: " + setting);
                        System.err.println(" Expected <key>=<value>");
                    }
                }
            }
        }));
        CL_OPTIONS.add(new Pair<>("--settings-file", list -> {
            if (list.size() != 1) {
                System.err.println("Invalid syntax for '--settings-file': Expected exactly one argument.");
                System.exit(13);
            }
            try {
                final Path path = Paths.get(list.get(0));
                try (Reader in = Files.newBufferedReader(path)) {
                    Settings.INSTANCE.loadSettingsFile(in);
                }
            } catch (Exception e) {
                System.err.println("Reading the settings file (" + list.get(0) + ") failed");
                System.exit(13);
            }
        }));
        CL_OPTIONS.add(new Pair<>("--assemble", list -> {
            Assembler a;
            Path file;
            Path dir = PROJECT_PATH == null ? Paths.get(System.getProperty("user.dir")) : PROJECT_PATH;
            if (list.size() >= 1) {
                file = Paths.get(list.get(0));

                if (list.size() == 2) {
                    a = Assembler.of(list.get(1));
                } else if (list.size() > 2) {
                    System.err.println("Invalid syntax for '--assemble': Expected 2 arguments at most.");
                    System.exit(7);
                    return;
                } else {
                    a = Assembler.of("8051");
                }

                List<Problem<?>> problems = new LinkedList<>();

                System.out.println("Start assembling of '" + file + "'...");
                a.assemble(file, dir, problems);
                System.out.println("Finish assembling.");
                if (problems.size() > 0) {
                    System.out.println("\nSome problems occurred!:");
                    problems.forEach(System.out::println);
                    System.out.println("Total problems: " + problems.size());
                }
                System.out.println("\nOutput was saved to '" + dir + "'.");

                System.exit((int) problems.stream().filter(Problem::isError).count());

            } else {
                System.err.println("Invalid syntax for '--assemble': No file specified.");
                System.exit(6);
            }

        }));
        CL_OPTIONS.add(new Pair<>("--open-state-dump", list -> {
            if (list.size() != 1) {
                System.err.println("Invalid syntax for '--open-state-dump': Expected exactly one argument");
                System.exit(1);
            }
            try {
                final Path path = Paths.get(list.get(0));
                if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                    System.err.println("Invalid syntax for '--open-state-dump': "
                            + "Expected a path to a regular, readable file");
                    System.exit(1);
                }
                Emulator emulator = new MC8051(path);
                SwingUtilities.invokeLater(() -> new EmulatorWindow(emulator, null));
                exitAfterOption = true;
            } catch (InvalidPathException e) {
                System.err.println("Invalid syntax for '--open-state-dump': Expected valid path");
                System.exit(1);
            } catch (IOException e) {
                System.err.println("Error: Couldn't load path.");
            }
        }));
        CL_OPTIONS.add(new Pair<>("--emulate", list -> {
            if (list.size() != 1) {
                System.err.println("Invalid syntax for '--emulate': Expected exactly one argument");
                System.exit(1);
            }
            try {
                final Path path = Paths.get(list.get(0));
                if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                    System.err.println("Invalid syntax for '--emulate': Expected a path to a regular, readable file");
                    System.exit(1);
                }
                byte[] code = Files.readAllBytes(path);
                RAM codeMemory = new RAM(65_536);
                int i = 0;
                for (byte b : code) codeMemory.set(i++, b);
                Emulator emulator = new MC8051(codeMemory, new RAM(256));
                SwingUtilities.invokeLater(() -> new EmulatorWindow(emulator, null));
                exitAfterOption = true;
            } catch (InvalidPathException e) {
                System.err.println("Invalid syntax for '--emulate': Expected valid path");
                System.exit(1);
            } catch (IOException e) {
                System.err.println("Error: Couldn't load path.");
            }
        }));
     }

    private static Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = (thread, throwable) -> {
        if (MAIN_WINDOW != null) try {
            MAIN_WINDOW.panic();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (throwable instanceof Error) {
            System.err.println("An error occurred in B8E...");
        } else if (throwable instanceof Exception) {
            if (MAIN_WINDOW != null) MAIN_WINDOW.reportException((Exception)throwable, true);
            else System.err.println("An exception occurred in B8E...");
        }
        throwable.printStackTrace();
    };

    private static void setUpLookAndFeel() {
        final String lookAndFeel = Settings.INSTANCE.getProperty(LOOK_AND_FEEL_SETTING);
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e1) {
                throw new IllegalStateException("could not set up look and feel", e1);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Thread.setDefaultUncaughtExceptionHandler(EXCEPTION_HANDLER);

        int i = 0; // argument loop counter
        if (args.length >= 1 && !args[0].startsWith("--")) {
            PROJECT_PATH = Paths.get(args[0]);
            PROJECT_PERMANENT = true;
            ++i;
        } else {
            PROJECT_PATH = Paths.get(System.getProperty("user.dir"));
            PROJECT_PERMANENT = false;
        }
        outer: for (; i < args.length; ++i) {
            if (args[i].startsWith("--")) {
                for (Pair<String, Consumer<List<String>>> pair : CL_OPTIONS)
                    if (pair.x.equals(args[i])) {
                        List<String> argArgs = new LinkedList<>();
                        while (i < args.length - 1 && !args[++i].startsWith("--")) argArgs.add(args[i]);
                        pair.y.accept(argArgs);
                        continue outer;
                    }
                System.err.println("Illegal argument: " + args[i]);
                System.exit(12);
            }
        }

        if (!exitAfterOption) {
            PROJECT = new Project(PROJECT_PATH, PROJECT_PERMANENT);
            setUpLookAndFeel();
            SwingUtilities.invokeLater(() -> MAIN_WINDOW = new MainWindow("B8E: " + PROJECT.getName(), PROJECT));
        }
    }
}
