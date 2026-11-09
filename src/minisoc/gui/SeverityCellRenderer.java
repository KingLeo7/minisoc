package minisoc.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Custom table cell renderer that draws severity levels as colored pill badges.
 */
public class SeverityCellRenderer extends DefaultTableCellRenderer {

    public SeverityCellRenderer() {
        setHorizontalAlignment(CENTER);
        setOpaque(false);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        String severity = value != null ? value.toString() : "";
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, severity, isSelected, hasFocus, row, column);
        label.setFont(SocTheme.FONT_BODY_BOLD);
        label.setText(severity);

        Color fg = SocTheme.getSeverityColor(severity);
        Color bg = isSelected ? SocTheme.BG_SELECTED : SocTheme.getSeverityBg(severity);

        return new JPanel() {
            {
                setOpaque(true);
                setBackground(isSelected ? SocTheme.BG_SELECTED : (row % 2 == 0 ? SocTheme.BG_PANEL : SocTheme.BG_CARD));
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int badgeWidth = Math.max(76, g2.getFontMetrics(SocTheme.FONT_BODY_BOLD).stringWidth(severity) + 16);
                int badgeHeight = 20;
                int x = (getWidth() - badgeWidth) / 2;
                int y = (getHeight() - badgeHeight) / 2;

                // Pill background
                g2.setColor(bg);
                g2.fillRoundRect(x, y, badgeWidth, badgeHeight, 10, 10);

                // Pill border
                g2.setColor(fg);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(x, y, badgeWidth, badgeHeight, 10, 10);

                // Text
                g2.setFont(SocTheme.FONT_BODY_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int textX = x + (badgeWidth - fm.stringWidth(severity)) / 2;
                int textY = y + ((badgeHeight - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(severity, textX, textY);

                g2.dispose();
            }
        };
    }
}

