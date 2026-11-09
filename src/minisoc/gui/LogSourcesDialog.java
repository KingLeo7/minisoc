package minisoc.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog to configure monitored log files, output destination, and alert cooldown.
 */
public class LogSourcesDialog extends JDialog {

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField txtOutFile;
    private final JSpinner spnCooldown;
    private final List<Path> resultPaths = new ArrayList<>();

    private boolean confirmed = false;

    public LogSourcesDialog(Frame owner, List<Path> currentPaths, String currentOutFile, long currentCooldownMs) {
        super(owner, "Configure Log Sources & Sensor Settings", true);
        setSize(650, 420);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(SocTheme.BG_PANEL);
        setLayout(new BorderLayout(0, 10));

        // Header
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(new EmptyBorder(12, 16, 6, 16));

        JLabel lblTitle = new JLabel("MONITORED LOG FILES & OUTPUT");
        lblTitle.setFont(SocTheme.FONT_HEADER);
        lblTitle.setForeground(SocTheme.CYAN_ACCENT);
        pnlHeader.add(lblTitle, BorderLayout.WEST);

        add(pnlHeader, BorderLayout.NORTH);

        // Center: Log Files Table
        String[] cols = {"Log File Path", "Status", "Size"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setFont(SocTheme.FONT_MONO);
        table.getColumnModel().getColumn(0).setPreferredWidth(380);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);

        for (Path p : currentPaths) {
            addPathToTable(p);
        }

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(new LineBorder(SocTheme.BORDER));
        scrollTable.getViewport().setBackground(SocTheme.BG_PANEL);

        // Left button panel
        JPanel pnlTableActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pnlTableActions.setOpaque(false);

        JButton btnAdd = SocTheme.createStyledButton("+ Add Log File...", "secondary");
        btnAdd.addActionListener(e -> chooseFile());

        JButton btnRemove = SocTheme.createStyledButton("- Remove Selected", "secondary");
        btnRemove.addActionListener(e -> removeSelected());

        JButton btnReset = SocTheme.createStyledButton("Reset to Lab Samples", "secondary");
        btnReset.addActionListener(e -> resetToDefaultSamples());

        pnlTableActions.add(btnAdd);
        pnlTableActions.add(btnRemove);
        pnlTableActions.add(btnReset);

        JPanel pnlCenter = new JPanel(new BorderLayout(0, 6));
        pnlCenter.setOpaque(false);
        pnlCenter.setBorder(new EmptyBorder(0, 16, 0, 16));
        pnlCenter.add(scrollTable, BorderLayout.CENTER);
        pnlCenter.add(pnlTableActions, BorderLayout.SOUTH);

        add(pnlCenter, BorderLayout.CENTER);

        // Bottom Form: Output file & Cooldown
        JPanel pnlBottom = new JPanel(new BorderLayout());
        pnlBottom.setOpaque(false);
        pnlBottom.setBorder(new EmptyBorder(8, 16, 14, 16));

        JPanel pnlSettings = new JPanel(new GridLayout(2, 2, 10, 8));
        pnlSettings.setOpaque(false);
        pnlSettings.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel lblOut = new JLabel("Alerts output file (JSON Lines):");
        lblOut.setFont(SocTheme.FONT_BODY);
        lblOut.setForeground(SocTheme.TEXT_SECONDARY);
        txtOutFile = new JTextField(currentOutFile);
        txtOutFile.setFont(SocTheme.FONT_MONO);

        JLabel lblCool = new JLabel("Alert Cooldown (seconds):");
        lblCool.setFont(SocTheme.FONT_BODY);
        lblCool.setForeground(SocTheme.TEXT_SECONDARY);
        spnCooldown = new JSpinner(new SpinnerNumberModel((int) (currentCooldownMs / 1000), 1, 3600, 5));
        spnCooldown.setFont(SocTheme.FONT_BODY);

        pnlSettings.add(lblOut);
        pnlSettings.add(txtOutFile);
        pnlSettings.add(lblCool);
        pnlSettings.add(spnCooldown);

        JPanel pnlDialogButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlDialogButtons.setOpaque(false);

        JButton btnCancel = SocTheme.createStyledButton("Cancel", "secondary");
        btnCancel.addActionListener(e -> dispose());

        JButton btnApply = SocTheme.createStyledButton("Apply Settings", "primary");
        btnApply.addActionListener(e -> applyAndClose());

        pnlDialogButtons.add(btnCancel);
        pnlDialogButtons.add(btnApply);

        pnlBottom.add(pnlSettings, BorderLayout.CENTER);
        pnlBottom.add(pnlDialogButtons, BorderLayout.SOUTH);

        add(pnlBottom, BorderLayout.SOUTH);
    }

    private void addPathToTable(Path p) {
        boolean exists = Files.exists(p);
        String status = exists ? "Ready" : "Missing";
        String size = "-";
        if (exists) {
            try {
                long bytes = Files.size(p);
                size = bytes < 1024 ? bytes + " B" : (bytes / 1024) + " KB";
            } catch (Exception ignored) {}
        }
        tableModel.addRow(new Object[]{p.toString(), status, size});
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setDialogTitle("Select Log File to Monitor");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            addPathToTable(f.toPath());
        }
    }

    private void removeSelected() {
        int[] rows = table.getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) {
            tableModel.removeRow(rows[i]);
        }
    }

    private void resetToDefaultSamples() {
        tableModel.setRowCount(0);
        addPathToTable(Paths.get("samples/auth.log"));
        addPathToTable(Paths.get("samples/access.log"));
        addPathToTable(Paths.get("samples/eve.json"));
        addPathToTable(Paths.get("samples/arp.log"));
    }

    private void applyAndClose() {
        resultPaths.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String pathStr = (String) tableModel.getValueAt(i, 0);
            resultPaths.add(Paths.get(pathStr));
        }
        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public List<Path> getResultPaths() {
        return resultPaths;
    }

    public String getOutFile() {
        return txtOutFile.getText().trim();
    }

    public long getCooldownMs() {
        return ((Integer) spnCooldown.getValue()) * 1000L;
    }
}

