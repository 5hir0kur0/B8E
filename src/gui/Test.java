package gui;

import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author 5hir0kur0
 */
public class Test {
    public static void main(String[] args) throws InterruptedException, IOException {
        JFrame leWindow = new JFrame("Le testWindow");
        LineNumberSyntaxPane lnsp = new LineNumberSyntaxPane("hex");
        JScrollPane jsp = new JScrollPane(lnsp);
        leWindow.add(jsp);
        leWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        leWindow.pack();
        leWindow.setVisible(true);
    }

}