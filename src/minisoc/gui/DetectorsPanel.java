package minisoc.gui;

import minisoc.Detector;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interactive panel displaying the status, rules, thresholds, and toggle switches
 * for each MiniSOC intrusion detector module.
 */
public class DetectorsPanel extends JPanel {

    private final SocEngine engine;
    private final Map<String, JCheckBox> toggleMap = new HashMap<>();
    private final Map<String, JLabel> countBadges = new HashMap<>();

    public DetectorsPanel(SocEngine engine) {
        this.engine = engine;
        setLayout(new BorderLayout(0, 10));
        setBackground(SocTheme.BG_PANEL);
        setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        JLabel lblTitle = new JLabel("INTRUSION DETECTION RULES & SENSORS");
        lblTitle.setFont(SocTheme.FONT_HEADER);
        lblTitle.setForeground(SocTheme.CYAN_ACCENT);
        pnlHeader.add(lblTitle, BorderLayout.WEST);

        JLabel lblSub = new JLabel("Enable/disable rules dynamically • Zero external dependencies");
        lblSub.setFont(SocTheme.FONT_SMALL);
        lblSub.setForeground(SocTheme.TEXT_MUTED);
        pnlHeader.add(lblSub, BorderLayout.EAST);

        add(pnlHeader, BorderLayout.NORTH);

        JPanel pnlGrid = new JPanel(new GridLayout(0, 3, 10, 10));
        pnlGrid.setOpaque(false);

        pnlGrid.add(createDetectorCard("ssh-brute-force",
                "SSH Brute Force Detector",
                "/var/log/auth.log",
                "Triggers HIGH alert when >= 5 failed logins occur from one IP within 60 seconds. "
                        + "Triggers CRITICAL alert if a successful login immediately follows brute-force attempts.",
                "HIGH / CRITICAL"));

        pnlGrid.add(createDetectorCard("port-scan",
                "Suricata Flow Port Scanner",
                "eve.json (flow events)",
                "Analyzes Suricata flow events. Raises HIGH alert when >= 15 distinct destination ports "
                        + "are touched from a single source IP in under 10 seconds.",
                "HIGH"));

        pnlGrid.add(createDetectorCard("web-attack",
                "Web Exploits & Scanner Detector",
                "Apache / Nginx access.log",
                "Decodes URL requests twice. Flags SQL Injection (HIGH), XSS (MEDIUM), Directory Traversal (MEDIUM), "
                        + "and automated scanner tools like sqlmap, nikto, dirbuster (MEDIUM).",
                "HIGH / MEDIUM"));

        pnlGrid.add(createDetectorCard("arp-spoof",
                "ARP Poisoning / Spoofing Detector",
                "arp.log (ip neigh / arp -an)",
                "Maintains IP-to-MAC neighbor tables. Raises HIGH alert if an IP suddenly maps to a different MAC address, "
                        + "or when a MAC address claims multiple IP addresses.",
                "HIGH"));

        pnlGrid.add(createDetectorCard("dns-exfiltration",
                "DNS Tunneling & Exfil Detector",
                "dns.log / Suricata eve.json",
                "Inspects DNS queries for abnormally long subdomains (> 32 chars) typical of base32/64/hex data exfiltration, "
                        + "and rapid query bursts to single apex domains.",
                "HIGH"));

        add(pnlGrid, BorderLayout.CENTER);
    }

    private JPanel createDetectorCard(String id, String title, String logSource, String ruleDesc, String severities) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(SocTheme.BG_CARD);
        card.setBorder(new CompoundBorder(new LineBorder(SocTheme.BORDER), new EmptyBorder(12, 14, 12, 14)));

        // Top Row: Checkbox + Title + Status Pill
        JPanel pnlTop = new JPanel(new BorderLayout(8, 0));
        pnlTop.setOpaque(false);

        JCheckBox chk = new JCheckBox(title, engine.isDetectorEnabled(id));
        chk.setOpaque(false);
        chk.setFont(SocTheme.FONT_BODY_BOLD);
        chk.setForeground(SocTheme.TEXT_PRIMARY);
        chk.addActionListener(e -> engine.setDetectorEnabled(id, chk.isSelected()));
        toggleMap.put(id, chk);
        pnlTop.add(chk, BorderLayout.WEST);

        JLabel lblBadge = new JLabel("0 ALERTS");
        lblBadge.setFont(SocTheme.FONT_SMALL);
        lblBadge.setForeground(SocTheme.TEXT_MUTED);
        countBadges.put(id, lblBadge);
        pnlTop.add(lblBadge, BorderLayout.EAST);

        card.add(pnlTop, BorderLayout.NORTH);

        // Center: Description & source
        JPanel pnlCenter = new JPanel();
        pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));
        pnlCenter.setOpaque(false);

        JLabel lblSrc = new JLabel("Log Source: " + logSource);
        lblSrc.setFont(SocTheme.FONT_MONO_SMALL);
        lblSrc.setForeground(SocTheme.CYAN_ACCENT);
        pnlCenter.add(lblSrc);
        pnlCenter.add(Box.createVerticalStrut(6));

        JTextArea txtDesc = new JTextArea(ruleDesc);
        txtDesc.setFont(SocTheme.FONT_SMALL);
        txtDesc.setForeground(SocTheme.TEXT_SECONDARY);
        txtDesc.setBackground(SocTheme.BG_CARD);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setEditable(false);
        pnlCenter.add(txtDesc);

        card.add(pnlCenter, BorderLayout.CENTER);

        // Bottom row: Severity level tag
        JLabel lblSev = new JLabel("Severity: " + severities);
        lblSev.setFont(SocTheme.FONT_SMALL);
        lblSev.setForeground(SocTheme.TEXT_MUTED);
        card.add(lblSev, BorderLayout.SOUTH);

        return card;
    }

    public void updateAlertCounts(Map<String, Integer> counts) {
        SwingUtilities.invokeLater(() -> {
            for (Map.Entry<String, JLabel> entry : countBadges.entrySet()) {
                String id = entry.getKey();
                int total = 0;
                for (Map.Entry<String, Integer> c : counts.entrySet()) {
                    String type = c.getKey().toLowerCase();
                    if (id.equals("ssh-brute-force") && type.contains("ssh")) {
                        total += c.getValue();
                    } else if (id.equals("port-scan") && type.contains("port")) {
                        total += c.getValue();
                    } else if (id.equals("web-attack") && (type.contains("sql") || type.contains("xss") || type.contains("traversal") || type.contains("scanner"))) {
                        total += c.getValue();
                    } else if (id.equals("arp-spoof") && type.contains("arp")) {
                        total += c.getValue();
                    } else if (id.equals("dns-exfiltration") && type.contains("dns")) {
                        total += c.getValue();
                    }
                }
                JLabel badge = entry.getValue();
                badge.setText(total + " ALERTS");
                if (total > 0) {
                    badge.setForeground(SocTheme.CYAN_ACCENT);
                } else {
                    badge.setForeground(SocTheme.TEXT_MUTED);
                }
            }
        });
    }
}
