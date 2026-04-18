package main_plugin.items;

import main_plugin.NexusCore;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 세트 장비(갑옷)의 실시간 효과 및 특수 기능을 처리하는 리스너입니다.
 * 증강체와는 독립적으로 작동하며, 순수 유틸리티 효과를 제공합니다.
 */
public class SetItemListener implements Listener {

    private final NexusCore plugin;
    private final NamespacedKey nameKey;
    private final Random random = new Random();

    public SetItemListener(NexusCore plugin) {
        this.plugin = plugin;
        this.nameKey = plugin.getSetItemManager().getNameKey();
        startTask();
    }

    /**
     * 1초(20틱)마다 모든 플레이어의 착용 장비를 검사하여 효과를 적용합니다.
     */
    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    applySetEffects(p);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void applySetEffects(Player player) {
        Map<String, Integer> counts = new HashMap<>();
        
        // 1. 플레이어가 입고 있는 방어구의 세트 이름 카운트
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.hasItemMeta()) {
                String name = item.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
                if (name != null) counts.put(name, counts.getOrDefault(name, 0) + 1);
            }
        }

        // 2. 2부위 이상 착용 시 해당 세트의 고유 버프 적용
        counts.forEach((setName, count) -> {
            if (count >= 2) { 
                applyBonus(player, setName);
            }
        });
    }

    /**
     * 세트 이름에 따른 포션 효과 부여
     */
    private void applyBonus(Player player, String setName) {
        switch (setName) {
            case "방어" -> // 골드 등급: 야간 투시
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false));
            case "격류" -> // 프리즘 등급: 이동 속도 I
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 31, 0, false, false));
            case "황금" -> // 신화 등급: 재생 I
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 31, 0, false, false));
            // '신속(실버)' 세트는 내구도 관련 기능이므로 아래 별도 이벤트에서 처리합니다.
        }
    }

    /**
     * [실버 등급 기능] 장비 내구도 보호 로직
     * 신속(실버) 세트를 2부위 이상 입고 있을 경우 15% 확률로 내구도 감소를 무시합니다.
     */
    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        
        int silverCount = 0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.hasItemMeta()) {
                String name = item.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
                if ("신속".equals(name)) silverCount++;
            }
        }

        // 2부위 이상 착용 시 15% 확률로 데미지 취소
        if (silverCount >= 2) {
            if (random.nextDouble() < 0.15) {
                event.setCancelled(true);
            }
        }
    }
}