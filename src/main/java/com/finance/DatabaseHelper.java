package com.finance;

import java.sql.*;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:finance_tracker.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    base_currency TEXT DEFAULT 'BDT',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    type TEXT NOT NULL CHECK(type IN ('INCOME','EXPENSE')),
                    category TEXT NOT NULL,
                    amount REAL NOT NULL,
                    currency TEXT NOT NULL DEFAULT 'BDT',
                    amount_in_base REAL NOT NULL DEFAULT 0,
                    description TEXT,
                    date DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS budgets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    monthly_limit REAL NOT NULL,
                    month TEXT NOT NULL,
                    UNIQUE(user_id, category, month),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── User ──────────────────────────────────────────────────────────────────
    public static boolean registerUser(String username, String password) {
        try (Connection conn = getConnection();
             PreparedStatement p = conn.prepareStatement("INSERT INTO users (username,password) VALUES (?,?)")) {
            p.setString(1, username); p.setString(2, password);
            p.executeUpdate(); return true;
        } catch (SQLException e) { return false; }
    }

    public static int loginUser(String username, String password) {
        try (Connection conn = getConnection();
             PreparedStatement p = conn.prepareStatement("SELECT id FROM users WHERE username=? AND password=?")) {
            p.setString(1, username); p.setString(2, password);
            ResultSet rs = p.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public static String getBaseCurrency(int userId) {
        try (Connection conn = getConnection();
             PreparedStatement p = conn.prepareStatement("SELECT base_currency FROM users WHERE id=?")) {
            p.setInt(1, userId);
            ResultSet rs = p.executeQuery();
            if (rs.next()) return rs.getString("base_currency");
        } catch (SQLException e) { e.printStackTrace(); }
        return "BDT";
    }

    public static void setBaseCurrency(int userId, String currency) {
        try (Connection conn = getConnection();
             PreparedStatement p = conn.prepareStatement("UPDATE users SET base_currency=? WHERE id=?")) {
            p.setString(1, currency); p.setInt(2, userId); p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Transactions ──────────────────────────────────────────────────────────
    public static boolean addTransaction(int userId, String type, String category,
                                          double amount, String currency, double amountInBase, String description) {
        try (Connection conn = getConnection();
             PreparedStatement p = conn.prepareStatement(
                "INSERT INTO transactions (user_id,type,category,amount,currency,amount_in_base,description) VALUES (?,?,?,?,?,?,?)")) {
            p.setInt(1, userId); p.setString(2, type); p.setString(3, category);
            p.setDouble(4, amount); p.setString(5, currency);
            p.setDouble(6, amountInBase); p.setString(7, description);
            p.executeUpdate(); return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static ResultSet getTransactionsByType(int userId, String type) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement p = conn.prepareStatement(
            "SELECT * FROM transactions WHERE user_id=? AND type=? ORDER BY date DESC");
        p.setInt(1, userId); p.setString(2, type);
        return p.executeQuery();
    }

    public static ResultSet getAllTransactions(int userId) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement p = conn.prepareStatement(
            "SELECT * FROM transactions WHERE user_id=? ORDER BY date DESC");
        p.setInt(1, userId);
        return p.executeQuery();
    }

    public static double getTotal(int userId, String type) {
        try (Connection conn = getConnection();
             PreparedStatement p = conn.prepareStatement(
                "SELECT SUM(amount_in_base) FROM transactions WHERE user_id=? AND type=?")) {
            p.setInt(1, userId); p.setString(2, type);
            ResultSet rs = p.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public static ResultSet getCategoryExpenses(int userId) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement p = conn.prepareStatement("""
            SELECT category, SUM(amount_in_base) as total
            FROM transactions WHERE user_id=? AND type='EXPENSE'
            GROUP BY category ORDER BY total DESC
        """);
        p.setInt(1, userId);
        return p.executeQuery();
    }

    public static ResultSet getMonthlyData(int userId) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement p = conn.prepareStatement("""
            SELECT strftime('%Y-%m', date) as month,
                   SUM(CASE WHEN type='INCOME' THEN amount_in_base ELSE 0 END) as income,
                   SUM(CASE WHEN type='EXPENSE' THEN amount_in_base ELSE 0 END) as expense
            FROM transactions WHERE user_id=?
            GROUP BY month ORDER BY month DESC LIMIT 6
        """);
        p.setInt(1, userId);
        return p.executeQuery();
    }

    // ── Budget ────────────────────────────────────────────────────────────────
    public static boolean setBudget(int userId, String category, double limit, String month) {
        try (Connection conn = getConnection();
             PreparedStatement p = conn.prepareStatement("""
                INSERT INTO budgets (user_id,category,monthly_limit,month) VALUES (?,?,?,?)
                ON CONFLICT(user_id,category,month) DO UPDATE SET monthly_limit=excluded.monthly_limit
             """)) {
            p.setInt(1, userId); p.setString(2, category);
            p.setDouble(3, limit); p.setString(4, month);
            p.executeUpdate(); return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static ResultSet getBudgets(int userId, String month) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement p = conn.prepareStatement("""
            SELECT b.category, b.monthly_limit,
                   COALESCE(SUM(t.amount_in_base),0) as spent
            FROM budgets b
            LEFT JOIN transactions t ON t.user_id=b.user_id
                AND t.category=b.category AND t.type='EXPENSE'
                AND strftime('%Y-%m', t.date)=b.month
            WHERE b.user_id=? AND b.month=?
            GROUP BY b.category, b.monthly_limit
        """);
        p.setInt(1, userId); p.setString(2, month);
        return p.executeQuery();
    }

    public static double getMonthlyIncomeBudget(int userId, String month) {
        try (Connection conn = getConnection();
             PreparedStatement p = conn.prepareStatement(
                "SELECT monthly_limit FROM budgets WHERE user_id=? AND category='__INCOME__' AND month=?")) {
            p.setInt(1, userId); p.setString(2, month);
            ResultSet rs = p.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}
