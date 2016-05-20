package gui;

import controller.Project;
import controller.TextFile;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import misc.Pair;
import org.junit.*;

import javax.swing.*;
import javax.swing.event.TreeModelListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * @author T.
 */
public class MainWindow extends JFrame {
    private final Project project;

    private final JTabbedPane jTabbedPane;
    private final JSplitPane problemsSplit;
    private final JSplitPane mainSplit;
    private final JTable problemTable;
    private JTree fsTree;
    private final JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
    private Action openFile, newFile, saveFile, saveAs, saveAll, cut, copy, paste, undo, redo, refreshTree;
    { setUpActions(); }

    private final List<Pair<TextFile, LineNumberSyntaxPane>> openFiles;

    private final static String FILE_EXTENSION_SEPARATOR = ".";
    // used when creating a new tab without a corresponding file
    private final static String DEFAULT_FILE_EXTENSION = "asm";

    public MainWindow(String title, Project project) {
        super.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // TODO: Add close handler
        super.setSize(420, 420); // TODO: Use #.pack() later
        super.setLocationRelativeTo(null);
        super.setTitle(title);
        super.setLayout(new BorderLayout());


        super.setJMenuBar(makeMenu());

        this.project = Objects.requireNonNull(project);

        this.problemTable = new JTable();
        this.problemTable.setFillsViewportHeight(true);

        this.jTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM, JTabbedPane.SCROLL_TAB_LAYOUT);

        this.problemsSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        this.problemsSplit.setOneTouchExpandable(true);
        this.problemsSplit.setBottomComponent(new JScrollPane(this.problemTable));
        this.problemsSplit.setTopComponent(this.jTabbedPane);

        // Default to collapsed ... sort of
        // TODO: Find a better way to do that
        this.problemTable.setMinimumSize(new Dimension());
        this.problemsSplit.setDividerLocation(super.getHeight());
        this.mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        this.mainSplit.setOneTouchExpandable(true);
        this.fsTree = new JTree(makeFileSystemTree());
        this.mainSplit.setRightComponent(new JScrollPane((this.fsTree)));
        this.mainSplit.setLeftComponent(this.problemsSplit);
        super.add(this.mainSplit, BorderLayout.CENTER);

        this.openFiles = new ArrayList<>(10);

        super.setVisible(true);

        // doesn't work when called directly
        SwingUtilities.invokeLater(() -> toggleSplit(true, this.problemsSplit));
        if (!this.project.isPermanent())
            SwingUtilities.invokeLater(() -> toggleSplit(true, this.mainSplit));
    }


    public void reportException(Exception e, boolean severe) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        JTextArea jta = new JTextArea(sw.toString());
        jta.setTabSize(4);
        jta.setEditable(false);
        JScrollPane jsp = new JScrollPane(jta);
        if (System.getProperty("os.name").toLowerCase().contains("windows"))
            jta.setToolTipText("This " + (severe ? "definitely" : "probably")
                    + " happened because you are using windows.");
        JOptionPane.showMessageDialog(this, jsp, "An Exception occurred: "+e.getClass().getSimpleName(),
                severe ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    public void panic() {
        throw new UnsupportedOperationException("oopsie"); //TODO
    }

    private JMenuBar makeMenu() {
        setUpActions();

        final String FILE_MENU_TEXT = "File";
        final String EDIT_MENU_TEXT = "Edit";

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu(FILE_MENU_TEXT);

        JMenuItem newFile = new JMenuItem(this.newFile);
        newFile.setMnemonic('n');
        JMenuItem openFile = new JMenuItem(this.openFile);
        openFile.setMnemonic('o');
        JMenuItem saveFile = new JMenuItem(this.saveFile);
        saveFile.setMnemonic('s');
        JMenuItem saveFileAs = new JMenuItem(this.saveAs);
        saveFileAs.setMnemonic('v');
        JMenuItem saveAll = new JMenuItem(this.saveAll);
        saveAll.setMnemonic('a');

        fileMenu.add(newFile);
        fileMenu.add(openFile);
        fileMenu.addSeparator();
        fileMenu.add(saveFile);
        fileMenu.add(saveFileAs);
        fileMenu.add(saveAll);
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
        JMenuItem refreshTree = new JMenuItem(this.refreshTree);
        refreshTree.setMnemonic('f');

        editMenu.add(undo);
        editMenu.add(redo);
        editMenu.addSeparator();
        editMenu.add(copy);
        editMenu.add(cut);
        editMenu.add(paste);
        editMenu.add(refreshTree);
        menuBar.add(editMenu);

        return menuBar;
    }

    private static String getFileExtension(Path path) {
        final String fileName = path.getFileName().toString();
        final int lastIndex = fileName.lastIndexOf(FILE_EXTENSION_SEPARATOR) + 1;
        if (lastIndex < 0 || lastIndex >= fileName.length()) return "";
        else return fileName.substring(lastIndex);
    }

    private Pair<TextFile, LineNumberSyntaxPane> getCurrentFile() {
        LineNumberSyntaxPane syntaxPane = (LineNumberSyntaxPane) this.jTabbedPane.getSelectedComponent();
        for (Pair<TextFile, LineNumberSyntaxPane> p : this.openFiles) {
            if (p.y == syntaxPane) return p;
        }
        return null;
    }

    private Pair<TextFile, LineNumberSyntaxPane> getFileAt(int index) {
        LineNumberSyntaxPane syntaxPane = (LineNumberSyntaxPane) this.jTabbedPane.getComponentAt(index);
        for (Pair<TextFile, LineNumberSyntaxPane> p : this.openFiles) {
            if (p.y == syntaxPane) return p;
        }
        return null;
    }

    private void saveFile(Pair<TextFile, LineNumberSyntaxPane> file) {
        if (file != null)
            if (file.x != null)
                try {
                    file.y.store(file.x.getWriter());
                } catch (IOException e1) {
                    this.reportException(e1, false);
                    e1.printStackTrace();
                }
            else
                saveFileAs(file);
    }

    private void saveFileAs(Pair<TextFile, LineNumberSyntaxPane> file) {
        if (this.fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            final Path path = this.fileChooser.getSelectedFile().toPath();
            try {
                file.x = this.project.requestResource(path, true);
                file.y.store(file.x.getWriter());
                file.y.setFileExtension(getFileExtension(path)); // update syntax highlighting
                file.y.load(file.x.getReader());
                for (int i = 0, stop = this.jTabbedPane.getTabCount(); i < stop; ++i) {
                    if (file.y == this.jTabbedPane.getComponentAt(i)) {
                        this.jTabbedPane.setTitleAt(i, file.x.getPath().getFileName().toString());
                        return;
                    }
                }
                throw new IllegalStateException("couldn't get file name of new file: " + file.x.getPath());
            } catch (IOException e1) {
                this.reportException(e1, false);
                e1.printStackTrace();
            }
        }
    }

    private void refreshTree() {
        this.fsTree = new JTree(MainWindow.this.makeFileSystemTree());
        this.mainSplit.setRightComponent(new JScrollPane((MainWindow.this.fsTree)));
    }

    private void openTab(LineNumberSyntaxPane syntaxPane, String title) {
        final JScrollPane scrollPane = new JScrollPane(syntaxPane);
        scrollPane.getVerticalScrollBar().setUnitIncrement(5);
        if (this.jTabbedPane.getTabCount() != 0) {
            this.jTabbedPane.add(scrollPane, this.jTabbedPane.getSelectedIndex() + 1);
            this.jTabbedPane.setSelectedIndex(this.jTabbedPane.getSelectedIndex() + 1);
        } else
            this.jTabbedPane.add(scrollPane);
        this.jTabbedPane.setTitleAt(this.jTabbedPane.getSelectedIndex(), title);
    }

    private void setUpActions() {
        final String UNDO_TEXT = "Undo";
        final String REDO_TEXT = "Redo";
        final String COPY_TEXT = "Copy";
        final String CUT_TEXT = "Cut";
        final String PASTE_TEXT = "Paste";
        final String SAVE_FILE_TEXT = "Save";
        final String SAVE_FILE_AS_TEXT = "Save as…";
        final String SAVE_ALL_FILES_TEXT = "Save all";
        final String NEW_FILE_TEXT = "New";
        final String OPEN_FILE_TEXT = "Open";
        final String REFRESH_TREE_TEXT = "Refresh file system tree";

        final MainWindow mw = this;

        this.openFile = new AbstractAction(OPEN_FILE_TEXT) {
            public void actionPerformed(ActionEvent e) {
                if (mw.fileChooser.showOpenDialog(mw) == JFileChooser.APPROVE_OPTION) {
                    final Path file = mw.fileChooser.getSelectedFile().toPath();
                    try {
                        final LineNumberSyntaxPane syntaxPane = new LineNumberSyntaxPane(getFileExtension(file));
                        TextFile textFile = mw.project.requestResource(file, false);
                        syntaxPane.load(textFile.getReader());
                        mw.openTab(syntaxPane, file.getFileName().toString());
                        mw.openFiles.add(new Pair<>(textFile, syntaxPane));
                    } catch (IOException e1) {
                        System.err.println("opening the file failed");
                        e1.printStackTrace();
                        mw.reportException(e1, false);
                    }
                }
            }
        };
        this.newFile = new AbstractAction(NEW_FILE_TEXT) {
            public void actionPerformed(ActionEvent e) {
                try {
                    final LineNumberSyntaxPane syntaxPane = new LineNumberSyntaxPane(DEFAULT_FILE_EXTENSION);
                    mw.openTab(syntaxPane, "(untitled)");
                    mw.openFiles.add(new Pair<>(null, syntaxPane));
                } catch (IOException e1) {
                    mw.reportException(e1, false);
                }
            }
        };
        this.saveFile = new AbstractAction(SAVE_FILE_TEXT) {
            public void actionPerformed(ActionEvent e) {
                mw.saveFile(mw.getCurrentFile());
                mw.refreshTree();
            }
        };
        this.saveAll = new AbstractAction(SAVE_ALL_FILES_TEXT) {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0, all = mw.jTabbedPane.getTabCount(); i < all; ++i) {
                    mw.saveFile(mw.getFileAt(i));
                    mw.refreshTree();
                }
            }
        };
        this.saveAs = new AbstractAction(SAVE_FILE_AS_TEXT) {
            public void actionPerformed(ActionEvent e) {
                mw.saveFileAs(mw.getCurrentFile());
                mw.refreshTree();
            }
        };
        this.undo = new AbstractAction(UNDO_TEXT) {
            public void actionPerformed(ActionEvent e) {
                LineNumberSyntaxPane lnsp = (LineNumberSyntaxPane) mw.jTabbedPane.getSelectedComponent();
                if (lnsp != null) {
                    throw new UnsupportedOperationException("kill stuff");
                    //TODO
                }
            }
        };
        this.redo = new AbstractAction(REDO_TEXT) {
            public void actionPerformed(ActionEvent e) {
                LineNumberSyntaxPane lnsp = (LineNumberSyntaxPane) mw.jTabbedPane.getSelectedComponent();
                if (lnsp != null) {
                    throw new UnsupportedOperationException("pill stuff");
                    //TODO
                }
            }
        };
        this.copy = new AbstractAction(COPY_TEXT) {
            public void actionPerformed(ActionEvent e) {
                LineNumberSyntaxPane syntaxPane = (LineNumberSyntaxPane) mw.jTabbedPane.getSelectedComponent();
                syntaxPane.copy();
            }
        };
        this.cut = new AbstractAction(CUT_TEXT) {
            public void actionPerformed(ActionEvent e) {
                LineNumberSyntaxPane syntaxPane = (LineNumberSyntaxPane) mw.jTabbedPane.getSelectedComponent();
                syntaxPane.cut();
            }
        };
        this.paste = new AbstractAction(PASTE_TEXT) {
            public void actionPerformed(ActionEvent e) {
                LineNumberSyntaxPane syntaxPane = (LineNumberSyntaxPane) mw.jTabbedPane.getSelectedComponent();
                syntaxPane.paste();
            }
        };
        this.refreshTree = new AbstractAction(REFRESH_TREE_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                mw.refreshTree();
            }
        };
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
                System.out.println("MainWindow.valueForPathChanged");
            }

            @Override
            public int getIndexOfChild(Object parent, Object child) {
                return getIndex(((PathNode) parent).path, ((PathNode) child).path);
            }

            @Override
            public void addTreeModelListener(TreeModelListener l) {
                System.out.println("MainWindow.addTreeModelListener");
            }

            @Override
            public void removeTreeModelListener(TreeModelListener l) {
                System.out.println("MainWindow.removeTreeModelListener");
            }
        };
    }
}
