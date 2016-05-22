package gui;

import emulator.Emulator;
import emulator.NumeralSystem;
import emulator.RAM;
import emulator.Register;
import emulator.arc8051.MC8051;
import misc.Settings;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author Gordian, Tobias
 */
public class EmulatorWindow extends JFrame {
    private final Emulator emulator;
    private final Listing listing;
    private final NumeralSystem registerNumeralSystem;
    private final NumeralSystem memoryNumeralSystem;
    private boolean running;

    private final static String[] REGISTER_TABLE_HEADER = {"Register", "Value"};
    private final static String[] LISTING_TABLE_HEADER = {"Line", "Code"};
    private final static String[] MEMORY_TABLE_HEADER = {"Address", "00", "01", "02", "03", "04", "05", "06", "07",
            "08", "09", "0A", "0B", "0C", "0D", "0E", "0F"};
    private final static String WINDOW_TITLE = "Emulator";

    private final static String REGISTER_NUMERAL_SYSTEM_SETTING = "gui.emulator.registerNumeralSystem";
    private final static String REGISTER_NUMERAL_SYSTEM_SETTING_DEFAULT = NumeralSystem.BINARY.name();
    private final static Predicate<String> IS_VALID_NUMERAL_SYSTEM = s -> {
        try {
            NumeralSystem.valueOf(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    };
    private final static String MEMORY_NUMERAL_SYSTEM_SETTING = "gui.emulator.memoryNumeralSystem";
    private final static String MEMORY_NUMERAL_SYSTEM_SETTING_DEFAULT = NumeralSystem.HEXADECIMAL.name();
    static {
        Settings.INSTANCE.setDefault(REGISTER_NUMERAL_SYSTEM_SETTING, REGISTER_NUMERAL_SYSTEM_SETTING_DEFAULT);
        Settings.INSTANCE.setDefault(MEMORY_NUMERAL_SYSTEM_SETTING, MEMORY_NUMERAL_SYSTEM_SETTING_DEFAULT);
    }

    /**
     * Create a new emulator window.
     * @param emulator the {@link Emulator} to be used; must not be {@code null}
     * @param listing the {@link Listing} to be used; may be {@code null}
     */
    public EmulatorWindow(Emulator emulator, Listing listing) {
        this.emulator = Objects.requireNonNull(emulator, "emulator must not be null");
        this.listing = listing;
        this.registerNumeralSystem = NumeralSystem.valueOf(
                Settings.INSTANCE.getProperty(REGISTER_NUMERAL_SYSTEM_SETTING,
                REGISTER_NUMERAL_SYSTEM_SETTING_DEFAULT, IS_VALID_NUMERAL_SYSTEM));
        this.memoryNumeralSystem = NumeralSystem.valueOf(Settings.INSTANCE.getProperty(MEMORY_NUMERAL_SYSTEM_SETTING,
                MEMORY_NUMERAL_SYSTEM_SETTING_DEFAULT, IS_VALID_NUMERAL_SYSTEM));
        this.running = false;
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setSize(new Dimension(420, 420));
        createAndShowGUI();
    }

    private void createAndShowGUI() {
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setOneTouchExpandable(true);
        JPanel registersAndListing = new JPanel(new BorderLayout());
        JScrollPane registers = this.makeTable(new RegisterTableModel(), false);
        if (this.listing != null) {
            JScrollPane listing = this.makeTable(new ListingModel(this.listing), false);
            JTable tmpTable = ((JTable)((BorderLayout)((JPanel) listing.getViewport().getView()).getLayout())
                    .getLayoutComponent(BorderLayout.CENTER));
            DefaultTableCellRenderer tmp = new DefaultTableCellRenderer();
            tmp.setHorizontalAlignment(JLabel.RIGHT);
            tmpTable.getColumnModel().getColumn(0).setCellRenderer(tmp);
            tmpTable.getColumnModel().getColumn(0).setMaxWidth(42);
            tmpTable.getColumnModel().getColumn(0).setMinWidth(10);
            JSplitPane listingSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            listingSplit.setLeftComponent(registers);
            listingSplit.setRightComponent(listing);
            listingSplit.setResizeWeight(0.2);
            listingSplit.setDividerLocation(0.2);
            registersAndListing.add(listingSplit, BorderLayout.CENTER);
        } else registersAndListing.add(registers, BorderLayout.CENTER);
        mainSplit.setLeftComponent(registersAndListing);
        JSplitPane memorySplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JScrollPane internalRAM = this.makeTable(new MemoryModel(this.emulator.getMainMemory(),
                this.memoryNumeralSystem, true, true), true);
        memorySplit.setTopComponent(internalRAM);
        if (this.emulator.hasSecondaryMemory())
            memorySplit.setBottomComponent(this.makeTable(new MemoryModel(this.emulator.getSecondaryMemory(),
                    this.memoryNumeralSystem, true, false), true));
        mainSplit.setRightComponent(this.emulator.hasSecondaryMemory() ? memorySplit : internalRAM);
        this.add(mainSplit);

        this.setTitle(WINDOW_TITLE);
        this.setVisible(true);
    }

    private JScrollPane makeTable(AbstractTableModel model, boolean isMemory) {
        JTable table = new JTable(model);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(table.getTableHeader(), BorderLayout.NORTH);
        panel.add(table, BorderLayout.CENTER);
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        if (isMemory) {
            for (int i = 0; i < 17; ++i) {
                TableColumn column = table.getColumnModel().getColumn(i);
                if (i == 0) column.setPreferredWidth(42);
                else column.setPreferredWidth(12);
            }
        }
        return scrollPane;
    }

    private class RegisterTableModel extends AbstractTableModel {
        private final java.util.List<Register> registers = EmulatorWindow.this.emulator.getRegisters();
        private final NumeralSystem numeralSystem = EmulatorWindow.this.registerNumeralSystem;

        @Override
        public int getRowCount() {
            return this.registers.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int index) {
            return EmulatorWindow.REGISTER_TABLE_HEADER[index];
        }

        @Override
        public Object getValueAt(int row, int col) {
            return col == 0 ?
                    this.registers.get(row).getName() :
                    this.registers.get(row).getDisplayValue(this.numeralSystem);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return !EmulatorWindow.this.running && col == 1 && row < this.registers.size();
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col != 1 || EmulatorWindow.this.running) return;
            final String tmpValue = value.toString();
            this.registers.get(row).setValueFromString(this.numeralSystem, tmpValue);
        }
    }

    private class ListingModel extends AbstractTableModel {
        private final Listing listing;
        private final String[] data;

        ListingModel(Listing listing) {
            this.listing = Objects.requireNonNull(listing, "listing must not be null");
            this.data = listing.getData();
        }

        @Override
        public int getRowCount() {
            return this.data.length;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int col) {
            return EmulatorWindow.LISTING_TABLE_HEADER[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (col == 0) return Integer.toString(row + 1);
            else return this.data[row];
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }

    private class MemoryModel extends AbstractTableModel {
        private final RAM ram;
        private final boolean modifiable;
        private final boolean isInternal;
        private final NumeralSystem numeralSystem;

        MemoryModel(RAM ram, NumeralSystem numeralSystem, boolean modifiable, boolean isInternal) {
            this.ram = Objects.requireNonNull(ram, "memory must not be null");
            this.modifiable = modifiable;
            this.isInternal = isInternal;
            this.numeralSystem = Objects.requireNonNull(numeralSystem);
        }

        @Override
        public int getRowCount() {
            return this.ram.getSize() / 16;
        }

        @Override
        public int getColumnCount() {
            return 17;
        }

        @Override
        public String getColumnName(int index) {
            return index == 0 ?
                    this.isInternal ?
                            "Internal " + EmulatorWindow.MEMORY_TABLE_HEADER[index] :
                            "External " + EmulatorWindow.MEMORY_TABLE_HEADER[index] :
                    "00" + EmulatorWindow.MEMORY_TABLE_HEADER[index];
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (col == 0) return this.numeralSystem.toString(row, 4);
            return this.numeralSystem.toString(this.ram.get(row * 16 + col - 1) & 0xFF, 2);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return !EmulatorWindow.this.running && this.modifiable;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (!this.modifiable || EmulatorWindow.this.running) return;
            final String tmpValue = value.toString();
            this.ram.set(row * 16 + col - 1, (byte)this.numeralSystem.getValue(tmpValue));
        }
    }

    public static void main(String[] iDontNeedNoArgs) {
        SwingUtilities.invokeLater(() -> new EmulatorWindow(new MC8051(new RAM(65536), new RAM(65536)),
                new Listing() {
                    private String[] data = {
                        "mov  @r0 ,  #4Dh",
                        "mov  @r1 ,  #4Fh",
                        "mov  @r0 ,    a",
                        "mov  @r1 ,    a",
                        "mov  @r0 ,   56h",
                        "mov    a ,  #20h",
                        "mov    a ,  @r0",
                        "mov    a ,  @r1",
                        "mov    a ,   r0",
                        "mov    a ,   r1",
                        "mov    a ,   r2",
                        "mov    a ,   r3",
                        "mov    a ,   r4",
                        "mov    a ,   r5",
                        "mov    a ,   r6",
                        "mov    a ,   r7",
                        "mov    a ,   61h",
                        "mov    c ,   6Eh",
                        "mov dptr ,#6420h",
                        "mov   r0 ,  #69h",
                        "mov   r1 ,  #74h",
                        "mov   r2 ,  #73h",
                        "mov   r3 ,  #20h",
                        "mov   r4 ,  #62h",
                        "mov   r5 ,  #61h",
                        "mov   r6 ,  #7Ah",
                        "mov   r7 ,  #69h",
                        "mov   r0 ,    a",
                        "mov   r1 ,    a",
                        "mov   r2 ,    a",
                        "mov   r3 ,    a",
                        "mov   r4 ,    a",
                        "mov   r5 ,    a",
                        "mov   r6 ,    a",
                        "mov   r7 ,    a",
                        "mov   r0 ,   6Ch",
                        "mov   r1 ,   6Ch",
                        "mov   r2 ,   69h",
                        "mov   r3 ,   6Fh",
                        "mov   r4 ,   6Eh",
                        "mov   r5 ,   20h",
                        "mov   r6 ,   6Fh",
                        "mov   r7 ,   70h",
                        "mov   63h,  #6Fh",
                        "mov   64h,    c",
                        "mov   65h,  @r0",
                        "mov   73h,  @r1",
                        "mov   3Bh,   r0",
                        "mov   20h,   r1",
                        "mov   64h,   r2",
                        "mov   61h,   r3",
                        "mov   7Ah,   r4",
                        "mov   7Ah,   r5",
                        "mov   6Ch,   r6",
                        "mov   69h,   r7",
                        "mov   6Eh,    a",
                        "mov   67h,   21h"
                    };
                    @Override
                    public String[] getData() {
                        return this.data;
                    }

                    @Override
                    public int getIndexForPCValue(int pc) {
                        return pc / 2;
                    }
                }));
    }
}

interface Listing { // TODO REPLACE BY REAL ONE
    String[] getData();
    int getIndexForPCValue(int pc);
}
