package main_plugin.augments.synergies;

import main_plugin.NexusCore;
import main_plugin.augments.Augment;
import main_plugin.augments.AugmentTier;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;
import java.util.List;

public class BombSynergy {

    public static class BombLauncher implements Augment {
        @Override public String getId() { return "bomb_launcher"; }
        @Override public String getName() { return "폭탄 발사기"; }
        @Override public AugmentTier getTier() { return AugmentTier.SILVER; }
        @Override public List<String> getTags() { return Arrays.asList("BOMB"); }
        @Override public Material getIcon() { return Material.FIRE_CHARGE; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList(
                "§f웅크린(Shift) 상태로 좌클릭 시",
                "§f바라보는 방향으로 §c화염구§f를 발사합니다.",
                "§8(지형 파괴 없음 / 쿨타임: 10초)"
            );
        }

        @Override
        public long getCooldown() {
            return 10000; 
        }

        @Override
        public void execute(Player player, Event event) {
            if (event instanceof PlayerInteractEvent interactEvent) {
                Action action = interactEvent.getAction();
                if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) && player.isSneaking()) {
                    
                    Fireball fireball = player.launchProjectile(Fireball.class);
                    fireball.setYield(0); 
                    fireball.setIsIncendiary(false); 
                    
                    player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.0f);

                    // [추가됨] 쿨타임 시작
                    NexusCore.getInstance().getAugmentManager().startCooldown(player, this);
                }
            }
        }
    }

    public static class ExplosionResistance implements Augment {
        @Override public String getId() { return "explosion_resistance"; }
        @Override public String getName() { return "폭발 내성"; }
        @Override public AugmentTier getTier() { return AugmentTier.GOLD; }
        @Override public List<String> getTags() { return Arrays.asList("BOMB"); }
        @Override public Material getIcon() { return Material.CREEPER_HEAD; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList("§f크리퍼, TNT 등 모든 종류의", "§c폭발 데미지에 면역§f이 됩니다.");
        }

        @Override
        public void execute(Player player, Event event) {
            if (event instanceof EntityDamageEvent damageEvent) {
                if (damageEvent.getEntity().equals(player)) {
                    EntityDamageEvent.DamageCause cause = damageEvent.getCause();
                    if (cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || 
                        cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                        damageEvent.setCancelled(true);
                    }
                }
            }
        }
    }

    public static class SuicideBomber implements Augment {
        @Override public String getId() { return "suicide_bomber"; }
        @Override public String getName() { return "자폭병"; }
        @Override public AugmentTier getTier() { return AugmentTier.PRISM; }
        @Override public List<String> getTags() { return Arrays.asList("BOMB"); }
        @Override public Material getIcon() { return Material.TNT; }

        @Override
        public List<String> getDescription() {
            return Arrays.asList(
                "§f사망 시 자신의 위치에",
                "§c강력한 폭발§f을 일으켜 적을 공격합니다.",
                "§8(지형 파괴 없음)"
            );
        }

        @Override
        public void execute(Player player, Event event) {
            if (event instanceof PlayerDeathEvent deathEvent) {
                if (deathEvent.getEntity().equals(player)) {
                    player.getWorld().createExplosion(player.getLocation(), 4.0F, false, false);
                }
            }
        }
    }
}