package com.finance;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        DatabaseHelper.initializeDatabase();
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
            new LoginFrame().setVisible(true);
        });
    }
}
