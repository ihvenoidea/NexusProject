package main_plugin.politics;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
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
    public void donateTribute(Player player, double amount) {
        if (amount <= 0) {
            player.sendMessage(Component.text("조공 액수는 0보다 커야 합니다.", NamedTextColor.RED));
            return;
        }

        // 1. 성주 존재 여부 확인
        UUID lordUuid = plugin.getSiegeManager().getCurrentLordUniqueId();
        if (lordUuid == null) {
            player.sendMessage(Component.text("현재 성주가 존재하지 않아 조공을 바칠 수 없습니다.", NamedTextColor.RED));
            return;
        }

        if (lordUuid.equals(player.getUniqueId())) {
            player.sendMessage(Component.text("성주 본인에게는 조공할 수 없습니다.", NamedTextColor.RED));
            return;
        }

        // 2. 경제 시스템(Vault) 연동 확인 및 잔액 체크
        Economy econ = NexusCore.getEconomy();
        if (econ == null || !econ.has(player, amount)) {
            player.sendMessage(Component.text("잔액이 부족하여 조공을 바칠 수 없습니다.", NamedTextColor.RED));
            return;
        }

        // 3. 유저 데이터 가져오기 및 처리
        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            // [핵심] 실제 돈 차감
            econ.withdrawPlayer(player, amount);

            // 유저의 누적 조공액 업데이트
            user.setTotalTribute(user.getTotalTribute() + amount);
            
            // 성주에게 혜택 전달 (조공액의 10%를 성주의 조공 수입으로 추가)
            plugin.getUserManager().getUser(lordUuid).ifPresent(lord -> {
                lord.setTotalTribute(lord.getTotalTribute() + (amount / 10));
            });

            // 전역 공지 및 메시지 출력
            if (amount >= 1000000) {
                Bukkit.broadcast(Component.text("§6[조공] §e" + player.getName() + "§f님이 성주에게 §6" + String.format("%,.0f", amount) + "§f재화를 바쳐 충성을 맹세했습니다!"));
            } else {
                player.sendMessage(Component.text("성주에게 " + String.format("%,.0f", amount) + "재화를 조공했습니다.", NamedTextColor.GREEN));
                player.sendMessage(Component.text("현재 누적 조공량: " + String.format("%,.0f", user.getTotalTribute()), NamedTextColor.GOLD));
            }

            // DB 저장 (비동기 권장되나 현재 UserManager 구조에 따름)
            plugin.getUserManager().saveUserData(user);
            
            // 업적 보상 체크
            checkRewardThresholds(user);
        });
    }

    /**
     * 일정 조공액 달성 시 보상을 체크합니다.
     */
    public void checkRewardThresholds(UserData user) {
        double tribute = user.getTotalTribute();
        
        // 조공액 1,000만 달성 시 '충성스러운 신하' 증강체 부여
        if (tribute >= 10000000 && !user.getAugments().contains("loyal_subject")) {
            user.getAugments().add("loyal_subject");
            Player player = Bukkit.getPlayer(user.getUuid());
            if (player != null) {
                player.sendMessage(Component.text("§b[업적] §f누적 조공 1,000만 달성! §e'충성스러운 신하' §f증강체를 획득했습니다."));
            }
        }
    }
}