package minisoc.gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * Modern dark Cyber SOC theme constants, styles, and UI helpers.
 */
public class SocTheme {

    // Palette: Cyber Dark Slate
    public static final Color BG_DARK        = new Color(13, 17, 23);     // #0d1117 (main app bg)
    public static final Color BG_PANEL       = new Color(22, 27, 34);     // #161b22 (panels & cards)
    public static final Color BG_CARD        = new Color(27, 31, 36);     // #1b1f24
    public static final Color BG_HEADER      = new Color(30, 36, 46);     // #1e242e
    public static final Color BG_INPUT       = new Color(18, 22, 28);     // #12161c
    public static final Color BG_HOVER       = new Color(38, 44, 54);     // #262c36
    public static final Color BG_SELECTED    = new Color(31, 53, 79);     // #1f354f

    public static final Color BORDER         = new Color(48, 54, 61);     // #30363d
    public static final Color BORDER_SUBTLE  = new Color(38, 43, 50);     // #262b32
    public static final Color BORDER_ACTIVE  = new Color(56, 189, 248);   // cyan

    public static final Color TEXT_PRIMARY   = new Color(240, 246, 252);  // bright white
    public static final Color TEXT_SECONDARY = new Color(142, 153, 166);  // muted gray
    public static final Color TEXT_MUTED     = new Color(100, 110, 122);  // dark gray

    // Severity & Status Accents
    public static final Color CYAN_ACCENT    = new Color(56, 189, 248);   // #38bdf8
    public static final Color CRITICAL       = new Color(248, 81, 73);    // #f85149 Red
    public static final Color CRITICAL_BG    = new Color(60, 22, 24);     // subtle dark red
    public static final Color HIGH           = new Color(240, 136, 62);   // #f0883e Orange
    public static final Color HIGH_BG        = new Color(56, 35, 20);     // subtle dark orange
    public static final Color MEDIUM         = new Color(210, 153, 34);   // #d29922 Yellow
    public static final Color MEDIUM_BG      = new Color(54, 42, 18);     // subtle dark yellow
    public static final Color LOW            = new Color(88, 166, 255);   // #58a6ff Blue
    public static final Color LOW_BG         = new Color(18, 36, 61);     // subtle dark blue
    public static final Color SUCCESS        = new Color(63, 185, 80);    // #3fb950 Green
    public static final Color SUCCESS_BG     = new Color(19, 44, 28);

    // Fonts
    public static final Font FONT_TITLE      = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_HEADER     = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BODY_BOLD  = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_SMALL      = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO       = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FONT_MONO_BOLD  = new Font("Consolas", Font.BOLD, 12);
    public static final Font FONT_MONO_SMALL = new Font("Consolas", Font.PLAIN, 11);
    public static final Font FONT_STAT_VAL   = new Font("Segoe UI", Font.BOLD, 22);

    /**
     * Initializes dark LookAndFeel defaults across Swing.
     */
    public static void applySystemTheme() {
        try {
            // Set basic dark defaults
            UIManager.put("Panel.background", BG_PANEL);
            UIManager.put("Panel.foreground", TEXT_PRIMARY);
            UIManager.put("Label.foreground", TEXT_PRIMARY);
            UIManager.put("Label.font", FONT_BODY);

            UIManager.put("Table.background", BG_PANEL);
            UIManager.put("Table.foreground", TEXT_PRIMARY);
            UIManager.put("Table.selectionBackground", BG_SELECTED);
            UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
            UIManager.put("Table.gridColor", BORDER_SUBTLE);
            UIManager.put("TableHeader.background", BG_HEADER);
            UIManager.put("TableHeader.foreground", TEXT_PRIMARY);
            UIManager.put("TableHeader.font", FONT_HEADER);

            UIManager.put("TextField.background", BG_INPUT);
            UIManager.put("TextField.foreground", TEXT_PRIMARY);
            UIManager.put("TextField.caretForeground", CYAN_ACCENT);
            UIManager.put("TextField.border", new CompoundBorder(new LineBorder(BORDER), new EmptyBorder(5, 8, 5, 8)));

            UIManager.put("TextArea.background", BG_INPUT);
            UIManager.put("TextArea.foreground", TEXT_PRIMARY);
            UIManager.put("TextArea.caretForeground", CYAN_ACCENT);

            UIManager.put("ScrollPane.background", BG_PANEL);
            UIManager.put("ScrollPane.border", new LineBorder(BORDER));

            UIManager.put("TabbedPane.background", BG_PANEL);
            UIManager.put("TabbedPane.foreground", TEXT_SECONDARY);
            UIManager.put("TabbedPane.selected", BG_HEADER);
            UIManager.put("TabbedPane.selectedForeground", CYAN_ACCENT);
            UIManager.put("TabbedPane.borderHighlightColor", BORDER);
            UIManager.put("TabbedPane.contentBorderInsets", new Insets(1, 1, 1, 1));

            UIManager.put("ComboBox.background", BG_INPUT);
            UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
            UIManager.put("ComboBox.selectionBackground", BG_SELECTED);
            UIManager.put("ComboBox.selectionForeground", TEXT_PRIMARY);

            UIManager.put("ToolTip.background", BG_HEADER);
            UIManager.put("ToolTip.foreground", TEXT_PRIMARY);
            UIManager.put("ToolTip.border", new LineBorder(BORDER_ACTIVE));
        } catch (Exception ignored) {}
    }

    public static Border createPanelBorder() {
        return new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(8, 8, 8, 8));
    }

    public static Border createCardBorder() {
        return new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(12, 14, 12, 14));
    }

    public static JButton createStyledButton(String text, String styleType) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Color bg;
        Color fg = TEXT_PRIMARY;
        Color border;

        switch (styleType.toLowerCase()) {
            case "primary":
                bg = new Color(20, 85, 140);
                border = CYAN_ACCENT;
                break;
            case "success":
                bg = new Color(22, 90, 45);
                border = SUCCESS;
                break;
            case "danger":
                bg = new Color(110, 30, 30);
                border = CRITICAL;
                break;
            case "warning":
                bg = new Color(110, 65, 20);
                border = HIGH;
                break;
            default: // secondary
                bg = BG_CARD;
                border = BORDER;
                break;
        }

        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setBorder(new CompoundBorder(new LineBorder(border, 1), new EmptyBorder(6, 12, 6, 12)));

        Color hoverBg = styleType.equalsIgnoreCase("primary") ? new Color(26, 110, 180)
                      : styleType.equalsIgnoreCase("success") ? new Color(30, 115, 58)
                      : styleType.equalsIgnoreCase("danger") ? new Color(140, 40, 40)
                      : BG_HOVER;

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(hoverBg);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bg);
            }
        });

        return btn;
    }

    public static JScrollBar createDarkScrollBar(int orientation) {
        JScrollBar scrollBar = new JScrollBar(orientation);
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(50, 58, 69);
                this.trackColor = BG_PANEL;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton jbutton = new JButton();
                jbutton.setPreferredSize(new Dimension(0, 0));
                jbutton.setMinimumSize(new Dimension(0, 0));
                jbutton.setMaximumSize(new Dimension(0, 0));
                return jbutton;
            }
        });
        return scrollBar;
    }

    public static Color getSeverityColor(String severity) {
        if (severity == null) return TEXT_MUTED;
        switch (severity.toUpperCase()) {
            case "CRITICAL": return CRITICAL;
            case "HIGH": return HIGH;
            case "MEDIUM": return MEDIUM;
            case "LOW": return LOW;
            default: return TEXT_SECONDARY;
        }
    }

    public static Color getSeverityBg(String severity) {
        if (severity == null) return BG_CARD;
        switch (severity.toUpperCase()) {
            case "CRITICAL": return CRITICAL_BG;
            case "HIGH": return HIGH_BG;
            case "MEDIUM": return MEDIUM_BG;
            case "LOW": return LOW_BG;
            default: return BG_CARD;
        }
    }
}

