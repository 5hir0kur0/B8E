package gui;

import javax.sound.sampled.Line;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

/**
 * @author T.
 */
public class MainWindow extends JFrame {
    public static void main(String[] a) throws IOException {
        MainWindow m = new MainWindow("FooBar FTW");
        //m.reportException(new IllegalArgumentException("lkasdjflköjasdlfk"), false);
    }


    private JTabbedPane jTabbedPane;
    private JSplitPane jSplitPane;
    private JTable problemTable;
    private JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
    private Action openFile, newFile, saveFile, saveAs, saveAll, cut, copy, paste, undo, redo;
    { setUpActions(); }


    public MainWindow(String title) {
        super.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // TODO: Add close handler
        super.setSize(420, 420); // TODO: Use #.pack() later
        super.setLocationRelativeTo(null);
        super.setTitle(title);

        super.setJMenuBar(makeMenu());

        problemTable = new JTable();
        problemTable.setFillsViewportHeight(true);

        this.jTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM, JTabbedPane.SCROLL_TAB_LAYOUT);

        jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        jSplitPane.setOneTouchExpandable(true);
        jSplitPane.setBottomComponent(new JScrollPane(problemTable));
        jSplitPane.setTopComponent(jTabbedPane);

        // Default to collapsed ... sort of
        // TODO: Find a better way to do that
        problemTable.setMinimumSize(new Dimension());
        jSplitPane.setDividerLocation(super.getHeight());
        super.add(this.jSplitPane);
        super.setVisible(true);
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


        editMenu.add(undo);
        editMenu.add(redo);
        editMenu.addSeparator();
        editMenu.add(copy);
        editMenu.add(cut);
        editMenu.add(paste);
        menuBar.add(editMenu);


        return menuBar;
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

        final MainWindow mw = this;

        this.openFile = new AbstractAction(OPEN_FILE_TEXT) {
            public void actionPerformed(ActionEvent e) {
                if (mw.fileChooser.showOpenDialog(mw) == JFileChooser.APPROVE_OPTION) {
                    final Path file = mw.fileChooser.getSelectedFile().toPath();
                    try {
                        final LineNumberSyntaxPane lnsp = new LineNumberSyntaxPane("asm"); //TODO: Implement file type recognition
                        if (mw.jTabbedPane.getTabCount() != 0) {
                            mw.jTabbedPane.add(lnsp, mw.jTabbedPane.getSelectedIndex() + 1);
                            mw.jTabbedPane.setSelectedIndex(mw.jTabbedPane.getSelectedIndex() + 1);
                        } else
                            mw.jTabbedPane.add(lnsp);
                        mw.jTabbedPane.setTitleAt(mw.jTabbedPane.getSelectedIndex(), file.getFileName().toString());
                        // lnsp.load(file); //TODO: Implement load path method ?
                    } catch (IOException e1) {
                        mw.reportException(e1, false);
                    }
                }
                //TODO
            }
        };
        this.newFile = new AbstractAction(NEW_FILE_TEXT) {
            public void actionPerformed(ActionEvent e) {
                try {
                    final LineNumberSyntaxPane lnsp = new LineNumberSyntaxPane("asm");
                    if (mw.jTabbedPane.getTabCount() != 0) {
                        mw.jTabbedPane.add(lnsp, mw.jTabbedPane.getSelectedIndex() + 1);
                        mw.jTabbedPane.setSelectedIndex(mw.jTabbedPane.getSelectedIndex() + 1);
                    } else
                        mw.jTabbedPane.add(lnsp);
                    mw.jTabbedPane.setTitleAt(mw.jTabbedPane.getSelectedIndex(),"(untitled)");
                } catch (IOException e1) {
                    mw.reportException(e1, false);
                }
            }
        };
        this.saveFile = new AbstractAction(SAVE_FILE_TEXT) {
            public void actionPerformed(ActionEvent e) {
                LineNumberSyntaxPane lnsp = (LineNumberSyntaxPane) mw.jTabbedPane.getSelectedComponent();
                if (lnsp != null /* &&  */) // If LNSP can write themselves: Should they know the target Path?
                    if (true /* lnsp.isWritten()*/)
                        lnsp.store(); // ?!
                    else {
                        if (mw.fileChooser.showSaveDialog(mw) == JFileChooser.APPROVE_OPTION) {
                            final Path file = mw.fileChooser.getSelectedFile().toPath();

                            // lnsp.store(mw.fileChooser.getSelectedFile().toPath()); //TODO: Implement store path method?
                           //TODO
                        }
                    }
            }
        };
        this.saveAll = new AbstractAction(SAVE_ALL_FILES_TEXT) {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0, all = mw.jTabbedPane.getTabCount(); i < all; ++i) {
                    LineNumberSyntaxPane lnsp = (LineNumberSyntaxPane) mw.jTabbedPane.getComponentAt(i);
                    if (lnsp != null /* && lnsp.isWritten() */) // If LNSP can write themselves: Should they know the target Path?
                        lnsp.store(); // ?!
                    //TODO
                }
            }
        };
        this.saveAs = new AbstractAction(SAVE_FILE_AS_TEXT) {
            public void actionPerformed(ActionEvent e) {
                LineNumberSyntaxPane lnsp = (LineNumberSyntaxPane) mw.jTabbedPane.getSelectedComponent();
                if (lnsp != null) {
                    if (mw.fileChooser.showSaveDialog(mw) == JFileChooser.APPROVE_OPTION) {
                        final Path file = mw.fileChooser.getSelectedFile().toPath();

                        // lnsp.store(mw.fileChooser.getSelectedFile()); //TODO: Implement store path method

                    }
                }
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
                LineNumberSyntaxPane lnsp = (LineNumberSyntaxPane) mw.jTabbedPane.getSelectedComponent();
                if (lnsp != null) {
                    throw new UnsupportedOperationException("fill stuff");
                    //TODO
                }
            }
        };
        this.cut = new AbstractAction(CUT_TEXT) {
            public void actionPerformed(ActionEvent e) {
                LineNumberSyntaxPane lnsp = (LineNumberSyntaxPane) MainWindow.this.jTabbedPane.getSelectedComponent();
                if (lnsp != null) {
                    throw new UnsupportedOperationException("mill stuff");
                    //TODO
                }
            }
        };
        this.paste = new AbstractAction(PASTE_TEXT) {
            public void actionPerformed(ActionEvent e) {
                LineNumberSyntaxPane lnsp = (LineNumberSyntaxPane) MainWindow.this.jTabbedPane.getSelectedComponent();
                if (lnsp != null) {
                    throw new UnsupportedOperationException("will stuff");
                    //TODO
                }
            }
        };
    }
}
