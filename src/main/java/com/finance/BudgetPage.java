package com.finance;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.YearMonth;

import static com.finance.DashboardFrame.*;

public class BudgetPage extends JPanel {
    private final DashboardFrame dash;
    private DefaultTableModel tableModel;
    private JLabel summaryLabel;
    private static final DecimalFormat FMT = new DecimalFormat("#,##0.00");

    public BudgetPage(DashboardFrame dash) {
        this.dash = dash;
        setBackground(BG);
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false);
        JLabel title = new JLabel("Monthly Budget — " + YearMonth.now().toString());
        title.setFont(new Font("Arial", Font.BOLD, 18)); title.setForeground(TEXT);
        summaryLabel = new JLabel(" ");
        summaryLabel.setFont(new Font("Arial", Font.PLAIN, 13)); summaryLabel.setForeground(YELLOW);
        p.add(title, BorderLayout.WEST); p.add(summaryLabel, BorderLayout.EAST);
        return p;
    }

    private JSplitPane buildContent() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildForm(), buildTable());
        split.setDividerLocation(340); split.setDividerSize(4); split.setBackground(BG); split.setBorder(null);
        return split;
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FBRD, 1, true),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        JLabel heading = new JLabel("Set Monthly Budget");
        heading.setFont(new Font("Arial", Font.BOLD, 15)); heading.setForeground(TEXT); heading.setAlignmentX(0f);

        JLabel info = new JLabel("<html><body style='color:rgb(130,130,180);'>Set a spending limit per category.<br>You'll get a warning if you exceed it!</body></html>");
        info.setAlignmentX(0f);

        // Income goal section
        JLabel incomeHeading = new JLabel("Income Goal (monthly)");
        incomeHeading.setFont(new Font("Arial", Font.BOLD, 13)); incomeHeading.setForeground(GREEN); incomeHeading.setAlignmentX(0f);
        JTextField incomeField = styledField("Expected income this month");
        JButton incomeBtn = makeBtn("Set Income Goal", GREEN);
        incomeBtn.setAlignmentX(0f);
        incomeBtn.addActionListener(e -> {
            String val = incomeField.getText().trim();
            if (val.isEmpty() || val.equals("Expected income this month")) { err("Enter an amount!"); return; }
            try {
                double limit = Double.parseDouble(val);
                if (limit <= 0) throw new NumberFormatException();
                DatabaseHelper.setBudget(dash.userId, "__INCOME__", limit, YearMonth.now().toString());
                incomeField.setText("");
                JOptionPane.showMessageDialog(this, "Income goal set!", "Done", JOptionPane.INFORMATION_MESSAGE);
                refresh();
            } catch (NumberFormatException ex) { err("Enter a valid amount!"); }
        });

        JSeparator sep = new JSeparator(); sep.setForeground(FBRD); sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        // Expense budget section
        JLabel expHeading = new JLabel("Expense Budget by Category");
        expHeading.setFont(new Font("Arial", Font.BOLD, 13)); expHeading.setForeground(RED); expHeading.setAlignmentX(0f);
        JComboBox<String> catBox = styledCombo(new String[]{"Food","Transport","Bills","Shopping","Health","Education","Entertainment","Other"});
        catBox.setAlignmentX(0f);
        JTextField limitField = styledField("Budget limit (e.g. 3000)");
        JButton expBtn = makeBtn("Set Expense Budget", ACCENT);
        expBtn.setAlignmentX(0f);
        expBtn.addActionListener(e -> {
            String val = limitField.getText().trim();
            if (val.isEmpty() || val.equals("Budget limit (e.g. 3000)")) { err("Enter a budget limit!"); return; }
            try {
                double limit = Double.parseDouble(val);
                if (limit <= 0) throw new NumberFormatException();
                String cat = (String) catBox.getSelectedItem();
                DatabaseHelper.setBudget(dash.userId, cat, limit, YearMonth.now().toString());
                limitField.setText("");
                JOptionPane.showMessageDialog(this, "Budget set for " + cat + "!", "Done", JOptionPane.INFORMATION_MESSAGE);
                refresh();
            } catch (NumberFormatException ex) { err("Enter a valid amount!"); }
        });

        panel.add(heading); panel.add(Box.createVerticalStrut(5)); panel.add(info);
        panel.add(Box.createVerticalStrut(18));
        panel.add(incomeHeading); panel.add(Box.createVerticalStrut(8));
        panel.add(flbl("Monthly Income Goal (" + dash.baseCurrency + ")")); panel.add(Box.createVerticalStrut(5)); panel.add(incomeField);
        panel.add(Box.createVerticalStrut(10)); panel.add(incomeBtn);
        panel.add(Box.createVerticalStrut(16)); panel.add(sep); panel.add(Box.createVerticalStrut(16));
        panel.add(expHeading); panel.add(Box.createVerticalStrut(8));
        panel.add(flbl("Category")); panel.add(Box.createVerticalStrut(5)); panel.add(catBox);
        panel.add(Box.createVerticalStrut(12));
        panel.add(flbl("Budget Limit (" + dash.baseCurrency + ")")); panel.add(Box.createVerticalStrut(5)); panel.add(limitField);
        panel.add(Box.createVerticalStrut(14)); panel.add(expBtn);
        return panel;
    }

    private JPanel buildTable() {
        JPanel panel = new JPanel(new BorderLayout()); panel.setBackground(CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FBRD, 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        JLabel heading = new JLabel("Budget Status — " + YearMonth.now().toString());
        heading.setFont(new Font("Arial", Font.BOLD, 15)); heading.setForeground(TEXT);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        String[] cols = {"Category", "Budget (" + dash.baseCurrency + ")", "Spent (" + dash.baseCurrency + ")", "Remaining", "Status"};
        tableModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };

        JTable table = new JTable(tableModel);
        table.setFont(new Font("Arial", Font.PLAIN, 12)); table.setRowHeight(34);
        table.setBackground(new Color(20, 20, 45)); table.setForeground(TEXT); table.setGridColor(FBRD);
        table.setSelectionBackground(ACCENT.darker()); table.setShowHorizontalLines(true); table.setShowVerticalLines(false);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(35, 35, 65)); table.getTableHeader().setForeground(SUB);

        // Status column
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                String val = v == null ? "" : v.toString();
                setHorizontalAlignment(CENTER);
                setFont(new Font("Arial", Font.BOLD, 12));
                if (val.contains("OVER"))      { setForeground(RED);    setBackground(new Color(60, 15, 15)); }
                else if (val.contains("WARN")) { setForeground(YELLOW); setBackground(new Color(50, 40, 10)); }
                else                           { setForeground(GREEN);  setBackground(new Color(10, 40, 20)); }
                if (sel) setBackground(ACCENT.darker());
                return this;
            }
        });

        DefaultTableCellRenderer def = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setForeground(TEXT); setBackground(sel ? ACCENT.darker() : new Color(20, 20, 45)); return this;
            }
        };
        DefaultTableCellRenderer ra = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(RIGHT); setForeground(TEXT); setBackground(sel ? ACCENT.darker() : new Color(20, 20, 45)); return this;
            }
        };
        table.getColumnModel().getColumn(0).setCellRenderer(def);
        for (int i = 1; i <= 3; i++) table.getColumnModel().getColumn(i).setCellRenderer(ra);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(new Color(20, 20, 45)); scroll.setBorder(null);
        panel.add(heading, BorderLayout.NORTH); panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    public void refresh() {
        tableModel.setRowCount(0);
        String month = YearMonth.now().toString();
        double totalBudget = 0, totalSpent = 0;

        double incomeGoal   = DatabaseHelper.getMonthlyIncomeBudget(dash.userId, month);
        double actualIncome = DatabaseHelper.getTotal(dash.userId, "INCOME");
        if (incomeGoal > 0) {
            double diff = incomeGoal - actualIncome;
            tableModel.addRow(new Object[]{"Income Goal", FMT.format(incomeGoal), FMT.format(actualIncome),
                actualIncome >= incomeGoal ? "REACHED!" : FMT.format(diff) + " to go",
                actualIncome >= incomeGoal ? "GOAL MET" : "In Progress"});
        }

        try {
            ResultSet rs = DatabaseHelper.getBudgets(dash.userId, month);
            while (rs.next()) {
                String cat = rs.getString("category");
                if (cat.equals("__INCOME__")) continue;
                double limit = rs.getDouble("monthly_limit"), spent = rs.getDouble("spent");
                double remaining = limit - spent;
                totalBudget += limit; totalSpent += spent;
                String status = spent > limit ? "OVER BUDGET!" : spent >= limit * 0.8 ? "WARN: 80%+" : "OK";
                tableModel.addRow(new Object[]{cat, FMT.format(limit), FMT.format(spent),
                    remaining >= 0 ? FMT.format(remaining) : "-" + FMT.format(-remaining), status});
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (totalBudget > 0) {
            double pct = totalSpent / totalBudget * 100;
            summaryLabel.setText(String.format("Total: %s %.2f / %.2f  (%.1f%%)", dash.baseCurrency, totalSpent, totalBudget, pct));
            summaryLabel.setForeground(totalSpent > totalBudget ? RED : totalSpent >= totalBudget * 0.8 ? YELLOW : GREEN);
        } else {
            summaryLabel.setText("No budgets set yet"); summaryLabel.setForeground(SUB);
        }
    }

    private void err(String m) { JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE); }
    private JLabel flbl(String t) { JLabel l = new JLabel(t); l.setFont(new Font("Arial", Font.BOLD, 11)); l.setForeground(SUB); l.setAlignmentX(0f); return l; }
}
