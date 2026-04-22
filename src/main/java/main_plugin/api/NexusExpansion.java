package main_plugin.api;

import main_plugin.NexusCore;
import main_plugin.mastery.MasteryRankingManager;
import main_plugin.user.UserData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

public class NexusExpansion extends PlaceholderExpansion {

    private final NexusCore plugin;
    private final DecimalFormat formatter = new DecimalFormat("#,###"); 

    public NexusExpansion(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nexus"; 
    }

    @Override
    public @NotNull String getAuthor() {
        return "Admin";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.1"; // 버전 업그레이드
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true; 
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // 1. 서버 종합 랭킹 데이터 처리 (%nexus_rank_...)
        if (params.startsWith("rank_")) {
            if (plugin.getMasteryRankingManager() == null) return "§c[매니저로드오류]";

            String[] split = params.split("_");
            if (split.length < 4) return null;

            String category = split[1]; // money, points, tribute, mastery
            int rank;
            try {
                rank = Integer.parseInt(split[2]); // 순위 (1~10)
            } catch (NumberFormatException e) {
                return "§c[순위숫자오류]";
            }
            String type = split[3]; // name, value

            List<MasteryRankingManager.RankingData> dataList = plugin.getMasteryRankingManager().getCachedRanking(category);
            
            if (dataList == null || dataList.size() < rank) {
                return "§7-"; 
            }

            MasteryRankingManager.RankingData data = dataList.get(rank - 1);

            if (type.equalsIgnoreCase("name")) return data.name;
            if (type.equalsIgnoreCase("value")) {
                return switch (category.toLowerCase()) {
                    case "money" -> String.format("§e%,.0f원", data.value);
                    case "points" -> String.format("§b%,d DP", (int) data.value);
                    case "tribute" -> String.format("§d%,.0f 조공", data.value);
                    case "mastery" -> {
                        if (plugin.getMasteryManager() == null) yield "§c[DB]";
                        yield "§a" + plugin.getMasteryManager().getLevelFromExp((long) data.value) + "Lv";
                    }
                    default -> String.valueOf((int) data.value);
                };
            }
            return null;
        }

        // 2. 개인 유저 데이터 처리 (%nexus_dp%, %nexus_money% 등)
        if (player == null) return "";

        Optional<UserData> userOpt = plugin.getUserManager().getUser(player.getUniqueId());
        if (userOpt.isEmpty()) {
            return "0"; 
        }

        UserData user = userOpt.get();

        if (params.equalsIgnoreCase("dp")) return formatter.format(user.getPoints());
        if (params.equalsIgnoreCase("money")) return formatter.format(user.getMoney());
        if (params.equalsIgnoreCase("tribute")) return formatter.format(user.getTotalTribute());

        return null;
    }
}