package main_plugin.gui;

import main_plugin.NexusCore;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class VanillaShopManager implements Listener {

    private final NexusCore plugin;
    private final String MENU_PREFIX = ChatColor.BLACK + "시장 > ";

    public VanillaShopManager(NexusCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 메인 카테고리 선택 메뉴를 엽니다.
     */
    public void openCategoryMenu(Player player) {
        FileConfiguration config = plugin.getMarketConfig();
        String title = ChatColor.translateAlternateColorCodes('&', config.getString("shop-settings.title", "중앙 시장"));
        int size = config.getInt("shop-settings.main-size", 27);
        Inventory inv = Bukkit.createInventory(null, size, title);

        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) { 
            paneMeta.setDisplayName(" "); 
            pane.setItemMeta(paneMeta); 
        }
        for (int i = 0; i < size; i++) inv.setItem(i, pane);

        ConfigurationSection categories = config.getConfigurationSection("categories");
        if (categories != null) {
            for (String key : categories.getKeys(false)) {
                ConfigurationSection cat = categories.getConfigurationSection(key);
                if (cat != null) {
                    inv.setItem(cat.getInt("slot"), createCategoryItem(cat));
                }
            }
        }
        player.openInventory(inv);
    }

    private ItemStack createCategoryItem(ConfigurationSection cat) {
        Material mat = Material.matchMaterial(cat.getString("material", "BOOK"));
        ItemStack item = new ItemStack(mat == null ? Material.BOOK : mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = cat.getString("display-name", "&8이름 없음");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "클릭하여 상점을 엽니다.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openShop(Player player, String categoryKey) {
        FileConfiguration config = plugin.getMarketConfig();
        ConfigurationSection catSection = config.getConfigurationSection("categories." + categoryKey);
        if (catSection == null) return;

        int size = config.getInt("shop-settings.category-size", 54);
        String catName = catSection.getString("display-name", "상점");
        String title = MENU_PREFIX + ChatColor.translateAlternateColorCodes('&', catName);
        Inventory inv = Bukkit.createInventory(null, size, title);

        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) { 
            paneMeta.setDisplayName(" "); 
            pane.setItemMeta(paneMeta); 
        }
        for (int i = 0; i < size; i++) inv.setItem(i, pane);

        ConfigurationSection items = catSection.getConfigurationSection("items");
        if (items != null) {
            for (String itemKey : items.getKeys(false)) {
                ConfigurationSection itemData = items.getConfigurationSection(itemKey);
                if (itemData != null) {
                    inv.setItem(itemData.getInt("slot"), createShopItem(itemData));
                }
            }
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "이전 메뉴로");
            back.setItemMeta(backMeta);
        }
        inv.setItem(size - 1, back);

        player.openInventory(inv);
    }

    private ItemStack createShopItem(ConfigurationSection data) {
        Material mat = Material.matchMaterial(data.getString("material", "BARRIER"));
        ItemStack item = new ItemStack(mat == null ? Material.BARRIER : mat);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String displayName = data.getString("display-name", "&8이름 없는 아이템");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            
            List<String> lore = new ArrayList<>();
            double buy = data.getDouble("buy-price", 0.0);
            double sell = data.getDouble("sell-price", 0.0);
            
            lore.add("");
            // 구매 가격이 음수면 '구매 불가'로 표시
            if (buy < 0) {
                lore.add(ChatColor.RED + "구매: " + ChatColor.BOLD + "구매 불가 (파밍 전용)");
            } else {
                lore.add(ChatColor.YELLOW + "구매: " + ChatColor.GOLD + String.format("%,.0f원", buy) + ChatColor.GRAY + " (좌클릭)");
            }
            
            lore.add(ChatColor.AQUA + "판매: " + ChatColor.DARK_AQUA + String.format("%,.0f원", sell) + ChatColor.GRAY + " (우클릭)");
            lore.add(ChatColor.GRAY + "Shift + 클릭 시 64개 단위 거래");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        String title = event.getView().getTitle();
        FileConfiguration config = plugin.getMarketConfig();
        if (config == null) return;

        String mainTitle = ChatColor.translateAlternateColorCodes('&', config.getString("shop-settings.title", "중앙 시장"));

        if (title.equals(mainTitle)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            handleMainClick(player, event.getSlot());
        } else if (title.startsWith(MENU_PREFIX)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            handleShopClick(player, title, event);
        }
    }

    private void handleMainClick(Player player, int slot) {
        ConfigurationSection categories = plugin.getMarketConfig().getConfigurationSection("categories");
        if (categories == null) return;

        for (String key : categories.getKeys(false)) {
            if (categories.getInt(key + ".slot") == slot) {
                openShop(player, key);
                return;
            }
        }
    }

    private void handleShopClick(Player player, String title, InventoryClickEvent event) {
        if (event.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) return;
        if (event.getSlot() == event.getInventory().getSize() - 1) {
            openCategoryMenu(player);
            return;
        }

        ConfigurationSection categories = plugin.getMarketConfig().getConfigurationSection("categories");
        if (categories == null) return;

        for (String catKey : categories.getKeys(false)) {
            String catDisplayName = categories.getString(catKey + ".display-name", "상점");
            String catFullTitle = MENU_PREFIX + ChatColor.translateAlternateColorCodes('&', catDisplayName);
            
            if (catFullTitle.equals(title)) {
                ConfigurationSection items = categories.getConfigurationSection(catKey + ".items");
                if (items == null) return;

                for (String itemKey : items.getKeys(false)) {
                    if (items.getInt(itemKey + ".slot") == event.getSlot()) {
                        processTrade(player, items.getConfigurationSection(itemKey), event.getClick());
                        return;
                    }
                }
            }
        }
    }

    /**
     * [버그 수정 완료] -1원 등 음수 가격 구매 시 차단 로직 적용
     */
    private void processTrade(Player player, ConfigurationSection itemData, ClickType clickType) {
        if (itemData == null) return;

        Material mat = Material.matchMaterial(itemData.getString("material", ""));
        if (mat == null) return;

        double buyPrice = itemData.getDouble("buy-price", 0.0);
        double sellPrice = itemData.getDouble("sell-price", 0.0);
        Economy econ = NexusCore.getEconomy();

        // 구매 시도 (좌클릭)
        if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
            // 가격이 -1 등 음수면 구매 불가 처리
            if (buyPrice < 0) {
                player.sendMessage(ChatColor.RED + "[!] 이 아이템은 상점에서 구매할 수 없습니다.");
                return;
            }

            int amount = (clickType == ClickType.SHIFT_LEFT) ? 64 : 1;
            double cost = buyPrice * amount;

            if (econ.getBalance(player) < cost) {
                player.sendMessage(ChatColor.RED + "[!] 잔액이 부족합니다.");
                return;
            }

            econ.withdrawPlayer(player, cost);
            player.getInventory().addItem(new ItemStack(mat, amount));
            player.sendMessage(ChatColor.GREEN + String.format("%,.0f원", cost) + "을 지불하고 구매했습니다.");
        } 
        // 판매 시도 (우클릭)
        else if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            int amount = (clickType == ClickType.SHIFT_RIGHT) ? 64 : 1;
            
            if (!player.getInventory().containsAtLeast(new ItemStack(mat), amount)) {
                player.sendMessage(ChatColor.RED + "[!] 판매할 아이템이 부족합니다.");
                return;
            }

            player.getInventory().removeItem(new ItemStack(mat, amount));
            double gain = sellPrice * amount;
            econ.depositPlayer(player, gain);
            player.sendMessage(ChatColor.AQUA + String.format("%,.0f원", gain) + "을 받고 판매했습니다.");
        }
    }
}