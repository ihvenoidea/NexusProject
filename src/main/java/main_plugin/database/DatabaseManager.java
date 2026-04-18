package main_plugin.database;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {

    private final NexusCore plugin;
    private Connection connection;

    public DatabaseManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    public void connect(String host, int port, String database, String user, String password) {
        try {
            if (connection != null && !connection.isClosed()) return;

            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true",
                user, password
            );
            createTable();
            plugin.getLogger().info("Successfully connected to MySQL database.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    private void createTable() {
        // [수정됨] 디스코드 봇 연동을 위한 discord_id, last_attendance 컬럼 추가
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                     "uuid VARCHAR(36) PRIMARY KEY, " +
                     "name VARCHAR(16), " +
                     "discord_id VARCHAR(32), " +
                     "last_attendance VARCHAR(15), " +
                     "money DOUBLE DEFAULT 0, " +
                     "points INT DEFAULT 0, " +
                     "collection_data TEXT, " +
                     "total_points INT DEFAULT 0, " +
                     "reward_tier INT DEFAULT 0" +
                     ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- [컴파일 에러 해결] 기존 시스템 호환용 메서드 (String 기반) ---

    public int getDiscordPoints(String uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT points FROM users WHERE uuid = ?")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("points");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public boolean deductDiscordPoints(String uuid, int amount) {
        if (getDiscordPoints(uuid) < amount) return false;
        try (PreparedStatement ps = connection.prepareStatement("UPDATE users SET points = points - ? WHERE uuid = ?")) {
            ps.setInt(1, amount);
            ps.setString(2, uuid);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public double getMoney(String uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT money FROM users WHERE uuid = ?")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("money");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public boolean addMoney(String uuid, double amount) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE users SET money = money + ? WHERE uuid = ?")) {
            ps.setDouble(1, amount);
            ps.setString(2, uuid);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deductMoney(String uuid, double amount) {
        if (getMoney(uuid) < amount) return false;
        return addMoney(uuid, -amount);
    }

    public boolean setMoney(String uuid, double amount) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE users SET money = ? WHERE uuid = ?")) {
            ps.setDouble(1, amount);
            ps.setString(2, uuid);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public void setupPlayer(String uuid, String name) {
        loadUserData(UUID.fromString(uuid));
    }

    // --- 데이터 로드 및 저장 (신규 시스템) ---

    public void loadUserData(UUID uuid) {
        String sql = "SELECT * FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                UserData userData = new UserData(uuid, rs.getString("name"), rs.getDouble("money"), rs.getInt("points"));
                plugin.getUserManager().addUser(userData);
                
                String collectionStr = rs.getString("collection_data");
                int totalPoints = rs.getInt("total_points");
                int rewardTier = rs.getInt("reward_tier");
                
                plugin.getCollectionManager().loadUserData(
                    uuid, 
                    collectionStr != null ? collectionStr : "", 
                    totalPoints, 
                    rewardTier
                );
            } else {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                createNewUser(uuid, name != null ? name : "Unknown");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveUserData(UUID uuid) {
        plugin.getUserManager().getUser(uuid).ifPresent(data -> {
            main_plugin.collection.CollectionData collectionData = plugin.getCollectionManager().getCollectionData(uuid);
            if (collectionData == null) return;

            String sql = "UPDATE users SET money = ?, points = ?, collection_data = ?, total_points = ?, reward_tier = ? WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setDouble(1, data.getMoney());
                pstmt.setInt(2, data.getPoints());
                pstmt.setString(3, collectionData.toDataString());
                pstmt.setInt(4, collectionData.getTotalPoints());
                pstmt.setInt(5, collectionData.getRewardTier());
                pstmt.setString(6, uuid.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void createNewUser(UUID uuid, String name) {
        String sql = "INSERT INTO users (uuid, name, money, points, collection_data, total_points, reward_tier) VALUES (?, ?, 0, 0, '', 0, 0)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, name);
            pstmt.executeUpdate();
            loadUserData(uuid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}