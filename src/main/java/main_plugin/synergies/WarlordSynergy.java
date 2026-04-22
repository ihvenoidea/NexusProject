package main_plugin.augments.synergies;

import main_plugin.augments.Augment;
import main_plugin.augments.AugmentTier;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * [패왕] 태그를 공유하는 근접 전투 및 딜러 특화 증강체 모음입니다.
 */
public class WarlordSynergy {

    // ==========================================
    // 1. 커다란 힘 (SILVER)
    // ==========================================
    public static class GreatPower implements Augment {
        @Override public String getId() { return "great_power"; }
        @Override public String getName() { return "커다란 힘"; }
        @Override public AugmentTier getTier() { return AugmentTier.SILVER; }
        @Override public List<String> getTags() { return Arrays.asList("WARLORD"); }
        @Override public Material getIcon() { return Material.IRON_SWORD; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList("§f가하는 근접 물리 데미지가 §c10% 증가§f합니다.");
        }

        @Override
        public void execute(Player player, Event event) {
            // 플레이어가 공격자일 때 데미지 1.1배 증폭
            if (event instanceof EntityDamageByEntityEvent damageEvent) {
                if (damageEvent.getDamager().equals(player)) {
                    double originalDamage = damageEvent.getDamage();
                    damageEvent.setDamage(originalDamage * 1.10);
                }
            }
        }
    }

    // ==========================================
    // 2. 신속한 살인마 (GOLD)
    // ==========================================
    public static class SwiftKiller implements Augment {
        @Override public String getId() { return "swift_killer"; }
        @Override public String getName() { return "신속한 살인마"; }
        @Override public AugmentTier getTier() { return AugmentTier.GOLD; }
        @Override public List<String> getTags() { return Arrays.asList("WARLORD"); }
        @Override public Material getIcon() { return Material.RABBIT_FOOT; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList("§f적(플레이어 또는 몹) 처치 시", "§b3초간 신속 II §f효과를 얻습니다.");
        }

        @Override
        public void execute(Player player, Event event) {
            // 적 처치(사망) 이벤트 발생 시 신속 부여
            if (event instanceof EntityDeathEvent deathEvent) {
                if (player.equals(deathEvent.getEntity().getKiller())) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1)); // 60틱 = 3초, 레벨 1 = 신속 II
                    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }
    }

    // ==========================================
    // 3. 피의 갈증 (PRISM)
    // ==========================================
    public static class Bloodthirst implements Augment {
        private final Random random = new Random();

        @Override public String getId() { return "bloodthirst"; }
        @Override public String getName() { return "피의 갈증"; }
        @Override public AugmentTier getTier() { return AugmentTier.PRISM; }
        @Override public List<String> getTags() { return Arrays.asList("WARLORD"); }
        @Override public Material getIcon() { return Material.REDSTONE; } // 기획서 아이콘 반영

        @Override
        public List<String> getDescription() {
            return Arrays.asList(
                "§f공격 시 §e10% 확률§f로 가한 데미지의",
                "§c20%§f를 체력으로 회복합니다."
            );
        }

        @Override
        public void execute(Player player, Event event) {
            if (event instanceof EntityDamageByEntityEvent damageEvent) {
                if (damageEvent.getDamager().equals(player)) {
                    // 10% 확률 계산
                    if (random.nextDouble() <= 0.10) {
                        double healAmount = damageEvent.getFinalDamage() * 0.20;
                        double maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                        
                        // 현재 체력 + 회복량이 최대 체력을 넘지 않도록 안전하게 회복
                        player.setHealth(Math.min(maxHp, player.getHealth() + healAmount));
                        
                        // 흡혈 이펙트 및 사운드
                        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 5);
                        player.playSound(player.getLocation(), Sound.ENTITY_WITCH_DRINK, 0.5f, 1.2f);
                    }
                }
            }
        }
    }
}