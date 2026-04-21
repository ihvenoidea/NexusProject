package main_plugin.database;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
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
            
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + 
                         "?useSSL=false" +
                         "&allowPublicKeyRetrieval=true" +
                         "&serverTimezone=Asia/Seoul" +
                         "&characterEncoding=UTF-8" +
                         "&autoReconnect=true" +
                         "&createDatabaseIfNotExist=true";
                         
            connection = DriverManager.getConnection(url, user, password);
            createTable();
            plugin.getLogger().info("Successfully connected to MySQL database.");
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe("DB 연결 실패! 설정(비밀번호, 포트)을 확인하세요.");
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    private void createTable() {
        // 1. 유저 기본 데이터 테이블 (+ 생활 숙련도 컬럼 추가)
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                     "uuid VARCHAR(36) PRIMARY KEY, " +
                     "name VARCHAR(16), " +
                     "discord_id VARCHAR(32), " +
                     "last_attendance VARCHAR(15), " +
                     "money DOUBLE DEFAULT 0, " +
                     "points INT DEFAULT 0, " +
                     "collection_data TEXT, " +
                     "total_points INT DEFAULT 0, " +
                     "reward_tier INT DEFAULT 0, " +
                     "augments TEXT, " +
                     "total_tribute DOUBLE DEFAULT 0, " +
                     "unlocked_boxes INT DEFAULT 1, " + 
                     "job VARCHAR(16) DEFAULT 'NONE', " +  // [신규] 직업 저장
                     "job_exp BIGINT DEFAULT 0" +          // [신규] 100레벨 경험치를 버티기 위해 BIGINT 사용
                     ");";
                     
        // 2. 우편함 테이블
        String sqlMail = "CREATE TABLE IF NOT EXISTS user_mail (" +
                     "id INT AUTO_INCREMENT PRIMARY KEY, " +
                     "receiver VARCHAR(16) NOT NULL, " +
                     "item_data LONGTEXT NOT NULL, " +
                     "message VARCHAR(255), " +
                     "sent_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                     ");";

        // 3. 창고 아이템 저장 테이블
        String sqlStorage = "CREATE TABLE IF NOT EXISTS user_storage (" +
                     "uuid VARCHAR(36), " +
                     "box_index INT, " +
                     "items LONGTEXT, " +
                     "PRIMARY KEY(uuid, box_index)" +
                     ");";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlUsers);
            stmt.execute(sqlMail); 
            stmt.execute(sqlStorage);

            // [마이그레이션] 기존 유저 테이블에 신규 컬럼 자동 추가 방어 로직
            try { stmt.execute("ALTER TABLE users ADD COLUMN discord_id VARCHAR(32) AFTER name;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN last_attendance VARCHAR(15) AFTER discord_id;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE user_mail MODIFY COLUMN item_data LONGTEXT;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN augments TEXT;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN total_tribute DOUBLE DEFAULT 0;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN unlocked_boxes INT DEFAULT 1;"); } catch (SQLException ignored) {}
            // [신규 마이그레이션]
            try { stmt.execute("ALTER TABLE users ADD COLUMN job VARCHAR(16) DEFAULT 'NONE';"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN job_exp BIGINT DEFAULT 0;"); } catch (SQLException ignored) {}
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void syncBalanceFromDB(UUID uuid) {
        String sql = "SELECT money, points FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                double dbMoney = rs.getDouble("money");
                int dbPoints = rs.getInt("points");
                
                plugin.getUserManager().getUser(uuid).ifPresent(u -> {
                    u.setMoney(dbMoney);
                    u.setPoints(dbPoints);
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                plugin.getUserManager().getUser(UUID.fromString(uuid)).ifPresent(u -> u.setPoints(u.getPoints() - amount));
            }
            return success;
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

    public void loadUserData(UUID uuid) {
        String sql = "SELECT * FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                UserData userData = new UserData(uuid, rs.getString("name"), rs.getDouble("money"), rs.getInt("points"));
                
                String augmentsStr = rs.getString("augments");
                if (augmentsStr != null && !augmentsStr.isEmpty()) {
                    userData.setAugments(new ArrayList<>(Arrays.asList(augmentsStr.split(","))));
                }
                
                userData.setTotalTribute(rs.getDouble("total_tribute"));
                userData.setUnlockedBoxes(rs.getInt("unlocked_boxes"));

                // [신규] 직업 및 경험치 데이터 로드
                String job = rs.getString("job");
                userData.setJob(job != null ? job : "NONE");
                userData.setJobExp(rs.getLong("job_exp"));

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

            final int points = data.getPoints();
            final String colData = collectionData.toDataString();
            final int totalPoints = collectionData.getTotalPoints();
            final int rewardTier = collectionData.getRewardTier();
            final String augments = String.join(",", data.getAugments());
            final double tribute = data.getTotalTribute();
            final int boxCount = data.getUnlockedBoxes();
            
            // [신규] 직업 데이터 추가
            final String job = data.getJob();
            final long jobExp = data.getJobExp();
            final String uuidStr = uuid.toString();

            Runnable saveTask = () -> {
                String sql = "UPDATE users SET points = ?, collection_data = ?, total_points = ?, " +
                             "reward_tier = ?, augments = ?, total_tribute = ?, unlocked_boxes = ?, " +
                             "job = ?, job_exp = ? WHERE uuid = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setInt(1, points);
                    pstmt.setString(2, colData);
                    pstmt.setInt(3, totalPoints);
                    pstmt.setInt(4, rewardTier);
                    pstmt.setString(5, augments);
                    pstmt.setDouble(6, tribute);
                    pstmt.setInt(7, boxCount);
                    pstmt.setString(8, job);     // [신규]
                    pstmt.setLong(9, jobExp);    // [신규]
                    pstmt.setString(10, uuidStr);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            };

            if (plugin.isEnabled()) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, saveTask);
            } else {
                saveTask.run();
            }
        });
    }

    private void createNewUser(UUID uuid, String name) {
        String sql = "INSERT INTO users (uuid, name, money, points, collection_data, total_points, " +
                     "reward_tier, augments, total_tribute, unlocked_boxes, job, job_exp) " +
                     "VALUES (?, ?, 0, 0, '', 0, 0, '', 0, 1, 'NONE', 0)";
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

    public int getMailCount(String playerName) {
        String sql = "SELECT COUNT(*) FROM user_mail WHERE receiver = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}