package main_plugin.mastery;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MasteryRankingManager {

    private final NexusCore plugin;
    private final Map<String, List<RankingData>> cachedRankings = new HashMap<>();

    public MasteryRankingManager(NexusCore plugin) {
        this.plugin = plugin;
        startRankingUpdateTask();
    }

    /**
     * 특정 카테고리의 캐싱된 랭킹 데이터를 반환합니다.
     */
    public List<RankingData> getCachedRanking(String category) {
        return cachedRankings.getOrDefault(category, new ArrayList<>());
    }

    /**
     * 5분(6000틱)마다 DB에서 랭킹을 비동기로 긁어옵니다.
     */
    private void startRankingUpdateTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            updateCategory("money", "money");
            updateCategory("points", "points");
            updateCategory("tribute", "total_tribute");
            updateCategory("mastery", "job_exp");
            plugin.getLogger().info("[NexusCore] 전체 랭킹 데이터가 성공적으로 갱신되었습니다.");
        }, 0L, 6000L); // 서버 켜질 때 즉시 실행 후 5분마다 반복
    }

    /**
     * DB 조회 헬퍼 메서드
     */
    private void updateCategory(String categoryKey, String dbColumnName) {
        List<RankingData> list = new ArrayList<>();
        String sql = "SELECT name, " + dbColumnName + " FROM users ORDER BY " + dbColumnName + " DESC LIMIT 10";

        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null || conn.isClosed()) return;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    list.add(new RankingData(
                            rs.getString("name"),
                            rs.getDouble(dbColumnName)
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cachedRankings.put(categoryKey, list);
    }

    // ==========================================
    // 랭킹 데이터 전송 객체 (DTO)
    // ==========================================
    public static class RankingData {
        public String name;
        public double value; // 돈, DP, 조공, 경험치 등 모두 담을 수 있도록 double 사용

        public RankingData(String name, double value) {
            this.name = name;
            this.value = value;
        }
    }
}