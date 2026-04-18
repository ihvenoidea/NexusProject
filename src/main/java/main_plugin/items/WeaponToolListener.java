package main_plugin.items;

import main_plugin.NexusCore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

/**
 * 넥서스 특수 무기와 도구의 고유 기능(스킬)을 담당하는 리스너입니다.
 */
public class WeaponToolListener implements Listener {

    private final NexusCore plugin;
    private final NamespacedKey nameKey;

    // 골드 도구(자동 정제)를 위한 화로 레시피 맵
    private final Map<Material, Material> smeltMap = new HashMap<>();

    public WeaponToolListener(NexusCore plugin) {
        this.plugin = plugin;
        this.nameKey = plugin.getSetItemManager().getNameKey();

        // 자동 정제 맵 초기화
        smeltMap.put(Material.IRON_ORE, Material.IRON_INGOT);
        smeltMap.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        smeltMap.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        smeltMap.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        smeltMap.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        smeltMap.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
    }

    // ==========================================
    // [ 1. 검 (Sword) 기능 ] - 둔화(프리즘), 흡혈(신화)
    // ==========================================
    @EventHandler
    public void onSwordHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!weapon.hasItemMeta()) return;

        String setName = weapon.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        if (setName == null) return;

        if (weapon.getType().toString().contains("SWORD")) {
            switch (setName) {
                case "격류": // 둔화의 칼날
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1)); // 2초 둔화
                    target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 10);
                    break;
                case "황금": // 흡혈 (피해량의 4%)
                    double heal = event.getFinalDamage() * 0.04;
                    double maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    player.setHealth(Math.min(maxHp, player.getHealth() + heal));
                    target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 5);
                    break;
            }
        }
    }

    // ==========================================
    // [ 2. 활 (Bow) 기능 - 발사 ] - 발사 속도(실버), 활 태그 부여
    // ==========================================
    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null || !bow.hasItemMeta()) return;

        String setName = bow.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        if (setName == null) return;

        // 화살 엔티티 자체에 어떤 활에서 쏴졌는지 태그(PDC)를 남깁니다.
        event.getProjectile().getPersistentDataContainer().set(nameKey, PersistentDataType.STRING, setName);

        if (setName.equals("신속")) { // 발사 속도(투사체 속도) 증가
            Vector velocity = event.getProjectile().getVelocity();
            event.getProjectile().setVelocity(velocity.multiply(1.5));
        } else if (setName.equals("격류")) { // 추적 화살 로직 (1틱마다 방향 보정)
            startHomingArrowTask((Arrow) event.getProjectile());
        }
    }

    // ==========================================
    // [ 3. 활 (Bow) 기능 - 적중 ] - 폭발 화살(신화)
    // ==========================================
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        
        String setName = arrow.getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        if (setName == null) return;

        if (setName.equals("황금")) { // 폭발 화살
            Location hitLoc = arrow.getLocation();
            // 블록 파괴 없이(false) 폭발 데미지와 이펙트만 발생 (위력 2.0F)
            arrow.getWorld().createExplosion(hitLoc, 2.0F, false, false);
            arrow.remove(); // 화살 삭제
        }
    }

    // ==========================================
    // [ 4. 도구 (Tools) 기능 ] - 자동 정제(골드), 3x3 범위 채광(신화)
    // ==========================================
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!tool.hasItemMeta()) return;

        String setName = tool.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        if (setName == null) return;

        // 3x3 광역 채광 시 무한 반복 루프 방지 메타데이터 확인
        if (player.hasMetadata("is_mining_skill")) return;

        Block block = event.getBlock();
        String type = tool.getType().toString();

        // 4-1. [골드] 자동 정제 (곡괭이 한정)
        if (setName.equals("방어") && type.contains("PICKAXE")) {
            if (smeltMap.containsKey(block.getType())) {
                event.setDropItems(false); // 바닐라 드랍 취소
                // 구워진 아이템 드랍
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(smeltMap.get(block.getType())));
                player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1f);
                player.getWorld().spawnParticle(Particle.FLAME, block.getLocation().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0.05);
            }
        } 
        // 4-2. [신화] 3x3 범위 채광 (곡괭이, 도끼, 삽 모두 적용)
        else if (setName.equals("황금") && (type.contains("PICKAXE") || type.contains("AXE") || type.contains("SHOVEL"))) {
            // 스킬 작동 중임을 표시 (무한 재귀 호출 방지)
            player.setMetadata("is_mining_skill", new FixedMetadataValue(plugin, true));

            Location center = block.getLocation();
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block targetBlock = center.clone().add(x, y, z).getBlock();
                        // 베드락 등 부술 수 없는 블록 방지
                        if (targetBlock.getType().getHardness() >= 0 && targetBlock.getType() != Material.AIR) {
                            targetBlock.breakNaturally(tool);
                        }
                    }
                }
            }
            player.removeMetadata("is_mining_skill", plugin);
        }
    }

    // ==========================================
    // [ 5. 보조 유틸 ] - 경험치 1.5배(골드 검)
    // ==========================================
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            ItemStack weapon = killer.getInventory().getItemInMainHand();
            
            if (weapon.hasItemMeta()) {
                String setName = weapon.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
                if ("방어".equals(setName) && weapon.getType().toString().contains("SWORD")) {
                    event.setDroppedExp((int) (event.getDroppedExp() * 1.5));
                }
            }
        }
    }

    // ==========================================
    // [ 보조 메서드 ] - 유도 화살 로직
    // ==========================================
    private void startHomingArrowTask(Arrow arrow) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (arrow.isDead() || arrow.isOnGround()) {
                    this.cancel();
                    return;
                }
                
                // 화살 주변 10블록 이내의 가장 가까운 몬스터/플레이어 찾기
                LivingEntity closest = null;
                double closestDist = 100.0;
                for (LivingEntity ent : arrow.getWorld().getLivingEntities()) {
                    if (ent == arrow.getShooter() || ent.isDead()) continue;
                    
                    double dist = ent.getLocation().distanceSquared(arrow.getLocation());
                    if (dist < 100 && dist < closestDist) { // 10블록 = 100(거리 제곱)
                        closestDist = dist;
                        closest = ent;
                    }
                }

                // 타겟이 있다면 화살 방향을 타겟 쪽으로 약간씩 휘게 함
                if (closest != null) {
                    Vector direction = closest.getLocation().add(0, closest.getHeight() / 2, 0).toVector().subtract(arrow.getLocation().toVector()).normalize();
                    arrow.setVelocity(arrow.getVelocity().add(direction.multiply(0.2)).normalize().multiply(arrow.getVelocity().length()));
                    arrow.getWorld().spawnParticle(Particle.END_ROD, arrow.getLocation(), 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L); // 1틱마다 방향 수정
    }
}