package main_plugin.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import main_plugin.NexusCore;
import main_plugin.user.UserData;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class DatabaseManager {

    private final NexusCore plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    public void connect(String host, int port, String database, String user, String password) {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=Asia/Seoul"
                    + "&characterEncoding=UTF-8"
                    + "&createDatabaseIfNotExist=true");
            config.setUsername(user);
            config.setPassword(password);

            // 풀 최적화 설정
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(10000); 
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setKeepaliveTime(60000);
            config.setPoolName("HikariPool-Nexus");

            dataSource = new HikariDataSource(config);
            createTable();
            plugin.getLogger().info("HikariCP 풀로 MySQL 연결 성공!");
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe("DB 연결 실패! 설정(비밀번호, 포트)을 확인하세요.");
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("DataSource가 초기화되지 않았습니다.");
        return dataSource.getConnection();
    }

    private void createTable() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 유저 데이터 테이블
            stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "name VARCHAR(16), "
                    + "money DOUBLE DEFAULT 0, "
                    + "points INT DEFAULT 0, "
                    + "collection_data TEXT, "
                    + "total_points INT DEFAULT 0, "
                    + "reward_tier INT DEFAULT 0, "
                    + "augments TEXT, "
                    + "total_tribute DOUBLE DEFAULT 0, "
                    + "unlocked_boxes INT DEFAULT 1, "
                    + "job VARCHAR(16) DEFAULT 'NONE', "
                    + "job_exp BIGINT DEFAULT 0, "
                    + "is_newbie INT DEFAULT 1" 
                    + ");");

            try {
                stmt.execute("ALTER TABLE users ADD COLUMN is_newbie INT DEFAULT 0"); 
            } catch (SQLException ignored) {}

            // 우편함 테이블
            stmt.execute("CREATE TABLE IF NOT EXISTS user_mail ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "receiver VARCHAR(16) NOT NULL, "
                    + "item_data LONGTEXT NOT NULL, "
                    + "message VARCHAR(255), "
                    + "sent_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ");");

            // 창고 테이블
            stmt.execute("CREATE TABLE IF NOT EXISTS user_storage ("
                    + "uuid VARCHAR(36), "
                    + "box_index INT, "
                    + "items LONGTEXT, "
                    + "PRIMARY KEY(uuid, box_index)"
                    + ");");
                    
            // [신규] 타운(부동산) 테이블
            stmt.execute("CREATE TABLE IF NOT EXISTS towns ("
                    + "owner_uuid VARCHAR(36) PRIMARY KEY, "
                    + "spawn_world VARCHAR(64), "
                    + "spawn_x DOUBLE, "
                    + "spawn_y DOUBLE, "
                    + "spawn_z DOUBLE, "
                    + "spawn_yaw FLOAT, "
                    + "spawn_pitch FLOAT, "
                    + "claimed_chunks LONGTEXT"
                    + ");");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean claimNewbiePackage(UUID uuid) {
        String selectSql = "SELECT is_newbie FROM users WHERE uuid = ?";
        String updateSql = "UPDATE users SET is_newbie = 0 WHERE uuid = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
            
            psSelect.setString(1, uuid.toString());
            try (ResultSet rs = psSelect.executeQuery()) {
                if (rs.next() && rs.getInt("is_newbie") == 1) {
                    try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                        psUpdate.setString(1, uuid.toString());
                        psUpdate.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; 
    }

    public void syncBalanceFromDB(UUID uuid) {
        String sql = "SELECT money, points FROM users WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double dbMoney = rs.getDouble("money");
                    int dbPoints = rs.getInt("points");
                    plugin.getUserManager().getUser(uuid).ifPresent(u -> {
                        u.setMoney(dbMoney);
                        u.setPoints(dbPoints);
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getDiscordPoints(String uuid) {
        String sql = "SELECT points FROM users WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("points");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public boolean deductDiscordPoints(String uuid, int amount) {
        if (getDiscordPoints(uuid) < amount) return false;

        String sql = "UPDATE users SET points = points - ? WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, amount);
            ps.setString(2, uuid);
            
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                plugin.getUserManager().getUser(UUID.fromString(uuid))
                        .ifPresent(user -> user.setPoints(user.getPoints() - amount));
            }
            return success;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public double getMoney(String uuid) {
        String sql = "SELECT money FROM users WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("money");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public boolean addMoney(String uuid, double amount) {
        String sql = "UPDATE users SET money = money + ? WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        String sql = "UPDATE users SET money = ? WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setString(2, uuid);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public void loadUserData(UUID uuid) {
        String sql = "SELECT * FROM users WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UserData userData = new UserData(uuid, rs.getString("name"),
                            rs.getDouble("money"), rs.getInt("points"));

                    String augmentsStr = rs.getString("augments");
                    if (augmentsStr != null && !augmentsStr.isEmpty()) {
                        userData.setAugments(new ArrayList<>(Arrays.asList(augmentsStr.split(","))));
                    }

                    userData.setTotalTribute(rs.getDouble("total_tribute"));
                    userData.setUnlockedBoxes(rs.getInt("unlocked_boxes"));

                    String job = rs.getString("job");
                    userData.setJob(job != null ? job : "NONE");
                    userData.setJobExp(rs.getLong("job_exp"));

                    plugin.getUserManager().addUser(userData);

                    plugin.getCollectionManager().loadUserData(
                            uuid,
                            rs.getString("collection_data"),
                            rs.getInt("total_points"),
                            rs.getInt("reward_tier")
                    );
                } else {
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    createNewUser(uuid, name != null ? name : "Unknown");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveUserData(UUID uuid) {
        plugin.getUserManager().getUser(uuid).ifPresent(data -> {
            String sql = "UPDATE users SET points = ?, collection_data = ?, total_points = ?, "
                       + "reward_tier = ?, augments = ?, total_tribute = ?, unlocked_boxes = ?, "
                       + "job = ?, job_exp = ? WHERE uuid = ?";
            
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, data.getPoints());
                pstmt.setString(2, plugin.getCollectionManager().getCollectionData(uuid).toDataString());
                pstmt.setInt(3, plugin.getCollectionManager().getCollectionData(uuid).getTotalPoints());
                pstmt.setInt(4, plugin.getCollectionManager().getCollectionData(uuid).getRewardTier());
                pstmt.setString(5, String.join(",", data.getAugments()));
                pstmt.setDouble(6, data.getTotalTribute());
                pstmt.setInt(7, data.getUnlockedBoxes());
                pstmt.setString(8, data.getJob());
                pstmt.setLong(9, data.getJobExp());
                pstmt.setString(10, uuid.toString());
                
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void createNewUser(UUID uuid, String name) {
        String sql = "INSERT INTO users (uuid, name, money, points, collection_data, total_points, "
                   + "reward_tier, augments, total_tribute, unlocked_boxes, job, job_exp, is_newbie) "
                   + "VALUES (?, ?, 0, 0, '', 0, 0, '', 0, 1, 'NONE', 0, 1)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, name);
            pstmt.executeUpdate();
            loadUserData(uuid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getMailCount(String playerName) {
        String sql = "SELECT COUNT(*) FROM user_mail WHERE receiver = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ==========================================
    // [신규] 타운(부동산) 시스템 DB 관리 로직
    // ==========================================
    public void loadAllTowns(main_plugin.town.TownManager tm) {
        String sql = "SELECT * FROM towns";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("owner_uuid"));
                org.bukkit.World world = Bukkit.getWorld(rs.getString("spawn_world"));
                if (world == null) continue;

                org.bukkit.Location loc = new org.bukkit.Location(world,
                        rs.getDouble("spawn_x"), rs.getDouble("spawn_y"), rs.getDouble("spawn_z"),
                        rs.getFloat("spawn_yaw"), rs.getFloat("spawn_pitch"));

                main_plugin.town.TownData town = new main_plugin.town.TownData(uuid, loc);
                String chunksStr = rs.getString("claimed_chunks");
                
                if (chunksStr != null && !chunksStr.isEmpty()) {
                    String[] chunks = chunksStr.split(";");
                    for (String c : chunks) {
                        if (!c.trim().isEmpty()) {
                            town.addChunk(c);
                            tm.getChunkMap().put(c, uuid); // 빠른 조회를 위해 캐시에 등록
                        }
                    }
                }
                tm.getTowns().put(uuid, town);
            }
            plugin.getLogger().info("[NexusCore] DB에서 모든 타운 데이터를 성공적으로 불러왔습니다.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveTown(main_plugin.town.TownData town) {
        String sql = "INSERT INTO towns (owner_uuid, spawn_world, spawn_x, spawn_y, spawn_z, spawn_yaw, spawn_pitch, claimed_chunks) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE spawn_world=?, spawn_x=?, spawn_y=?, spawn_z=?, spawn_yaw=?, spawn_pitch=?, claimed_chunks=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String uuid = town.getOwner().toString();
            String world = town.getSpawnLoc().getWorld().getName();
            double x = town.getSpawnLoc().getX();
            double y = town.getSpawnLoc().getY();
            double z = town.getSpawnLoc().getZ();
            float yaw = town.getSpawnLoc().getYaw();
            float pitch = town.getSpawnLoc().getPitch();
            String chunks = String.join(";", town.getClaimedChunks()); // YML 대신 세미콜론(;)으로 구분하여 저장

            ps.setString(1, uuid);
            ps.setString(2, world);
            ps.setDouble(3, x);
            ps.setDouble(4, y);
            ps.setDouble(5, z);
            ps.setFloat(6, yaw);
            ps.setFloat(7, pitch);
            ps.setString(8, chunks);

            ps.setString(9, world);
            ps.setDouble(10, x);
            ps.setDouble(11, y);
            ps.setDouble(12, z);
            ps.setFloat(13, yaw);
            ps.setFloat(14, pitch);
            ps.setString(15, chunks);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteTownData(UUID uuid) {
        String sql = "DELETE FROM towns WHERE owner_uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("HikariCP 풀이 정상적으로 종료되었습니다.");
        }
    }
}