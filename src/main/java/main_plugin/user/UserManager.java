package main_plugin.user;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {

    private final NexusCore plugin;
    // 비동기 스레드 충돌을 막기 위한 ConcurrentHashMap
    private final Map<UUID, UserData> users;

    public UserManager(NexusCore plugin) {
        this.plugin = plugin;
        this.users = new ConcurrentHashMap<>();
    }

    public void addUser(UserData userData) {
        if (userData == null) return;
        users.put(userData.getUuid(), userData);
    }

    public Optional<UserData> getUser(UUID uuid) {
        return Optional.ofNullable(users.get(uuid));
    }

    public void removeUser(UUID uuid) {
        users.remove(uuid);
    }

    public void loadUserData(UUID uuid, String name) {
        plugin.getDatabaseManager().loadUserData(uuid);
    }

    // [핵심 픽스] DB 저장을 항상 비동기로 던져서 서버 메인 스레드가 절대 멈추지 않게 방어합니다.
    public void saveUserData(UserData data) {
        if (data != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().saveUserData(data.getUuid());
            });
        }
    }

    public String getOfflinePlayerName(UUID uuid) {
        String sql = "SELECT name FROM users WHERE uuid = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
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