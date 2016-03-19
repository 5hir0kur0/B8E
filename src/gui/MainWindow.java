package gui;

import oracle.jrockit.jfr.JFR;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

/**
 * @author Tobias
 */
public class MainWindow extends JFrame {
    public static void main(String[] a) {
        MainWindow m = new MainWindow();
        m.reportException(new IllegalArgumentException("lkasdjflköjasdlfk"), false);
    }

    private JTabbedPane jTabbedPane;
    private JSplitPane jSplitPane;
    private JTable problemTable;

    public MainWindow() {
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // TODO: Add close handler
        this.setSize(420, 420); // TODO: Use #.pack() later
        this.setLocationRelativeTo(null);

        problemTable = new JTable();
        problemTable.setFillsViewportHeight(true);

        this.jTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM, JTabbedPane.SCROLL_TAB_LAYOUT);

        jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        jSplitPane.setOneTouchExpandable(true);
        jSplitPane.setBottomComponent(new JScrollPane(problemTable));

        // Default to collapsed ... sort of
        // TODO: Find a better way to do that
        problemTable.setMinimumSize(new Dimension());
        jSplitPane.setDividerLocation(super.getHeight());
        jSplitPane.setTopComponent(jTabbedPane);
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
    }
}
