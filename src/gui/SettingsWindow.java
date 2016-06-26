package gui;

import controller.Main;
import misc.Settings;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author 5hir0kur0
 */
class SettingsWindow extends JFrame {

    private final List<String> keys;
    private final static String TITLE = "B8E Settings";
    private static final String[] COLUMN_NAMES = {"Setting", "Value"};

    public SettingsWindow() {
        for (String clazz : Main.CLASSES_WITH_SETTINGS)
            try {
                Class.forName(clazz);
            } catch (ClassNotFoundException ignored) {
                ignored.printStackTrace();
            }
        this.keys = new ArrayList<>(Settings.INSTANCE.getKeys());
        Collections.sort(this.keys);
        final JTable table = new JTable(new SettingsTableModel());
        super.add(new JScrollPane(table));
        super.setTitle(TITLE);
        super.setVisible(true);
        super.setPreferredSize(new Dimension(400, 600));
        super.setSize(400, 600);
        super.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    private class SettingsTableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return SettingsWindow.this.keys.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int col) {
            return COLUMN_NAMES[col];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) return SettingsWindow.this.keys.get(rowIndex);
            else return Settings.INSTANCE.getProperty(SettingsWindow.this.keys.get(rowIndex));
        }

        @Override
        public boolean isCellEditable(int row,  int col) {
            return col == 1;
        }

        @Override
        public void setValueAt(Object newValue, int row, int col) {
            if (col != 1 || null == newValue || row < 0 || row >= SettingsWindow.this.keys.size()) return;
            Settings.INSTANCE.setProperty(SettingsWindow.this.keys.get(row), newValue.toString());
        }
    }

    public static void main(String[] args) throws ClassNotFoundException {
        SwingUtilities.invokeLater(SettingsWindow::new);
    }
}
