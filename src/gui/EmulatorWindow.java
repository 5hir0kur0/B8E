package gui;

import emulator.*;
import emulator.arc8051.MC8051;
import misc.Settings;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Exchanger;
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
    private final SwingWorker<Void, Void> emulatorRunner;
    private JButton nextButton, runButton, pauseButton, codeButton;
    private JTable listingTable;
    private ByteRegister PCH, PCL;

    private final static String[] REGISTER_TABLE_HEADER = {"Register", "Value"};
    private final static String[] LISTING_TABLE_HEADER = {"Line", "Code"};
    private final static String[] MEMORY_TABLE_HEADER = {"Address", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "A", "B", "C", "D", "E", "F"};
    private final static String WINDOW_TITLE = "Emulator";
    private final static Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 11);
    private final static Font HEADER_FONT = new Font(Font.MONOSPACED, Font.BOLD, 11);

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

        this.emulatorRunner = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                while (EmulatorWindow.this.running) try {
                    EmulatorWindow.this.emulator.next();
                    EmulatorWindow.this.updateListingTable();
                } catch (EmulatorException e) {
                    EmulatorWindow.this.reportException("An Exception occurred while running the program",
                            e.getClass().getSimpleName() + ": " + e.getMessage(), e);
                    return null;
                }
                return null;
            }
        };

        for (Register r : this.emulator.getRegisters()) {
            if (r.getName().equals("PCH")) this.PCH = (ByteRegister) r;
            else if (r.getName().equals("PCL")) this.PCL = (ByteRegister) r;
        }

        if (this.PCH == null || this.PCL == null) throw new IllegalArgumentException("Couldn't get PCH or PCL");

        createAndShowGUI();
    }

    public void reportException(String title, String message, Exception e) {
        JPanel panel = new JPanel(new BorderLayout());
        if (message != null) {
            JTextArea jta = new JTextArea(message);
            jta.setEditable(false);
            JScrollPane jsp = new JScrollPane(jta);
            panel.add(jsp, BorderLayout.CENTER);
        }
        panel.setPreferredSize(new Dimension(600, 200));
        panel.add(new JLabel("If you still want to continue the program, just hit run again."), BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this, panel, title, JOptionPane.WARNING_MESSAGE);
    }

    private void createAndShowGUI() {
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setOneTouchExpandable(true);
        JPanel registersAndListing = new JPanel(new BorderLayout());
        JScrollPane registers = this.makeTable(new RegisterTableModel(), false);
        if (this.listing != null) {
            JScrollPane listing = this.makeTable(new ListingModel(this.listing), false);
            JTable tmpTable = ((JTable)((BorderLayout)((JPanel) listing.getViewport().getView()).getLayout())
                    .getLayoutComponent(BorderLayout.CENTER)); // sorry...
            DefaultTableCellRenderer tmp = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table,
                                                               Object value, boolean isSelected, boolean hasFocus,
                                                               int row, int column) {
                    // adapted from https://stackoverflow.com/questions/16113950/jtable-change-column-font
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                            row, column);
                    this.setFont(EmulatorWindow.HEADER_FONT);
                    return this;
                }
            };
            tmp.setHorizontalAlignment(JLabel.RIGHT);
            tmp.setFont(EmulatorWindow.HEADER_FONT);
            tmpTable.getColumnModel().getColumn(0).setCellRenderer(tmp);
            tmpTable.getColumnModel().getColumn(0).setPreferredWidth(32);
            tmpTable.getColumnModel().getColumn(0).setMaxWidth(42);
            tmpTable.getColumnModel().getColumn(0).setMinWidth(10);
            this.listingTable = tmpTable;
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
        memorySplit.setDividerLocation(0.42);
        memorySplit.setResizeWeight(0.27);
        if (this.emulator.hasSecondaryMemory())
            memorySplit.setBottomComponent(this.makeTable(new MemoryModel(this.emulator.getSecondaryMemory(),
                    this.memoryNumeralSystem, true, false), true));
        mainSplit.setRightComponent(this.emulator.hasSecondaryMemory() ? memorySplit : internalRAM);
        mainSplit.setDividerLocation(0.5);
        mainSplit.setResizeWeight(0.5);
        JPanel content = new JPanel(new BorderLayout());
        content.add(mainSplit, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar("Emulator Toolbar", JToolBar.HORIZONTAL);
        this.addButtons(toolBar);
        content.add(toolBar, BorderLayout.NORTH);

        this.add(content);

        this.setTitle(WINDOW_TITLE);
        this.setVisible(true);
    }

    private void addButtons(JToolBar toolBar) {
        this.nextButton  = new JButton("Next Instruction");
        this.nextButton.setMnemonic('n');
        this.nextButton.addActionListener(this::nextInstruction);
        this.runButton   = new JButton("Run Program");
        this.runButton.setMnemonic('r');
        this.runButton.addActionListener(this::runProgram);
        this.pauseButton = new JButton("Pause Program");
        this.pauseButton.setMnemonic('p');
        this.pauseButton.addActionListener(this::pauseProgram);
        this.codeButton  = new JButton("Show Code Memory");
        this.codeButton.setMnemonic('c');
        this.codeButton.addActionListener(this::showCodeMemory);

        toolBar.add(this.nextButton);
        toolBar.add(this.runButton);
        toolBar.add(this.pauseButton);
        toolBar.addSeparator();
        toolBar.addSeparator();
        toolBar.add(this.codeButton);
    }

    private void showCodeMemory(ActionEvent e) {
        final JScrollPane codeMemory = this.makeTable(new MemoryModel(
                this.emulator.getCodeMemory(),
                this.memoryNumeralSystem,
                false,
                false
                ), true);
        codeMemory.setPreferredSize(new Dimension(800, 400));
        JOptionPane.showMessageDialog(this, codeMemory, "Code Memory", JOptionPane.PLAIN_MESSAGE);
    }

    private void pauseProgram(ActionEvent e) {
        this.running = false;
        this.enableElements();
        super.repaint();
    }

    private void runProgram(ActionEvent e) {
        this.running = true;
        this.disableElements(true, true, false, false);
        this.emulatorRunner.execute();
    }

    private void nextInstruction(ActionEvent e) {
        this.running = true;
        this.disableElements(true, true, true, false);
        try {
            this.emulator.next();
            this.updateListingTable();
            super.repaint();
        } catch (EmulatorException e1) {
            EmulatorWindow.this.reportException("An Exception occurred while running the instruction",
                    e1.getClass().getSimpleName() + ": " + e1.getMessage(), e1);
        }
        this.running = false;
        this.enableElements();
    }

    private void disableElements(boolean nextButton, boolean runButton, boolean pauseButton, boolean codeButton) {
        if (nextButton) this.nextButton.setEnabled(false);
        if (runButton) this.runButton.setEnabled(false);
        if (pauseButton) this.pauseButton.setEnabled(false);
        if (codeButton) this.codeButton.setEnabled(false);
    }

    private void enableElements() {
        this.nextButton.setEnabled(true);
        this.runButton.setEnabled(true);
        this.pauseButton.setEnabled(true);
        this.codeButton.setEnabled(true);
    }

    private void updateListingTable() {
        if (this.listingTable == null || this.listing == null) return;
        char pc = (char)((this.PCH.getValue() << 8) & 0xFF00 | this.PCL.getValue() & 0xFF);
        int row = this.listing.getIndexForPCValue(pc);
        if (row >= this.listingTable.getRowCount()) {
            this.listingTable.setRowSelectionInterval(0, this.listingTable.getRowCount() - 1);
            return;
        }
        this.listingTable.setRowSelectionInterval(row, row);
    }

    private JScrollPane makeTable(AbstractTableModel model, boolean isMemory) {
        JTable table = new JTable(model);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(table.getTableHeader(), BorderLayout.NORTH);
        panel.add(table, BorderLayout.CENTER);
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        table.setFont(EmulatorWindow.FONT);
        table.getTableHeader().setFont(EmulatorWindow.HEADER_FONT);
        if (isMemory) {
            for (int i = 0; i < 17; ++i) {
                TableColumn column = table.getColumnModel().getColumn(i);
                if (i == 0) {
                    column.setPreferredWidth(42);
                    column.setMinWidth(30);
                }
                else {
                    column.setPreferredWidth(21);
                    column.setMinWidth(20);
                }
            }
        }
        else for (TableColumn column : Collections.list(table.getColumnModel().getColumns())) {
            column.setMinWidth(20);
        }
        DefaultTableCellRenderer tmp = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value, boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                // adapted from https://stackoverflow.com/questions/16113950/jtable-change-column-font
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                        row, column);
                this.setFont(EmulatorWindow.HEADER_FONT);
                return this;
            }
        };
        table.getColumnModel().getColumn(0).setCellRenderer(tmp);
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
        private final ROM rom;
        private final RAM ram;
        private final boolean modifiable;
        private final boolean isInternal;
        private final NumeralSystem numeralSystem;

        MemoryModel(ROM rom, NumeralSystem numeralSystem, boolean modifiable, boolean isInternal) {
            this.rom = Objects.requireNonNull(rom, "memory must not be null");
            if (this.rom instanceof RAM) this.ram = (RAM) this.rom;
            else this.ram = null;
            this.modifiable = modifiable;
            this.isInternal = isInternal;
            this.numeralSystem = Objects.requireNonNull(numeralSystem);
        }

        @Override
        public int getRowCount() {
            return this.rom.getSize() / 16;
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
                    "" + EmulatorWindow.MEMORY_TABLE_HEADER[index];
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (col == 0) return this.numeralSystem.toString(row, 3) + "0";
            return this.numeralSystem.toString(this.rom.get(row * 16 + col - 1) & 0xFF, 2);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return !EmulatorWindow.this.running && this.modifiable;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (!this.modifiable || EmulatorWindow.this.running || this.ram == null) return;
            final String tmpValue = value.toString();
            this.ram.set(row * 16 + col - 1, (byte) this.numeralSystem.getValue(tmpValue));
        }
    }

    public static void main(String[] iDontNeedNoArgs) throws IOException {
        byte[] code = Files.readAllBytes(Paths.get("/tmp/test.bin"));
        int i = 0;
        RAM codeMemory = new RAM(65536);
        for (byte b : code) codeMemory.set(i++, b);
        SwingUtilities.invokeLater(() -> new EmulatorWindow(new MC8051(codeMemory, new RAM(65536)),
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
                        return pc;
                    }
                }));
    }
}

interface Listing { // TODO REPLACE BY REAL ONE
    String[] getData();
    int getIndexForPCValue(int pc);
}
