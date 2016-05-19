package controller;

import gui.MainWindow;
import misc.Pair;
import misc.Settings;

import javax.swing.*;
import java.io.IOException;
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

    private static final String[] CLASSES_WITH_SETTINGS = {
            "gui.LineNumberSyntaxPane",
            "gui.SyntaxThemes",
            "emulator.arc8051.State8051",
            //"assembler.util.AssemblerSettings"
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
            if (!(list.size() == 1)) {
                System.err.println("Invalid syntax for '--not-permanent' (exactly one argument required)");
                System.exit(5);
                PROJECT_PATH = Paths.get(list.get(0));
                PROJECT_PERMANENT = false;
            }
        }));
     }

    public static Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = (thread, throwable) -> {
        if (MAIN_WINDOW != null) try {
            MAIN_WINDOW.panic();
        if (throwable instanceof Error) {
            System.err.println("An error occurred in B8E...");
        } else if (throwable instanceof Exception) {
            if (MAIN_WINDOW != null) MAIN_WINDOW.reportException((Exception)throwable, true);
            else System.err.println("An exception occurred in B8E...");
        }
        throwable.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    public static void main(String[] args) throws IOException {
        Thread.setDefaultUncaughtExceptionHandler(EXCEPTION_HANDLER);

        if (args.length == 1 && !args[0].startsWith("--")) {
            PROJECT_PATH = Paths.get(args[0]);
            PROJECT_PERMANENT = true;
        } else if (args.length == 0) {
            PROJECT_PATH = Paths.get(System.getProperty("user.dir"));
            PROJECT_PERMANENT = false;
        }
        else for (int i = 0; i < args.length; ++i) {
            if (args[i].startsWith("--"))
                for (Pair<String, Consumer<List<String>>> pair : CL_OPTIONS)
                    if (pair.x.equals(args[i])) {
                        List<String> argArgs = new LinkedList<>();
                        while (i < args.length - 1 && !args[++i].startsWith("--")) argArgs.add(args[i]);
                        pair.y.accept(argArgs);
                    }
        }

        PROJECT = new Project(PROJECT_PATH, PROJECT_PERMANENT);

        SwingUtilities.invokeLater(() -> MAIN_WINDOW = new MainWindow("B8E: " + PROJECT.getName(), PROJECT));
    }
}
