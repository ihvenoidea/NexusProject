package main_plugin.user;

import main_plugin.NexusCore;
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
    // 모든 유저 데이터를 UUID를 키로 하여 메모리(HashMap)에 보관합니다.
    private final Map<UUID, UserData> users = new HashMap<>();

    public UserManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 유저가 접속할 때 DB에서 데이터를 로드하여 메모리에 저장합니다.
     * @param uuid 유저의 UUID
     * @param name 유저의 닉네임
     */
    public void loadUserData(UUID uuid, String name) {
        // NexusCore에 구현된 getDbManager()를 호출하여 커넥션을 가져옵니다.
        try (Connection conn = plugin.getDbManager().getConnection()) {
            // users 테이블에서 유저 정보를 조회합니다.
            String sql = "SELECT * FROM users WHERE uuid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    // DB 컬럼명에 맞춰 데이터를 추출합니다.
                    double money = rs.getDouble("money");
                    int points = rs.getInt("discord_points");
                    // UserData.java에서 추가한 total_tribute 등도 로드할 수 있습니다.
                    double tribute = rs.getDouble("total_tribute");

                    // UserData 객체를 생성하고 메모리에 올립니다.
                    UserData userData = new UserData(uuid, name, money, points);
                    userData.setTotalTribute(tribute);
                    
                    users.put(uuid, userData);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(name + "님의 데이터를 로드하는 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 유저가 나갈 때 또는 저장 시 메모리의 데이터를 DB에 반영합니다.
     */
    public void saveUserData(UserData user) {
        try (Connection conn = plugin.getDbManager().getConnection()) {
            // 경제, 포인트, 조공 데이터를 모두 업데이트합니다.
            String sql = "UPDATE users SET money = ?, discord_points = ?, total_tribute = ? WHERE uuid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, user.getMoney());
                pstmt.setInt(2, user.getPoints());
                pstmt.setDouble(3, user.getTotalTribute());
                pstmt.setString(4, user.getUuid().toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(user.getName() + "님의 데이터를 저장하는 중 오류 발생: " + e.getMessage());
        }
    }

    public Optional<UserData> getUser(UUID uuid) {
        return Optional.ofNullable(users.get(uuid));
    }

    public void removeUser(UUID uuid) {
        users.remove(uuid);
    }
}