package com.finance;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;

import static com.finance.DashboardFrame.*;

public class HomePage extends JPanel {
    private final DashboardFrame dash;
    private JLabel balanceLabel, incomeLabel, expenseLabel;
    private DefaultTableModel tableModel;
    private static final DecimalFormat FMT = new DecimalFormat("#,##0.00");

    public HomePage(DashboardFrame dash) {
        this.dash = dash;
        setBackground(BG);
        setLayout(new BorderLayout(0, 15));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        add(buildSummaryCards(), BorderLayout.NORTH);
        add(buildRecentTransactions(), BorderLayout.CENTER);
        // Load data immediately after UI is built
        SwingUtilities.invokeLater(this::refresh);
    }

    // ── Summary Cards ─────────────────────────────────────────────────────────
    private JPanel buildSummaryCards() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 15, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 100));

        balanceLabel = new JLabel(dash.baseCurrency + " 0.00");
        incomeLabel  = new JLabel(dash.baseCurrency + " 0.00");
        expenseLabel = new JLabel(dash.baseCurrency + " 0.00");

        panel.add(summaryCard("Balance",       balanceLabel, ACCENT));
        panel.add(summaryCard("Total Income",  incomeLabel,  GREEN));
        panel.add(summaryCard("Total Expense", expenseLabel, RED));
        return panel;
    }

    private JPanel summaryCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent.darker(), 1, true),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Arial", Font.PLAIN, 13));
        t.setForeground(SUB);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 22));
        valueLabel.setForeground(accent);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(t);
        card.add(Box.createVerticalStrut(8));
        card.add(valueLabel);
        return card;
    }

    // ── Recent Transactions ───────────────────────────────────────────────────
    private JPanel buildRecentTransactions() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FBRD, 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        JLabel heading = new JLabel("Recent Transactions");
        heading.setFont(new Font("Arial", Font.BOLD, 15));
        heading.setForeground(TEXT);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        String[] cols = {"Type", "Category", "Currency", "Amount", dash.baseCurrency + " (Base)", "Description", "Date"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.setRowHeight(32);
        table.setBackground(new Color(20, 20, 45));
        table.setForeground(TEXT);
        table.setGridColor(FBRD);
        table.setSelectionBackground(ACCENT.darker());
        table.setSelectionForeground(new Color(240, 240, 255));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(35, 35, 65));
        table.getTableHeader().setForeground(SUB);

        // Type column — GREEN for INCOME, RED for EXPENSE
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                String val = v == null ? "" : v.toString();
                setForeground(val.equals("INCOME") ? GREEN : RED);
                setFont(new Font("Arial", Font.BOLD, 12));
                setHorizontalAlignment(CENTER);
                setBackground(sel ? ACCENT.darker() : new Color(20, 20, 45));
                return this;
            }
        });

        // Right-align amount columns
        DefaultTableCellRenderer ra = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(RIGHT);
                setForeground(TEXT);
                setBackground(sel ? ACCENT.darker() : new Color(20, 20, 45));
                return this;
            }
        };
        table.getColumnModel().getColumn(3).setCellRenderer(ra);
        table.getColumnModel().getColumn(4).setCellRenderer(ra);

        // Default renderer for other cols
        DefaultTableCellRenderer def = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setForeground(TEXT);
                setBackground(sel ? ACCENT.darker() : new Color(20, 20, 45));
                return this;
            }
        };
        for (int i : new int[]{1, 2, 5, 6}) table.getColumnModel().getColumn(i).setCellRenderer(def);

        table.getColumnModel().getColumn(0).setPreferredWidth(75);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(55);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(120);
        table.getColumnModel().getColumn(6).setPreferredWidth(130);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(new Color(20, 20, 45));
        scroll.setBorder(null);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ── Refresh ───────────────────────────────────────────────────────────────
    public void refresh() {
        double income  = DatabaseHelper.getTotal(dash.userId, "INCOME");
        double expense = DatabaseHelper.getTotal(dash.userId, "EXPENSE");
        double balance = income - expense;

        incomeLabel.setText(dash.baseCurrency + " " + FMT.format(income));
        expenseLabel.setText(dash.baseCurrency + " " + FMT.format(expense));
        balanceLabel.setText(dash.baseCurrency + " " + FMT.format(balance));
        balanceLabel.setForeground(balance >= 0 ? ACCENT : RED);

        tableModel.setRowCount(0);
        try {
            ResultSet rs = DatabaseHelper.getAllTransactions(dash.userId);
            int count = 0;
            while (rs.next() && count < 20) { // show last 20
                String type = rs.getString("type");
                String cat  = rs.getString("category");
                String cur  = rs.getString("currency");
                String amt  = FMT.format(rs.getDouble("amount"));
                String base = FMT.format(rs.getDouble("amount_in_base"));
                String desc = rs.getString("description");
                String date = rs.getString("date");
                if (date != null && date.length() > 16) date = date.substring(0, 16);
                tableModel.addRow(new Object[]{type, cat, cur, amt, base, desc, date});
                count++;
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
