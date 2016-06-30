package gui;

import assembler.Assembler;
import assembler.util.Listing;
import assembler.util.problems.Problem;
import controller.Project;
import controller.TextFile;
import emulator.RAM;
import misc.Pair;
import misc.Settings;

import javax.swing.*;
import javax.swing.event.TreeModelListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author T., Noxgrim, 5hir0kur0
 */
public class MainWindow extends JFrame {
    private final Project project;

    private final JTabbedPane jTabbedPane;
    private final JSplitPane problemsSplit;
    private final JSplitPane mainSplit;
    private JTable problemTable;
    private JTree fsTree;
    private final JFileChooser fileChooser;
    private Action openFile, newFile, saveFile, saveAs, saveAll, cut, copy, paste, undo, redo,
            refreshTree, zoomIn, zoomOut, nextTab, prevTab, reloadFile, buildRunMain, buildRunCurrent,
            buildMain, buildCurrent, runMain, runCurrent, setMain, settings;

    private final static String AUTOSAVE_SETTING = "gui.autosave-on-build";
    private final static String AUTOSAVE_SETTING_DEFAULT = "true";

    static {Settings.INSTANCE.setDefault(AUTOSAVE_SETTING, AUTOSAVE_SETTING_DEFAULT);}
    { setUpActions(); }

    final static String UNDO_TEXT = "Undo";
    final static String REDO_TEXT = "Redo";
    final static String COPY_TEXT = "Copy";
    final static String CUT_TEXT = "Cut";
    final static String PASTE_TEXT = "Paste";
    final static String SAVE_FILE_TEXT = "Save";
    final static String SAVE_FILE_AS_TEXT = "Save as…";
    final static String SAVE_ALL_FILES_TEXT = "Save all";
    final static String NEW_FILE_TEXT = "New";
    final static String OPEN_FILE_TEXT = "Open";
    final static String REFRESH_TREE_TEXT = "Refresh file system tree";
    final static String ZOOM_IN_TEXT = "Zoom in";
    final static String ZOOM_OUT_TEXT = "Zoom out";
    final static String NEXT_TAB_TEXT = "Next tab";
    final static String PREV_TAB_TEXT = "Previous tab";
    final static String RELOAD_FILE_TEXT = "Reload file";
    final static String BUILD_RUN_MAIN_TEXT = "Build and run main file";
    final static String BUILD_MAIN_TEXT = "Build main file";
    final static String RUN_MAIN_TEXT = "Run main file";
    final static String BUILD_RUN_CURR_TEXT = "Build and run current file";
    final static String BUILD_CURR_TEXT = "Build current file";
    final static String RUN_CURR_TEXT = "Run current file";
    final static String SET_MAIN_TEXT = "Set main file";
    final static String SETTINGS_TEXT = "Settings";

    private Path lastBuilt;
    private List<Problem<?>> problems;

    private final static String FILE_EXTENSION_SEPARATOR = ".";
    // used when creating a new tab without a corresponding file
    private final static String DEFAULT_FILE_EXTENSION = "asm";

    /**
     * Should be thrown, when the user has to be notified about an exception by the user of a method, but can't be
     * notified by the method itself (because that would cause other exceptions, etc.).
     * Should only be used for exceptions, that are <b>not severe</b>.
     * Example:
     * <pre>
     *     try {
     *         this.myMethod();
     *     } catch (NotifyUserException e) {
     *         this.reportException(e.getMessage(), e, <b>false</b>);
     *     }
     * </pre>
     */
    private static final class NotifyUserException extends Exception {
        public NotifyUserException(String message, Exception e) {
            super(message, e);
        }
    }

    public MainWindow(String title, Project project) {
        super.setSize(420, 420); // TODO: Use #.pack() later
        super.setLocationRelativeTo(null);
        super.setTitle(title);
        super.setLayout(new BorderLayout());

        super.setJMenuBar(this.makeMenu());

        this.project = Objects.requireNonNull(project);


        this.jTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM, JTabbedPane.SCROLL_TAB_LAYOUT);

        this.problemsSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        this.problemsSplit.setOneTouchExpandable(true);
        this.setUpProblemTable();
        this.problemsSplit.setTopComponent(this.jTabbedPane);

        // Default to collapsed ... sort of
        // TODO: Find a better way to do that
        this.problemTable.setMinimumSize(new Dimension());
        this.problemsSplit.setDividerLocation(super.getHeight());
        this.mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        this.mainSplit.setOneTouchExpandable(true);
        this.mainSplit.setLeftComponent(this.problemsSplit);
        this.refreshTree();
        super.add(this.mainSplit, BorderLayout.CENTER);

        this.mainSplit.setResizeWeight(0.75);
        this.problemsSplit.setResizeWeight(0.75);

        this.setUpKeyBindings();

        super.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                MainWindow.this.windowClosing();
            }
        });
        super.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        this.fileChooser = new JFileChooser(this.project.getProjectPath().toString());

        super.setVisible(true);

        // doesn't work when called directly
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(15);
            } catch (InterruptedException ignored) { }
            toggleSplit(true, this.problemsSplit);
        });
        if (!this.project.isPermanent())
            SwingUtilities.invokeLater(() -> {
                try {
                    Thread.sleep(15);
                } catch (InterruptedException ignored) { }
                toggleSplit(true, this.mainSplit);
            });
    }


    public void reportException(Exception e, boolean severe) {
        reportException("An Exception occurred: " + e.getClass().getSimpleName(), e, severe);
    }

    public void reportException(String message, Exception e, boolean severe) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea jta = new JTextArea(sw.toString());
        jta.setTabSize(4);
        jta.setEditable(false);
        JScrollPane jsp = new JScrollPane(jta);
        JLabel details = new JLabel("Details:");
        panel.add(details, BorderLayout.NORTH);
        panel.add(jsp, BorderLayout.CENTER);
        if (severe) {
            JLabel saved =
                    new JLabel("The program tried to save all your unsaved changes (except new files) to the disk.");
            panel.add(saved, BorderLayout.SOUTH);
        }
        panel.setPreferredSize(new Dimension(600, 200));
        if (System.getProperty("os.name").toLowerCase().contains("windows"))
            jta.setToolTipText("This " + (severe ? "definitely" : "probably")
                    + " happened because you are using windows.");
        JOptionPane.showMessageDialog(this, panel, message,
                severe ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    public void panic() {
        for (int i = 0; i < this.jTabbedPane.getTabCount(); ++i) {
            AccessibleScrollPaneHack pane = (AccessibleScrollPaneHack) this.jTabbedPane.getComponentAt(i);
            if (pane.textFile == null || pane.child == null) continue;
            try {
                pane.child.store(pane.textFile.getWriter());
            } catch (IOException e) {
                System.err.println("error during panic-save:");
                e.printStackTrace();
            }
        }
        try {
            this.project.close();
        } catch (IOException e) {
            System.err.println("error during panic-save:");
            e.printStackTrace();
        }
    }

    private void windowClosing() {
        boolean unsavedFiles = false;
        final StringBuilder dialogText = new StringBuilder();
        for (int i = 0, all = MainWindow.this.jTabbedPane.getTabCount(); i < all; ++i) try {
            final Pair<TextFile, LineNumberSyntaxPane> pair = MainWindow.this.getFileAt(i);
            if (pair.y.isChanged()) {
                unsavedFiles = true;
                final String tmpName = pair.x == null ? "[untitled file]" : pair.x.getPath().getFileName().toString();
                dialogText.append(tmpName).append('\n');
            }
        } catch (NotifyUserException ignored) {
            ignored.printStackTrace();
        }
        if (unsavedFiles) {
            final JTextArea textArea = new JTextArea(dialogText.toString());
            textArea.setEditable(false);
            final JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JLabel("The following files have not been saved:"), BorderLayout.PAGE_START);
            panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
            panel.add(new JLabel("Do you want to save them?"), BorderLayout.PAGE_END);
            final int result = JOptionPane.showConfirmDialog(MainWindow.this,
                    panel,
                    "Do you want to save unsaved files?",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            switch (result) {
                case JOptionPane.YES_OPTION:
                    MainWindow.this.saveAllFiles();
                    break;
                case JOptionPane.NO_OPTION:
                    break;
                case JOptionPane.CANCEL_OPTION:
                case JOptionPane.CLOSED_OPTION:
                    return;
            }

            try {
                MainWindow.this.project.close();
            } catch (IOException e1) {
                MainWindow.this.reportException("An Error occurred while closing the project", e1, false);
                e1.printStackTrace();
            }
        }

        try {
            this.project.close();
        } catch (IOException e) {
            this.reportException("An error occurred while closing the project", e, false);
        }

        MainWindow.super.dispose();
    }

    private JMenuBar makeMenu() {
        final String FILE_MENU_TEXT = "File";
        final String EDIT_MENU_TEXT = "Edit";
        final String VIEW_MENU_TEXT = "View";
        final String PROJECT_MENU_TEXT = "Project";

        class SyntaxThemeAction extends AbstractAction {
            private final String name;
            SyntaxThemeAction(String name) {
                super(name);
                if (Objects.requireNonNull(name, "syntax theme name must not be null").trim().isEmpty())
                    throw new IllegalArgumentException("syntax theme name must not be empty");
                if (!SyntaxThemes.INSTANCE.getAvailableThemes().contains(name) && !"DEFAULT".equals(name))
                    throw new IllegalArgumentException(name + " is not a valid theme name");
                this.name = name;
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                SyntaxThemes.INSTANCE.setCurrentTheme(this.name);
                MainWindow.this.updateThemes();
            }
        }

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu(FILE_MENU_TEXT);

        JMenuItem newFile = new JMenuItem(this.newFile);
        newFile.setMnemonic('n');
        JMenuItem openFile = new JMenuItem(this.openFile);
        openFile.setMnemonic('o');
        JMenuItem reloadFile = new JMenuItem(this.reloadFile);
        reloadFile.setMnemonic('r');
        JMenuItem saveFile = new JMenuItem(this.saveFile);
        saveFile.setMnemonic('s');
        JMenuItem saveFileAs = new JMenuItem(this.saveAs);
        saveFileAs.setMnemonic('v');
        JMenuItem saveAll = new JMenuItem(this.saveAll);
        saveAll.setMnemonic('a');
        JMenuItem settings = new JMenuItem(this.settings);
        settings.setMnemonic('t');

        fileMenu.add(newFile);
        fileMenu.add(openFile);
        fileMenu.add(reloadFile);
        fileMenu.addSeparator();
        fileMenu.add(saveFile);
        fileMenu.add(saveFileAs);
        fileMenu.add(saveAll);
        fileMenu.addSeparator();
        fileMenu.add(settings);
        fileMenu.setMnemonic('F');
        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu(EDIT_MENU_TEXT);
        editMenu.setMnemonic('e');
        JMenuItem undo = new JMenuItem(this.undo);
        undo.setMnemonic('u');
        JMenuItem redo = new JMenuItem(this.redo);
        redo.setMnemonic('r');
        JMenuItem copy = new JMenuItem(this.copy);
        copy.setMnemonic('c');
        JMenuItem cut = new JMenuItem(this.cut);
        cut.setMnemonic('t');
        JMenuItem paste = new JMenuItem(this.paste);
        paste.setMnemonic('p');

        editMenu.add(undo);
        editMenu.add(redo);
        editMenu.addSeparator();
        editMenu.add(copy);
        editMenu.add(cut);
        editMenu.add(paste);
        menuBar.add(editMenu);

        JMenu viewMenu = new JMenu(VIEW_MENU_TEXT);
        viewMenu.setMnemonic('v');
        JMenuItem zoomIn = new JMenuItem(this.zoomIn);
        zoomIn.setMnemonic('i');
        viewMenu.add(zoomIn);
        JMenuItem zoomOut = new JMenuItem(this.zoomOut);
        zoomOut.setMnemonic('o');
        viewMenu.add(zoomOut);
        viewMenu.addSeparator();
        JMenuItem nextTab = new JMenuItem(this.nextTab);
        nextTab.setMnemonic('n');
        viewMenu.add(nextTab);
        JMenuItem prevTab = new JMenuItem(this.prevTab);
        prevTab.setMnemonic('p');
        viewMenu.add(prevTab);
        viewMenu.addSeparator();
        JMenuItem refreshTree = new JMenuItem(this.refreshTree);
        refreshTree.setMnemonic('f');
        viewMenu.add(refreshTree);
        JMenu themeMenu = new JMenu("Select theme");
        themeMenu.setMnemonic('s');
        for (String themeName : SyntaxThemes.INSTANCE.getAvailableThemes()) {
            JMenuItem tmp = new JMenuItem(new SyntaxThemeAction(themeName));
            themeMenu.add(tmp);
        }
        viewMenu.addSeparator();
        viewMenu.add(themeMenu);
        menuBar.add(viewMenu);

        JMenu projectMenu = new JMenu(PROJECT_MENU_TEXT);
        projectMenu.setMnemonic('p');
        JMenuItem buildRunMain = new JMenuItem(this.buildRunMain);
        buildRunMain.setMnemonic('r');
        projectMenu.add(buildRunMain);
        JMenuItem buildMain = new JMenuItem(this.buildMain);
        buildMain.setMnemonic('b');
        projectMenu.add(buildMain);
        JMenuItem runMain = new JMenuItem(this.runMain);
        runMain.setMnemonic('m');
        projectMenu.add(runMain);
        projectMenu.addSeparator();
        JMenuItem buildRunCurrent = new JMenuItem(this.buildRunCurrent);
        buildRunCurrent.setMnemonic('u');
        projectMenu.add(buildRunCurrent);
        JMenuItem buildCurrent = new JMenuItem(this.buildCurrent);
        buildCurrent.setMnemonic('i');
        projectMenu.add(buildCurrent);
        JMenuItem runCurrent = new JMenuItem(this.runCurrent);
        runCurrent.setMnemonic('c');
        projectMenu.add(runCurrent);
        projectMenu.addSeparator();
        JMenuItem setMain = new JMenuItem(this.setMain);
        setMain.setMnemonic('s');
        projectMenu.add(setMain);
        menuBar.add(projectMenu);


        return menuBar;
    }

    private void updateThemes() {
        for (int i = 0; i < this.jTabbedPane.getTabCount(); ++i) {
            final AccessibleScrollPaneHack pane = (AccessibleScrollPaneHack) this.jTabbedPane.getComponentAt(i);
            pane.child.updateTheme();
        }
    }

    private static String getFileExtension(Path path) {
        final String fileName = path.getFileName().toString();
        final int lastIndex = fileName.lastIndexOf(FILE_EXTENSION_SEPARATOR) + 1;
        if (lastIndex < 0 || lastIndex >= fileName.length()) return "";
        else return fileName.substring(lastIndex);
    }

    private Pair<TextFile, LineNumberSyntaxPane> getCurrentFile() throws NotifyUserException {
        try {
            AccessibleScrollPaneHack pane = (AccessibleScrollPaneHack)
                    this.jTabbedPane.getComponentAt(this.jTabbedPane.getSelectedIndex());
            return new Pair<>(pane.textFile, pane.child);
        } catch (IndexOutOfBoundsException e) {
            throw new NotifyUserException("Error: Couldn't get the current file (probably there are no open files)", e);
        }
    }

    private Pair<TextFile, LineNumberSyntaxPane> getFileAt(int index) throws NotifyUserException {
        try {
            AccessibleScrollPaneHack pane = (AccessibleScrollPaneHack) this.jTabbedPane.getComponentAt(index);
            return new Pair<>(pane.textFile, pane.child);
        } catch (IndexOutOfBoundsException e) {
            throw new NotifyUserException("Error: Couldn't get the file at index #" + index, e);
        }
    }

    private void saveFile(int index) {
        final Pair<TextFile, LineNumberSyntaxPane> file;
        try {
            file = this.getFileAt(index);
        } catch (NotifyUserException e) {
            this.reportException(e.getMessage(), e, false);
            return;
        }
        if (file != null)
            if (file.x != null)
                try {
                    file.y.store(file.x.getWriter());
                } catch (IOException e1) {
                    this.reportException("Error: Saving the file \"" + file.x.getPath().getFileName()
                            + "\" failed", e1, false);
                }
            else
                saveFileAs(index);
    }

    private void saveFileAs(int index) {
        if (this.fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            final Path path = this.fileChooser.getSelectedFile().toPath();
            final Pair<TextFile, LineNumberSyntaxPane> file;
            try {
                file = this.getFileAt(index);
            } catch (NotifyUserException e) {
                this.reportException(e.getMessage(), e, false);
                return;
            }
            try {
                file.x = this.project.requestResource(path, true);
                file.y.store(file.x.getWriter());
                file.y.setFileExtension(getFileExtension(path)); // update syntax highlighting
                file.y.load(file.x.getReader());
                this.jTabbedPane.setTitleAt(index, file.x.getPath().getFileName().toString());
                this.refreshTree();
            } catch (IOException e1) {
                this.reportException("Error: Saving the file \""
                        + file.x.getPath().getFileName() + "\" as \"" + path + "\" failed", e1, false);
            }
        }
    }

    private void saveAllFiles() {
        for (int i = 0, all = this.jTabbedPane.getTabCount(); i < all; ++i) {
            try {
                this.saveFile(i);
                this.refreshTree();
            } catch (Exception e1) {
                this.reportException(e1.getMessage(), e1, false);
            }
        }
    }

    private void refreshTree() {
        this.fsTree = new JTree(MainWindow.this.makeFileSystemTree());
        this.fsTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    int row = MainWindow.this.fsTree.getRowForLocation(e.getX(), e.getY());
                    if (row < 0) return;
                    TreePath path = MainWindow.this.fsTree.getPathForRow(row);
                    PathNode leaf = (PathNode)path.getLastPathComponent();
                    if (Files.isRegularFile(leaf.path, LinkOption.NOFOLLOW_LINKS))
                        MainWindow.this.openOrSwitchToFile(leaf.path);
                }
            }
        });
        this.mainSplit.setRightComponent(new JScrollPane((MainWindow.this.fsTree)));
        this.mainSplit.setDividerLocation(0.75);
    }

    private void setUpProblemTable() {
        this.problemTable = new JTable(new ProblemTableModel());
        this.problemTable.setDefaultRenderer(Object.class, new ProblemTableCellRenderer());

        TableColumnModel tcm = this.problemTable.getColumnModel();
        tcm.getColumn(0).setWidth(70);
        tcm.getColumn(1).setWidth(30);
        tcm.getColumn(2).setWidth(40);
        tcm.getColumn(4).setWidth(100);

        JScrollPane jsp = new JScrollPane(this.problemTable);
        jsp.setMinimumSize(new Dimension());
        this.problemsSplit.setBottomComponent(jsp);

        this.problemTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                JPopupMenu menu = new JPopupMenu("ProblemMenu");
                JTable table = MainWindow.this.problemTable;
                Action copy, goTo, copyProblem;

                copy = new AbstractAction("Copy cell") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int row = table.rowAtPoint(me.getPoint());
                        int column = table.columnAtPoint(me.getPoint());
                        if (row < 0 || column < 0)
                            return;
                        Object val = table.getValueAt(row, column);
                        StringSelection selection = new StringSelection(val.toString());
                        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clip.setContents(selection, (c, o) -> {});
                    }
                };
                goTo = new AbstractAction("Show") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int row = table.rowAtPoint(me.getPoint());
                        if (row < 0)
                            return;
                        Object val  = null;
                        Object line = null;
                        for (int i = 0, m = 0; m < 2 || i < table.getColumnCount(); ++i)
                            if (table.getColumnName(i).equals("Line")) {
                                line = table.getValueAt(row, i);
                                ++m;
                            } else if (table.getColumnName(i).equals("File")) {
                                val = table.getValueAt(row, i);
                                ++m;
                            }


                        if (val != null && val instanceof Path) {
                            Path file = (Path) val;
                            openOrSwitchToFile(file.toAbsolutePath());
                            if (line != null && line instanceof Integer) {
                                int i = (Integer) line;
                                if (i > 0)
                                    try {
                                        LineNumberSyntaxPane open = MainWindow.this.getCurrentFile().y;
                                        open.setCaret(i - 1, 0);
                                    } catch (NotifyUserException ex) {
                                        MainWindow.this.reportException(ex, false);
                                    }
                            }
                        }
                    }
                };
                copyProblem = new AbstractAction("Copy problem string") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int row = table.rowAtPoint(me.getPoint());
                        if (row < 0)
                            return;
                        // Assuming rows and indices are synchronized... Nasty exceptions for the win.
                        StringSelection selection = new StringSelection(problems.get(row).toString());
                        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clip.setContents(selection, (c, o) -> {});

                    }
                };
                JMenuItem copyMenu = new JMenuItem(copy);
                copyMenu.setMnemonic('c');
                JMenuItem goToMenu = new JMenuItem(goTo);
                goToMenu.setMnemonic('s');
                JMenuItem copyPMenu = new JMenuItem(copyProblem);
                copyPMenu.setMnemonic('p');
                menu.add(copyMenu);
                menu.add(goToMenu);
                menu.add(copyPMenu);

                if (me.getButton() == MouseEvent.BUTTON1 && me.getClickCount() >= 2) { // Double left click
                    goTo.actionPerformed(new ActionEvent(menu, 0, menu.getLabel()));
                } else if (me.getButton() == MouseEvent.BUTTON3) {                     // Right click
                    menu.show(me.getComponent(), me.getX(), me.getY());
                }
            }
        });
    }

    private static class ProblemTableModel extends AbstractTableModel {

        private static final String[] columns = {"Type", "File", "Line", "Message", "Cause"};
        private final ArrayList<Object[]> data;

        private static Color ERROR_COLOR = new Color(0x990000); // Crimson red
        private static Color WARNING_COLOR = Color.ORANGE;
        private static Color INFO_COLOR = Color.LIGHT_GRAY;
        private static final Color LIGHT_FOREGROUND = Color.LIGHT_GRAY;
        private static final Color DARK_FOREGROUND = Color.DARK_GRAY;

        public ProblemTableModel() {
            this.data = new ArrayList<>(42);
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data.get(rowIndex)[columnIndex];
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            while (data.size() <= rowIndex)
                data.add(new Object[columns.length]);

            data.get(rowIndex)[columnIndex] = aValue;

            fireTableCellUpdated(rowIndex, columnIndex);
        }

        public void removeRow(int row) {
            data.remove(row);
            fireTableRowsDeleted(row, row);
        }

        public Pair<Color, Color> getRowColor(int row) {
            if (data.size() > row)
                for (int i = 0; i < columns.length; ++i)
                    if (getValueAt(row, i) instanceof Problem.Type)
                        switch ((Problem.Type)data.get(row)[i]) {
                            case ERROR:
                                return new Pair<>(LIGHT_FOREGROUND, ERROR_COLOR);
                            case WARNING:
                                return new Pair<>(DARK_FOREGROUND, WARNING_COLOR);
                            default:
                                return new Pair<>(DARK_FOREGROUND,  INFO_COLOR);
                        }
            return new Pair<>(DARK_FOREGROUND, INFO_COLOR);
        }
    }

    private class ProblemTableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            ProblemTableModel ptm = (ProblemTableModel) table.getModel();

            Component c  = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Pair<Color, Color> colors = ptm.getRowColor(row);
            c.setForeground(colors.x);
            c.setBackground(colors.y);
            return c;
        }
    }

    private void refreshProblemTable() {
        if (this.problems.isEmpty()) {
            this.problemTable.setVisible(false);
        } else {
            this.problemTable.setVisible(true);

            ProblemTableModel tm = (ProblemTableModel) this.problemTable.getModel();

            int rowCount = tm.getRowCount();
            for (int i = rowCount - 1; i >= this.problems.size(); i--)
                tm.removeRow(i);

            for (int row = 0; row < this.problems.size(); ++row) {
                Problem<?> p = this.problems.get(row);
                for (int i = 0; i < this.problemTable.getColumnCount(); ++i)
                    switch (this.problemTable.getColumnName(i)) {
                        case "Type":
                            this.problemTable.setValueAt(p.getType(), row, i);
                            break;
                        case "File":
                            this.problemTable.setValueAt(p.getPath() == null ? "-" :
                                    this.project.getProjectPath().relativize(p.getPath()), row, i);
                            break;
                        case "Line":
                            this.problemTable.setValueAt(p.getLine() == -1 ? "-" : p.getLine(), row, i);
                            break;
                        case "Message":
                            this.problemTable.setValueAt(p.getMessage(), row, i);
                            break;
                        case "Cause":
                            this.problemTable.setValueAt(p.getCause(), row, 4);
                            break;
                        default:
                            throw new IllegalStateException("Unknown table header name: " +
                                    this.problemTable.getColumnName(i));
                    }
            }

            this.problemTable.revalidate();

        }
    }

    private void highlightProblems(TextFile file, LineNumberSyntaxPane pane) {
        if (problems == null)
            return;
        final Map<Integer, Color> lines = new HashMap<>();
        List<Problem> relevant =
                problems.stream().filter(p -> p.getPath().equals(file.getPath())).collect(Collectors.toList());
        if (!relevant.isEmpty()) {
            Problem.Type[] types = Problem.Type.values();
            for (int i = types.length - 1; i >= 0; --i) {
                Problem.Type type = types[i];
                Color color;
                switch (i) {
                    case 0: color = SyntaxThemes.INSTANCE.getCurrentTheme().getErrorColor(); break;
                    case 1: color = SyntaxThemes.INSTANCE.getCurrentTheme().getWarningColor(); break;
                    default: color = SyntaxThemes.INSTANCE.getCurrentTheme().getInformationColor();
                }
                relevant.stream().filter(p -> p.getType() == type).forEach(p -> lines.put(p.getLine() - 1, color));
            }
        }
        pane.highlightLines(lines);
    }

    private void openOrSwitchToFile(Path path) {
        for (int i = 0; i < this.jTabbedPane.getTabCount(); ++i) {
            AccessibleScrollPaneHack pane = (AccessibleScrollPaneHack) this.jTabbedPane.getComponentAt(i);
            final Path open = (pane.textFile == null) ? null : pane.textFile.getPath();
            if (open != null && open.equals(path)) {
                this.jTabbedPane.setSelectedIndex(i);
                return;
            }
        }
        this.openFile(path);
    }

    private void openTab(LineNumberSyntaxPane syntaxPane, TextFile textFile, String title) {
        final JScrollPane scrollPane = new AccessibleScrollPaneHack(syntaxPane, textFile);
        scrollPane.getVerticalScrollBar().setUnitIncrement(5);
        if (this.jTabbedPane.getTabCount() != 0) {
            this.jTabbedPane.add(scrollPane, this.jTabbedPane.getSelectedIndex() + 1);
            this.jTabbedPane.setSelectedIndex(this.jTabbedPane.getSelectedIndex() + 1);
        } else
            this.jTabbedPane.add(scrollPane);
        scrollPane.requestFocusInWindow();
        syntaxPane.setCaret(0, 0);
        this.jTabbedPane.setTitleAt(this.jTabbedPane.getSelectedIndex(), title);
    }

    private void openFile(Path path) {
        try {
            TextFile textFile = this.project.requestResource(path, false);
            final LineNumberSyntaxPane syntaxPane = new LineNumberSyntaxPane(getFileExtension(path));
            syntaxPane.load(textFile.getReader());
            this.openTab(syntaxPane, textFile, path.getFileName().toString());
            this.highlightProblems(textFile, syntaxPane);
        } catch (IOException e1) {
            this.reportException("Error: Opening the file \"" + path.getFileName() + "\" failed", e1, false);
        }
    }

    private void setUpActions() {

        final MainWindow mw = this;

        this.openFile = new AbstractAction(OPEN_FILE_TEXT) {
            public void actionPerformed(ActionEvent e) {
                if (mw.fileChooser.showOpenDialog(mw) == JFileChooser.APPROVE_OPTION) {
                    final Path file = mw.fileChooser.getSelectedFile().toPath();
                    mw.openFile(file);
                }
            }
        };
        this.newFile = new AbstractAction(NEW_FILE_TEXT) {
            public void actionPerformed(ActionEvent e) {
                final LineNumberSyntaxPane syntaxPane = new LineNumberSyntaxPane(DEFAULT_FILE_EXTENSION);
                mw.openTab(syntaxPane, null, "(untitled)");
            }
        };
        this.saveFile = new AbstractAction(SAVE_FILE_TEXT) {
            public void actionPerformed(ActionEvent e) {
                try {
                    mw.saveFile(mw.jTabbedPane.getSelectedIndex());
                    mw.refreshTree();
                } catch (Exception e1) {
                    mw.reportException("Saving the file failed", e1, false);
                }
            }
        };
        this.saveAll = new AbstractAction(SAVE_ALL_FILES_TEXT) {
            public void actionPerformed(ActionEvent e) {
                mw.saveAllFiles();
            }
        };
        this.saveAs = new AbstractAction(SAVE_FILE_AS_TEXT) {
            public void actionPerformed(ActionEvent e) {
                try {
                    mw.saveFileAs(mw.jTabbedPane.getSelectedIndex());
                    mw.refreshTree();
                } catch (Exception e1) {
                    mw.reportException("Saving the file failed", e1, false);
                }
            }
        };
        this.undo = new AbstractAction(UNDO_TEXT) {
            public void actionPerformed(ActionEvent e) {
                try {
                    mw.getCurrentFile().y.undo();
                } catch (NotifyUserException e1) {
                    mw.reportException("Error: 'Undo' failed", e1, false);
                }
            }
        };
        this.redo = new AbstractAction(REDO_TEXT) {
            public void actionPerformed(ActionEvent e) {
                try {
                    mw.getCurrentFile().y.redo();
                } catch (NotifyUserException e1) {
                    mw.reportException("Error: 'Redo' failed", e1, false);
                }
            }
        };
        this.copy = new AbstractAction(COPY_TEXT) {
            public void actionPerformed(ActionEvent e) {
                try {
                    mw.getCurrentFile().y.copy();
                } catch (NotifyUserException e1) {
                    mw.reportException("Error: 'Copy' failed", e1, false);
                }
            }
        };
        this.cut = new AbstractAction(CUT_TEXT) {
            public void actionPerformed(ActionEvent e) {
                try {
                    mw.getCurrentFile().y.cut();
                } catch (NotifyUserException e1) {
                    mw.reportException("Error: 'Cut' failed", e1, false);
                }
            }
        };
        this.paste = new AbstractAction(PASTE_TEXT) {
            public void actionPerformed(ActionEvent e) {
                try {
                    mw.getCurrentFile().y.paste();
                } catch (NotifyUserException e1) {
                    mw.reportException("Error: 'Paste' failed", e1, false);
                }
            }
        };
        this.refreshTree = new AbstractAction(REFRESH_TREE_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                mw.refreshTree();
            }
        };
        this.zoomIn = new AbstractAction(ZOOM_IN_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    mw.getCurrentFile().y.setFontSize(Math.abs(mw.getCurrentFile().y.getFontSize() + 2) + 1);
                } catch (NotifyUserException e1) {
                    mw.reportException("Error: 'Zoom in' failed", e1, false);
                }
            }
        };
        this.zoomOut = new AbstractAction(ZOOM_OUT_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    mw.getCurrentFile().y.setFontSize(Math.abs(mw.getCurrentFile().y.getFontSize() - 4) + 1);
                } catch (NotifyUserException e1) {
                    mw.reportException("Error: 'Zoom out' failed", e1, false);
                }
            }
        };
        this.nextTab = new AbstractAction(NEXT_TAB_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mw.jTabbedPane.getTabCount() <= 1) return;
                mw.jTabbedPane.setSelectedIndex((mw.jTabbedPane.getSelectedIndex() + 1) % mw.jTabbedPane.getTabCount());
            }
        };
        this.prevTab = new AbstractAction(PREV_TAB_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mw.jTabbedPane.getTabCount() <= 1) return;
                mw.jTabbedPane.setSelectedIndex((mw.jTabbedPane.getSelectedIndex() + mw.jTabbedPane.getTabCount() - 1)
                                                 % mw.jTabbedPane.getTabCount());
            }
        };
        this.reloadFile = new AbstractAction(RELOAD_FILE_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Pair<TextFile, LineNumberSyntaxPane> file;
                try {
                    file = mw.getCurrentFile();
                    if (file != null && file.x != null)
                            file.y.load(file.x.getReader());
                } catch (IOException|NotifyUserException e1) {
                    mw.reportException("Error: Reloading the file failed", e1, false);
                }
            }
        };
        this.buildRunMain = new AbstractAction(BUILD_RUN_MAIN_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String main = Settings.INSTANCE.getProperty("project.main-file", "");
                if (main.isEmpty())
                    JOptionPane.showMessageDialog(mw, "No main file specified.", "Could not build.",
                            JOptionPane.INFORMATION_MESSAGE);
                else {
                    Path path = Paths.get(main);
                    mw.build(path);
                    mw.run(path);
                }
            }
        };
        this.buildMain = new AbstractAction(BUILD_MAIN_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String main = Settings.INSTANCE.getProperty("project.main-file", "");
                if (main.isEmpty())
                    JOptionPane.showMessageDialog(mw, "No main file specified.", "Could not build.",
                            JOptionPane.INFORMATION_MESSAGE);
                else {
                    Path path = Paths.get(main);
                    mw.build(path);
                }
            }
        };
        this.runMain = new AbstractAction(RUN_MAIN_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String main = Settings.INSTANCE.getProperty("project.main-file", "");
                if (main.isEmpty())
                    JOptionPane.showMessageDialog(mw, "No main file specified.", "Could not run.",
                            JOptionPane.INFORMATION_MESSAGE);
                else {
                    Path path = Paths.get(main);
                    mw.build(path);
                }
            }
        };
        this.buildRunCurrent = new AbstractAction(BUILD_RUN_CURR_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {

                    mw.build(mw.getCurrentFile().x.getPath());
                    mw.run(mw.getCurrentFile().x.getPath());

                } catch (NotifyUserException e1) {
                    mw.reportException(e1.getMessage(), e1, false);
                }
            }
        };
        this.buildCurrent = new AbstractAction(BUILD_CURR_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {

                    mw.build(mw.getCurrentFile().x.getPath());

                } catch (NotifyUserException e1) {
                    mw.reportException(e1.getMessage(), e1, false);
                }
            }
        };
        this.runCurrent = new AbstractAction(RUN_CURR_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {

                    mw.run(mw.getCurrentFile().x.getPath());

                } catch (NotifyUserException e1) {
                    mw.reportException(e1.getMessage(), e1, false);
                }

            }
        };
        this.setMain = new AbstractAction(SET_MAIN_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fileChooser.showDialog(mw, "Choose") == JFileChooser.APPROVE_OPTION) {
                    Settings.INSTANCE.setProperty("project.main-file",
                            fileChooser.getSelectedFile().toPath().toAbsolutePath().toString());
                }
            }
        };
        this.settings = new AbstractAction(SETTINGS_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(SettingsWindow::new);
            }
        };

        ActionMap map = super.getRootPane().getActionMap();
        map.put(OPEN_FILE_TEXT, this.openFile);
        map.put(NEW_FILE_TEXT, this.newFile);
        map.put(SAVE_FILE_TEXT, this.saveFile);
        map.put(SAVE_FILE_AS_TEXT, this.saveAs);
        map.put(SAVE_ALL_FILES_TEXT, this.saveAll);
        map.put(CUT_TEXT, this.cut);
        map.put(COPY_TEXT, this.copy);
        map.put(PASTE_TEXT, this.paste);
        map.put(UNDO_TEXT, this.undo);
        map.put(REDO_TEXT, this.redo);
        map.put(REFRESH_TREE_TEXT, this.refreshTree);
        map.put(ZOOM_IN_TEXT, this.zoomIn);
        map.put(ZOOM_OUT_TEXT, this.zoomOut);
        map.put(NEXT_TAB_TEXT, this.nextTab);
        map.put(PREV_TAB_TEXT, this.prevTab);
        map.put(RELOAD_FILE_TEXT, this.reloadFile);
        map.put(BUILD_RUN_MAIN_TEXT, this.buildRunMain);
        map.put(BUILD_MAIN_TEXT, this.buildMain);
        map.put(RUN_MAIN_TEXT, this.runMain);
        map.put(BUILD_RUN_CURR_TEXT, this.buildRunCurrent);
        map.put(BUILD_CURR_TEXT, this.buildCurrent);
        map.put(RUN_CURR_TEXT, this.runCurrent);
        map.put(SET_MAIN_TEXT, this.setMain);
    }

    private void setUpKeyBindings() {
        InputMap input = super.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK), ZOOM_IN_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), ZOOM_IN_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), ZOOM_OUT_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), ZOOM_OUT_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), NEXT_TAB_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK), PREV_TAB_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                NEW_FILE_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), OPEN_FILE_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), SAVE_FILE_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                SAVE_FILE_AS_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK), RELOAD_FILE_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                REFRESH_TREE_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), BUILD_RUN_MAIN_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK), BUILD_MAIN_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                RUN_MAIN_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK),
                BUILD_RUN_CURR_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK),
                BUILD_CURR_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK |
                InputEvent.SHIFT_DOWN_MASK), RUN_CURR_TEXT);
        // use VIM key bindings, because binding arrows seems not to work ;-)
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK), "resizeTreeLeft");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "resizeTreeRight");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), "resizeProblemsUp");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.CTRL_DOWN_MASK), "resizeProblemsDown");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), UNDO_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), REDO_TEXT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                REDO_TEXT);
        ActionMap tmp = super.getRootPane().getActionMap();
        tmp.put("resizeTreeLeft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainWindow.this.mainSplit.setDividerLocation(MainWindow.this.mainSplit.getDividerLocation() - 10);
            }
        });
        tmp.put("resizeTreeRight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainWindow.this.mainSplit.setDividerLocation(MainWindow.this.mainSplit.getDividerLocation() + 10);
            }
        });
        tmp.put("resizeProblemsUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainWindow.this.problemsSplit.setDividerLocation(
                        MainWindow.this.problemsSplit.getDividerLocation() - 10);
            }
        });
        tmp.put("resizeProblemsDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainWindow.this.problemsSplit.setDividerLocation(
                        MainWindow.this.problemsSplit.getDividerLocation() + 10);
            }
        });
    }

    private void blockBuild(boolean enabled) {
        enabled = !enabled;
        this.buildMain.setEnabled(enabled);
        this.buildRunMain.setEnabled(enabled);
        this.buildCurrent.setEnabled(enabled);
        this.buildRunCurrent.setEnabled(enabled);
    }

    private void build(Path path) {
        blockBuild(true);
        if (Settings.INSTANCE.getBoolProperty(AUTOSAVE_SETTING))
            this.saveAll.actionPerformed(new ActionEvent(AUTOSAVE_SETTING, 0, "autosave"));
        try {
            Assembler a = this.project.getAssembler();
            List<Problem<?>> problems = new LinkedList<>();
            this.problems = problems;
            a.assemble(path, this.project.getProjectPath(), problems);

            this.lastBuilt = path;
        } catch (Exception e) {
            this.reportException("Could not build!", e, false);
        }
        this.refreshProblemTable();
        for (int i = 0; i < this.jTabbedPane.getTabCount(); ++i) {
            AccessibleScrollPaneHack pane = (AccessibleScrollPaneHack) this.jTabbedPane.getComponentAt(i);
            this.highlightProblems(pane.textFile, pane.child);
        }
        blockBuild(false);
    }

    private void run(Path path) {
        byte[] code;
        Listing listing;
        if (this.lastBuilt == null || !this.lastBuilt.equals(path)) {
            code = new byte[0];
            listing = null;
            // TODO: Search File and Listing on disk
        } else {
            code = this.project.getAssembler().getResult();
            if (code == null) {
                JOptionPane.showMessageDialog(this, "File not yet build!", "Cannot run!", JOptionPane.INFORMATION_MESSAGE);
                return;
            } else if (!this.project.getAssembler().wasSuccessful()) {
                JOptionPane.showMessageDialog(this, "The last assembling was not successful!", "Cannot run!",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            listing = this.project.getAssembler().getListing();
        }

        SwingUtilities.invokeLater(() -> new EmulatorWindow(this.project.makeEmulator(code), listing));
    }

    /**
     * toggleSplit JSplitPane
     * http://stackoverflow.com/questions/4934499/how-to-set-jsplitpane-divider-collapse-expand-state/11283453#11283453
     * @param collapse - if {@code true}, the component will be collapsed
     * @param sp the pane to toggle
     */
    public void toggleSplit(boolean collapse, JSplitPane sp) {
        try {
            //get divider object
            final BasicSplitPaneDivider divider = ((BasicSplitPaneUI) sp.getUI()).getDivider();
            final Field buttonField;
            //get field button from divider
            buttonField = BasicSplitPaneDivider.class.getDeclaredField(collapse ? "rightButton" : "leftButton");
            //allow access
            buttonField.setAccessible(true);
            //get instance of button to click at
            JButton button = (JButton) buttonField.get(divider);
            //click it
            button.doClick();
            //if you manage more dividers at same time before returning from event,
            //you should update layout and ui, otherwise nothing happens on some dividers:
            sp.updateUI();
            sp.doLayout();
            button.doClick();
        } catch (NullPointerException expected) { }
        catch (Exception e) {
            System.err.println("collapsing the split pane failed:");
            e.printStackTrace();
            this.reportException(e, false);
        }
    }

    private static final Comparator<Path> PATH_SORTER = (x, y) -> {
        if (Files.isDirectory(x) && Files.isDirectory(y) || !Files.isDirectory(x) && !Files.isDirectory(y))
            return x.compareTo(y);
        else if (Files.isDirectory(x)) return -1;
        else return 1;
    };

    private static final List<Path> dirList = new ArrayList<>();

    private static PathNode getChild(Path parent, int index) {
        dirList.clear();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(parent,
                x -> !Files.isHidden(x) && Files.isReadable(x))) {
            ds.forEach(dirList::add);
        } catch (IOException ignored) { ignored.printStackTrace(); }

        Collections.sort(dirList, MainWindow.PATH_SORTER);
        return new PathNode(dirList.get(index));
    }

    private static class PathNode {
        final Path path;

        private PathNode(Path path) {
            this.path = Objects.requireNonNull(path);
        }

        @Override
        public String toString() {
            return this.path.getFileName().toString();
        }
    }

    private static int getChildCount(Path parent) {
        if (!Files.isDirectory(parent))
            return 0;
        int[] count = {0};
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(parent,
                x -> !Files.isHidden(x) && Files.isReadable(x))) {
            ds.forEach(x -> ++count[0]);
        } catch (IOException ignored) { ignored.printStackTrace(); }
        return count[0];
    }

    private static int getIndex(Path parent, Path child) {
        dirList.clear();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(parent,
                x -> !Files.isHidden(x) && Files.isReadable(x))) {
            ds.forEach(dirList::add);
        } catch (IOException ignored) { ignored.printStackTrace(); }

        Collections.sort(dirList, MainWindow.PATH_SORTER);
        return dirList.indexOf(child);
    }

    private TreeModel makeFileSystemTree() {
        return new TreeModel() {
            @Override
            public Object getRoot() {
                return new PathNode(MainWindow.this.project.getProjectPath()) {
                    @Override
                    public String toString() {
                        return this.path.toString();
                    }
                };
            }

            @Override
            public Object getChild(Object parent, int index) {
                return MainWindow.getChild(((PathNode)parent).path, index);
            }

            @Override
            public int getChildCount(Object parent) {
                return MainWindow.getChildCount(((PathNode) parent).path);
            }

            @Override
            public boolean isLeaf(Object node) {
                return Files.isRegularFile(((PathNode) node).path);
            }

            @Override
            public void valueForPathChanged(TreePath path, Object newValue) {
            }

            @Override
            public int getIndexOfChild(Object parent, Object child) {
                return getIndex(((PathNode) parent).path, ((PathNode) child).path);
            }

            @Override
            public void addTreeModelListener(TreeModelListener l) {
            }

            @Override
            public void removeTreeModelListener(TreeModelListener l) {
            }
        };
    }

    private final static class AccessibleScrollPaneHack extends JScrollPane { // sorry
        final LineNumberSyntaxPane child;
        final TextFile textFile;
        AccessibleScrollPaneHack(LineNumberSyntaxPane child, TextFile textFile) {
            super(child);
            this.child = child;
            this.textFile = textFile;
        }
    }
}
