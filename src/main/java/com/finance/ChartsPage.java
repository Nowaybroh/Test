package com.finance;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

import static com.finance.DashboardFrame.*;

public class ChartsPage extends JPanel {
    private final DashboardFrame dash;
    private PiePanel piePanel;
    private BarPanel barPanel;
    private static final DecimalFormat FMT = new DecimalFormat("#,##0.00");

    public ChartsPage(DashboardFrame dash) {
        this.dash = dash;
        setBackground(BG);
        setLayout(new GridLayout(1,2,15,0));
        setBorder(BorderFactory.createEmptyBorder(18,18,18,18));
        piePanel = new PiePanel(); barPanel = new BarPanel();
        add(wrap("Expense Breakdown by Category", piePanel));
        add(wrap("Monthly Income vs Expense", barPanel));
    }

    private JPanel wrap(String title, JPanel content) {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(FBRD,1,true),BorderFactory.createEmptyBorder(15,15,15,15)));
        JLabel lbl = new JLabel(title); lbl.setFont(new Font("Arial",Font.BOLD,14)); lbl.setForeground(TEXT);
        lbl.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
        p.add(lbl,BorderLayout.NORTH); p.add(content,BorderLayout.CENTER); return p;
    }

    public void refresh() {
        piePanel.loadData(); barPanel.loadData(); repaint();
    }

    // ── Pie Chart ─────────────────────────────────────────────────────────────
    class PiePanel extends JPanel {
        private final List<String> labels = new ArrayList<>();
        private final List<Double> values = new ArrayList<>();
        private static final Color[] COLORS = {
            new Color(99,102,241),new Color(34,197,94),new Color(239,68,68),
            new Color(251,191,36),new Color(59,130,246),new Color(236,72,153),
            new Color(20,184,166),new Color(249,115,22),new Color(168,85,247),new Color(16,185,129)
        };
        PiePanel() { setBackground(CARD); }

        void loadData() {
            labels.clear(); values.clear();
            try {
                ResultSet rs = DatabaseHelper.getCategoryExpenses(dash.userId);
                while (rs.next()) { labels.add(rs.getString("category")); values.add(rs.getDouble("total")); }
            } catch (SQLException e) { e.printStackTrace(); }
            repaint();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();
            if (labels.isEmpty()) {
                g2.setColor(SUB); g2.setFont(new Font("Arial",Font.PLAIN,13));
                String m="No expenses yet"; g2.drawString(m,(w-g2.getFontMetrics().stringWidth(m))/2,h/2); return;
            }
            double total = values.stream().mapToDouble(Double::doubleValue).sum();
            int sz = Math.min(w*2/3-20, h-80); int px=10, py=(h-sz)/2;
            double angle=0;
            for (int i=0;i<values.size();i++) {
                double sweep = (values.get(i)/total)*360.0;
                g2.setColor(COLORS[i%COLORS.length]);
                g2.fill(new Arc2D.Double(px,py,sz,sz,angle,sweep,Arc2D.PIE));
                g2.setColor(CARD); g2.setStroke(new BasicStroke(2));
                g2.draw(new Arc2D.Double(px,py,sz,sz,angle,sweep,Arc2D.PIE));
                angle+=sweep;
            }
            int lx=px+sz+15, ly=py+10;
            for (int i=0;i<labels.size();i++) {
                if (ly>h-20) break;
                g2.setColor(COLORS[i%COLORS.length]); g2.fillRoundRect(lx,ly-9,11,11,3,3);
                g2.setColor(TEXT); g2.setFont(new Font("Arial",Font.BOLD,11)); g2.drawString(labels.get(i),lx+15,ly);
                g2.setColor(SUB); g2.setFont(new Font("Arial",Font.PLAIN,10));
                double pct = values.get(i)/total*100;
                g2.drawString(String.format("%.1f%%  %s %s",pct,dash.baseCurrency,FMT.format(values.get(i))),lx+15,ly+13);
                ly+=32;
            }
        }
    }

    // ── Bar Chart ─────────────────────────────────────────────────────────────
    class BarPanel extends JPanel {
        private final List<String> months = new ArrayList<>();
        private final List<Double> incomes = new ArrayList<>();
        private final List<Double> expenses = new ArrayList<>();

        BarPanel() { setBackground(CARD); }

        void loadData() {
            months.clear(); incomes.clear(); expenses.clear();
            try {
                ResultSet rs = DatabaseHelper.getMonthlyData(dash.userId);
                List<String> ms=new ArrayList<>(); List<Double> is=new ArrayList<>(),es=new ArrayList<>();
                while (rs.next()) { ms.add(rs.getString("month")); is.add(rs.getDouble("income")); es.add(rs.getDouble("expense")); }
                Collections.reverse(ms); Collections.reverse(is); Collections.reverse(es);
                months.addAll(ms); incomes.addAll(is); expenses.addAll(es);
            } catch (SQLException e) { e.printStackTrace(); }
            repaint();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();
            if (months.isEmpty()) {
                g2.setColor(SUB); g2.setFont(new Font("Arial",Font.PLAIN,13));
                String m="No data yet"; g2.drawString(m,(w-g2.getFontMetrics().stringWidth(m))/2,h/2); return;
            }
            int pad=50, chartW=w-pad*2, chartH=h-pad*2;
            double maxVal = Math.max(incomes.stream().mapToDouble(Double::doubleValue).max().orElse(1),
                                     expenses.stream().mapToDouble(Double::doubleValue).max().orElse(1));
            int n=months.size(), barW=chartW/(n*2+1), gap=barW/2;
            // Axes
            g2.setColor(FBRD); g2.setStroke(new BasicStroke(1));
            g2.drawLine(pad,pad,pad,pad+chartH);
            g2.drawLine(pad,pad+chartH,pad+chartW,pad+chartH);
            // Bars
            for (int i=0;i<n;i++) {
                int x = pad + gap + i*(barW*2+gap);
                int ih=(int)(incomes.get(i)/maxVal*chartH);
                int eh=(int)(expenses.get(i)/maxVal*chartH);
                g2.setColor(GREEN); g2.fillRoundRect(x, pad+chartH-ih, barW, ih, 4, 4);
                g2.setColor(RED);   g2.fillRoundRect(x+barW, pad+chartH-eh, barW, eh, 4, 4);
                g2.setColor(SUB); g2.setFont(new Font("Arial",Font.PLAIN,9));
                String m=months.get(i).substring(5);
                g2.drawString(m, x+barW/2-4, pad+chartH+14);
            }
            // Legend
            g2.setColor(GREEN); g2.fillRect(w-100,10,12,12); g2.setColor(TEXT); g2.setFont(new Font("Arial",Font.PLAIN,11)); g2.drawString("Income",w-85,21);
            g2.setColor(RED);   g2.fillRect(w-100,28,12,12); g2.setColor(TEXT); g2.drawString("Expense",w-85,39);
        }
    }
}
