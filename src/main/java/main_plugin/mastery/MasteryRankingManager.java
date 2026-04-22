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

    public List<RankingData> getCachedRanking(String category) {
        return cachedRankings.getOrDefault(category, new ArrayList<>());
    }

    private void startRankingUpdateTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            updateCategory("money", "money");
            updateCategory("points", "points");
            updateCategory("tribute", "total_tribute");
            updateCategory("mastery", "job_exp");
            plugin.getLogger().info("[NexusCore] 전체 랭킹 데이터가 성공적으로 갱신되었습니다.");
        }, 0L, 6000L);
    }

    private void updateCategory(String categoryKey, String dbColumnName) {
        // 화이트리스트로 SQL Injection 방지
        List<String> allowed = List.of("money", "points", "total_tribute", "job_exp");
        if (!allowed.contains(dbColumnName)) return;

        List<RankingData> list = new ArrayList<>();
        String sql = "SELECT name, " + dbColumnName + " FROM users ORDER BY " + dbColumnName + " DESC LIMIT 10";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new RankingData(rs.getString("name"), rs.getDouble(dbColumnName)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cachedRankings.put(categoryKey, list);
    }

    public static class RankingData {
        public String name;
        public double value;

        public RankingData(String name, double value) {
            this.name = name;
            this.value = value;
        }
    }
}