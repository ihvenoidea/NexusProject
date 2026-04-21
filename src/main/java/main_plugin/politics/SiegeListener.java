package main_plugin.politics;

import main_plugin.NexusCore;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 공성전(레이드) 코어 타격 및 오토마우스 방지 로직을 담당합니다.
 */
public class SiegeListener implements Listener {

    private final NexusCore plugin;
    // 오토마우스 방지를 위한 플레이어별 마지막 타격 시간 저장 (UUID, 밀리초)
    private final Map<UUID, Long> attackCooldowns = new HashMap<>();

    public SiegeListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCoreHit(PlayerInteractEvent event) {
        // 1. 좌클릭으로 블록을 때렸을 때만 작동
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        SiegeManager siegeManager = plugin.getSiegeManager();
        
        // 2. 공성전이 활성화 상태인지 확인
        if (!siegeManager.isSiegeActive()) return;

        Location clickedLoc = event.getClickedBlock().getLocation();
        Location coreLoc = siegeManager.getCoreLocation();

        // 3. 때린 블록이 설정된 코어 위치와 일치하는지 확인
        if (coreLoc != null && clickedLoc.equals(coreLoc)) {
            // 블록이 실제로 부서지는 것을 방지
            event.setCancelled(true);
            
            Player player = event.getPlayer();

            // 4. 오토마우스 및 매크로 방지 (500ms = 0.5초 쿨타임)
            long currentTime = System.currentTimeMillis();
            if (attackCooldowns.containsKey(player.getUniqueId())) {
                long lastAttack = attackCooldowns.get(player.getUniqueId());
                if (currentTime - lastAttack < 500) {
                    return; // 0.5초가 지나지 않았으면 데미지 무시
                }
            }
            attackCooldowns.put(player.getUniqueId(), currentTime);

            // ==============================================================
            // [버그 픽스] 5. 플레이어의 실제 공격 데미지 정밀 계산
            // ==============================================================
            double damage = 1.0; // 기본 맨손 데미지
            
            // 5-1. 기본 스탯 (힘 포션, 무기 기본 데미지 포함됨)
            AttributeInstance attackAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (attackAttr != null) {
                damage = attackAttr.getValue();
            }

            // 5-2. 날카로움(Sharpness) 인챈트 데미지 수동 합산 (기본 스탯에 포함 안 됨)
            ItemStack weapon = player.getInventory().getItemInMainHand();
            if (weapon.hasItemMeta()) {
                int sharpLevel = 0;
                for (org.bukkit.enchantments.Enchantment ench : weapon.getEnchantments().keySet()) {
                    if (ench.getKey().getKey().equals("sharpness")) {
                        sharpLevel = weapon.getEnchantments().get(ench);
                        break;
                    }
                }
                if (sharpLevel > 0) {
                    damage += (0.5 * sharpLevel) + 0.5; // 날카로움 공식: 0.5 * 레벨 + 0.5
                }
            }

            // 5-3. 점프 크리티컬 히트 (플레이어가 공중에 있고 낙하 중일 때 데미지 1.5배)
            if (!player.isOnGround() && player.getVelocity().getY() < 0) {
                damage *= 1.5;
                // 크리티컬 떴음을 알려주는 파티클
                player.getWorld().spawnParticle(Particle.CRIT, clickedLoc.clone().add(0.5, 0.5, 0.5), 15);
            }

            // 6. 매니저를 통해 코어에 최종 데미지 전달
            siegeManager.damageCore(player, damage);
        }
    }
}