package minisoc.gui;

import minisoc.Alert;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Visual SOC Analytics panel with pure Java2D charts:
 * 1. Severity Distribution Bar
 * 2. Attack Category Breakdown
 * 3. Top Attacker IPs Leaderboard
 */
public class ChartsPanel extends JPanel {

    private final AlertsTableModel tableModel;
    private final SeverityBarChart severityChart;
    private final AttackTypeBarChart typeChart;
    private final TopIpsPanel topIpsPanel;

    private Consumer<String> onIpSelectConsumer;

    public ChartsPanel(AlertsTableModel tableModel) {
        this.tableModel = tableModel;
        setLayout(new GridLayout(1, 3, 12, 0));
        setBackground(SocTheme.BG_PANEL);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        severityChart = new SeverityBarChart();
        typeChart = new AttackTypeBarChart();
        topIpsPanel = new TopIpsPanel();

        add(severityChart);
        add(typeChart);
        add(topIpsPanel);

        // Listen for table updates
        tableModel.addTableModelListener(e -> refreshData());
    }

    public void setOnIpSelect(Consumer<String> consumer) {
        this.onIpSelectConsumer = consumer;
        topIpsPanel.setOnIpClick(consumer);
    }

    public void refreshData() {
        int crit = tableModel.getCountBySeverity("CRITICAL");
        int high = tableModel.getCountBySeverity("HIGH");
        int med  = tableModel.getCountBySeverity("MEDIUM");
        int low  = tableModel.getCountBySeverity("LOW");

        severityChart.updateCounts(crit, high, med, low);
        typeChart.updateData(tableModel.getTypeCounts());
        topIpsPanel.updateData(tableModel.getIpCounts());

        repaint();
    }

    /**
     * Stacked severity bar chart showing proportions of Critical, High, Medium, Low.
     */
    private static class SeverityBarChart extends JPanel {
        private int critical, high, medium, low;

        SeverityBarChart() {
            setOpaque(false);
            setBorder(new LineBorder(SocTheme.BORDER));
        }

        void updateCounts(int c, int h, int m, int l) {
            this.critical = c;
            this.high = h;
            this.medium = m;
            this.low = l;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(SocTheme.BG_CARD);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Title
            g2.setColor(SocTheme.CYAN_ACCENT);
            g2.setFont(SocTheme.FONT_HEADER);
            g2.drawString("SEVERITY BREAKDOWN", 14, 24);

            int total = critical + high + medium + low;
            if (total == 0) {
                g2.setColor(SocTheme.TEXT_MUTED);
                g2.setFont(SocTheme.FONT_BODY);
                g2.drawString("No alerts recorded", 14, 60);
                g2.dispose();
                return;
            }

            // Stacked Bar
            int barX = 14;
            int barY = 40;
            int barW = getWidth() - 28;
            int barH = 22;

            int wCrit = (int) Math.round(((double) critical / total) * barW);
            int wHigh = (int) Math.round(((double) high / total) * barW);
            int wMed  = (int) Math.round(((double) medium / total) * barW);
            int wLow  = barW - (wCrit + wHigh + wMed);

            int curX = barX;
            if (critical > 0) {
                g2.setColor(SocTheme.CRITICAL);
                g2.fillRect(curX, barY, wCrit, barH);
                curX += wCrit;
            }
            if (high > 0) {
                g2.setColor(SocTheme.HIGH);
                g2.fillRect(curX, barY, wHigh, barH);
                curX += wHigh;
            }
            if (medium > 0) {
                g2.setColor(SocTheme.MEDIUM);
                g2.fillRect(curX, barY, wMed, barH);
                curX += wMed;
            }
            if (low > 0) {
                g2.setColor(SocTheme.LOW);
                g2.fillRect(curX, barY, Math.max(0, wLow), barH);
            }

            // Legend rows
            int legendY = 86;
            drawLegendItem(g2, "CRITICAL", critical, total, SocTheme.CRITICAL, 14, legendY);
            drawLegendItem(g2, "HIGH", high, total, SocTheme.HIGH, 14, legendY + 24);
            drawLegendItem(g2, "MEDIUM", medium, total, SocTheme.MEDIUM, 14, legendY + 48);
            drawLegendItem(g2, "LOW", low, total, SocTheme.LOW, 14, legendY + 72);

            g2.dispose();
        }

        private void drawLegendItem(Graphics2D g2, String name, int count, int total, Color color, int x, int y) {
            g2.setColor(color);
            g2.fillOval(x, y - 9, 10, 10);

            g2.setFont(SocTheme.FONT_BODY_BOLD);
            g2.setColor(SocTheme.TEXT_PRIMARY);
            g2.drawString(name, x + 16, y);

            int pct = total > 0 ? (int) Math.round((count * 100.0) / total) : 0;
            String stat = String.format("%d (%d%%)", count, pct);
            FontMetrics fm = g2.getFontMetrics();
            int statX = getWidth() - 14 - fm.stringWidth(stat);
            g2.setColor(color);
            g2.drawString(stat, statX, y);
        }
    }

    /**
     * Horizontal bar chart of attack categories.
     */
    private static class AttackTypeBarChart extends JPanel {
        private Map<String, Integer> typeData = Collections.emptyMap();

        AttackTypeBarChart() {
            setOpaque(false);
            setBorder(new LineBorder(SocTheme.BORDER));
        }

        void updateData(Map<String, Integer> data) {
            this.typeData = new LinkedHashMap<>(data);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(SocTheme.BG_CARD);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(SocTheme.CYAN_ACCENT);
            g2.setFont(SocTheme.FONT_HEADER);
            g2.drawString("ATTACK CATEGORIES", 14, 24);

            if (typeData.isEmpty()) {
                g2.setColor(SocTheme.TEXT_MUTED);
                g2.setFont(SocTheme.FONT_BODY);
                g2.drawString("No attack patterns registered", 14, 60);
                g2.dispose();
                return;
            }

            int maxCount = 1;
            for (int count : typeData.values()) {
                if (count > maxCount) maxCount = count;
            }

            int startY = 46;
            int maxBarWidth = getWidth() - 130;

            int i = 0;
            for (Map.Entry<String, Integer> entry : typeData.entrySet()) {
                if (i >= 5) break; // Top 5
                int y = startY + (i * 26);

                String label = simplifyName(entry.getKey());
                g2.setFont(SocTheme.FONT_SMALL);
                g2.setColor(SocTheme.TEXT_SECONDARY);
                g2.drawString(label, 14, y + 10);

                int barW = (int) Math.round(((double) entry.getValue() / maxCount) * maxBarWidth);
                barW = Math.max(8, barW);

                g2.setColor(new Color(30, 90, 140));
                g2.fillRoundRect(110, y, barW, 14, 4, 4);

                g2.setColor(SocTheme.CYAN_ACCENT);
                g2.setFont(SocTheme.FONT_MONO_BOLD);
                g2.drawString(String.valueOf(entry.getValue()), 115 + barW, y + 12);

                i++;
            }

            g2.dispose();
        }

        private String simplifyName(String type) {
            if (type.length() > 14) return type.substring(0, 13) + "...";
            return type;
        }
    }

    /**
     * Top offending IP addresses with alert counts and click-to-filter.
     */
    private static class TopIpsPanel extends JPanel {
        private final DefaultListModel<String> listModel = new DefaultListModel<>();
        private final JList<String> ipList;
        private Consumer<String> onIpClick;

        TopIpsPanel() {
            setLayout(new BorderLayout(0, 6));
            setBackground(SocTheme.BG_CARD);
            setBorder(new LineBorder(SocTheme.BORDER));

            JLabel lblTitle = new JLabel("TOP OFFENDING SOURCE IPS");
            lblTitle.setFont(SocTheme.FONT_HEADER);
            lblTitle.setForeground(SocTheme.CYAN_ACCENT);
            lblTitle.setBorder(new EmptyBorder(10, 14, 0, 14));
            add(lblTitle, BorderLayout.NORTH);

            ipList = new JList<>(listModel);
            ipList.setBackground(SocTheme.BG_CARD);
            ipList.setForeground(SocTheme.TEXT_PRIMARY);
            ipList.setFont(SocTheme.FONT_MONO);
            ipList.setSelectionBackground(SocTheme.BG_SELECTED);
            ipList.setSelectionForeground(SocTheme.CYAN_ACCENT);
            ipList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    setBorder(new EmptyBorder(4, 12, 4, 12));
                    return c;
                }
            });

            ipList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && ipList.getSelectedValue() != null) {
                    String selected = ipList.getSelectedValue();
                    String ip = selected.split("\\s+")[0].trim();
                    if (onIpClick != null) onIpClick.accept(ip);
                }
            });

            JScrollPane scroll = new JScrollPane(ipList);
            scroll.setBorder(null);
            scroll.setVerticalScrollBar(SocTheme.createDarkScrollBar(JScrollBar.VERTICAL));
            add(scroll, BorderLayout.CENTER);
        }

        void setOnIpClick(Consumer<String> consumer) {
            this.onIpClick = consumer;
        }

        void updateData(Map<String, Integer> ipCounts) {
            listModel.clear();
            if (ipCounts.isEmpty()) {
                listModel.addElement("(No attacker IPs)");
                return;
            }
            int rank = 1;
            for (Map.Entry<String, Integer> entry : ipCounts.entrySet()) {
                if (rank > 10) break;
                listModel.addElement(String.format("%-16s [%d alerts]", entry.getKey(), entry.getValue()));
                rank++;
            }
        }
    }
}

