package gui;

import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author Gordian
 */
public class Test {
    public static void main(String[] args) throws InterruptedException, IOException {
        JFrame leWindow = new JFrame("Le testWindow");
        LineNumberSyntaxPane lnsp = new LineNumberSyntaxPane("properties");
        JScrollPane jsp = new JScrollPane(lnsp);
        leWindow.add(jsp);
        leWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        leWindow.pack();
        leWindow.setVisible(true);
    }

}
