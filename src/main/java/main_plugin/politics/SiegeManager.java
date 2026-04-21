package main_plugin.politics;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class SiegeManager {

    private final NexusCore plugin;
    
    private boolean isSiegeActive = false;
    private Location coreLocation;
    
    private double maxCoreHp;
    private double currentCoreHP;
    
    private BossBar siegeBar;

    public SiegeManager(NexusCore plugin) {
        this.plugin = plugin;
        this.siegeBar = Bukkit.createBossBar("§d§l[ 넥서스 코어 레이드 ]", BarColor.PINK, BarStyle.SEGMENTED_10);
    }

    public void setCoreLocation(Location loc) { this.coreLocation = loc; }
    public Location getCoreLocation() { return coreLocation; }
    public boolean isSiegeActive() { return isSiegeActive; }

    public void startSiege() {
        if (isSiegeActive) return;
        if (coreLocation == null) {
            Bukkit.getLogger().warning("[공성전] 코어 위치가 설정되지 않았습니다!");
            return;
        }

        this.maxCoreHp = plugin.getConfig().getDouble("siege.core-hp", 50000.0);
        this.currentCoreHP = maxCoreHp;
        this.isSiegeActive = true;

        for (Player p : Bukkit.getOnlinePlayers()) {
            siegeBar.addPlayer(p);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        }
        updateBossBar();

        Bukkit.broadcastMessage("\n§d§l[ 넥서스 레이드 ] §f코어가 활성화되었습니다!");
        Bukkit.broadcastMessage("§e▶ 마지막 타격을 가한 용사에게 막대한 보상이 주어집니다!\n");
    }

    public void damageCore(Player attacker, double damage) {
        if (!isSiegeActive) return;

        currentCoreHP -= damage;
        coreLocation.getWorld().spawnParticle(Particle.WITCH, coreLocation.clone().add(0.5, 0.5, 0.5), 15);
        coreLocation.getWorld().playSound(coreLocation, Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.5f);
        
        updateBossBar();

        if (currentCoreHP <= 0) {
            endSiege(attacker);
        }
    }

    private void updateBossBar() {
        double progress = currentCoreHP / maxCoreHp;
        siegeBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        siegeBar.setTitle("§d§l[ 넥서스 코어 ] §f" + Math.round(Math.max(0, currentCoreHP)) + " / " + Math.round(maxCoreHp));
    }

    public void endSiege(Player destroyer) {
        isSiegeActive = false;
        siegeBar.removeAll(); 

        Bukkit.broadcastMessage("\n§d§l[ 레이드 종료 ]");
        if (destroyer != null) {
            Bukkit.broadcastMessage("§e§l" + destroyer.getName() + "§f님이 코어를 파괴하여 전설을 써내려갔습니다!");
            
            // [신규 로직] 오류 없는 안전한 DP 다이렉트 지급
            plugin.getUserManager().getUser(destroyer.getUniqueId()).ifPresent(user -> {
                user.setPoints(user.getPoints() + 5000);
                plugin.getUserManager().saveUserData(user); // DB 동기화
                
                destroyer.sendMessage("§a§l[!] 공성전 막타 보상으로 §b5,000 DP§a가 지급되었습니다!");
                destroyer.playSound(destroyer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            });
            
        } else {
            Bukkit.broadcastMessage("§7코어 레이드가 관리자에 의해 중단되었습니다.");
        }
        Bukkit.broadcastMessage("\n");
    }
}