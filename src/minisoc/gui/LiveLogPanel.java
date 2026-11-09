package minisoc.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Real-time dark terminal console displaying tailed log lines as they are processed.
 */
public class LiveLogPanel extends JPanel {

    private static final int MAX_BUFFER_LINES = 2500;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTextArea txtLogs;
    private final JCheckBox chkAutoScroll;
    private final JTextField txtFilter;
    private final JLabel lblCounter;
    private final JButton btnPause;

    private boolean paused = false;
    private long totalLinesCount = 0;
    private int bufferLineCount = 0;

    public LiveLogPanel() {
        setLayout(new BorderLayout(0, 6));
        setBackground(SocTheme.BG_PANEL);
        setBorder(new EmptyBorder(8, 10, 8, 10));

        // Toolbar
        JPanel pnlToolbar = new JPanel(new BorderLayout(8, 0));
        pnlToolbar.setOpaque(false);

        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlLeft.setOpaque(false);

        JLabel lblTitle = new JLabel("LIVE LOG STREAM");
        lblTitle.setFont(SocTheme.FONT_HEADER);
        lblTitle.setForeground(SocTheme.CYAN_ACCENT);
        pnlLeft.add(lblTitle);

        pnlLeft.add(new JLabel("  Filter:"));
        txtFilter = new JTextField(15);
        txtFilter.setFont(SocTheme.FONT_MONO);
        pnlLeft.add(txtFilter);

        chkAutoScroll = new JCheckBox("Auto-scroll", true);
        chkAutoScroll.setOpaque(false);
        chkAutoScroll.setFont(SocTheme.FONT_SMALL);
        chkAutoScroll.setForeground(SocTheme.TEXT_SECONDARY);
        pnlLeft.add(chkAutoScroll);

        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlRight.setOpaque(false);

        lblCounter = new JLabel("Lines: 0");
        lblCounter.setFont(SocTheme.FONT_SMALL);
        lblCounter.setForeground(SocTheme.TEXT_MUTED);
        pnlRight.add(lblCounter);

        btnPause = SocTheme.createStyledButton("Pause", "secondary");
        btnPause.setFont(SocTheme.FONT_SMALL);
        btnPause.addActionListener(e -> togglePause());
        pnlRight.add(btnPause);

        JButton btnClear = SocTheme.createStyledButton("Clear", "secondary");
        btnClear.setFont(SocTheme.FONT_SMALL);
        btnClear.addActionListener(e -> clear());
        pnlRight.add(btnClear);

        pnlToolbar.add(pnlLeft, BorderLayout.WEST);
        pnlToolbar.add(pnlRight, BorderLayout.EAST);

        // Terminal text area
        txtLogs = new JTextArea();
        txtLogs.setEditable(false);
        txtLogs.setFont(SocTheme.FONT_MONO);
        txtLogs.setBackground(SocTheme.BG_DARK);
        txtLogs.setForeground(new Color(190, 205, 225));
        txtLogs.setCaretColor(SocTheme.CYAN_ACCENT);
        DefaultCaret caret = (DefaultCaret) txtLogs.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scrollPane = new JScrollPane(txtLogs);
        scrollPane.setBorder(new LineBorder(SocTheme.BORDER));
        scrollPane.setVerticalScrollBar(SocTheme.createDarkScrollBar(JScrollBar.VERTICAL));
        scrollPane.setHorizontalScrollBar(SocTheme.createDarkScrollBar(JScrollBar.HORIZONTAL));

        add(pnlToolbar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void appendLogLine(String sourceName, String line) {
        totalLinesCount++;
        if (paused) {
            updateCounter();
            return;
        }

        String filterText = txtFilter.getText().trim();
        if (!filterText.isEmpty() && !line.toLowerCase().contains(filterText.toLowerCase())) {
            updateCounter();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            String time = LocalTime.now().format(TIME_FMT);
            String entry = String.format("[%s] [%-12s] %s\n", time, sourceName, line);
            txtLogs.append(entry);
            bufferLineCount++;

            // Trim buffer if too large
            if (bufferLineCount > MAX_BUFFER_LINES) {
                try {
                    int endPos = txtLogs.getLineEndOffset(bufferLineCount - MAX_BUFFER_LINES);
                    txtLogs.replaceRange("", 0, endPos);
                    bufferLineCount = MAX_BUFFER_LINES;
                } catch (Exception ignored) {}
            }

            if (chkAutoScroll.isSelected()) {
                txtLogs.setCaretPosition(txtLogs.getDocument().getLength());
            }
            updateCounter();
        });
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            btnPause.setText("Resume");
            btnPause.setForeground(SocTheme.SUCCESS);
        } else {
            btnPause.setText("Pause");
            btnPause.setForeground(SocTheme.TEXT_PRIMARY);
        }
    }

    public void clear() {
        txtLogs.setText("");
        bufferLineCount = 0;
        updateCounter();
    }

    public void resetCounter() {
        totalLinesCount = 0;
        bufferLineCount = 0;
        clear();
    }

    private void updateCounter() {
        lblCounter.setText(String.format("Buffered: %,d | Total lines: %,d", bufferLineCount, totalLinesCount));
    }
}

