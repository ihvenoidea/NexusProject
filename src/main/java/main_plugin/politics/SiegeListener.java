package main_plugin.politics;

import main_plugin.NexusCore;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

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

            // 5. 플레이어의 실제 공격 데미지 계산 (무기, 인챈트, 포션 효과 포함)
            double damage = 1.0; // 기본 맨손 데미지
            AttributeInstance attackAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (attackAttr != null) {
                damage = attackAttr.getValue();
            }

            // 6. 매니저를 통해 코어에 데미지 전달
            siegeManager.damageCore(player, damage);
        }
    }
}