package main_plugin.augments.synergies;

import main_plugin.NexusCore;
import main_plugin.augments.Augment;
import main_plugin.augments.AugmentTier;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * [철갑] 태그를 공유하는 생존/방어 특화 증강체 모음입니다.
 */
public class IroncladSynergy {

    // ==========================================
    // 1. 철갑 기사 (SILVER)
    // ==========================================
    public static class IroncladKnight implements Augment {
        @Override public String getId() { return "ironclad_knight"; }
        @Override public String getName() { return "철갑 기사"; }
        @Override public AugmentTier getTier() { return AugmentTier.SILVER; }
        @Override public List<String> getTags() { return Arrays.asList("IRONCLAD"); }
        @Override public Material getIcon() { return Material.IRON_CHESTPLATE; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList("§f상시 최대 체력이 §a+4 (하트 2칸) §f증가합니다.");
        }

        @Override
        public void execute(Player player, Event event) {
            // 패시브 스탯 적용: 체력 증가 모디파이어가 없다면 추가합니다.
            applyHealthModifier(player, "ironclad_knight_hp", 4.0);
        }
    }

    // ==========================================
    // 2. 피돼지 (GOLD)
    // ==========================================
    public static class BloodPig implements Augment {
        @Override public String getId() { return "blood_pig"; }
        @Override public String getName() { return "피돼지"; }
        @Override public AugmentTier getTier() { return AugmentTier.GOLD; }
        @Override public List<String> getTags() { return Arrays.asList("IRONCLAD"); }
        @Override public Material getIcon() { return Material.PORKCHOP; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList(
                "§f상시 최대 체력이 §a+10 (하트 5칸) §f증가하지만,",
                "§f자신이 가하는 모든 데미지가 §c15% 감소§f합니다."
            );
        }

        @Override
        public void execute(Player player, Event event) {
            // 1. 패시브 스탯 적용
            applyHealthModifier(player, "blood_pig_hp", 10.0);

            // 2. 공격 시 데미지 15% 감소 (디버프)
            if (event instanceof EntityDamageByEntityEvent damageEvent) {
                if (damageEvent.getDamager().equals(player)) {
                    double originalDamage = damageEvent.getDamage();
                    damageEvent.setDamage(originalDamage * 0.85);
                }
            }
        }
    }

    // ==========================================
    // 3. 가시갑옷 (PRISM)
    // ==========================================
    public static class ThornArmor implements Augment {
        private final Random random = new Random();

        @Override public String getId() { return "thorn_armor"; }
        @Override public String getName() { return "가시갑옷"; }
        @Override public AugmentTier getTier() { return AugmentTier.PRISM; }
        @Override public List<String> getTags() { return Arrays.asList("IRONCLAD"); }
        @Override public Material getIcon() { return Material.CACTUS; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList(
                "§f피격 시 §e20% 확률§f로 받은 데미지의",
                "§c20%§f를 공격자에게 반사합니다."
            );
        }

        @Override
        public void execute(Player player, Event event) {
            // 피격 당했을 때만 발동
            if (event instanceof EntityDamageByEntityEvent damageEvent) {
                if (damageEvent.getEntity().equals(player) && damageEvent.getDamager() instanceof LivingEntity attacker) {
                    if (random.nextDouble() <= 0.20) { // 20% 확률
                        double reflectDamage = damageEvent.getDamage() * 0.20;
                        attacker.damage(reflectDamage, player); // 반사 데미지 적용
                        
                        // 쏜 사람에게 가시 찔리는 소리
                        attacker.getWorld().playSound(attacker.getLocation(), org.bukkit.Sound.ENCHANT_THORNS_HIT, 1.0f, 1.0f);
                    }
                }
            }
        }
    }

    // ==========================================
    // [유틸리티] 1.21.4 호환 체력 증가 모디파이어 안전 적용 메서드
    // ==========================================
    private static void applyHealthModifier(Player player, String keyName, double amount) {
        AttributeInstance hpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hpAttr == null) return;

        // 1.21.4 방식: NamespacedKey 사용
        NamespacedKey key = new NamespacedKey(NexusCore.getInstance(), keyName);

        // 이미 적용되어 있는지 확인 (무한 중첩 방지)
        for (AttributeModifier mod : hpAttr.getModifiers()) {
            if (mod.getKey().equals(key)) return; 
        }

        AttributeModifier modifier = new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER);
        hpAttr.addModifier(modifier);
    }
}