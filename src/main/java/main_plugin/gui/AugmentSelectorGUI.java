package main_plugin.gui;

import main_plugin.NexusCore;
import main_plugin.augments.Augment;
import main_plugin.augments.AugmentTier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class AugmentSelectorGUI implements InventoryHolder {

    private final Inventory inventory;
    private final AugmentTier tier; // 티어 정보를 저장할 필드

    public AugmentSelectorGUI(NexusCore plugin, AugmentTier tier, List<Augment> options) {
        this.tier = tier;
        String title = "§8증강체 선택 - " + getTierColor(tier) + tier.name();
        this.inventory = Bukkit.createInventory(this, 27, title);

        setupOptions(plugin, options);
    }

    private void setupOptions(NexusCore plugin, List<Augment> options) {
        // 배경 유리판 설치
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // 선택지 배치 (11, 13, 15번 슬롯)
        int[] slots = {11, 13, 15};
        NamespacedKey key = new NamespacedKey(plugin, "augment_id");

        for (int i = 0; i < options.size() && i < slots.length; i++) {
            Augment aug = options.get(i);
            ItemStack item = new ItemStack(aug.getIcon());
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(getTierColor(aug.getTier()) + "§l" + aug.getName());
                
                List<String> lore = new ArrayList<>();
                lore.add("§8" + aug.getTier().name() + " 등급");
                lore.add("");
                lore.addAll(aug.getDescription());
                lore.add("");
                lore.add("§e▶ 클릭하여 선택");
                
                meta.setLore(lore);
                // 클릭 이벤트 처리를 위한 PersistentData 저장
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, aug.getId());
                item.setItemMeta(meta);
            }
            inventory.setItem(slots[i], item);
        }
    }

    // 티어별 색상 코드 반환
    private String getTierColor(AugmentTier tier) {
        return switch (tier) {
            case SILVER -> "§7";
            case GOLD -> "§6";
            case PRISM -> "§b";
            case MYTHIC -> "§d";
            default -> "§f";
        };
    }

    /**
     * 추가된 메서드: 현재 GUI의 증강체 등급을 반환합니다.
     * AugmentGUIListener에서 환급액을 계산할 때 사용됩니다.
     */
    public AugmentTier getTier() {
        return tier;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}