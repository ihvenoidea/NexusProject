package main_plugin.gui;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class PointShopManager implements CommandExecutor, Listener {

    private final NexusCore plugin;
    private final String GUI_NAME = ChatColor.DARK_AQUA + "Nexus 특별 조공 상점";

    public PointShopManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    // 1. /포인트 명령어 처리
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        player.sendMessage(ChatColor.GRAY + "디스코드 포인트 정보를 동기화 중...");

        // 비동기로 유저의 DP 정보를 가져와서 GUI 오픈
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int currentDP = plugin.getDatabaseManager().getDiscordPoints(player.getUniqueId().toString());
            
            Bukkit.getScheduler().runTask(plugin, () -> openShopGui(player, currentDP));
        });

        return true;
    }

    // 2. 상점 GUI 구성 (노션 기획 반영)
    private void openShopGui(Player player, int currentDP) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);

        // 상단 정보 표시: 내 현재 포인트
        inv.setItem(4, createGuiItem(Material.PLAYER_HEAD, ChatColor.GOLD + "내 정보", 
                ChatColor.WHITE + "보유 DP: " + ChatColor.AQUA + String.format("%, d", currentDP) + " DP",
                "",
                ChatColor.YELLOW + "디스코드 활동을 통해 포인트를 얻을 수 있습니다."));

        // [상품 1] 증강체 랜덤 박스 (기획 핵심: 증강체 시스템)
        inv.setItem(11, createGuiItem(Material.CHEST_MINECART, ChatColor.LIGHT_PURPLE + "📦 증강체 랜덤 박스", 
                ChatColor.GRAY + "가격: " + ChatColor.AQUA + "500 DP", 
                "", 
                ChatColor.WHITE + "사용 시 랜덤한 등급의", 
                ChatColor.WHITE + "증강체를 획득합니다.",
                "",
                ChatColor.YELLOW + "클릭하여 구매"));

        // [상품 2] 조공의 서 (기획 핵심: 조공 시스템 활성화)
        inv.setItem(13, createGuiItem(Material.ENCHANTED_BOOK, ChatColor.GOLD + "📜 조공의 서 (1.5x)", 
                ChatColor.GRAY + "가격: " + ChatColor.AQUA + "300 DP", 
                "", 
                ChatColor.WHITE + "30분 동안 자신의", 
                ChatColor.WHITE + "조공 효율이 50% 증가합니다.",
                "",
                ChatColor.YELLOW + "클릭하여 구매"));

        // [상품 3] 공성전 참여권 (기획 핵심: 성주 및 공성전 자격)
        inv.setItem(15, createGuiItem(Material.NETHER_STAR, ChatColor.RED + "🛡️ 공성전 참여권", 
                ChatColor.GRAY + "가격: " + ChatColor.AQUA + "1,000 DP", 
                "", 
                ChatColor.WHITE + "다음 공성전에 공격 측으로", 
                ChatColor.WHITE + "참여할 수 있는 자격 증명입니다.",
                "",
                ChatColor.YELLOW + "클릭하여 구매"));

        // 빈 공간 배경 채우기
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    // 3. 클릭 이벤트 처리
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        String uuid = player.getUniqueId().toString();

        switch (slot) {
            case 11 -> processPurchase(player, uuid, 500, Material.CHEST_MINECART, ChatColor.LIGHT_PURPLE + "증강체 랜덤 박스");
            case 13 -> processPurchase(player, uuid, 300, Material.ENCHANTED_BOOK, ChatColor.GOLD + "조공의 서 (1.5x)");
            case 15 -> processPurchase(player, uuid, 1000, Material.NETHER_STAR, ChatColor.RED + "공성전 참여권");
        }
    }

    // 4. 구매 로직 (포인트 차감 및 아이템 지급)
    private void processPurchase(Player player, String uuid, int cost, Material itemMaterial, String itemName) {
        player.closeInventory();
        player.sendMessage(ChatColor.GRAY + "포인트를 정산 중입니다...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = plugin.getDatabaseManager().deductDiscordPoints(uuid, cost);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.getInventory().addItem(createGuiItem(itemMaterial, itemName));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    player.sendMessage(ChatColor.GREEN + "구매 완료: " + itemName);
                    player.sendMessage(ChatColor.AQUA + "남은 포인트는 디스코드에서 확인할 수 있습니다.");
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "디스코드 포인트가 부족합니다! 디스코드에서 활동량을 높여보세요.");
                }
            });
        });
    }

    // 아이템 생성 유틸리티
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}