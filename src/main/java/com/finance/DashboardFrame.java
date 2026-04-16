package com.finance;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.*;

public class DashboardFrame extends JFrame {

    static final Color BG     = new Color(15, 15, 30);
    static final Color CARD   = new Color(25, 25, 50);
    static final Color ACCENT = new Color(99, 102, 241);
    static final Color GREEN  = new Color(34, 197, 94);
    static final Color RED    = new Color(239, 68, 68);
    static final Color TEXT   = new Color(220, 220, 255);
    static final Color SUB    = new Color(130, 130, 180);
    static final Color FBG    = new Color(35, 35, 65);
    static final Color FBRD   = new Color(60, 60, 100);
    static final Color YELLOW = new Color(251, 191, 36);

    final int userId;
    final String username;
    String baseCurrency;

    private JLabel baseCurrencyLabel;
    private JTabbedPane tabs;

    HomePage   homePage;
    IncomePage  incomePage;
    ExpensePage expensePage;
    ChartsPage  chartsPage;
    BudgetPage  budgetPage;

    public DashboardFrame(int userId, String username) {
        this.userId       = userId;
        this.username     = username;
        this.baseCurrency = DatabaseHelper.getBaseCurrency(userId);
        setTitle("Finance Tracker — " + username);
        setSize(1100, 720);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // Fix all combo boxes globally
        fixComboBoxColors();
        initUI();
    }

    private void fixComboBoxColors() {
        UIManager.put("ComboBox.background",        FBG);
        UIManager.put("ComboBox.foreground",        TEXT);
        UIManager.put("ComboBox.selectionBackground", ACCENT);
        UIManager.put("ComboBox.selectionForeground", new Color(240, 240, 255));
        UIManager.put("ComboBox.buttonBackground",  FBG);
        UIManager.put("ComboBox.disabledBackground", FBG);
        UIManager.put("ComboBox.disabledForeground", SUB);
        UIManager.put("ComboBox.border", BorderFactory.createLineBorder(FBRD, 1));
        UIManager.put("ComboBoxUI", "javax.swing.plaf.basic.BasicComboBoxUI");

        // Popup / dropdown list colors
        UIManager.put("ComboBox.listBackground",         FBG);
        UIManager.put("ComboBox.listForeground",         TEXT);
        UIManager.put("List.background",                 FBG);
        UIManager.put("List.foreground",                 TEXT);
        UIManager.put("List.selectionBackground",        ACCENT);
        UIManager.put("List.selectionForeground",        new Color(240, 240, 255));
        UIManager.put("PopupMenu.background",            FBG);
        UIManager.put("PopupMenu.foreground",            TEXT);
        UIManager.put("PopupMenu.border",                BorderFactory.createLineBorder(FBRD, 1));

        // TabbedPane — make tab labels clearly visible
        UIManager.put("TabbedPane.selected",             CARD);
        UIManager.put("TabbedPane.unselectedBackground", new Color(40, 40, 75));
        UIManager.put("TabbedPane.background",           BG);
        UIManager.put("TabbedPane.foreground",           TEXT);
        UIManager.put("TabbedPane.selectedForeground",   new Color(240, 240, 255));
        UIManager.put("TabbedPane.contentAreaColor",     BG);
        UIManager.put("TabbedPane.borderHightlightColor", FBRD);
        UIManager.put("TabbedPane.darkShadow",           FBRD);
        UIManager.put("TabbedPane.light",                new Color(50, 50, 90));
        UIManager.put("TabbedPane.shadow",               new Color(30, 30, 60));
        UIManager.put("TabbedPane.focus",                ACCENT);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(buildTabs(),   BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CARD);
        bar.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        JLabel title = new JLabel("  Finance Tracker");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(TEXT);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        baseCurrencyLabel = new JLabel("Base: " + baseCurrency);
        baseCurrencyLabel.setFont(new Font("Arial", Font.BOLD, 12));
        baseCurrencyLabel.setForeground(YELLOW);

        JButton settingsBtn = new JButton("Settings");
        settingsBtn.setFont(new Font("Arial", Font.BOLD, 11));
        settingsBtn.setBackground(ACCENT.darker());
        settingsBtn.setForeground(TEXT);
        settingsBtn.setBorder(BorderFactory.createLineBorder(ACCENT, 1, true));
        settingsBtn.setFocusPainted(false);
        settingsBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsBtn.addActionListener(e -> showSettings());

        JLabel user = new JLabel("User: " + username);
        user.setFont(new Font("Arial", Font.PLAIN, 12));
        user.setForeground(SUB);

        JButton logout = new JButton("Logout");
        logout.setFont(new Font("Arial", Font.BOLD, 11));
        logout.setBackground(new Color(60, 20, 20));
        logout.setForeground(RED);
        logout.setBorder(BorderFactory.createLineBorder(RED, 1, true));
        logout.setFocusPainted(false);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });

        right.add(baseCurrencyLabel);
        right.add(settingsBtn);
        right.add(user);
        right.add(logout);

        bar.add(title, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JTabbedPane buildTabs() {
        tabs = new JTabbedPane();
        tabs.setFont(new Font("Arial", Font.BOLD, 13));
        tabs.setBackground(BG);
        tabs.setForeground(TEXT);

        homePage    = new HomePage(this);
        incomePage  = new IncomePage(this);
        expensePage = new ExpensePage(this);
        chartsPage  = new ChartsPage(this);
        budgetPage  = new BudgetPage(this);

        tabs.addTab("  Home  ",    homePage);
        tabs.addTab("  Income  ",  incomePage);
        tabs.addTab("  Expense  ", expensePage);
        tabs.addTab("  Charts  ",  chartsPage);
        tabs.addTab("  Budget  ",  budgetPage);

        tabs.addChangeListener(e -> refreshCurrentTab());
        // Load all data immediately when dashboard opens
        SwingUtilities.invokeLater(this::refreshAll);
        return tabs;
    }

    public void refreshAll() {
        homePage.refresh();
        incomePage.refresh();
        expensePage.refresh();
        chartsPage.refresh();
        budgetPage.refresh();
    }

    private void refreshCurrentTab() {
        int i = tabs.getSelectedIndex();
        if      (i == 0) homePage.refresh();
        else if (i == 1) incomePage.refresh();
        else if (i == 2) expensePage.refresh();
        else if (i == 3) chartsPage.refresh();
        else if (i == 4) budgetPage.refresh();
    }

    private void showSettings() {
        JDialog dialog = new JDialog(this, "Settings", true);
        dialog.setSize(420, 240);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        JLabel ttl = new JLabel("Choose Base Currency");
        ttl.setFont(new Font("Arial", Font.BOLD, 15)); ttl.setForeground(TEXT); ttl.setAlignmentX(0f);

        JLabel desc = new JLabel("All amounts will be shown in this currency");
        desc.setFont(new Font("Arial", Font.PLAIN, 12)); desc.setForeground(SUB); desc.setAlignmentX(0f);

        JComboBox<String> box = styledCombo(CurrencyService.getCurrencyDisplayNames());
        box.setAlignmentX(0f);
        String[] codes = CurrencyService.getCurrencyCodes();
        for (int i = 0; i < codes.length; i++)
            if (codes[i].equals(baseCurrency)) { box.setSelectedIndex(i); break; }

        JButton save = makeBtn("Save", ACCENT);
        save.setAlignmentX(0f);
        save.addActionListener(e -> {
            baseCurrency = CurrencyService.getCurrencyCodes()[box.getSelectedIndex()];
            DatabaseHelper.setBaseCurrency(userId, baseCurrency);
            baseCurrencyLabel.setText("Base: " + baseCurrency);
            refreshAll();
            dialog.dispose();
        });

        JLabel cl = new JLabel("Currency");
        cl.setFont(new Font("Arial", Font.BOLD, 11)); cl.setForeground(SUB); cl.setAlignmentX(0f);

        panel.add(ttl); panel.add(Box.createVerticalStrut(5)); panel.add(desc);
        panel.add(Box.createVerticalStrut(15)); panel.add(cl);
        panel.add(Box.createVerticalStrut(6)); panel.add(box);
        panel.add(Box.createVerticalStrut(18)); panel.add(save);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    // ── Shared UI Helpers ─────────────────────────────────────────────────────
    static JButton makeBtn(String text, Color color) {
        JButton b = new JButton(text) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? color.brighter() : color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(240, 240, 255)); g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setFont(new Font("Arial", Font.BOLD, 13));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.setBorderPainted(false); b.setContentAreaFilled(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> box = new JComboBox<>(items);
        box.setFont(new Font("Arial", Font.PLAIN, 12));
        box.setBackground(FBG);
        box.setForeground(TEXT);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        box.setBorder(BorderFactory.createLineBorder(FBRD, 1));

        // Override renderer so popup list also has dark colors
        box.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? ACCENT : FBG);
                setForeground(TEXT);
                setFont(new Font("Arial", Font.PLAIN, 12));
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return this;
            }
        });
        return box;
    }

    static JTextField styledField(String ph) {
        JTextField f = new JTextField();
        f.setFont(new Font("Arial", Font.PLAIN, 13));
        f.setBackground(FBG); f.setForeground(SUB); f.setCaretColor(TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FBRD, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setAlignmentX(0f); f.setText(ph);
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (f.getText().equals(ph)) { f.setText(""); f.setForeground(TEXT); }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(ph); f.setForeground(SUB); }
            }
        });
        return f;
    }
}
