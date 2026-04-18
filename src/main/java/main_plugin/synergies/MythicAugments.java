package main_plugin.augments.synergies;

import main_plugin.NexusCore;
import main_plugin.augments.Augment;
import main_plugin.augments.AugmentTier;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
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
 * 게임 내 최고 등급인 [신화] 증강체 모음입니다.
 */
public class MythicAugments {

    // ==========================================
    // 1. [철갑] 불사의 방패
    // ==========================================
    public static class ImmortalShield implements Augment {
        @Override public String getId() { return "immortal_shield"; }
        @Override public String getName() { return "불사의 방패"; }
        @Override public AugmentTier getTier() { return AugmentTier.MYTHIC; }
        @Override public List<String> getTags() { return Arrays.asList("IRONCLAD"); }
        @Override public Material getIcon() { return Material.NETHERITE_CHESTPLATE; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList("§f상시 최대 체력이 §a+20 (하트 10칸) §f증가하는", "§f궁극의 방어 증강체입니다.");
        }

        @Override
        public void execute(Player player, Event event) {
            AttributeInstance hpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (hpAttr == null) return;

            NamespacedKey key = new NamespacedKey(NexusCore.getInstance(), "immortal_shield_hp");
            for (AttributeModifier mod : hpAttr.getModifiers()) {
                if (mod.getKey().equals(key)) return; 
            }
            hpAttr.addModifier(new AttributeModifier(key, 20.0, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    // ==========================================
    // 2. [패왕] 전쟁의 신
    // ==========================================
    public static class GodOfWar implements Augment {
        @Override public String getId() { return "god_of_war"; }
        @Override public String getName() { return "전쟁의 신"; }
        @Override public AugmentTier getTier() { return AugmentTier.MYTHIC; }
        @Override public List<String> getTags() { return Arrays.asList("WARLORD"); }
        @Override public Material getIcon() { return Material.NETHERITE_SWORD; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList("§f자신이 가하는 모든 근접 피해량이", "§c상시 20% 증폭§f됩니다.");
        }

        @Override
        public void execute(Player player, Event event) {
            if (event instanceof EntityDamageByEntityEvent damageEvent) {
                if (damageEvent.getDamager().equals(player)) {
                    damageEvent.setDamage(damageEvent.getDamage() * 1.20);
                }
            }
        }
    }

    // ==========================================
    // 3. [맹독] 역병의 군주
    // ==========================================
    public static class PlagueLord implements Augment {
        private final Random random = new Random();

        @Override public String getId() { return "plague_lord"; }
        @Override public String getName() { return "역병의 군주"; }
        @Override public AugmentTier getTier() { return AugmentTier.MYTHIC; }
        @Override public List<String> getTags() { return Arrays.asList("TOXIC"); }
        @Override public Material getIcon() { return Material.WITHER_SKELETON_SKULL; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList("§f공격 시 §e30% 확률§f로 대상에게", "§8위더 II (3초)§f를 부여합니다.");
        }

        @Override
        public void execute(Player player, Event event) {
            if (event instanceof EntityDamageByEntityEvent damageEvent) {
                if (damageEvent.getDamager().equals(player) && damageEvent.getEntity() instanceof LivingEntity target) {
                    if (random.nextDouble() <= 0.30) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));
                        // 1.21 API 변경점 적용: SMOKE_LARGE -> LARGE_SMOKE
                        target.getWorld().spawnParticle(Particle.LARGE_SMOKE, target.getLocation().add(0, 1, 0), 15);
                    }
                }
            }
        }
    }

    // ==========================================
    // 4. [폭탄] 아마겟돈 (액티브)
    // ==========================================
    public static class Armageddon implements Augment {
        @Override public String getId() { return "armageddon"; }
        @Override public String getName() { return "아마겟돈"; }
        @Override public AugmentTier getTier() { return AugmentTier.MYTHIC; }
        @Override public List<String> getTags() { return Arrays.asList("BOMB"); }
        @Override public Material getIcon() { return Material.TNT_MINECART; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList(
                "§f웅크린(Shift) 상태로 좌클릭 시",
                "§f파괴적인 §8위더 해골§f을 발사합니다.",
                "§8(지형 파괴 없음 / 쿨타임: 15초)"
            );
        }

        @Override
        public long getCooldown() { return 15000; }

        @Override
        public void execute(Player player, Event event) {
            if (event instanceof PlayerInteractEvent interactEvent) {
                Action action = interactEvent.getAction();
                if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) && player.isSneaking()) {
                    WitherSkull skull = player.launchProjectile(WitherSkull.class);
                    skull.setYield(0); // 지형 파괴 방지
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);
                }
            }
        }
    }
}