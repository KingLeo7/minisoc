package minisoc.gui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Interactive dialog to trigger simulated attack traffic on the fly.
 */
public class AttackSimulatorDialog extends JDialog {

    public AttackSimulatorDialog(Frame owner) {
        super(owner, "MiniSOC - Live Attack Simulator", true);
        setSize(560, 480);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(SocTheme.BG_PANEL);
        setLayout(new BorderLayout(0, 10));

        // Header
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(new EmptyBorder(12, 16, 6, 16));

        JLabel lblTitle = new JLabel("TARGETED ATTACK TRAFFIC GENERATOR");
        lblTitle.setFont(SocTheme.FONT_HEADER);
        lblTitle.setForeground(SocTheme.HIGH);
        pnlHeader.add(lblTitle, BorderLayout.WEST);

        JLabel lblSub = new JLabel("Inject live attack payloads directly into monitored logs");
        lblSub.setFont(SocTheme.FONT_SMALL);
        lblSub.setForeground(SocTheme.TEXT_MUTED);
        pnlHeader.add(lblSub, BorderLayout.SOUTH);

        add(pnlHeader, BorderLayout.NORTH);

        // Center: Attack options
        JPanel pnlCenter = new JPanel();
        pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));
        pnlCenter.setOpaque(false);
        pnlCenter.setBorder(new EmptyBorder(4, 16, 4, 16));

        JTextField txtIp = new JTextField("192.168.56.10");
        txtIp.setFont(SocTheme.FONT_MONO);

        JPanel pnlIp = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pnlIp.setOpaque(false);
        JLabel lblIp = new JLabel("Attacker Source IP:");
        lblIp.setFont(SocTheme.FONT_BODY);
        lblIp.setForeground(SocTheme.TEXT_SECONDARY);
        pnlIp.add(lblIp);
        pnlIp.add(txtIp);
        pnlCenter.add(pnlIp);
        pnlCenter.add(Box.createVerticalStrut(6));

        // Attack Buttons Grid
        JPanel pnlGrid = new JPanel(new GridLayout(4, 2, 8, 8));
        pnlGrid.setOpaque(false);

        pnlGrid.add(createAttackBtn("⚡ SSH Brute Force + Login", "5 failed logins + 1 root compromised", () -> {
            try {
                AttackSimulator.simulateSshBruteForce(txtIp.getText().trim(), "root");
                showToast("SSH brute-force activity", "samples/auth.log");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }));

        pnlGrid.add(createAttackBtn("⚡ Port Scan (18 Ports)", "Suricata eve.json flow flood", () -> {
            try {
                AttackSimulator.simulatePortScan(txtIp.getText().trim(), 18);
                showToast("Port-scan flow records", "samples/eve.json");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }));

        pnlGrid.add(createAttackBtn("⚡ SQL Injection", "UNION SELECT password dump", () -> {
            try {
                AttackSimulator.simulateSqlInjection(txtIp.getText().trim());
                showToast("SQL injection request", "samples/access.log");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }));

        pnlGrid.add(createAttackBtn("⚡ XSS & Path Traversal", "<script> and ../../../../etc/passwd", () -> {
            try {
                AttackSimulator.simulateXssAndTraversal(txtIp.getText().trim());
                showToast("XSS and path-traversal requests", "samples/access.log");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }));

        pnlGrid.add(createAttackBtn("⚡ Vulnerability Scanner", "sqlmap / nikto User-Agent", () -> {
            try {
                AttackSimulator.simulateScannerTool(txtIp.getText().trim(), "sqlmap");
                showToast("Vulnerability-scanner request", "samples/access.log");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }));

        pnlGrid.add(createAttackBtn("⚡ ARP Cache Poisoning", "MAC conflict & IP takeover", () -> {
            try {
                AttackSimulator.simulateArpSpoof(txtIp.getText().trim(), "0a:00:27:00:00:00", "08:00:27:de:ad:01");
                showToast("ARP address-conflict event", "samples/arp.log");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }));

        pnlGrid.add(createAttackBtn("⚡ DNS Tunneling / Exfil", "Base64 exfiltration payload query", () -> {
            try {
                AttackSimulator.simulateDnsExfiltration(txtIp.getText().trim());
                showToast("DNS tunneling query", "samples/dns.log");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }));

        JButton btnFullWave = SocTheme.createStyledButton("🚀 Full Red Team Wave", "danger");
        btnFullWave.setFont(SocTheme.FONT_BODY_BOLD);
        btnFullWave.addActionListener(e -> {
            new Thread(() -> {
                try {
                    String ip = txtIp.getText().trim();
                    showToast("Full simulation started", "multiple sample logs");
                    AttackSimulator.simulateSshBruteForce(ip, "oracle");
                    Thread.sleep(400);
                    AttackSimulator.simulateSqlInjection(ip);
                    Thread.sleep(400);
                    AttackSimulator.simulatePortScan(ip, 16);
                    Thread.sleep(400);
                    AttackSimulator.simulateXssAndTraversal(ip);
                    Thread.sleep(400);
                    AttackSimulator.simulateArpSpoof(ip, "0a:00:27:00:00:00", "08:00:27:de:ad:01");
                    Thread.sleep(400);
                    AttackSimulator.simulateDnsExfiltration(ip);
                    showToast("Full simulation complete", "multiple sample logs");
                } catch (Exception ex) {
                    showError(ex.getMessage());
                }
            }).start();
        });
        pnlGrid.add(btnFullWave);

        pnlCenter.add(pnlGrid);
        add(pnlCenter, BorderLayout.CENTER);

        // Bottom: Close button
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        pnlBottom.setOpaque(false);
        JButton btnClose = SocTheme.createStyledButton("Close", "secondary");
        btnClose.addActionListener(e -> dispose());
        pnlBottom.add(btnClose);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    private JPanel createAttackBtn(String title, String subtitle, Runnable action) {
        JPanel p = new JPanel(new BorderLayout(4, 2));
        p.setBackground(SocTheme.BG_CARD);
        p.setBorder(new CompoundBorder(new LineBorder(SocTheme.BORDER), new EmptyBorder(6, 8, 6, 8)));

        JButton btn = SocTheme.createStyledButton(title, "secondary");
        btn.setFont(SocTheme.FONT_BODY_BOLD);
        btn.addActionListener(e -> action.run());

        JLabel lblSub = new JLabel(subtitle, SwingConstants.CENTER);
        lblSub.setFont(SocTheme.FONT_SMALL);
        lblSub.setForeground(SocTheme.TEXT_MUTED);

        p.add(btn, BorderLayout.CENTER);
        p.add(lblSub, BorderLayout.SOUTH);
        return p;
    }

    private void showToast(String activity, String logFile) {
        SwingUtilities.invokeLater(() -> {
            String message = "Activity: " + activity + "\n\n"
                    + "Log: " + logFile + "\n\n"
                    + "After the simulation finishes, choose “Replay Lab Samples” on the dashboard "
                    + "to review the resulting alerts.";
            SocTheme.showMessageDialog(this, "Simulation Event Added", message,
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void showError(String msg) {
        SwingUtilities.invokeLater(() -> {
            String detail = msg == null || msg.trim().isEmpty()
                    ? "The simulation could not be completed. Check the application logs for details."
                    : msg;
            SocTheme.showMessageDialog(this, "Simulation Failed",
                    "The simulation could not be completed.\n\n" + detail,
                    JOptionPane.ERROR_MESSAGE);
        });
    }
}
