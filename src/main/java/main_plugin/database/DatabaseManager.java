package main_plugin.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import main_plugin.NexusCore;

import java.sql.*;

public class DatabaseManager {
    private final NexusCore plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    public void connect(String host, int port, String database, String user, String password) {
        try {
            HikariConfig config = new HikariConfig();
            config.setDriverClassName("org.mariadb.jdbc.Driver");
            config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database);
            config.setUsername(user);
            config.setPassword(password);

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.setMaximumPoolSize(10);

            this.dataSource = new HikariDataSource(config);
            plugin.getLogger().info("✔ MariaDB 연결 성공!");
        } catch (Exception e) {
            plugin.getLogger().severe("❌ DB 연결 실패: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("HikariDataSource가 null입니다. DB 연결 상태를 확인하세요.");
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) dataSource.close();
    }

    // ==========================================
    // [ 유저 초기화 ] 유저가 없을 경우 자동 생성
    // ==========================================
    public void setupPlayer(String uuid, String name) {
        // INSERT IGNORE를 사용하여 이미 존재하면 무시하고, 없으면 새로 생성합니다.
        String sql = "INSERT IGNORE INTO users (uuid, username, money, discord_points) VALUES (?, ?, 0, 0)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
            plugin.getLogger().info("[DB-INIT] 유저 데이터 확인/생성 완료: " + name);
        } catch (SQLException e) {
            plugin.getLogger().warning("[DB-INIT] 유저 등록 중 오류 발생: " + e.getMessage());
        }
    }

    // ==========================================
    // [ 경제 시스템 ] 돈 관련 메서드 (로그 포함)
    // ==========================================

    public double getMoney(String uuid) {
        String sql = "SELECT money FROM users WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("money");
        } catch (SQLException e) {
            plugin.getLogger().warning("[ECON-DEBUG] 잔액 조회 실패: " + e.getMessage());
        }
        return 0.0;
    }

    public boolean addMoney(String uuid, double amount) {
        plugin.getLogger().info("[ECON-DEBUG] 지급 시도 -> UUID: " + uuid + " | 금액: " + amount);
        String sql = "UPDATE users SET money = money + ? WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, uuid);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                plugin.getLogger().info("[ECON-DEBUG] 지급 성공! (수정된 행: " + affectedRows + ")");
                return true;
            } else {
                plugin.getLogger().warning("[ECON-DEBUG] 지급 실패: DB에 UUID(" + uuid + ") 유저가 없습니다.");
                return false;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[ECON-DEBUG] SQL 에러: " + e.getMessage());
            return false;
        }
    }

    public boolean deductMoney(String uuid, double amount) {
        if (getMoney(uuid) < amount) return false;
        String sql = "UPDATE users SET money = money - ? WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, uuid);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean setMoney(String uuid, double amount) {
        String sql = "UPDATE users SET money = ? WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, uuid);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // ==========================================
    // [ 포인트 시스템 ] 포인트 관련 메서드
    // ==========================================

    public int getDiscordPoints(String uuid) {
        String sql = "SELECT discord_points FROM users WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("discord_points");
        } catch (SQLException e) {
            plugin.getLogger().warning("[POINT-DEBUG] 포인트 조회 실패: " + e.getMessage());
        }
        return 0;
    }

    public boolean deductDiscordPoints(String uuid, int amount) {
        if (getDiscordPoints(uuid) < amount) return false;
        String sql = "UPDATE users SET discord_points = discord_points - ? WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            pstmt.setString(2, uuid);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("[POINT-DEBUG] 포인트 차감 중 에러: " + e.getMessage());
            return false;
        }
    }
}