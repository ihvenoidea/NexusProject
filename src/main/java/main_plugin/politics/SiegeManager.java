package main_plugin.politics;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.UUID;

public class SiegeManager {

    private final NexusCore plugin;
    
    private UUID currentLordUniqueId; // 현재 성주의 UUID
    private String currentLordTownName; // 현재 성주가 소속된 마을 이름
    private boolean isSiegeInProgress = false; // 공성전 진행 여부

    public SiegeManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 공성전 승리자를 설정하고 권력을 이양합니다.
     * @param winner 새로운 성주 플레이어
     * @param townName 성주 소속 마을
     */
    public void setNewLord(Player winner, String townName) {
        this.currentLordUniqueId = winner.getUniqueId();
        this.currentLordTownName = townName;
        
        // 전역 공지
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§6[공성전] §f새로운 성주가 탄생했습니다! §e" + winner.getName() + " §7(" + townName + ")"));
        
        // 디스코드 연동 알림 (나중에 봇과 연결)
        plugin.getLogger().info("New Lord set in DB: " + winner.getName());

        // 신화 증강체 지급 또는 권한 부여 로직 호출
        applyLordPower(winner);
    }

    /**
     * 성주에게 특별한 권한(신화 증강체 등)을 부여합니다.
     */
    private void applyLordPower(Player player) {
        // 유저 데이터에 신화 등급 증강 ID 강제 추가 (예: mythic_crown)
        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            if (!user.getAugments().contains("mythic_crown")) {
                user.getAugments().add("mythic_crown");
                player.sendMessage("§d§l[권력] §f신화급 증강체 '제왕의 왕관'의 힘이 느껴집니다.");
            }
        });
    }

    /**
     * 현재 플레이어가 성주인지 확인합니다.
     */
    public boolean isLord(UUID uuid) {
        return currentLordUniqueId != null && currentLordUniqueId.equals(uuid);
    }

    // --- 공성전 상태 관리 ---

    public void startSiege() {
        this.isSiegeInProgress = true;
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§c[공성전] §l중앙 성지 점령전이 시작되었습니다!"));
    }

    public void endSiege() {
        this.isSiegeInProgress = false;
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§a[공성전] §f공성전이 종료되었습니다."));
    }

    // --- Getter & Setter ---

    public UUID getCurrentLordUniqueId() {
        return currentLordUniqueId;
    }

    public String getCurrentLordTownName() {
        return currentLordTownName;
    }

    public boolean isSiegeInProgress() {
        return isSiegeInProgress;
    }
}