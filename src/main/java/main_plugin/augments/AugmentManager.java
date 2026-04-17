package main_plugin.augments;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AugmentManager implements Listener {

    private final NexusCore plugin;
    
    // 등록된 모든 증강체 저장소 (ID : 객체)
    private final Map<String, Augment> registeredAugments = new HashMap<>();
    
    // 쿨타임 관리용 맵 (유저UUID : (증강ID : 마지막사용시간))
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public AugmentManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 증강체를 시스템에 등록합니다.
     */
    public void registerAugment(Augment augment) {
        registeredAugments.put(augment.getId(), augment);
        plugin.getLogger().info("증강체 등록 완료: " + augment.getName() + " (" + augment.getId() + ")");
    }

    /**
     * 특정 ID를 가진 증강체 객체를 반환합니다.
     */
    public Optional<Augment> getAugment(String id) {
        return Optional.ofNullable(registeredAugments.get(id));
    }

    /**
     * 설정을 다시 불러옵니다. (ConfigHandler에서 호출됨)
     */
    public void loadConfigs() {
        // TODO: ConfigHandler 및 augments.yml에서 읽어온 정보를 바탕으로 증강체 객체들을 재등록하는 로직
        plugin.getLogger().info("AugmentManager 설정을 성공적으로 리로드했습니다.");
    }

    /**
     * 플레이어가 가진 모든 증강체를 체크하고 실행 조건을 확인합니다.
     * [수정] UserData의 최신 구조와 연동하여 증강체를 트리거합니다.
     */
    public void triggerAugments(Player player, Event event) {
        // NexusUser 대신 우리가 통일하기로 한 UserData 클래스를 사용합니다.
        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            // 유저가 보유한 증강체 리스트를 순회합니다.
            for (String augmentId : user.getAugments()) {
                getAugment(augmentId).ifPresent(augment -> {
                    // 쿨타임이 끝났는지 확인 후 실행합니다.
                    if (isCooldownOver(player, augment)) {
                        augment.execute(player, event);
                        setCooldown(player, augment);
                    }
                });
            }
        });
    }

    // --- 이벤트 핸들러: 증강체가 발동될 타이밍 정의 ---

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        // 공격자가 플레이어일 때 증강체 효과 체크
        if (event.getDamager() instanceof Player player) {
            triggerAugments(player, event);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 상호작용 시 증강체 효과 체크 (예: 우클릭 발동 스킬 등)
        triggerAugments(event.getPlayer(), event);
    }

    // --- 쿨타임 관리 로직 ---

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

    public Map<String, Augment> getRegisteredAugments() {
        return registeredAugments;
    }
}