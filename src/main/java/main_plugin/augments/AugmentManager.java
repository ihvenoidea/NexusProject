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

    public float getExplosionMultiplier(Player player) {
        return getSynergyCount(player, "BOMB") >= 3 ? 2.0f : 1.0f;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            if (getSynergyCount(attacker, "WARLORD") >= 3) {
                event.setDamage(event.getDamage() * 1.1);
            }

            if (getSynergyCount(attacker, "TOXIC") >= 3 && event.getEntity() instanceof LivingEntity target) {
                if (random.nextDouble() <= 0.05) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0)); 
                    target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0);
                }
            }
            triggerAugments(attacker, event);
        }

        if (event.getEntity() instanceof Player defender) {
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
            if (getSynergyCount(defender, "IRONCLAD") >= 3) {
                event.setDamage(event.getDamage() * 0.9);
            }
            triggerAugments(defender, event);
        }
    }

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

    public void triggerAugments(Player player, Event event) {
        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            for (String augmentId : user.getAugments()) {
                getAugment(augmentId).ifPresent(augment -> {
                    if (isCooldownOver(player, augment)) {
                        // [핵심 버그 수정] 무조건 쿨타임을 돌려버리던 코드를 삭제했습니다.
                        augment.execute(player, event);
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

    // [추가됨] 스킬 내부에서 진짜로 발동했을 때만 쿨타임을 돌릴 수 있도록 public으로 개방
    public void startCooldown(Player player, Augment augment) {
        if (augment.getCooldown() <= 0) return;
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                 .put(augment.getId(), System.currentTimeMillis());
    }

    public void loadConfigs() {
        plugin.getLogger().info("AugmentManager 설정을 성공적으로 리로드했습니다.");
    }
}