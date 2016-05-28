package gui;

import assembler.util.Listing;
import emulator.*;
import emulator.arc8051.MC8051;
import misc.Settings;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
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
    private JButton nextButton, runButton, pauseButton, codeButton, loadButton, storeButton;
    private JTable listingTable;
    private JToolBar toolBar;
    private RegisterTableModel registerTableModel;
    private JSplitPane registerSplit;
    private JPanel registerTableArea;
    private ByteRegister PCH, PCL;

    private final static String[] REGISTER_TABLE_HEADER = {"Register", "Value"};
    private final static String[] LISTING_TABLE_HEADER = {"Line", "Code"};
    private final static String[] MEMORY_TABLE_HEADER = {"Address", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "A", "B", "C", "D", "E", "F"};
    private final static String[] SINGLE_REGISTER_TABLE_VERTICAL_HEADER = {"Bit", "Value"};
    private final static String WINDOW_TITLE = "Emulator";
    private final static Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 11);
    private final static Font HEADER_FONT = new Font(Font.MONOSPACED, Font.BOLD, 11);

    private final static String REGISTER_NUMERAL_SYSTEM_SETTING = "gui.emulator.register-numeral-system";
    private final static String REGISTER_NUMERAL_SYSTEM_SETTING_DEFAULT = NumeralSystem.HEXADECIMAL.name();
    private final static Predicate<String> IS_VALID_NUMERAL_SYSTEM = s -> {
        try {
            NumeralSystem.valueOf(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    };
    private final static String MEMORY_NUMERAL_SYSTEM_SETTING = "gui.emulator.memory-numeral-system";
    private final static String MEMORY_NUMERAL_SYSTEM_SETTING_DEFAULT = NumeralSystem.HEXADECIMAL.name();
    static {
        Settings.INSTANCE.setDefault(REGISTER_NUMERAL_SYSTEM_SETTING, REGISTER_NUMERAL_SYSTEM_SETTING_DEFAULT);
        Settings.INSTANCE.setDefault(MEMORY_NUMERAL_SYSTEM_SETTING, MEMORY_NUMERAL_SYSTEM_SETTING_DEFAULT);
    }

    /**
     * Create a new emulator window.
     * NOTE: The emulator window should be started in a separate thread.
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
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
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

        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (e instanceof Exception)
                    EmulatorWindow.this.reportException("An (uncaught) exception occurred",
                            "An (uncaught) exception occurred", (Exception) e);
                else {
                    System.err.println("An error occurred while executing the emulator:");
                    e.printStackTrace();
                }
            }
        });

        createAndShowGUI();
    }

    void reportException(String title, String message, Exception e) {
        JPanel panel = new JPanel(new BorderLayout());
        if (message != null) {
            JTextArea jta = new JTextArea(message + "\nException (" + e.getClass().getSimpleName() + "): "
                    + e.getMessage());
            jta.setEditable(false);
            JScrollPane jsp = new JScrollPane(jta);
            panel.add(jsp, BorderLayout.CENTER);
        }
        panel.setPreferredSize(new Dimension(600, 200));
        panel.add(new JLabel("If you still want to continue the program, just hit run again."), BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this, panel, title, JOptionPane.WARNING_MESSAGE);
    }

    private void createAndShowGUI() {
        for (Register r : this.emulator.getRegisters()) {
            if (r.getName().equals("PCH")) this.PCH = (ByteRegister) r;
            else if (r.getName().equals("PCL")) this.PCL = (ByteRegister) r;
        }

        if (this.PCH == null || this.PCL == null) throw new IllegalArgumentException("Couldn't get PCH or PCL");

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setOneTouchExpandable(true);
        JPanel registersAndListing = new JPanel(new BorderLayout());
        this.registerTableModel = new RegisterTableModel();
        JScrollPane registers = this.makeTable(this.registerTableModel, false);
        this.registerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        this.registerSplit.setTopComponent(registers);
        this.registerTableArea = new JPanel(new GridLayout(0, 1));
        //registerSplit.setBottomComponent(new JScrollPane(this.registerTableArea));
        this.registerSplit.setDividerLocation(1.0);
        this.registerSplit.setResizeWeight(0.6);
        JTable tmpRegTable = ((JTable)((BorderLayout)((JPanel) registers.getViewport().getView()).getLayout())
                .getLayoutComponent(BorderLayout.CENTER)); // sorry...
        tmpRegTable.addMouseListener(new RegisterMouseListener());

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
            listingSplit.setLeftComponent(this.registerSplit);
            listingSplit.setRightComponent(listing);
            listingSplit.setResizeWeight(0.2);
            listingSplit.setDividerLocation(0.2);
            registersAndListing.add(listingSplit, BorderLayout.CENTER);
        } else registersAndListing.add(this.registerSplit, BorderLayout.CENTER);

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

        this.toolBar = new JToolBar("Emulator Toolbar", JToolBar.HORIZONTAL);
        this.addButtonsToToolBar();
        content.add(toolBar, BorderLayout.NORTH);

        this.add(content);

        this.setTitle(WINDOW_TITLE);
        this.setVisible(true);
    }

    private void addButtonsToToolBar() {
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
        this.loadButton = new JButton("Load State");
        this.loadButton.setMnemonic('l');
        this.loadButton.addActionListener(this::loadState);
        this.storeButton = new JButton("Store State");
        this.storeButton.setMnemonic('s');
        this.storeButton.addActionListener(this::storeState);

        this.toolBar.add(this.nextButton);
        this.toolBar.add(this.runButton);
        this.toolBar.add(this.pauseButton);
        this.toolBar.addSeparator();
        this.toolBar.add(this.codeButton);
        this.toolBar.addSeparator();
        this.toolBar.add(this.storeButton);
        this.toolBar.add(this.loadButton);
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

    private void loadState(ActionEvent e) {
        final JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            final Path path = fileChooser.getSelectedFile().toPath();
            try {
                this.emulator.loadStateFrom(path);
                super.getContentPane().removeAll();
                this.createAndShowGUI();
                super.revalidate();
                super.repaint();
            } catch (IOException e1) {
                this.reportException("An error occurred while loading the stored state", "The stored state at " + path
                        + " could not be loaded.", e1);
            }
        }
    }

    private void storeState(ActionEvent e) {
        final JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            final Path path = fileChooser.getSelectedFile().toPath();
            try {
                this.emulator.saveStateTo(path);
            } catch (IOException e1) {
                this.reportException("An error occurred while storing the current state", "The could not be stored at "
                        + path, e1);
            }
        }
    }

    private void pauseProgram(ActionEvent e) {
        this.running = false;
        this.enableElements();
        super.revalidate();
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
        super.revalidate();
        super.repaint();
    }

    private void disableElements(boolean nextButton, boolean runButton, boolean pauseButton, boolean codeButton) {
        if (nextButton) this.nextButton.setEnabled(false);
        if (runButton) this.runButton.setEnabled(false);
        if (pauseButton) this.pauseButton.setEnabled(false);
        if (codeButton) this.codeButton.setEnabled(false);
        this.loadButton.setEnabled(false);
        this.storeButton.setEnabled(false);
    }

    private void enableElements() {
        this.nextButton.setEnabled(true);
        this.runButton.setEnabled(true);
        this.pauseButton.setEnabled(true);
        this.codeButton.setEnabled(true);
        this.loadButton.setEnabled(true);
        this.storeButton.setEnabled(true);
    }

    private void updateListingTable() {
        if (this.listingTable == null || this.listing == null) return;
        char pc = (char)((this.PCH.getValue() << 8) & 0xFF00 | this.PCL.getValue() & 0xFF);
        Listing.ListingElement element = this.listing.getFromAddress(pc);
        if (element.getLine() >= this.listingTable.getRowCount() || element.getLine() < 0) {
            this.listingTable.setRowSelectionInterval(0, this.listingTable.getRowCount() - 1);
            return;
        }
        this.listingTable.setRowSelectionInterval(element.getLine(), element.getLine());
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

    private List<FlagRegister> shownRegisters = new LinkedList<>();

    private void showRegisterBits(FlagRegister register) {
        if (this.shownRegisters.contains(register)) return;
        if (this.shownRegisters.isEmpty()) this.registerSplit.setBottomComponent(this.registerTableArea);
        this.shownRegisters.add(register);
        JTable tmp = new JTable(new SingleRegisterTableModel(register));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value, boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                // adapted from https://stackoverflow.com/questions/16113950/jtable-change-column-font
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                        row, column);
                if (column == 0 || row == 0) super.setFont(EmulatorWindow.HEADER_FONT);
                return this;
            }
        };
        for (int i = 0; i < tmp.getColumnCount(); ++i) {
            tmp.getColumnModel().getColumn(i).setCellRenderer(renderer);
            tmp.getColumnModel().getColumn(i).setPreferredWidth(12);
        }
        tmp.getColumnModel().getColumn(0).setPreferredWidth(42);
        JPanel tmpPanel = new JPanel(new BorderLayout());
        tmpPanel.add(new JLabel(register.getName()), BorderLayout.NORTH);
        tmpPanel.add(tmp, BorderLayout.CENTER);
        this.registerTableArea.add(tmpPanel);
        super.revalidate();
        super.repaint();
    }

    private class RegisterMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent me) {
            if (me.getClickCount() != 2) return;
            final JTable table = (JTable) me.getSource();
            final int row = table.rowAtPoint(me.getPoint());
            final Register register = EmulatorWindow.this.registerTableModel.getRegisterAt(row);
            if (!(register instanceof FlagRegister)) return;
            EmulatorWindow.this.showRegisterBits((FlagRegister) register);
        }
    }

    private class SingleRegisterTableModel extends AbstractTableModel {

        private final FlagRegister register;

        SingleRegisterTableModel(FlagRegister register) {
            this.register = Objects.requireNonNull(register, "register must not be null");
        }

        @Override
        public int getRowCount() {
            return 2;
        }

        @Override
        public int getColumnCount() {
            return this.register.getFlags().size() + 1;
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (col == 0) return EmulatorWindow.SINGLE_REGISTER_TABLE_VERTICAL_HEADER[row];
            if (row == 0) return this.register.getFlags().get(col - 1).name;
            else return this.register.getBit(col - 1) ? "1" : "0";
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return !EmulatorWindow.this.running && col != 0 && row == 1;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (!(value instanceof String)) return;
            final String tmp = (String) value;
            if ("0".equals(tmp.trim())) this.register.setBit(false, col - 1);
            else if ("1".equals(tmp.trim())) this.register.setBit(true, col - 1);
            EmulatorWindow.this.revalidate();
            EmulatorWindow.this.repaint();
        }
    }

    private class RegisterTableModel extends AbstractTableModel {
        private final java.util.List<Register> registers;
        {
            this.registers = new ArrayList<>(EmulatorWindow.this.emulator.getRegisters());
            this.registers.sort((r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName()));
        }
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

        Register getRegisterAt(int row) {
            return this.registers.get(row);
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
            EmulatorWindow.this.revalidate();
            EmulatorWindow.this.repaint();
        }
    }

    private class ListingModel extends AbstractTableModel {
        private final Listing listing;
        private final List<Listing.ListingElement> data;

        ListingModel(Listing listing) {
            this.listing = Objects.requireNonNull(listing, "listing must not be null");
            this.data = listing.getElements();
        }

        @Override
        public int getRowCount() {
            return this.data.size();
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
            if (col == 0) return this.data.get(row).getLine();
            else return this.data.get(row);
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
            EmulatorWindow.this.revalidate();
            EmulatorWindow.this.repaint();
        }
    }

    public static void main(String[] iDontNeedNoArgs) throws IOException {
        byte[] code = Files.readAllBytes(Paths.get("/tmp/test.bin"));
        int i = 0;
        RAM codeMemory = new RAM(65536);
        for (byte b : code) codeMemory.set(i++, b);
        SwingUtilities.invokeLater(() -> new EmulatorWindow(new MC8051(codeMemory, new RAM(65536)), null));
    }
}
