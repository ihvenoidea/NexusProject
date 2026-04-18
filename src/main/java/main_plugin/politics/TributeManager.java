package main_plugin.politics;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TributeManager {

    private final NexusCore plugin;

    public TributeManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 유저가 디스코드 포인트(DP)를 자발적으로 조공합니다.
     */
    public void donateDP(Player player, int amount) {
        if (amount <= 0) {
            player.sendMessage(Component.text("§c[조공] 0보다 큰 금액을 입력해주세요."));
            return;
        }

        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            if (user.getPoints() < amount) {
                player.sendMessage(Component.text("§c[조공] 보유하신 DP가 부족합니다. (현재: " + user.getPoints() + " DP)"));
                return;
            }

            // 자발적 소모 및 누적 조공량 가산
            user.setPoints(user.getPoints() - amount);
            user.setTotalTribute(user.getTotalTribute() + amount);

            player.sendMessage(Component.text("§a[조공] 성공적으로 " + amount + " DP를 바쳤습니다!"));
            player.sendMessage(Component.text("§e[정보] 나의 누적 조공량: " + String.format("%,.0f", user.getTotalTribute()) + " DP"));

            if (amount >= 500) {
                Bukkit.broadcast(Component.text("§d§l[NEXUS 조공] §e" + player.getName() + "§f님이 §b" + amount + " DP§f를 자진해서 바쳤습니다!"));
            }

            plugin.getUserManager().saveUserData(user);
        });
    }

    /**
     * 조공 순위 TOP 5를 출력합니다.
     */
    public void showLeaderboard(Player player) {
        player.sendMessage(Component.text("§6§l--- [ 실시간 DP 조공 순위 TOP 5 ] ---"));
        
        List<UserData> topList = plugin.getServer().getOnlinePlayers().stream()
            .map(p -> plugin.getUserManager().getUser(p.getUniqueId()).orElse(null))
            .filter(u -> u != null && u.getTotalTribute() > 0)
            .sorted(Comparator.comparingDouble(UserData::getTotalTribute).reversed())
            .limit(5)
            .collect(Collectors.toList());

        if (topList.isEmpty()) {
            player.sendMessage(Component.text("§7아직 조공에 참여한 유저가 없습니다."));
            return;
        }

        for (int i = 0; i < topList.size(); i++) {
            UserData u = topList.get(i);
            player.sendMessage(Component.text("§e" + (i + 1) + "위: §f" + u.getName() + " §7- §b" + String.format("%,.0f", u.getTotalTribute()) + " DP"));
        }
    }

    /**
     * [신규 추가] 이벤트를 종료하고 조공 1등에게 신화 증강체를 지급합니다.
     * TributeCommand.java의 에러를 해결하는 핵심 메서드입니다.
     */
    public void rewardTopTributer() {
        plugin.getServer().getOnlinePlayers().stream()
            .map(p -> plugin.getUserManager().getUser(p.getUniqueId()).orElse(null))
            .filter(u -> u != null && u.getTotalTribute() > 0)
            .max(Comparator.comparingDouble(UserData::getTotalTribute))
            .ifPresent(topUser -> {
                // 신화 등급 증강체(mythic_crown) 지급
                if (!topUser.getAugments().contains("mythic_crown")) {
                    topUser.getAugments().add("mythic_crown");
                    
                    Player player = Bukkit.getPlayer(topUser.getUuid());
                    String playerName = (player != null) ? player.getName() : topUser.getName();
                    
                    Bukkit.broadcast(Component.text("§d§l[이벤트 종료] §f조공 1등 §e" + playerName + "§f님에게 §5§l신화 등급 증강체§f가 지급되었습니다!"));
                    
                    plugin.getUserManager().saveUserData(topUser);
                }
            });
    }
}