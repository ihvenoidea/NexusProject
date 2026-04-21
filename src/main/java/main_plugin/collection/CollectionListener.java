package main_plugin.collection;

import main_plugin.NexusCore;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class CollectionListener implements Listener {

    private final NexusCore plugin;

    public CollectionListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // [1] 땅에 떨어진 아이템을 주웠을 때 도감 자동 등록
    // ==========================================
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack item = event.getItem().getItemStack();
        checkAndRegisterVanillaItem(player, item);
    }

    // ==========================================
    // [2] 인벤토리/GUI 클릭 이벤트 처리
    // ==========================================
    @EventHandler
    public void onCollectionClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        String title = event.getView().getTitle();

        // 도감 GUI 자체를 클릭했을 때의 처리 (아이템 빼기 방지 및 버튼 작동)
        if (holder instanceof CollectionMenuGUI || holder instanceof CollectionGUI || title.equals("§8[ NEXUS ] 증강체 콜렉션")) {
            event.setCancelled(true); 

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == org.bukkit.Material.AIR) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

            // 메인 메뉴 클릭
            if (holder instanceof CollectionMenuGUI) {
                int slot = event.getRawSlot();
                if (slot == 12) {
                    player.openInventory(new CollectionGUI(plugin, player, 0).getInventory());
                } else if (slot == 14) {
                    plugin.getCollectionManager().openAugmentCollection(player);
                }
            }
            // 바닐라 도감 목록 클릭
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
                else if (slot == 48 || slot == 50 || slot == 52) {
                    handleRewardClaim(player, slot);
                    player.openInventory(new CollectionGUI(plugin, player, gui.getPage()).getInventory());
                }
            }
            return; // GUI 클릭이면 등록 로직으로 넘어가지 않고 여기서 종료
        }

        // 일반 인벤토리에서 아이템을 만질 때 (상자에서 꺼내기, 제작 등) 도감 자동 등록 검사
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getCurrentItem() != null) {
                checkAndRegisterVanillaItem(player, event.getCurrentItem());
            }
        }
    }

    // ==========================================
    // [3] 바닐라 아이템 도감 검사 및 등록 유틸리티
    // ==========================================
    private void checkAndRegisterVanillaItem(Player player, ItemStack item) {
        // 공기거나 아이템 형태가 아니면 무시
        if (item == null || item.getType().isAir() || !item.getType().isItem()) return;

        // [핵심 버그 수정] 이름이 바뀌어 있는 아이템(GUI 버튼, 특수 장비, 플러그인 아이템 등)은 바닐라 도감 등록에서 완벽히 제외!
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) return;

        String id = item.getType().name().toLowerCase();
        CollectionData data = plugin.getCollectionManager().getCollectionData(player.getUniqueId());
        
        // 아직 수집하지 않은 순정 바닐라 아이템이라면 등록
        if (data != null && !data.hasCollected(id)) {
            String displayName = item.getType().name().replace("_", " ");
            // 바닐라 아이템은 "common" 등급으로 취급하여 1포인트씩 지급
            plugin.getCollectionManager().registerEntry(player, id, "common", displayName);
        }
    }

    // ==========================================
    // [4] 마일스톤 보상 수령 로직
    // ==========================================
    private void handleRewardClaim(Player player, int slot) {
        CollectionData data = plugin.getCollectionManager().getCollectionData(player.getUniqueId());
        if (data == null) return;

        int currentPoints = data.getTotalPoints();
        int currentTier = data.getRewardTier();
        
        int targetTier = 0;
        int reqPoints = 0;
        int rewardDP = 0;

        // 슬롯 번호에 따른 보상 티어 설정
        if (slot == 48) { targetTier = 1; reqPoints = 100; rewardDP = 500; }
        else if (slot == 50) { targetTier = 2; reqPoints = 500; rewardDP = 2000; }
        else if (slot == 52) { targetTier = 3; reqPoints = 1000; rewardDP = 5000; }

        // 예외 처리: 이미 수령함
        if (currentTier >= targetTier) {
            player.sendMessage("§c[!] 이미 수령한 보상입니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 예외 처리: 이전 단계 건너뜀
        if (currentTier != targetTier - 1) {
            player.sendMessage("§c[!] 이전 단계의 도감 보상을 먼저 수령해야 합니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 예외 처리: 포인트 부족
        if (currentPoints < reqPoints) {
            player.sendMessage("§c[!] 포인트가 부족하여 보상을 수령할 수 없습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 보상 지급 처리
        data.setRewardTier(targetTier);
        final int finalRewardDP = rewardDP;
        
        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            user.setPoints(user.getPoints() + finalRewardDP);
            plugin.getUserManager().saveUserData(user); 
        });

        player.sendMessage("§a§l[도감 달성] §f축하합니다! 도감 마일스톤 보상으로 §b" + String.format("%,d", finalRewardDP) + " DP§f가 지급되었습니다!");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
    }
}