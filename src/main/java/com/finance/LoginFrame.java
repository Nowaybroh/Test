package com.finance;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {
    private static final Color BG     = new Color(15,15,30);
    private static final Color CARD   = new Color(25,25,50);
    private static final Color ACCENT = new Color(99,102,241);
    private static final Color GREEN  = new Color(34,197,94);
    private static final Color TEXT   = new Color(220,220,255);
    private static final Color SUB    = new Color(130,130,180);
    private static final Color FBG    = new Color(35,35,65);
    private static final Color FBRD   = new Color(60,60,100);

    private JTabbedPane tabs;
    private JTextField loginUser, signupUser;
    private JPasswordField loginPass, signupPass, signupConfirm;

    public LoginFrame() {
        setTitle("Finance Tracker"); setSize(520,620);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(440,540));
        setLocationRelativeTo(null); setResizable(true); initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(30,40,30,40));

        JPanel hdr = new JPanel(); hdr.setLayout(new BoxLayout(hdr,BoxLayout.Y_AXIS)); hdr.setOpaque(false);
        JLabel ico = new JLabel("$"); ico.setFont(new Font("Arial",Font.BOLD,52)); ico.setForeground(ACCENT); ico.setAlignmentX(.5f);
        JLabel ttl = new JLabel("Finance Tracker"); ttl.setFont(new Font("Arial",Font.BOLD,26)); ttl.setForeground(TEXT); ttl.setAlignmentX(.5f);
        JLabel sub = new JLabel("Track money in any currency"); sub.setFont(new Font("Arial",Font.PLAIN,13)); sub.setForeground(SUB); sub.setAlignmentX(.5f);
        hdr.add(ico); hdr.add(Box.createVerticalStrut(6)); hdr.add(ttl); hdr.add(Box.createVerticalStrut(4)); hdr.add(sub); hdr.add(Box.createVerticalStrut(25));

        tabs = new JTabbedPane();
        tabs.setFont(new Font("Arial",Font.BOLD,13)); tabs.setBackground(CARD); tabs.setForeground(TEXT);
        tabs.addTab("  Login  ", loginPanel());
        tabs.addTab("  Sign Up  ", signupPanel());
        root.add(hdr,BorderLayout.NORTH); root.add(tabs,BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel loginPanel() {
        JPanel p = panel();
        loginUser = field("Enter username"); loginPass = passField("Enter password");
        JButton btn = btn("Login", ACCENT); btn.addActionListener(e -> doLogin());
        loginPass.addKeyListener(new KeyAdapter(){ public void keyPressed(KeyEvent e){ if(e.getKeyCode()==KeyEvent.VK_ENTER) doLogin(); }});
        p.add(lbl("Username")); p.add(Box.createVerticalStrut(6)); p.add(loginUser);
        p.add(Box.createVerticalStrut(15)); p.add(lbl("Password")); p.add(Box.createVerticalStrut(6)); p.add(loginPass);
        p.add(Box.createVerticalStrut(25)); p.add(btn); return p;
    }

    private JPanel signupPanel() {
        JPanel p = panel();
        signupUser = field("Choose username"); signupPass = passField("Choose password"); signupConfirm = passField("Confirm password");
        JButton btn = btn("Create Account", GREEN); btn.addActionListener(e -> doSignup());
        p.add(lbl("Username")); p.add(Box.createVerticalStrut(5)); p.add(signupUser);
        p.add(Box.createVerticalStrut(12)); p.add(lbl("Password")); p.add(Box.createVerticalStrut(5)); p.add(signupPass);
        p.add(Box.createVerticalStrut(12)); p.add(lbl("Confirm Password")); p.add(Box.createVerticalStrut(5)); p.add(signupConfirm);
        p.add(Box.createVerticalStrut(20)); p.add(btn); return p;
    }

    private void doLogin() {
        String u = loginUser.getText().trim(), pw = new String(loginPass.getPassword()).trim();
        if (u.isEmpty()||pw.isEmpty()) { err("Please enter username and password!"); return; }
        int id = DatabaseHelper.loginUser(u,pw);
        if (id==-1) err("Wrong username or password!");
        else { dispose(); new DashboardFrame(id,u).setVisible(true); }
    }

    private void doSignup() {
        String u=signupUser.getText().trim(), pw=new String(signupPass.getPassword()).trim(), c=new String(signupConfirm.getPassword()).trim();
        if (u.isEmpty()||pw.isEmpty()) { err("Please fill all fields!"); return; }
        if (pw.length()<4) { err("Password must be at least 4 characters!"); return; }
        if (!pw.equals(c)) { err("Passwords do not match!"); return; }
        if (DatabaseHelper.registerUser(u,pw)) {
            JOptionPane.showMessageDialog(this,"Account created! Please login.","Success",JOptionPane.INFORMATION_MESSAGE);
            tabs.setSelectedIndex(0); loginUser.setText(u);
        } else err("Username already taken!");
    }

    private void err(String m){ JOptionPane.showMessageDialog(this,m,"Error",JOptionPane.ERROR_MESSAGE); }
    private JPanel panel(){ JPanel p=new JPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.setBackground(CARD); p.setBorder(BorderFactory.createEmptyBorder(25,25,25,25)); return p; }
    private JLabel lbl(String t){ JLabel l=new JLabel(t); l.setFont(new Font("Arial",Font.BOLD,12)); l.setForeground(SUB); l.setAlignmentX(0f); return l; }
    private JTextField field(String ph){
        JTextField f=new JTextField(); f.setFont(new Font("Arial",Font.PLAIN,14)); f.setBackground(FBG); f.setForeground(SUB); f.setCaretColor(TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(FBRD,1,true),BorderFactory.createEmptyBorder(8,12,8,12)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE,42)); f.setText(ph);
        f.addFocusListener(new FocusAdapter(){
            public void focusGained(FocusEvent e){ if(f.getText().equals(ph)){f.setText("");f.setForeground(TEXT);} }
            public void focusLost(FocusEvent e){ if(f.getText().isEmpty()){f.setText(ph);f.setForeground(SUB);} }
        }); return f;
    }
    private JPasswordField passField(String ph){
        JPasswordField f=new JPasswordField(); f.setFont(new Font("Arial",Font.PLAIN,14)); f.setBackground(FBG); f.setForeground(SUB); f.setCaretColor(TEXT);
        f.setEchoChar((char)0); f.setText(ph);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(FBRD,1,true),BorderFactory.createEmptyBorder(8,12,8,12)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));
        f.addFocusListener(new FocusAdapter(){
            public void focusGained(FocusEvent e){ if(new String(f.getPassword()).equals(ph)){f.setText("");f.setForeground(TEXT);f.setEchoChar('*');} }
            public void focusLost(FocusEvent e){ if(f.getPassword().length==0){f.setEchoChar((char)0);f.setText(ph);f.setForeground(SUB);} }
        }); return f;
    }
    private JButton btn(String text, Color color){
        JButton b=new JButton(text){
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed()?color.darker():getModel().isRollover()?color.brighter():color);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(TEXT); g2.setFont(getFont()); FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2); g2.dispose();
            }
        };
        b.setFont(new Font("Arial",Font.BOLD,14)); b.setMaximumSize(new Dimension(Integer.MAX_VALUE,44)); b.setAlignmentX(.5f);
        b.setBorderPainted(false); b.setContentAreaFilled(false); b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
