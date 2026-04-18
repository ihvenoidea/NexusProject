package main_plugin.augments;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class AugmentManager implements Listener {

    private final NexusCore plugin;
    private final Map<String, Augment> registeredAugments = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Random random = new Random();

    public AugmentManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    public void registerAugment(Augment augment) {
        registeredAugments.put(augment.getId(), augment);
    }

    public Optional<Augment> getAugment(String id) {
        return Optional.ofNullable(registeredAugments.get(id));
    }

    public Map<String, Augment> getRegisteredAugments() {
        return registeredAugments;
    }

    /**
     * 특정 시너지 태그를 몇 개 보유 중인지 확인합니다.
     */
    public int getSynergyCount(Player player, String targetTag) {
        return plugin.getUserManager().getUser(player.getUniqueId())
                .map(user -> (int) user.getAugments().stream()
                        .map(this::getAugment)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .filter(aug -> aug.getTags().contains(targetTag))
                        .count())
                .orElse(0);
    }

    /**
     * [핵심] 폭발 증폭 배율을 반환합니다. (BombSynergy에서 사용)
     */
    public float getExplosionMultiplier(Player player) {
        return getSynergyCount(player, "BOMB") >= 3 ? 2.0f : 1.0f;
    }

    // ==========================================
    // [ 데미지 관련 시너지 보너스 처리 ]
    // ==========================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 1. 공격자(Attacker) 관련 시너지 (패왕, 맹독)
        if (event.getDamager() instanceof Player attacker) {
            // [패왕 3세트] 가하는 피해량 10% 증가
            if (getSynergyCount(attacker, "WARLORD") >= 3) {
                event.setDamage(event.getDamage() * 1.1);
            }

            // [맹독 3세트] 공격 시 5% 확률로 적에게 위더 효과
            if (getSynergyCount(attacker, "TOXIC") >= 3 && event.getEntity() instanceof LivingEntity target) {
                if (random.nextDouble() <= 0.05) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0)); // 3초 위더
                    target.getWorld().spawnParticle(Particle.ENTITY_EFFECT, target.getLocation(), 10, 0, 0, 0, 0);
                }
            }
            
            triggerAugments(attacker, event);
        }

        // 2. 방어자(Defender) 관련 시너지 (철갑)
        if (event.getEntity() instanceof Player defender) {
            // [철갑 3세트] 받는 피해량 10% 감소
            if (getSynergyCount(defender, "IRONCLAD") >= 3) {
                event.setDamage(event.getDamage() * 0.9);
            }
            
            triggerAugments(defender, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGenericDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return;

        if (event.getEntity() instanceof Player defender) {
            // [철갑 3세트] 환경 데미지(낙하, 용암 등)도 10% 감소 적용
            if (getSynergyCount(defender, "IRONCLAD") >= 3) {
                event.setDamage(event.getDamage() * 0.9);
            }
            triggerAugments(defender, event);
        }
    }

    // ==========================================
    // [ 기타 이벤트 트리거 ]
    // ==========================================

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        triggerAugments(event.getPlayer(), event);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            triggerAugments(event.getEntity().getKiller(), event);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        triggerAugments(event.getEntity(), event);
    }

    // ==========================================
    // [ 공통 증강체 발동 로직 ]
    // ==========================================

    public void triggerAugments(Player player, Event event) {
        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            for (String augmentId : user.getAugments()) {
                getAugment(augmentId).ifPresent(augment -> {
                    if (isCooldownOver(player, augment)) {
                        augment.execute(player, event);
                        setCooldown(player, augment);
                    }
                });
            }
        });
    }

    private boolean isCooldownOver(Player player, Augment augment) {
        if (augment.getCooldown() <= 0) return true;
        long lastUse = cooldowns.getOrDefault(player.getUniqueId(), new HashMap<>())
                                .getOrDefault(augment.getId(), 0L);
        return (System.currentTimeMillis() - lastUse) >= augment.getCooldown();
    }

    private void setCooldown(Player player, Augment augment) {
        if (augment.getCooldown() <= 0) return;
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                 .put(augment.getId(), System.currentTimeMillis());
    }

    public void loadConfigs() {
        plugin.getLogger().info("AugmentManager 설정을 성공적으로 리로드했습니다.");
    }
}