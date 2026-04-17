package main_plugin.politics;

import main_plugin.NexusCore;
import main_plugin.user.NexusUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TributeManager {

    private final NexusCore plugin;

    public TributeManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 플레이어가 현재 성주에게 조공을 바칩니다.
     * @param player 조공을 바치는 플레이어
     * @param amount 조공 액수
     */
    public void donateTribute(Player player, long amount) {
        if (amount <= 0) {
            player.sendMessage(Component.text("조공 액수는 0보다 커야 합니다.", NamedTextColor.RED));
            return;
        }

        UUID lordUuid = plugin.getSiegeManager().getCurrentLordUniqueId();
        if (lordUuid == null) {
            player.sendMessage(Component.text("현재 성주가 존재하지 않아 조공을 바칠 수 없습니다.", NamedTextColor.RED));
            return;
        }

        if (lordUuid.equals(player.getUniqueId())) {
            player.sendMessage(Component.text("성주 본인에게는 조공할 수 없습니다.", NamedTextColor.RED));
            return;
        }

        // 1. 유저 데이터 가져오기
        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            // 여기에 실제 돈(Economy API) 차감 로직 추가 필요
            // if (!economy.has(player, amount)) return;

            // 2. 조공액 누적 업데이트
            user.setTotalTribute(user.getTotalTribute() + amount);
            
            // 3. 성주에게 알림 및 혜택 전달 (예: 성주의 DP 증가)
            plugin.getUserManager().getUser(lordUuid).ifPresent(lord -> {
                lord.setTotalTribute(lord.getTotalTribute() + (amount / 10)); // 조공액의 10%를 성주의 통치 자금으로 전환
            });

            // 4. 전역 공지 (고액 조공 시)
            if (amount >= 1000000) {
                Bukkit.broadcast(Component.text("§6[조공] §e" + player.getName() + "§f님이 성주에게 §6" + amount + "§f재화를 바쳐 충성을 맹세했습니다!"));
            } else {
                player.sendMessage(Component.text("성주에게 " + amount + "재화를 조공했습니다. 누적 조공량: " + user.getTotalTribute(), NamedTextColor.GREEN));
            }

            // 5. DB 비동기 저장 요청
            plugin.getUserManager().saveUserData(user);
        });
    }

    /**
     * 조공 순위를 확인합니다. (Top 5)
     */
    public void showTributeLeaderboard(Player player) {
        // 이 부분은 UserManager에서 DB 쿼리(ORDER BY total_tribute DESC LIMIT 5)를 통해 구현
        player.sendMessage(Component.text("§e--- [ 누적 조공 순위 ] ---"));
        // ... 리더보드 출력 로직
    }

    /**
     * 일정 조공액 달성 시 보상을 체크합니다.
     */
    public void checkRewardThresholds(NexusUser user) {
        long tribute = user.getTotalTribute();
        
        // 예: 조공액 1,000만 달성 시 '충성스러운 신하' 증강체 부여
        if (tribute >= 10000000 && !user.getAugments().contains("loyal_subject")) {
            user.getAugments().add("loyal_subject");
            Player player = Bukkit.getPlayer(user.getUuid());
            if (player != null) {
                player.sendMessage(Component.text("§b[업적] §f누적 조공 1,000만 달성! §e'충성스러운 신하' §f증강체를 획득했습니다."));
            }
        }
    }
}