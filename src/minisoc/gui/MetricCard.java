package minisoc.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Modern SOC KPI Metric Card with accent border, large stat number, and subtitle.
 */
public class MetricCard extends JPanel {

    private final JLabel lblTitle;
    private final JLabel lblValue;
    private final JLabel lblSubtitle;
    private final Color accentColor;

    public MetricCard(String title, String initialValue, String subtitle, Color accentColor) {
        this.accentColor = accentColor;
        setLayout(new BorderLayout(5, 5));
        setBackground(SocTheme.BG_CARD);
        setBorder(new EmptyBorder(10, 16, 10, 16));
        setPreferredSize(new Dimension(190, 84));

        lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(SocTheme.FONT_SMALL);
        lblTitle.setForeground(SocTheme.TEXT_MUTED);

        lblValue = new JLabel(initialValue);
        lblValue.setFont(SocTheme.FONT_STAT_VAL);
        lblValue.setForeground(SocTheme.TEXT_PRIMARY);

        lblSubtitle = new JLabel(subtitle);
        lblSubtitle.setFont(SocTheme.FONT_SMALL);
        lblSubtitle.setForeground(SocTheme.TEXT_SECONDARY);

        JPanel pnlCenter = new JPanel();
        pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));
        pnlCenter.setOpaque(false);
        pnlCenter.add(lblTitle);
        pnlCenter.add(Box.createVerticalStrut(2));
        pnlCenter.add(lblValue);
        pnlCenter.add(Box.createVerticalStrut(2));
        pnlCenter.add(lblSubtitle);

        add(pnlCenter, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        lblValue.setText(value);
    }

    public void setValue(int value) {
        lblValue.setText(String.format("%,d", value));
    }

    public void setValue(long value) {
        lblValue.setText(String.format("%,d", value));
    }

    public void setSubtitle(String subtitle) {
        lblSubtitle.setText(subtitle);
    }

    public void setValueColor(Color color) {
        lblValue.setForeground(color);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw left accent stripe
        g2.setColor(accentColor);
        g2.fillRoundRect(2, 6, 4, getHeight() - 12, 2, 2);

        // Outer subtle border
        g2.setColor(SocTheme.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

        g2.dispose();
    }
}

