package com.finance;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;

import static com.finance.DashboardFrame.*;

public class IncomePage extends JPanel {
    private final DashboardFrame dash;
    private JLabel totalLabel;
    private DefaultTableModel tableModel;
    private JLabel rateLabel;
    private static final DecimalFormat FMT = new DecimalFormat("#,##0.00");

    public IncomePage(DashboardFrame dash) {
        this.dash = dash;
        setBackground(BG);
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        add(buildSummary(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
    }

    private JPanel buildSummary() {
        JPanel p = new JPanel(new GridLayout(1, 1)); p.setOpaque(false);
        p.setPreferredSize(new Dimension(0, 80));
        totalLabel = new JLabel(dash.baseCurrency + " 0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 22)); totalLabel.setForeground(GREEN);
        JPanel card = new JPanel(new BorderLayout()); card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GREEN.darker(), 1, true),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        JLabel lbl = new JLabel("Total Income"); lbl.setFont(new Font("Arial", Font.PLAIN, 13)); lbl.setForeground(SUB);
        card.add(lbl, BorderLayout.NORTH); card.add(totalLabel, BorderLayout.CENTER);
        p.add(card); return p;
    }

    private JSplitPane buildContent() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildForm(), buildTable());
        split.setDividerLocation(320); split.setDividerSize(4); split.setBackground(BG); split.setBorder(null);
        return split;
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FBRD, 1, true),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        JLabel heading = new JLabel("+ Add Income");
        heading.setFont(new Font("Arial", Font.BOLD, 15)); heading.setForeground(TEXT); heading.setAlignmentX(0f);

        JComboBox<String> catBox = styledCombo(new String[]{"Salary","Business","Freelance","Investment","Gift","Other"});
        catBox.setAlignmentX(0f);

        JComboBox<String> currBox = styledCombo(CurrencyService.getCurrencyDisplayNames());
        currBox.setAlignmentX(0f);
        String[] codes = CurrencyService.getCurrencyCodes();
        for (int i = 0; i < codes.length; i++) if (codes[i].equals(dash.baseCurrency)) { currBox.setSelectedIndex(i); break; }

        rateLabel = new JLabel(" "); rateLabel.setFont(new Font("Arial", Font.PLAIN, 11)); rateLabel.setForeground(YELLOW); rateLabel.setAlignmentX(0f);
        currBox.addActionListener(e -> updateRate(currBox));

        JTextField amtField  = styledField("Amount (e.g. 5000)");
        JTextField descField = styledField("Description (optional)");

        JButton addBtn = makeBtn("Add Income", GREEN);
        addBtn.setAlignmentX(0f);
        addBtn.addActionListener(e -> {
            String amt = amtField.getText().trim();
            if (amt.isEmpty() || amt.equals("Amount (e.g. 5000)")) { err("Please enter amount!"); return; }
            try {
                double amount = Double.parseDouble(amt);
                if (amount <= 0) throw new NumberFormatException();
                String cur = CurrencyService.getCurrencyCodes()[currBox.getSelectedIndex()];
                double inBase = cur.equals(dash.baseCurrency) ? amount : CurrencyService.convert(amount, cur, dash.baseCurrency);
                if (inBase < 0) inBase = amount;
                String desc = descField.getText().trim().equals("Description (optional)") ? "" : descField.getText().trim();
                if (DatabaseHelper.addTransaction(dash.userId, "INCOME", (String) catBox.getSelectedItem(), amount, cur, inBase, desc)) {
                    amtField.setText("");
                    dash.refreshAll();
                }
            } catch (NumberFormatException ex) { err("Please enter a valid amount!"); }
        });

        panel.add(heading); panel.add(Box.createVerticalStrut(14));
        panel.add(flbl("Category")); panel.add(Box.createVerticalStrut(5)); panel.add(catBox);
        panel.add(Box.createVerticalStrut(12));
        panel.add(flbl("Currency")); panel.add(Box.createVerticalStrut(5)); panel.add(currBox);
        panel.add(Box.createVerticalStrut(3)); panel.add(rateLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(flbl("Amount")); panel.add(Box.createVerticalStrut(5)); panel.add(amtField);
        panel.add(Box.createVerticalStrut(12));
        panel.add(flbl("Description")); panel.add(Box.createVerticalStrut(5)); panel.add(descField);
        panel.add(Box.createVerticalStrut(18)); panel.add(addBtn);
        return panel;
    }

    private JPanel buildTable() {
        JPanel panel = new JPanel(new BorderLayout()); panel.setBackground(CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FBRD, 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        JLabel heading = new JLabel("Income History");
        heading.setFont(new Font("Arial", Font.BOLD, 15)); heading.setForeground(TEXT);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        String[] cols = {"Category","Currency","Amount", dash.baseCurrency+" (Base)","Description","Date"};
        tableModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable table = makeTable(tableModel);
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(new Color(20, 20, 45)); scroll.setBorder(null);
        panel.add(heading, BorderLayout.NORTH); panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    public void refresh() {
        totalLabel.setText(dash.baseCurrency + " " + FMT.format(DatabaseHelper.getTotal(dash.userId, "INCOME")));
        tableModel.setRowCount(0);
        try {
            ResultSet rs = DatabaseHelper.getTransactionsByType(dash.userId, "INCOME");
            while (rs.next()) {
                String date = rs.getString("date");
                if (date != null && date.length() > 16) date = date.substring(0, 16);
                tableModel.addRow(new Object[]{rs.getString("category"), rs.getString("currency"),
                    FMT.format(rs.getDouble("amount")), FMT.format(rs.getDouble("amount_in_base")),
                    rs.getString("description"), date});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateRate(JComboBox<String> box) {
        String sel = CurrencyService.getCurrencyCodes()[box.getSelectedIndex()];
        if (sel.equals(dash.baseCurrency)) { rateLabel.setText("Same as base currency"); return; }
        rateLabel.setText("Fetching rate...");
        new SwingWorker<String, Void>() {
            protected String doInBackground() { return CurrencyService.getRateDisplay(sel, dash.baseCurrency); }
            protected void done() { try { rateLabel.setText(get()); } catch (Exception ignored) {} }
        }.execute();
    }

    static JTable makeTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setFont(new Font("Arial", Font.PLAIN, 12)); t.setRowHeight(30);
        t.setBackground(new Color(20, 20, 45)); t.setForeground(TEXT); t.setGridColor(FBRD);
        t.setSelectionBackground(ACCENT.darker()); t.setSelectionForeground(new Color(240, 240, 255));
        t.setShowHorizontalLines(true); t.setShowVerticalLines(false);
        t.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(35, 35, 65)); t.getTableHeader().setForeground(SUB);
        DefaultTableCellRenderer def = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tt, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tt, v, sel, foc, r, c);
                setForeground(TEXT); setBackground(sel ? ACCENT.darker() : new Color(20, 20, 45)); return this;
            }
        };
        for (int i = 0; i < model.getColumnCount(); i++) t.getColumnModel().getColumn(i).setCellRenderer(def);
        return t;
    }

    private void err(String m) { JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE); }
    private JLabel flbl(String t) { JLabel l = new JLabel(t); l.setFont(new Font("Arial", Font.BOLD, 11)); l.setForeground(SUB); l.setAlignmentX(0f); return l; }
}
