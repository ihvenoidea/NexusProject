package main_plugin.items;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SetItemListener implements Listener {

    private final NexusCore plugin;
    private final NamespacedKey nameKey;
    private final Random random = new Random();

    // 부활 및 재생 효과를 위한 데이터 저장소
    private final Map<UUID, Long> lastReviveTime = new HashMap<>();
    private final Map<UUID, Long> lastCombatTime = new HashMap<>();

    public SetItemListener(NexusCore plugin) {
        this.plugin = plugin;
        this.nameKey = plugin.getSetItemManager().getNameKey();
        startTask();
    }

    // 1초마다 모든 플레이어의 세트 장착 상태를 체크하여 물약 효과 부여
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
        
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.hasItemMeta()) {
                String name = item.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
                if (name != null) {
                    counts.put(name, counts.getOrDefault(name, 0) + 1);
                }
            }
        }

        // --- 상시 물약 효과 부여 (2세트 이상) ---
        if (counts.getOrDefault("도약", 0) >= 2) player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 31, 1, false, false));
        if (counts.getOrDefault("풍요", 0) >= 2) player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false));
        if (counts.getOrDefault("화염", 0) >= 2) player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 31, 0, false, false));
        if (counts.getOrDefault("신속", 0) >= 2) player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 31, 0, false, false));
        if (counts.getOrDefault("권능", 0) >= 2) player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 31, 0, false, false));

        // [재생 세트] 비전투 시 체력 회복 로직 (10초간 전투가 없어야 함)
        if (counts.getOrDefault("재생", 0) >= 2) {
            long lastCombat = lastCombatTime.getOrDefault(player.getUniqueId(), 0L);
            if (System.currentTimeMillis() - lastCombat > 10000) {
                double maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                if (player.getHealth() < maxHp) {
                    player.setHealth(Math.min(maxHp, player.getHealth() + 1.0));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombatEffects(EntityDamageByEntityEvent event) {
        // --- 1. 피격자가 플레이어인 경우 (방어/회피/반격 효과) ---
        if (event.getEntity() instanceof Player defender) {
            Map<String, Integer> counts = getArmorCounts(defender);
            lastCombatTime.put(defender.getUniqueId(), System.currentTimeMillis());

            // [환영 세트] 15% 확률 회피
            if (counts.getOrDefault("환영", 0) >= 2 && random.nextDouble() < 0.15) {
                event.setCancelled(true);
                defender.sendMessage("§b§l[회피] §f환영처럼 공격을 흘려보냈습니다!");
                defender.playSound(defender.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 2.0f);
                return;
            }

            // [혹한 세트] 공격자에게 둔화 부여
            if (counts.getOrDefault("혹한", 0) >= 2 && event.getDamager() instanceof LivingEntity attacker) {
                attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                defender.sendMessage("§9§l[혹한] §f공격자가 얼어붙어 느려집니다.");
            }

            // [재앙 세트] 5% 확률로 벼락 반격
            if (counts.getOrDefault("재앙", 0) >= 2 && random.nextDouble() < 0.05 && event.getDamager() instanceof LivingEntity attacker) {
                attacker.getWorld().strikeLightningEffect(attacker.getLocation());
                attacker.damage(5.0, defender); // 방어 무시 고정 데미지
                defender.sendMessage("§5§l[재앙] §f하늘에서 심판의 벼락이 떨어집니다!");
            }

            // [불멸 세트] 죽기 직전 부활 (체력 10 회복 + 3초 무적)
            if (counts.getOrDefault("불멸", 0) >= 2 && defender.getHealth() - event.getFinalDamage() <= 0) {
                long lastRev = lastReviveTime.getOrDefault(defender.getUniqueId(), 0L);
                if (System.currentTimeMillis() - lastRev > 600000) { // 10분 쿨타임
                    event.setCancelled(true);
                    defender.setHealth(10.0);
                    defender.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 5)); // 3초 완전 무적
                    lastReviveTime.put(defender.getUniqueId(), System.currentTimeMillis());
                    defender.sendMessage("§d§l[불멸] §f치명상을 입었으나 죽음을 거부했습니다! (쿨타임 10분)");
                    defender.playSound(defender.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                    Bukkit.broadcastMessage("§d§l[공지] §e" + defender.getName() + "§f님이 불멸 세트의 힘으로 죽음에서 돌아왔습니다!");
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            Map<String, Integer> counts = getArmorCounts(killer);

            // [탐욕 세트] 10% 확률로 DP 드롭
            if (counts.getOrDefault("탐욕", 0) >= 2 && random.nextDouble() < 0.10) {
                int dpAmount = random.nextInt(41) + 10; // 10~50 DP
                plugin.getUserManager().getUser(killer.getUniqueId()).ifPresent(user -> {
                    user.setPoints(user.getPoints() + dpAmount);
                    plugin.getUserManager().saveUserData(user);
                    killer.sendMessage("§6§l[탐욕] §f시체에서 §b" + dpAmount + " DP§f를 찾아냈습니다!");
                    killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
                });
            }
        }
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        Map<String, Integer> counts = getArmorCounts(player);

        // [견고 세트] 15% 확률로 내구도 보호
        if (counts.getOrDefault("견고", 0) >= 2 && random.nextDouble() < 0.15) {
            event.setCancelled(true);
        }
    }

    private Map<String, Integer> getArmorCounts(Player player) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.hasItemMeta()) {
                String name = item.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
                if (name != null) {
                    counts.put(name, counts.getOrDefault(name, 0) + 1);
                }
            }
        }
        return counts;
    }
}