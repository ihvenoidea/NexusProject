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
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class AugmentGUIListener implements Listener {

    private final NexusCore plugin;
    private final NamespacedKey augmentKey;

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
                
                // [신규 로직] 중복 획득 시 DP 페이백
                if (user.getAugments().contains(augmentId)) {
                    int refundDP = switch (aug.getTier()) {
                        case SILVER -> 100;
                        case GOLD -> 300;
                        case PRISM -> 800;
                        case MYTHIC -> 2000;
                    };
                    
                    user.setPoints(user.getPoints() + refundDP);
                    plugin.getUserManager().saveUserData(user); // 변경된 DP 저장

                    player.sendMessage("§c[!] 이미 보유하고 있는 증강체입니다.");
                    player.sendMessage("§a[!] 중복 보상으로 §e" + refundDP + " DP§a가 반환되었습니다!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    player.closeInventory();
                    return;
                }
                
                // 정상 획득 로직
                user.getAugments().add(augmentId);
                plugin.getUserManager().saveUserData(user);
                
                plugin.getCollectionManager().registerEntry(player, augmentId, aug.getTier().name(), aug.getName());

                player.sendMessage("§a§l[!] §f성공적으로 §e" + aug.getName() + " §f증강체를 획득했습니다!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                player.closeInventory();
            }
        }
    }
}