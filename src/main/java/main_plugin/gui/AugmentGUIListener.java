package main_plugin.gui;

import main_plugin.NexusCore;
import main_plugin.augments.Augment;
import main_plugin.user.UserData;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AugmentGUIListener implements Listener {

    private final NexusCore plugin;
    private final NamespacedKey augmentKey;
    // 선택을 완료한 유저를 임시 저장하여 중복 환급(닫기 이벤트)을 방지합니다.
    private final Set<UUID> processingPlayers = new HashSet<>();

    public AugmentGUIListener(NexusCore plugin) {
        this.plugin = plugin;
        this.augmentKey = new NamespacedKey(plugin, "augment_id");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AugmentSelectorGUI)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (!clickedItem.hasItemMeta()) return;

        String augmentId = clickedItem.getItemMeta().getPersistentDataContainer().get(augmentKey, PersistentDataType.STRING);
        
        if (augmentId != null) {
            Optional<UserData> userData = plugin.getUserManager().getUser(player.getUniqueId());
            if (userData.isPresent()) {
                UserData user = userData.get();
                Optional<Augment> clickedAugmentOpt = plugin.getAugmentManager().getAugment(augmentId);
                
                if (clickedAugmentOpt.isEmpty()) return;
                Augment aug = clickedAugmentOpt.get();
                
                // 중복 획득 시 DP 페이백 로직
                if (user.getAugments().contains(augmentId)) {
                    int refundDP = getRefundAmount(aug.getTier());
                    
                    user.setPoints(user.getPoints() + refundDP);
                    plugin.getUserManager().saveUserData(user);

                    player.sendMessage("§c[!] 이미 보유하고 있는 증강체입니다.");
                    player.sendMessage("§a[!] 중복 보상으로 §e" + refundDP + " DP§a가 반환되었습니다!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    
                    processingPlayers.add(player.getUniqueId()); // 정상 처리됨을 기록
                    player.closeInventory();
                    return;
                }
                
                // 정상 획득 로직
                user.getAugments().add(augmentId);
                plugin.getUserManager().saveUserData(user);
                
                plugin.getCollectionManager().registerEntry(player, augmentId, aug.getTier().name(), aug.getName());

                player.sendMessage("§a§l[!] §f성공적으로 §e" + aug.getName() + " §f증강체를 획득했습니다!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                
                processingPlayers.add(player.getUniqueId()); // 정상 처리됨을 기록
                player.closeInventory();
            }
        }
    }

    /**
     * 증강체 선택창을 그냥 닫았을 때(ESC 등) 실행되는 환급 로직
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof AugmentSelectorGUI gui)) return;
        Player player = (Player) event.getPlayer();

        // 플레이어가 이미 클릭을 통해 증강체를 받았거나 중복 환급을 받은 경우 제외
        if (processingPlayers.contains(player.getUniqueId())) {
            processingPlayers.remove(player.getUniqueId());
            return;
        }

        // 아무것도 선택하지 않고 창을 닫은 경우 해당 티어만큼 DP 환급
        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            int refundAmount = getRefundAmount(gui.getTier()); // 수정된 GUI 클래스의 getTier() 호출
            
            user.setPoints(user.getPoints() + refundAmount);
            plugin.getUserManager().saveUserData(user);
            
            player.sendMessage("§e[!] 증강체를 선택하지 않고 창을 닫아 §b" + refundAmount + " DP§e가 환급되었습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);
        });
    }

    /**
     * 티어별 환급액 계산 유틸리티
     */
    private int getRefundAmount(main_plugin.augments.AugmentTier tier) {
        return switch (tier) {
            case SILVER -> 100;
            case GOLD -> 300;
            case PRISM -> 800;
            case MYTHIC -> 2000;
        };
    }
}