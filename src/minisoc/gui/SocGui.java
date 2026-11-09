package minisoc.gui;

import minisoc.Alert;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main MiniSOC Security Operations Center Dashboard GUI application.
 */
public class SocGui extends JFrame {

    private final SocEngine engine;
    private final AlertsTableModel tableModel;
    private final JTable alertsTable;
    private final TableRowSorter<AlertsTableModel> sorter;

    // Components
    private final MetricCard cardTotalAlerts;
    private final MetricCard cardCritical;
    private final MetricCard cardAttackers;
    private final MetricCard cardLines;

    private final JLabel lblStatusPill;
    private final JLabel lblBottomStatus;
    private final JLabel lblBottomOut;

    private final JButton btnStartLive;
    private final JButton btnReplay;
    private final JButton btnStop;
    private final JButton btnLogSources;
    private final JButton btnExport;
    private final JButton btnClear;

    private final JTextField txtSearch;
    private final JPanel pnlSeverityFilterPills;

    private final AlertInspectorPanel inspectorPanel;
    private final ChartsPanel chartsPanel;
    private final LiveLogPanel liveLogPanel;
    private final DetectorsPanel detectorsPanel;

    public SocGui() {
        super("MiniSOC - Security Operations Center Dashboard");

        // Apply dark styling
        SocTheme.applySystemTheme();

        this.engine = new SocEngine();
        this.tableModel = new AlertsTableModel();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 840);
        setMinimumSize(new Dimension(1080, 680));
        setLocationRelativeTo(null);
        setIconImage(createAppIcon());

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(SocTheme.BG_DARK);
        root.setBorder(new EmptyBorder(10, 14, 10, 14));
        setContentPane(root);

        // 1. Top Header & Control Toolbar
        JPanel pnlHeader = new JPanel(new BorderLayout(14, 0));
        pnlHeader.setOpaque(false);

        // Logo & Title
        JPanel pnlBrand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlBrand.setOpaque(false);

        JLabel lblLogo = new JLabel(new ImageIcon(createBrandLogo(28, 28)));
        JLabel lblTitle = new JLabel("MiniSOC");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(SocTheme.CYAN_ACCENT);

        JLabel lblSub = new JLabel("SECURITY OPERATIONS CENTER");
        lblSub.setFont(SocTheme.FONT_SMALL);
        lblSub.setForeground(SocTheme.TEXT_MUTED);

        JPanel pnlTitleBox = new JPanel();
        pnlTitleBox.setLayout(new BoxLayout(pnlTitleBox, BoxLayout.Y_AXIS));
        pnlTitleBox.setOpaque(false);
        pnlTitleBox.add(lblTitle);
        pnlTitleBox.add(lblSub);

        pnlBrand.add(lblLogo);
        pnlBrand.add(pnlTitleBox);

        // Center: Engine State Indicator Pill
        JPanel pnlCenterStatus = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        pnlCenterStatus.setOpaque(false);
        lblStatusPill = new JLabel(" SENSOR IDLE ");
        lblStatusPill.setFont(SocTheme.FONT_BODY_BOLD);
        lblStatusPill.setOpaque(true);
        lblStatusPill.setBackground(SocTheme.BG_CARD);
        lblStatusPill.setForeground(SocTheme.TEXT_SECONDARY);
        lblStatusPill.setBorder(new LineBorder(SocTheme.BORDER, 1, true));
        pnlCenterStatus.add(lblStatusPill);

        // Right Actions Toolbar
        JPanel pnlToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlToolbar.setOpaque(false);

        btnStartLive = SocTheme.createStyledButton("▶ Start Live Sensor", "success");
        btnStartLive.addActionListener(e -> startLiveMonitoring());

        btnReplay = SocTheme.createStyledButton("⚡ Replay Lab Samples", "primary");
        btnReplay.setToolTipText("Run 1-shot replay against the 4 sample logs");
        btnReplay.addActionListener(e -> startReplay());

        btnStop = SocTheme.createStyledButton("⏹ Stop", "danger");
        btnStop.setEnabled(false);
        btnStop.addActionListener(e -> stopEngine());

        JButton btnSimulate = SocTheme.createStyledButton("🎯 Attack Simulator", "warning");
        btnSimulate.setToolTipText("Inject simulated live attacks to test detectors");
        btnSimulate.addActionListener(e -> new AttackSimulatorDialog(this).setVisible(true));

        btnLogSources = SocTheme.createStyledButton("📂 Log Sources", "secondary");
        btnLogSources.addActionListener(e -> openLogSourcesDialog());

        btnExport = SocTheme.createStyledButton("💾 Export...", "secondary");
        btnExport.addActionListener(e -> openExportDialog());

        btnClear = SocTheme.createStyledButton("🗑 Clear", "secondary");
        btnClear.addActionListener(e -> clearAllAlerts());

        JButton btnReload = SocTheme.createStyledButton("🔄 Reload", "secondary");
        btnReload.setToolTipText("Reload alerts from alerts.json file");
        btnReload.addActionListener(e -> reloadFromAlertsJson());

        pnlToolbar.add(btnStartLive);
        pnlToolbar.add(btnReplay);
        pnlToolbar.add(btnStop);
        pnlToolbar.add(btnSimulate);
        pnlToolbar.add(btnLogSources);
        pnlToolbar.add(btnExport);
        pnlToolbar.add(btnClear);
        pnlToolbar.add(btnReload);

        pnlHeader.add(pnlBrand, BorderLayout.WEST);
        pnlHeader.add(pnlCenterStatus, BorderLayout.CENTER);
        pnlHeader.add(pnlToolbar, BorderLayout.EAST);

        // 2. Metric KPI Cards Row
        JPanel pnlCards = new JPanel(new GridLayout(1, 4, 12, 0));
        pnlCards.setOpaque(false);
        pnlCards.setBorder(new EmptyBorder(4, 0, 4, 0));

        cardTotalAlerts = new MetricCard("Total Alerts", "0", "0 in cooldown", SocTheme.CYAN_ACCENT);
        cardCritical = new MetricCard("Critical Threats", "0", "Require immediate action", SocTheme.CRITICAL);
        cardAttackers = new MetricCard("Attacker IPs", "0", "Unique source addresses", SocTheme.HIGH);
        cardLines = new MetricCard("Log Events Parsed", "0", "Across all log tailers", SocTheme.SUCCESS);

        pnlCards.add(cardTotalAlerts);
        pnlCards.add(cardCritical);
        pnlCards.add(cardAttackers);
        pnlCards.add(cardLines);

        // Combine Header + Cards into North Area
        JPanel pnlNorth = new JPanel(new BorderLayout(0, 10));
        pnlNorth.setOpaque(false);
        pnlNorth.add(pnlHeader, BorderLayout.NORTH);
        pnlNorth.add(pnlCards, BorderLayout.SOUTH);

        root.add(pnlNorth, BorderLayout.NORTH);

        // 3. Central Area: Table + Filter Bar + Alert Inspector
        JPanel pnlTableContainer = new JPanel(new BorderLayout(0, 8));
        pnlTableContainer.setOpaque(false);

        // Search & Filter Bar
        JPanel pnlFilterBar = new JPanel(new BorderLayout(10, 0));
        pnlFilterBar.setOpaque(false);

        // Severity Pills
        pnlSeverityFilterPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlSeverityFilterPills.setOpaque(false);
        pnlSeverityFilterPills.add(createFilterButton("ALL", SocTheme.CYAN_ACCENT));
        pnlSeverityFilterPills.add(createFilterButton("CRITICAL", SocTheme.CRITICAL));
        pnlSeverityFilterPills.add(createFilterButton("HIGH", SocTheme.HIGH));
        pnlSeverityFilterPills.add(createFilterButton("MEDIUM", SocTheme.MEDIUM));

        // Search text field
        JPanel pnlSearch = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlSearch.setOpaque(false);
        JLabel lblSearch = new JLabel("🔍 Search:");
        lblSearch.setFont(SocTheme.FONT_BODY);
        lblSearch.setForeground(SocTheme.TEXT_SECONDARY);

        txtSearch = new JTextField(20);
        txtSearch.setFont(SocTheme.FONT_BODY);
        txtSearch.putClientProperty("JTextField.placeholderText", "Search IP, attack type, payload...");
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSearch(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSearch(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSearch(); }
            private void updateSearch() {
                tableModel.setSearchFilter(txtSearch.getText());
                refreshStats();
            }
        });

        JButton btnResetFilter = SocTheme.createStyledButton("Reset", "secondary");
        btnResetFilter.setFont(SocTheme.FONT_SMALL);
        btnResetFilter.addActionListener(e -> {
            txtSearch.setText("");
            tableModel.resetFilters();
            highlightFilterButton("ALL");
            refreshStats();
        });

        pnlSearch.add(lblSearch);
        pnlSearch.add(txtSearch);
        pnlSearch.add(btnResetFilter);

        pnlFilterBar.add(pnlSeverityFilterPills, BorderLayout.WEST);
        pnlFilterBar.add(pnlSearch, BorderLayout.EAST);
        pnlTableContainer.add(pnlFilterBar, BorderLayout.NORTH);

        // Alerts Table
        alertsTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        alertsTable.setRowSorter(sorter);
        alertsTable.setRowHeight(30);
        alertsTable.setFont(SocTheme.FONT_BODY);
        alertsTable.setShowGrid(false);
        alertsTable.setIntercellSpacing(new Dimension(0, 0));
        alertsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Configure column widths & renderers
        alertsTable.getColumnModel().getColumn(0).setPreferredWidth(140); // Time
        alertsTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Severity
        alertsTable.getColumnModel().getColumn(1).setCellRenderer(new SeverityCellRenderer());
        alertsTable.getColumnModel().getColumn(2).setPreferredWidth(160); // Type
        alertsTable.getColumnModel().getColumn(3).setPreferredWidth(130); // IP
        alertsTable.getColumnModel().getColumn(4).setPreferredWidth(450); // Detail

        inspectorPanel = new AlertInspectorPanel();
        inspectorPanel.setOnFilterIpRequest(ip -> {
            txtSearch.setText(ip);
            tableModel.setSearchFilter(ip);
            refreshStats();
        });

        alertsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = alertsTable.getSelectedRow();
                if (selectedRow != -1) {
                    int modelRow = alertsTable.convertRowIndexToModel(selectedRow);
                    Alert alert = tableModel.getAlertAt(modelRow);
                    inspectorPanel.setAlert(alert);
                } else {
                    inspectorPanel.clear();
                }
            }
        });

        JScrollPane scrollTable = new JScrollPane(alertsTable);
        scrollTable.setBorder(new LineBorder(SocTheme.BORDER));
        scrollTable.getViewport().setBackground(SocTheme.BG_PANEL);
        scrollTable.setVerticalScrollBar(SocTheme.createDarkScrollBar(JScrollBar.VERTICAL));
        pnlTableContainer.add(scrollTable, BorderLayout.CENTER);

        // Split table & inspector horizontally
        JSplitPane splitCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlTableContainer, inspectorPanel);
        splitCenter.setResizeWeight(0.72);
        splitCenter.setDividerSize(6);
        splitCenter.setBorder(null);
        splitCenter.setBackground(SocTheme.BG_DARK);

        // 4. Bottom Tabs: Analytics, Live Log Console, Detectors
        JTabbedPane tabbedBottom = new JTabbedPane();
        tabbedBottom.setFont(SocTheme.FONT_HEADER);
        tabbedBottom.setBackground(SocTheme.BG_PANEL);
        tabbedBottom.setBorder(new LineBorder(SocTheme.BORDER));

        chartsPanel = new ChartsPanel(tableModel);
        chartsPanel.setOnIpSelect(ip -> {
            txtSearch.setText(ip);
            tableModel.setSearchFilter(ip);
            refreshStats();
        });

        liveLogPanel = new LiveLogPanel();
        detectorsPanel = new DetectorsPanel(engine);

        tabbedBottom.addTab("  📊 Threat Analytics & Attackers  ", chartsPanel);
        tabbedBottom.addTab("  📜 Live Log Stream  ", liveLogPanel);
        tabbedBottom.addTab("  🛡️ Detection Rules & Sensors  ", detectorsPanel);

        // Split Center and Bottom Tabs vertically
        JSplitPane splitVertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitCenter, tabbedBottom);
        splitVertical.setResizeWeight(0.65);
        splitVertical.setDividerSize(6);
        splitVertical.setBorder(null);
        splitVertical.setBackground(SocTheme.BG_DARK);

        root.add(splitVertical, BorderLayout.CENTER);

        // 5. Bottom Status Bar
        JPanel pnlStatusBar = new JPanel(new BorderLayout(10, 0));
        pnlStatusBar.setOpaque(false);
        pnlStatusBar.setBorder(new EmptyBorder(4, 4, 0, 4));

        lblBottomStatus = new JLabel("Ready • 4 detectors registered • 4 sample log sources configured");
        lblBottomStatus.setFont(SocTheme.FONT_SMALL);
        lblBottomStatus.setForeground(SocTheme.TEXT_SECONDARY);

        lblBottomOut = new JLabel("Alerts Sink: alerts.json");
        lblBottomOut.setFont(SocTheme.FONT_MONO_SMALL);
        lblBottomOut.setForeground(SocTheme.CYAN_ACCENT);

        pnlStatusBar.add(lblBottomStatus, BorderLayout.WEST);
        pnlStatusBar.add(lblBottomOut, BorderLayout.EAST);

        root.add(pnlStatusBar, BorderLayout.SOUTH);

        // Wire engine listeners
        wireEngineListeners();

        // Start real-time external alerts.json watcher & load existing alerts
        startAlertsFileWatcher();

        // Shutdown hook
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                engine.stop();
            }
        });
    }

    private void wireEngineListeners() {
        engine.addAlertListener(alert -> SwingUtilities.invokeLater(() -> {
            tableModel.addAlert(alert);
            refreshStats();
            detectorsPanel.updateAlertCounts(tableModel.getTypeCounts());
        }));

        engine.addLineListener((source, line) -> {
            liveLogPanel.appendLogLine(source, line);
            SwingUtilities.invokeLater(() -> {
                cardLines.setValue(engine.getTotalLinesParsed());
            });
        });

        engine.addStateListener((state, message) -> SwingUtilities.invokeLater(() -> {
            updateEngineStateUi(state, message);
        }));
    }

    private void updateEngineStateUi(SocEngine.State state, String message) {
        lblBottomStatus.setText(message);
        switch (state) {
            case MONITORING:
                lblStatusPill.setText(" ● MONITORING LIVE ");
                lblStatusPill.setForeground(SocTheme.SUCCESS);
                lblStatusPill.setBorder(new LineBorder(SocTheme.SUCCESS, 1, true));
                btnStartLive.setEnabled(false);
                btnReplay.setEnabled(false);
                btnStop.setEnabled(true);
                break;
            case REPLAYING:
                lblStatusPill.setText(" ⚡ REPLAYING SAMPLES ");
                lblStatusPill.setForeground(SocTheme.HIGH);
                lblStatusPill.setBorder(new LineBorder(SocTheme.HIGH, 1, true));
                btnStartLive.setEnabled(false);
                btnReplay.setEnabled(false);
                btnStop.setEnabled(true);
                break;
            case STOPPED:
            case IDLE:
            default:
                lblStatusPill.setText(" ○ SENSOR IDLE ");
                lblStatusPill.setForeground(SocTheme.TEXT_SECONDARY);
                lblStatusPill.setBorder(new LineBorder(SocTheme.BORDER, 1, true));
                btnStartLive.setEnabled(true);
                btnReplay.setEnabled(true);
                btnStop.setEnabled(false);
                break;
        }
    }

    private void startLiveMonitoring() {
        engine.startLive();
    }

    private void startReplay() {
        engine.startReplay(() -> SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "Replay finished! Processed " + engine.getTotalLinesParsed()
                            + " lines.\nTotal alerts generated: " + tableModel.getTotalCount(),
                    "Replay Completed", JOptionPane.INFORMATION_MESSAGE);
        }));
    }

    private void stopEngine() {
        engine.stop();
    }

    private void clearAllAlerts() {
        int opt = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear all alerts and reset counters?",
                "Confirm Clear", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            engine.clearAlerts();
            tableModel.clear();
            inspectorPanel.clear();
            liveLogPanel.resetCounter();
            refreshStats();
            detectorsPanel.updateAlertCounts(tableModel.getTypeCounts());
        }
    }

    private void openLogSourcesDialog() {
        LogSourcesDialog dlg = new LogSourcesDialog(this, engine.getLogPaths(),
                engine.getOutFile(), engine.getCooldownMs());
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            engine.setLogPaths(dlg.getResultPaths());
            engine.setOutFile(dlg.getOutFile());
            engine.setCooldownMs(dlg.getCooldownMs());
            lblBottomOut.setText("Alerts Sink: " + dlg.getOutFile());
            lblBottomStatus.setText("Configured " + dlg.getResultPaths().size() + " log sources");
        }
    }

    private void openExportDialog() {
        if (tableModel.getTotalCount() == 0) {
            JOptionPane.showMessageDialog(this, "No alerts to export.", "Export Alerts", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setDialogTitle("Export Alerts");
        chooser.setSelectedFile(new File("exported_alerts.json"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                if (f.getName().toLowerCase().endsWith(".csv")) {
                    engine.exportCsv(f.toPath(), tableModel.getAllAlerts());
                } else {
                    engine.exportJson(f.toPath(), tableModel.getAllAlerts());
                }
                JOptionPane.showMessageDialog(this, "Alerts successfully exported to " + f.getName(),
                        "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error exporting alerts: " + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private long lastAlertsFilePos = 0;
    private Timer alertsWatcherTimer;

    private void startAlertsFileWatcher() {
        reloadFromAlertsJson();

        alertsWatcherTimer = new Timer(1000, e -> pollExternalAlerts());
        alertsWatcherTimer.start();
    }

    public void reloadFromAlertsJson() {
        Path p = Paths.get(engine.getOutFile());
        if (Files.exists(p)) {
            List<Alert> loaded = engine.loadAlertsFromFile(p);
            tableModel.setAlerts(loaded);
            refreshStats();
            detectorsPanel.updateAlertCounts(tableModel.getTypeCounts());
            lblBottomStatus.setText("Loaded " + loaded.size() + " alerts from " + p.getFileName());
            try {
                lastAlertsFilePos = Files.size(p);
            } catch (Exception ignored) {}
        }
    }

    private void pollExternalAlerts() {
        Path p = Paths.get(engine.getOutFile());
        if (!Files.exists(p)) return;

        try {
            long currentSize = Files.size(p);
            if (currentSize < lastAlertsFilePos) {
                // File truncated or recreated
                lastAlertsFilePos = 0;
            }
            if (currentSize > lastAlertsFilePos) {
                try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(p.toFile(), "r")) {
                    raf.seek(lastAlertsFilePos);
                    String line;
                    boolean added = false;
                    while ((line = raf.readLine()) != null) {
                        if (!line.isBlank()) {
                            String utf8 = new String(line.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),
                                    java.nio.charset.StandardCharsets.UTF_8);
                            Alert a = Alert.fromJson(utf8);
                            if (a != null && !isDuplicateAlert(a)) {
                                tableModel.addAlert(a);
                                added = true;
                            }
                        }
                    }
                    lastAlertsFilePos = raf.getFilePointer();
                    if (added) {
                        refreshStats();
                        detectorsPanel.updateAlertCounts(tableModel.getTypeCounts());
                        lblBottomStatus.setText("Live synced new alerts from " + p.getFileName());
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private boolean isDuplicateAlert(Alert alert) {
        for (Alert existing : tableModel.getAllAlerts()) {
            if (existing.getType().equals(alert.getType())
                    && existing.getSourceIp().equals(alert.getSourceIp())
                    && existing.getDetail().equals(alert.getDetail())
                    && Math.abs(java.time.Duration.between(existing.getTime(), alert.getTime()).toMillis()) < 2000) {
                return true;
            }
        }
        return false;
    }

    private void loadExistingAlertsIfPresent() {
        reloadFromAlertsJson();
    }

    private void refreshStats() {
        int total = tableModel.getTotalCount();
        int crit = tableModel.getCountBySeverity("CRITICAL");
        int uniqueIps = tableModel.getUniqueIpCount();

        cardTotalAlerts.setValue(total);
        cardCritical.setValue(crit);
        cardAttackers.setValue(uniqueIps);
        cardLines.setValue(engine.getTotalLinesParsed());

        chartsPanel.refreshData();
    }

    private JButton createFilterButton(String severity, Color color) {
        JButton btn = new JButton(severity);
        btn.setFont(SocTheme.FONT_SMALL);
        btn.setFocusPainted(false);
        btn.setBackground(SocTheme.BG_CARD);
        btn.setForeground(SocTheme.TEXT_SECONDARY);
        btn.setBorder(new CompoundBorder(new LineBorder(SocTheme.BORDER), new EmptyBorder(4, 10, 4, 10)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            tableModel.setSeverityFilter(severity);
            highlightFilterButton(severity);
            refreshStats();
        });
        return btn;
    }

    private void highlightFilterButton(String activeSeverity) {
        for (Component c : pnlSeverityFilterPills.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                if (b.getText().equalsIgnoreCase(activeSeverity)) {
                    b.setBackground(new Color(25, 45, 65));
                    b.setForeground(SocTheme.CYAN_ACCENT);
                    b.setBorder(new CompoundBorder(new LineBorder(SocTheme.CYAN_ACCENT), new EmptyBorder(4, 10, 4, 10)));
                } else {
                    b.setBackground(SocTheme.BG_CARD);
                    b.setForeground(SocTheme.TEXT_SECONDARY);
                    b.setBorder(new CompoundBorder(new LineBorder(SocTheme.BORDER), new EmptyBorder(4, 10, 4, 10)));
                }
            }
        }
    }

    private Image createAppIcon() {
        return createBrandLogo(32, 32);
    }

    private BufferedImage createBrandLogo(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Cyber shield polygon
        int[] xs = {width / 2, width - 2, width - 4, width / 2, 4, 2};
        int[] ys = {2, 6, height - 10, height - 2, height - 10, 6};

        g2.setColor(new Color(15, 30, 50));
        g2.fillPolygon(xs, ys, xs.length);

        g2.setColor(SocTheme.CYAN_ACCENT);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawPolygon(xs, ys, xs.length);

        // Core glowing dot
        g2.setColor(SocTheme.SUCCESS);
        g2.fillOval(width / 2 - 3, height / 2 - 3, 6, 6);

        g2.dispose();
        return img;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SocGui gui = new SocGui();
            gui.setVisible(true);
        });
    }
}
