package main_plugin.collection;

import main_plugin.NexusCore;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class CollectionListener implements Listener {

    private final NexusCore plugin;

    public CollectionListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCollectionClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        String title = event.getView().getTitle();

        if (holder instanceof CollectionMenuGUI || holder instanceof CollectionGUI || title.equals("§8[ NEXUS ] 증강체 콜렉션")) {
            event.setCancelled(true); 

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == org.bukkit.Material.AIR) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

            if (holder instanceof CollectionMenuGUI) {
                int slot = event.getRawSlot();
                if (slot == 12) {
                    player.openInventory(new CollectionGUI(plugin, player, 0).getInventory());
                } else if (slot == 14) {
                    plugin.getCollectionManager().openAugmentCollection(player);
                }
            }
            
            else if (holder instanceof CollectionGUI) {
                CollectionGUI gui = (CollectionGUI) holder;
                int slot = event.getRawSlot();
                
                if (slot == 45) { 
                    player.openInventory(new CollectionMenuGUI().getInventory());
                } else if (slot == 46 && gui.getPage() > 0) { 
                    player.openInventory(new CollectionGUI(plugin, player, gui.getPage() - 1).getInventory());
                } else if (slot == 47) { 
                    player.openInventory(new CollectionGUI(plugin, player, gui.getPage() + 1).getInventory());
                } 
                // [신규] 하단 도감 보상 슬롯 클릭 감지 (48: 1단계, 50: 2단계, 52: 3단계)
                else if (slot == 48 || slot == 50 || slot == 52) {
                    handleRewardClaim(player, slot);
                    // 클릭 후 UI를 즉시 새로고침하여 수령 완료(배리어) 상태로 바꿔줍니다.
                    player.openInventory(new CollectionGUI(plugin, player, gui.getPage()).getInventory());
                }
            }
        }
    }

    // [신규] 보상 수령 로직
    private void handleRewardClaim(Player player, int slot) {
        CollectionData data = plugin.getCollectionManager().getCollectionData(player.getUniqueId());
        if (data == null) return;

        int currentPoints = data.getTotalPoints();
        int currentTier = data.getRewardTier();
        
        int targetTier = 0;
        int reqPoints = 0;
        int rewardDP = 0;

        // 슬롯 번호에 따른 보상 데이터 설정
        if (slot == 48) { targetTier = 1; reqPoints = 100; rewardDP = 500; }
        else if (slot == 50) { targetTier = 2; reqPoints = 500; rewardDP = 2000; }
        else if (slot == 52) { targetTier = 3; reqPoints = 1000; rewardDP = 5000; }

        // 1. 이미 수령한 경우
        if (currentTier >= targetTier) {
            player.sendMessage("§c[!] 이미 수령한 보상입니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 2. 이전 단계를 건너뛰고 수령하려는 경우
        if (currentTier != targetTier - 1) {
            player.sendMessage("§c[!] 이전 단계의 도감 보상을 먼저 수령해야 합니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 3. 포인트가 부족한 경우
        if (currentPoints < reqPoints) {
            player.sendMessage("§c[!] 포인트가 부족하여 보상을 수령할 수 없습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 4. 수령 조건 통과 (데이터 갱신 및 보상 지급)
        data.setRewardTier(targetTier);
        
        // 🔥 [에러 해결 부분] 값이 변하지 않는 final 변수로 복사하여 람다식으로 넘겨줍니다.
        final int finalRewardDP = rewardDP;
        
        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            user.setPoints(user.getPoints() + finalRewardDP);
            plugin.getUserManager().saveUserData(user); // DB 및 메모리 동기화!
        });

        player.sendMessage("§a§l[도감 달성] §f축하합니다! 도감 마일스톤 보상으로 §b" + String.format("%,d", finalRewardDP) + " DP§f가 지급되었습니다!");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
    }
}