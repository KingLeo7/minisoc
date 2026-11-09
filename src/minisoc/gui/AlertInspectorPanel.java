package minisoc.gui;

import minisoc.Alert;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Detailed Alert Inspector & SOC Analyst Playbook panel.
 */
public class AlertInspectorPanel extends JPanel {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
            .withZone(ZoneId.systemDefault());

    private final JLabel lblSeverityBadge;
    private final JLabel lblType;
    private final JLabel lblTimestamp;
    private final JLabel lblIp;
    private final JLabel lblRelativeTime;
    private final JTextArea txtDetail;
    private final JTextArea txtPlaybook;
    private final JTextArea txtJson;
    private final JButton btnFilterIp;
    private final JButton btnCopyIp;
    private final JButton btnCopyJson;

    private Alert currentAlert;
    private Consumer<String> onFilterIpRequest;

    public AlertInspectorPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(SocTheme.BG_PANEL);
        setBorder(new EmptyBorder(12, 14, 12, 14));
        setPreferredSize(new Dimension(380, 0));

        // Header Title
        JPanel pnlHeader = new JPanel(new BorderLayout(8, 0));
        pnlHeader.setOpaque(false);

        JLabel lblTitle = new JLabel("ALERT INSPECTOR");
        lblTitle.setFont(SocTheme.FONT_TITLE);
        lblTitle.setForeground(SocTheme.CYAN_ACCENT);

        lblSeverityBadge = new JLabel("NO SELECTION");
        lblSeverityBadge.setFont(SocTheme.FONT_BODY_BOLD);
        lblSeverityBadge.setForeground(SocTheme.TEXT_MUTED);

        pnlHeader.add(lblTitle, BorderLayout.WEST);
        pnlHeader.add(lblSeverityBadge, BorderLayout.EAST);

        // Content Area inside ScrollPane
        JPanel pnlContent = new JPanel();
        pnlContent.setLayout(new BoxLayout(pnlContent, BoxLayout.Y_AXIS));
        pnlContent.setOpaque(false);

        // Alert Type
        lblType = new JLabel("Select an alert from the table to inspect details");
        lblType.setFont(SocTheme.FONT_HEADER);
        lblType.setForeground(SocTheme.TEXT_PRIMARY);
        pnlContent.add(lblType);
        pnlContent.add(Box.createVerticalStrut(10));

        // Metadata grid
        JPanel pnlGrid = new JPanel(new GridLayout(3, 2, 8, 6));
        pnlGrid.setOpaque(false);
        pnlGrid.setBorder(new LineBorder(SocTheme.BORDER_SUBTLE, 1));
        pnlGrid.setBackground(SocTheme.BG_CARD);

        pnlGrid.add(createMetaLabel("Timestamp:"));
        lblTimestamp = createValueLabel("-");
        pnlGrid.add(lblTimestamp);

        pnlGrid.add(createMetaLabel("Time Ago:"));
        lblRelativeTime = createValueLabel("-");
        pnlGrid.add(lblRelativeTime);

        pnlGrid.add(createMetaLabel("Attacker IP:"));
        lblIp = createValueLabel("-");
        lblIp.setFont(SocTheme.FONT_MONO_BOLD);
        pnlGrid.add(lblIp);

        pnlContent.add(pnlGrid);
        pnlContent.add(Box.createVerticalStrut(8));

        // IP quick actions
        JPanel pnlIpActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlIpActions.setOpaque(false);
        btnFilterIp = SocTheme.createStyledButton("Filter by IP", "secondary");
        btnFilterIp.setFont(SocTheme.FONT_SMALL);
        btnFilterIp.setEnabled(false);
        btnFilterIp.addActionListener(e -> {
            if (currentAlert != null && onFilterIpRequest != null) {
                onFilterIpRequest.accept(currentAlert.getSourceIp());
            }
        });

        btnCopyIp = SocTheme.createStyledButton("Copy IP", "secondary");
        btnCopyIp.setFont(SocTheme.FONT_SMALL);
        btnCopyIp.setEnabled(false);
        btnCopyIp.addActionListener(e -> {
            if (currentAlert != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(currentAlert.getSourceIp()), null);
                btnCopyIp.setText("Copied!");
                Timer t = new Timer(1500, evt -> btnCopyIp.setText("Copy IP"));
                t.setRepeats(false);
                t.start();
            }
        });

        pnlIpActions.add(btnFilterIp);
        pnlIpActions.add(btnCopyIp);
        pnlContent.add(pnlIpActions);
        pnlContent.add(Box.createVerticalStrut(12));

        // Section: Payload / Detection Detail
        JLabel lblSecDetail = new JLabel("DETECTION DETAIL / PAYLOAD");
        lblSecDetail.setFont(SocTheme.FONT_SMALL);
        lblSecDetail.setForeground(SocTheme.TEXT_MUTED);
        pnlContent.add(lblSecDetail);
        pnlContent.add(Box.createVerticalStrut(4));

        txtDetail = createReadOnlyTextArea(3);
        pnlContent.add(txtDetail);
        pnlContent.add(Box.createVerticalStrut(12));

        // Section: SOC Playbook & Remediation
        JLabel lblSecPlaybook = new JLabel("SOC PLAYBOOK & REMEDIATION");
        lblSecPlaybook.setFont(SocTheme.FONT_SMALL);
        lblSecPlaybook.setForeground(SocTheme.CYAN_ACCENT);
        pnlContent.add(lblSecPlaybook);
        pnlContent.add(Box.createVerticalStrut(4));

        txtPlaybook = createReadOnlyTextArea(6);
        txtPlaybook.setForeground(SocTheme.TEXT_PRIMARY);
        txtPlaybook.setBackground(new Color(18, 25, 36));
        pnlContent.add(txtPlaybook);
        pnlContent.add(Box.createVerticalStrut(12));

        // Section: Raw JSON
        JPanel pnlJsonHeader = new JPanel(new BorderLayout());
        pnlJsonHeader.setOpaque(false);
        JLabel lblSecJson = new JLabel("RAW JSON EVENT");
        lblSecJson.setFont(SocTheme.FONT_SMALL);
        lblSecJson.setForeground(SocTheme.TEXT_MUTED);
        pnlJsonHeader.add(lblSecJson, BorderLayout.WEST);

        btnCopyJson = SocTheme.createStyledButton("Copy JSON", "secondary");
        btnCopyJson.setFont(SocTheme.FONT_SMALL);
        btnCopyJson.setEnabled(false);
        btnCopyJson.addActionListener(e -> {
            if (currentAlert != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(currentAlert.toJson()), null);
                btnCopyJson.setText("Copied!");
                Timer t = new Timer(1500, evt -> btnCopyJson.setText("Copy JSON"));
                t.setRepeats(false);
                t.start();
            }
        });
        pnlJsonHeader.add(btnCopyJson, BorderLayout.EAST);

        pnlContent.add(pnlJsonHeader);
        pnlContent.add(Box.createVerticalStrut(4));

        txtJson = createReadOnlyTextArea(4);
        txtJson.setFont(SocTheme.FONT_MONO_SMALL);
        txtJson.setForeground(SocTheme.CYAN_ACCENT);
        pnlContent.add(txtJson);

        JScrollPane scrollPane = new JScrollPane(pnlContent);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setVerticalScrollBar(SocTheme.createDarkScrollBar(JScrollBar.VERTICAL));

        add(pnlHeader, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setOnFilterIpRequest(Consumer<String> consumer) {
        this.onFilterIpRequest = consumer;
    }

    public void setAlert(Alert alert) {
        this.currentAlert = alert;
        if (alert == null) {
            clear();
            return;
        }

        lblSeverityBadge.setText("[" + alert.getSeverity() + "]");
        lblSeverityBadge.setForeground(SocTheme.getSeverityColor(alert.getSeverity()));

        lblType.setText(alert.getType());
        lblTimestamp.setText(FORMATTER.format(alert.getTime()));
        lblRelativeTime.setText(calculateRelativeTime(alert.getTime()));
        lblIp.setText(alert.getSourceIp());
        txtDetail.setText(alert.getDetail());
        txtPlaybook.setText(getPlaybookAdvice(alert.getType(), alert.getSeverity()));
        txtJson.setText(alert.toJson());

        btnFilterIp.setEnabled(true);
        btnCopyIp.setEnabled(true);
        btnCopyJson.setEnabled(true);
    }

    public void clear() {
        this.currentAlert = null;
        lblSeverityBadge.setText("NO SELECTION");
        lblSeverityBadge.setForeground(SocTheme.TEXT_MUTED);
        lblType.setText("Select an alert from the table to inspect details");
        lblTimestamp.setText("-");
        lblRelativeTime.setText("-");
        lblIp.setText("-");
        txtDetail.setText("");
        txtPlaybook.setText("");
        txtJson.setText("");
        btnFilterIp.setEnabled(false);
        btnCopyIp.setEnabled(false);
        btnCopyJson.setEnabled(false);
    }

    private JLabel createMetaLabel(String text) {
        JLabel l = new JLabel("  " + text);
        l.setFont(SocTheme.FONT_SMALL);
        l.setForeground(SocTheme.TEXT_MUTED);
        return l;
    }

    private JLabel createValueLabel(String text) {
        JLabel l = new JLabel(text + " ");
        l.setFont(SocTheme.FONT_BODY);
        l.setForeground(SocTheme.TEXT_PRIMARY);
        return l;
    }

    private JTextArea createReadOnlyTextArea(int rows) {
        JTextArea t = new JTextArea(rows, 20);
        t.setEditable(false);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setFont(SocTheme.FONT_MONO);
        t.setBackground(SocTheme.BG_INPUT);
        t.setForeground(SocTheme.TEXT_PRIMARY);
        t.setBorder(new LineBorder(SocTheme.BORDER_SUBTLE, 1));
        return t;
    }

    private String calculateRelativeTime(Instant time) {
        if (time == null) return "-";
        Duration d = Duration.between(time, Instant.now());
        long seconds = d.getSeconds();
        if (seconds < 5) return "Just now";
        if (seconds < 60) return seconds + "s ago";
        if (seconds < 3600) return (seconds / 60) + "m ago";
        return (seconds / 3600) + "h ago";
    }

    private String getPlaybookAdvice(String type, String severity) {
        if (type == null) return "Investigate source IP and relevant service logs.";
        switch (type.toLowerCase()) {
            case "ssh_login_after_brute_force":
                return "🚨 CRITICAL INCIDENT RESPONSE:\n"
                        + "1. Host is potentially compromised! Isolate sensor/server immediately.\n"
                        + "2. Kill active sessions: 'pkill -u <user>' or inspect active pts via 'w'.\n"
                        + "3. Force password reset and revoke SSH authorized_keys.\n"
                        + "4. Inspect /var/log/auth.log, audit.log and bash_history for lateral movement.";

            case "ssh_brute_force":
                return "⚠️ HIGH RECONNAISSANCE / ATTACK:\n"
                        + "1. Block source IP at perimeter firewall or iptables: 'iptables -A INPUT -s <IP> -j DROP'.\n"
                        + "2. Verify Fail2ban or SSH rate limiting is active.\n"
                        + "3. Enforce SSH public-key authentication only ('PasswordAuthentication no').";

            case "sql_injection":
                return "⚠️ APPLICATION ATTACK:\n"
                        + "1. Block source IP or request signature at reverse proxy / WAF.\n"
                        + "2. Verify database error handling doesn't leak stack traces.\n"
                        + "3. Ensure all backend queries use prepared statements / parameterized queries.";

            case "port_scan":
                return "⚠️ ACTIVE RECONNAISSANCE:\n"
                        + "1. Attacker is mapping open ports across your perimeter.\n"
                        + "2. Block or throttle source IP at firewall/IPS.\n"
                        + "3. Audit exposed service ports to ensure unnecessary ports are closed.";

            case "arp_spoofing":
                return "⚠️ LAN LAYER 2 ATTACK (MITM):\n"
                        + "1. Potential ARP cache poisoning on the local broadcast domain.\n"
                        + "2. Check switch MAC address table and enable Dynamic ARP Inspection (DAI).\n"
                        + "3. Isolate the offending MAC address port immediately.";

            case "xss_attempt":
                return "🟡 WEB EXPLOITATION ATTEMPT:\n"
                        + "1. Cross-Site Scripting vector detected in query string.\n"
                        + "2. Ensure context-aware HTML encoding on dynamic outputs.\n"
                        + "3. Implement strict Content-Security-Policy (CSP) headers.";

            case "path_traversal":
                return "🟡 DIRECTORY TRAVERSAL:\n"
                        + "1. Path traversal payload ('../' or system file reference) detected.\n"
                        + "2. Restrict web user file system permissions (chroot/jail).\n"
                        + "3. Sanitize and validate file path parameters against allowlists.";

            case "scanner_tool_detected":
                return "🟡 AUTOMATED SCANNER DETECTED:\n"
                        + "1. Automated vulnerability scanner (sqlmap/nikto/gobuster) detected.\n"
                        + "2. Block scanner user-agent string at ingress gateway.\n"
                        + "3. Enable CAPTCHA or rate limiting on repeated requests.";

            case "dns_exfiltration":
            case "dns_tunneling_burst":
                return "⚠️ COVERT CHANNEL / DNS EXFILTRATION:\n"
                        + "1. High entropy or rapid burst of DNS queries detected; possible data exfiltration via DNS tunneling (iodine, dnscat2).\n"
                        + "2. Inspect internal endpoint initiating the queries.\n"
                        + "3. Block malicious apex domain at internal DNS resolvers and sinkhole traffic.\n"
                        + "4. Enforce DNS query logging and rate limits at recursive resolvers.";

            default:
                return "1. Verify host and service logs for unauthorized activity.\n"
                        + "2. Correlate with firewall and IDS event timelines.\n"
                        + "3. Block malicious IP if verified untrusted.";
        }
    }
}

