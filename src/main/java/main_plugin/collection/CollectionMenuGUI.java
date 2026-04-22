package main_plugin.collection;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CollectionMenuGUI implements InventoryHolder {

    private final Inventory inventory;

    public CollectionMenuGUI() {
        this.inventory = Bukkit.createInventory(this, 27, "§8[ 넥서스 도감 시스템 ]");
        setupMenu();
    }

    private void setupMenu() {
        ItemStack background = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, background);
        }

        // 시너지 도감 버튼을 삭제하고, 남은 두 버튼을 중앙(12번, 14번)으로 예쁘게 모았습니다.
        List<String> vanillaLore = new ArrayList<>();
        vanillaLore.add("§7마인크래프트의 모든 기본 아이템의");
        vanillaLore.add("§7수집 현황을 확인하고 보상을 받습니다.");
        vanillaLore.add("");
        vanillaLore.add("§e▶ 클릭하여 이동");
        inventory.setItem(12, createItem(Material.GRASS_BLOCK, "§f§l[ 바닐라 아이템 도감 ]", vanillaLore));

        List<String> augmentLore = new ArrayList<>();
        augmentLore.add("§7자신이 획득한 증강체 목록과");
        augmentLore.add("§7각 시너지의 세트 효과를 확인합니다.");
        augmentLore.add("");
        augmentLore.add("§e▶ 클릭하여 이동");
        inventory.setItem(14, createItem(Material.ENCHANTING_TABLE, "§b§l[ 증강체 및 시너지 도감 ]", augmentLore));
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() { return inventory; }
}