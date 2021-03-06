package controller;

import assembler.Assembler;
import assembler.util.problems.Problem;
import emulator.Emulator;
import emulator.RAM;
import emulator.arc8051.MC8051;
import gui.EmulatorWindow;
import gui.MainWindow;
import misc.Logger;
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
    private static boolean PROJECT_CREATE;
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
            "assembler.util.AssemblerSettings",
            "misc.Logger",
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
            System.out.println(" DIRECTORY");
            System.out.println("  Same as '--open-project'. Can only be used before the first option or after '--'.");
            System.out.println(" --open-project DIRECTORY");
            System.out.println("  open project in DIRECTORY");
            System.out.println(" --new-project DIRECTORY [name]");
            System.out.println("  create new project. Projects will save the current settings if a new settings file");
            System.out.println("  must be created even if they are 'temporary'!");
            System.out.println("   DIRECTORY Path in which the project file will be created.");
            System.out.println("             Creates the directory structure if it is not present");
            System.out.println("   [name]    the name of the future project");
            System.out.println(" --temporary");
            System.out.println("  Flag the project as temporary. Temporary projects do not save their settings");
            System.out.println("  if they are closed");
            System.out.println(" --list-default-settings");
            System.out.println("  print default settings to stdout");
            System.out.println(" --assemble FILE [architecture]");
            System.out.println("  assemble a file without starting the GUI");
            System.out.println("   FILE           the file to assemble");
            System.out.println("   [architecture] the assembler's architecture (defaults to \"8051\")");
            System.out.println(" --settings <setting>...");
            System.out.println("  set settings to specific values on startup; settings don't have to exist");
            System.out.println("   <setting>... setting in <key>=<value> format");
            System.out.println(" --settings-file FILE");
            System.out.println("  load settings from a specified properties-file");
            System.out.println(" --open-state-dump FILE");
            System.out.println("  open specified state dump in a new emulator window");
            System.out.println(" --emulate FILE");
            System.out.println("  open specified binary file in a new emulator window");
            System.out.println(" --");
            System.out.println("  end option parsing");
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
                Logger.log("An error occurred while listing the settings:", Main.class, Logger.LogLevel.ERROR);
                Logger.logThrowable(e, Main.class, Logger.LogLevel.ERROR);
                System.exit(6);
            }
            Settings.INSTANCE.listDefaults(System.out);
            System.exit(0);
        }));
        CL_OPTIONS.add(new Pair<>("--new-project", list -> {
            if (list.size() < 1 && list.size() > 2) {
                System.err.println("Invalid syntax for '--project' (usage: '--new-project PATH [name]')");
                System.exit(3);
            }
            if (list.get(0).trim().isEmpty()) {
                System.err.println("Invalid project name: " + list.get(0));
                System.exit(4);
            }
            PROJECT_PATH = Paths.get(list.get(0));
            PROJECT_CREATE = true;
            Settings.INSTANCE.setProperty(Project.PROJECT_NAME_KEY, list.size() == 2 && !list.get(1).trim().isEmpty() ?
                            list.get(1) : Paths.get(list.get(0)).getFileName().toString());
        }));
        CL_OPTIONS.add(new Pair<>("--open-project", list -> {
            if (list.size() != 1) {
                System.err.println("Invalid syntax for '--open-project' (exactly one argument required)");
                System.exit(14);
            }
            PROJECT_PATH = Paths.get(list.get(0));
        }));
        CL_OPTIONS.add(new Pair<>("--temporary",list -> {
            if (list.size() != 0) {
                System.err.println("Invalid syntax for '--temporary' (no arguments required)");
                System.exit(5);
            }
            PROJECT_PERMANENT = false;
        }));
        CL_OPTIONS.add(new Pair<>("--settings", list -> {
            try {
                for (String className : CLASSES_WITH_SETTINGS)
                    Class.forName(className);
            } catch (ClassNotFoundException e) {
                Logger.log("An error occurred while loading the default settings:", Main.class, Logger.LogLevel.ERROR);
                Logger.logThrowable(e, Main.class, Logger.LogLevel.ERROR);
                System.exit(6);
            }

            if (list.isEmpty()) {
                System.err.println("Invalid syntax for '--settings': Expected at least one argument.");
                System.exit(8);
            } else {
                for (String setting : list) {
                    String[] value = setting.split("=");
                    if (value.length == 2) {
                        Settings.INSTANCE.setFileProperty(value[0], value[1]);
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
                Logger.log("Reading the settings file (" + list.get(0) + ") failed", Main.class, Logger.LogLevel.ERROR);
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
                Logger.log("Invalid syntax for '--open-state-dump': Expected valid path", Main.class,
                        Logger.LogLevel.ERROR);
                System.exit(1);
            } catch (IOException e) {
                Logger.log("Error: Couldn't load path.", Main.class, Logger.LogLevel.ERROR);
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
                Logger.log("Invalid syntax for '--emulate': Expected valid path", Main.class, Logger.LogLevel.ERROR);
                System.exit(1);
            } catch (IOException e) {
                Logger.log("Error: Couldn't load path.", Main.class, Logger.LogLevel.ERROR);
            }
        }));
     }

    private static Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = (thread, throwable) -> {
        if (MAIN_WINDOW != null) try {
            MAIN_WINDOW.panic();
        } catch (Exception e) {
            Logger.logThrowable(e, Thread.UncaughtExceptionHandler.class, Logger.LogLevel.ERROR);
        }
        if (throwable instanceof Error) {
            Logger.log("An error occurred in B8E...", Thread.UncaughtExceptionHandler.class, Logger.LogLevel.ERROR);
        } else if (throwable instanceof Exception) {
            if (MAIN_WINDOW != null)  {
                MAIN_WINDOW.reportException((Exception)throwable, true);
                return;
            }
            else {
                Logger.log("An exception occurred in B8E...", Thread.UncaughtExceptionHandler.class,
                        Logger.LogLevel.ERROR);
            }
        }
        Logger.logThrowable(throwable, Thread.UncaughtExceptionHandler.class, Logger.LogLevel.ERROR);
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
        PROJECT_CREATE = false;
        PROJECT_PERMANENT = true;

        int i = 0; // argument loop counter
        if (args.length >= 1 && !args[0].startsWith("--")) {
            PROJECT_PATH = Paths.get(args[0]);
            ++i;
            if (args.length > 1 && !args[1].startsWith("--")) {
                Logger.log("Unnecessary non-option arguments specified after project directory.", Main.class,
                        Logger.LogLevel.WARNING);
            }
        } else {
            PROJECT_PATH = Paths.get(System.getProperty("user.dir"));
        }
        outer: for (; i < args.length; ++i) {
            if (args[i].equals("--")) {
                ++i;
                break;
            }
            if (args[i].startsWith("--")) {
                for (Pair<String, Consumer<List<String>>> pair : CL_OPTIONS)
                    if (pair.x.equals(args[i])) {
                        List<String> argArgs = new LinkedList<>();
                        while (i < args.length - 1 && !args[i+1].startsWith("--")) argArgs.add(args[++i]);
                        pair.y.accept(argArgs);
                        continue outer;
                    }
                System.err.println("Illegal argument: " + args[i]);
                System.exit(12);
            }
        }
        if (args.length > i) {
            PROJECT_PATH = Paths.get(args[i]);
            ++i;
            if (args.length > i) {
                Logger.log("Unnecessary arguments specified after project directory and end of options.", Main.class,
                        Logger.LogLevel.WARNING);
            }
        }

        if (!exitAfterOption) {
            PROJECT = new Project(PROJECT_PATH, PROJECT_PERMANENT, PROJECT_CREATE);
            setUpLookAndFeel();
            SwingUtilities.invokeLater(() -> MAIN_WINDOW = new MainWindow("B8E: " + PROJECT.getName(), PROJECT));
        }
    }
}
