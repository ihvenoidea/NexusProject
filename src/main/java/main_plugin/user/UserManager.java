package main_plugin.user;

import main_plugin.NexusCore;
import main_plugin.database.DatabaseManager;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UserManager {

    private final NexusCore plugin;
    private final Map<UUID, UserData> users = new HashMap<>();

    public UserManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 유저 데이터를 DB에서 로드하여 메모리에 저장합니다.
     * @param uuid 유저의 UUID
     * @param name 유저의 닉네임 (신규 생성 시 필요)
     */
    public void loadUserData(UUID uuid, String name) {
        // [중요] NexusCore에 추가한 getDbManager()를 사용하여 연결을 가져옵니다.
        try (Connection conn = plugin.getDbManager().getConnection()) {
            String sql = "SELECT * FROM users WHERE uuid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    double money = rs.getDouble("money");
                    int points = rs.getInt("discord_points");

                    // UserData 객체 생성 및 맵에 저장
                    UserData userData = new UserData(uuid, name, money, points);
                    users.put(uuid, userData);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(name + "님의 데이터를 로드하는 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 메모리에 있는 유저 데이터를 DB에 영구 저장합니다.
     */
    public void saveUserData(UserData user) {
        try (Connection conn = plugin.getDbManager().getConnection()) {
            String sql = "UPDATE users SET money = ?, discord_points = ? WHERE uuid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, user.getMoney());
                pstmt.setInt(2, user.getPoints());
                pstmt.setString(3, user.getUuid().toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(user.getName() + "님의 데이터를 저장하는 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 특정 유저의 데이터를 가져옵니다.
     */
    public Optional<UserData> getUser(UUID uuid) {
        return Optional.ofNullable(users.get(uuid));
    }

    /**
     * 퇴장 시 메모리에서 유저 데이터를 제거합니다.
     */
    public void removeUser(UUID uuid) {
        users.remove(uuid);
    }
}