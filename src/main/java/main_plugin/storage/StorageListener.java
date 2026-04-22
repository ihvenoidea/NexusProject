package main_plugin.storage;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Optional;

public class StorageListener implements Listener {

    private final NexusCore plugin;

    public StorageListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        // [복사 꼼수 완벽 차단] 일반 상자의 이름을 바꿔서 연 경우를 걸러냅니다.
        if (event.getInventory().getHolder() != null) return; 

        String title = event.getView().getTitle();
        if (!title.equals("§8[ NEXUS ] 창고 메뉴")) return;
        event.setCancelled(true); 

        if (event.getCurrentItem() == null) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        Optional<UserData> userOpt = plugin.getUserManager().getUser(player.getUniqueId());
        if (userOpt.isEmpty()) return;
        UserData user = userOpt.get();
        int unlocked = user.getUnlockedBoxes();

        if (slot >= 11 && slot <= 11 + plugin.getStorageManager().MAX_BOXES - 1) {
            int targetBox = slot - 10;
            if (targetBox <= unlocked) {
                player.closeInventory();
                plugin.getStorageManager().openBox(player, targetBox);
            } 
            else if (targetBox == unlocked + 1) {
                double cost = (targetBox - 1) * plugin.getStorageManager().BASE_COST;
                if (plugin.getDatabaseManager().getMoney(player.getUniqueId().toString()) >= cost) {
                    plugin.getDatabaseManager().deductMoney(player.getUniqueId().toString(), cost);
                    user.setMoney(user.getMoney() - cost); 
                    user.setUnlockedBoxes(targetBox);
                    
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        plugin.getDatabaseManager().saveUserData(player.getUniqueId());
                    });

                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    player.sendMessage("§a[!] " + targetBox + "번 창고를 성공적으로 구매했습니다!");
                    plugin.getStorageManager().openStorageMenu(player); 
                } else {
                    player.sendMessage("§c[!] 돈이 부족합니다. (필요 금액: " + String.format("%,.0f원", cost) + ")");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onBoxClose(InventoryCloseEvent event) {
        // [복사 꼼수 완벽 차단] 진짜 넥서스 가상 창고가 맞는지 확인합니다.
        if (event.getInventory().getHolder() != null) return; 

        String title = event.getView().getTitle();
        if (title.startsWith("§8[ NEXUS ] 창고 - ")) {
            Player player = (Player) event.getPlayer();
            try {
                String boxStr = title.replace("§8[ NEXUS ] 창고 - ", "").replace("번 박스", "").trim();
                int boxIndex = Integer.parseInt(boxStr);
                
                // 가상 인벤토리의 내용물만 안전하게 DB로 전송합니다.
                plugin.getStorageManager().saveBox(player, boxIndex, event.getInventory().getContents());
            } catch (Exception e) {
                player.sendMessage("§c[!] 창고 저장 중 오류 발생!");
                e.printStackTrace();
            }
        }
    }
}