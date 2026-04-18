package main_plugin.augments.synergies;

import main_plugin.augments.Augment;
import main_plugin.augments.AugmentTier;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * [맹독] 태그를 공유하는 디버프 및 지속 딜, 리스크 특화 증강체 모음입니다.
 */
public class ToxicSynergy {

    // ==========================================
    // 1. 맹독 가시 (SILVER)
    // ==========================================
    public static class ToxicThorn implements Augment {
        private final Random random = new Random();

        @Override public String getId() { return "toxic_thorn"; }
        @Override public String getName() { return "맹독 가시"; }
        @Override public AugmentTier getTier() { return AugmentTier.SILVER; }
        @Override public List<String> getTags() { return Arrays.asList("TOXIC"); }
        @Override public Material getIcon() { return Material.SPIDER_EYE; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList("§f피격 시 §e20% 확률§f로 공격자에게", "§a독 I (5초)§f을 부여합니다.");
        }

        @Override
        public void execute(Player player, Event event) {
            // 맞았을 때 발동
            if (event instanceof EntityDamageByEntityEvent damageEvent) {
                if (damageEvent.getEntity().equals(player) && damageEvent.getDamager() instanceof LivingEntity attacker) {
                    if (random.nextDouble() <= 0.20) {
                        attacker.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0)); // 100틱 = 5초, 레벨 0 = 독 I
                        attacker.getWorld().spawnParticle(Particle.SNEEZE, attacker.getLocation().add(0, 1, 0), 15);
                    }
                }
            }
        }
    }

    // ==========================================
    // 2. 독성 무기 (GOLD)
    // ==========================================
    public static class ToxicWeapon implements Augment {
        private final Random random = new Random();

        @Override public String getId() { return "toxic_weapon"; }
        @Override public String getName() { return "독성 무기"; }
        @Override public AugmentTier getTier() { return AugmentTier.GOLD; }
        @Override public List<String> getTags() { return Arrays.asList("TOXIC"); }
        @Override public Material getIcon() { return Material.FERMENTED_SPIDER_EYE; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList("§f공격 시 §e15% 확률§f로 대상에게", "§a독 I (5초)§f을 부여합니다.");
        }

        @Override
        public void execute(Player player, Event event) {
            // 때렸을 때 발동
            if (event instanceof EntityDamageByEntityEvent damageEvent) {
                if (damageEvent.getDamager().equals(player) && damageEvent.getEntity() instanceof LivingEntity target) {
                    if (random.nextDouble() <= 0.15) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
                        target.getWorld().spawnParticle(Particle.SNEEZE, target.getLocation().add(0, 1, 0), 15);
                    }
                }
            }
        }
    }

    // ==========================================
    // 3. 스팀팩 (PRISM) - 액티브형 증강체
    // ==========================================
    public static class Stimpack implements Augment {
        @Override public String getId() { return "stimpack"; }
        @Override public String getName() { return "스팀팩"; }
        @Override public AugmentTier getTier() { return AugmentTier.PRISM; }
        @Override public List<String> getTags() { return Arrays.asList("TOXIC"); }
        @Override public Material getIcon() { return Material.POTION; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList(
                "§f웅크린(Shift) 상태로 좌클릭 시",
                "§f자신에게 §a독 I (3초)§f을 거는 대신",
                "§c힘 II (5초)§f를 얻습니다.",
                "§8(쿨타임: 15초)"
            );
        }

        @Override
        public long getCooldown() {
            return 15000; // 15초 (15000 밀리초)
        }

        @Override
        public void execute(Player player, Event event) {
            // 좌클릭 상호작용 감지
            if (event instanceof PlayerInteractEvent interactEvent) {
                Action action = interactEvent.getAction();
                if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) && player.isSneaking()) {
                    
                    // 독 I (60틱), 힘 II (100틱) 부여
                    player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 1));
                    
                    // 스팀팩 효과음 및 파티클
                    player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 2.0f);
                    player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }
    }
}