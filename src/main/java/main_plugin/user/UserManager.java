package main_plugin.user;

import main_plugin.NexusCore;

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

    public void saveUserData(UserData data) {
        if (data != null) {
            plugin.getDatabaseManager().saveUserData(data.getUuid());
        }
    }

    public String getOfflinePlayerName(UUID uuid) {
        String sql = "SELECT name FROM users WHERE uuid = ?";
        // [수정됨] Connection 자동 종료 방지
        try (PreparedStatement pstmt = plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    public Map<UUID, UserData> getAllUsers() {
        return users;
    }
}