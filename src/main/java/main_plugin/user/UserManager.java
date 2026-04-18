package main_plugin.user;

import main_plugin.NexusCore;
import org.bukkit.entity.Player;

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
    private final Map<UUID, UserData> users;

    public UserManager(NexusCore plugin) {
        this.plugin = plugin;
        this.users = new HashMap<>();
    }

    /**
     * DatabaseManager에서 로드한 유저를 메모리에 추가
     */
    public void addUser(UserData userData) {
        if (userData == null) return;
        users.put(userData.getUuid(), userData);
    }

    /**
     * 메모리에서 유저 데이터 가져오기
     */
    public Optional<UserData> getUser(UUID uuid) {
        return Optional.ofNullable(users.get(uuid));
    }

    /**
     * 유저 퇴장 시 메모리 제거
     */
    public void removeUser(UUID uuid) {
        users.remove(uuid);
    }

    // --- [에러 해결 포인트] 브릿지 메서드 추가 ---

    /**
     * 유저 데이터를 DB에서 로드하도록 DatabaseManager에 요청 (PlayerListener용)
     */
    public void loadUserData(UUID uuid, String name) {
        plugin.getDatabaseManager().loadUserData(uuid);
    }

    /**
     * 유저 데이터를 DB에 저장하도록 DatabaseManager에 요청 (TributeManager, DPCommand 등용)
     */
    public void saveUserData(UserData data) {
        if (data != null) {
            plugin.getDatabaseManager().saveUserData(data.getUuid());
        }
    }

    // ------------------------------------------

    /**
     * DB에서 직접 이름을 가져오는 유틸리티
     */
    public String getOfflinePlayerName(UUID uuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT name FROM users WHERE uuid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    public Map<UUID, UserData> getAllUsers() {
        return users;
    }
}