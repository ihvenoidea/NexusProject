package main_plugin.gui;

import main_plugin.NexusCore;
import main_plugin.augments.Augment;
import main_plugin.augments.AugmentTier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AugmentSelectorGUI implements InventoryHolder {

    private final NexusCore plugin;
    private final Player player;
    private final AugmentTier tier;
    private final Inventory inventory;
    private final NamespacedKey augmentKey;

    public AugmentSelectorGUI(NexusCore plugin, Player player, AugmentTier tier) {
        this.plugin = plugin;
        this.player = player;
        this.tier = tier;
        this.augmentKey = new NamespacedKey(plugin, "augment_id");
        
        String title = tier.getFormattedName() + " §f증강체 선택";
        this.inventory = Bukkit.createInventory(this, 27, title);
        
        setupGUI();
    }

    private void setupGUI() {
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        if (bgMeta != null) { 
            bgMeta.setDisplayName(" "); 
            bg.setItemMeta(bgMeta); 
        }
        for (int i = 0; i < 27; i++) inventory.setItem(i, bg);

        List<Augment> availableAugments = plugin.getAugmentManager().getRegisteredAugments().values().stream()
                .filter(a -> a.getTier() == tier)
                .collect(Collectors.toList());

        Collections.shuffle(availableAugments);
        List<Augment> selectedAugments = availableAugments.subList(0, Math.min(3, availableAugments.size()));

        int[] slots = {11, 13, 15};
        
        for (int i = 0; i < selectedAugments.size(); i++) {
            Augment aug = selectedAugments.get(i);
            ItemStack item = new ItemStack(aug.getIcon());
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName(tier.getColor() + "§l" + aug.getName());
                
                List<String> lore = new ArrayList<>();
                lore.add("§7태그: §e" + String.join(", ", aug.getTags()));
                lore.add("");
                lore.addAll(aug.getDescription());
                lore.add("");
                lore.add("§a[ 클릭하여 획득하기 ]");
                meta.setLore(lore);
                
                meta.getPersistentDataContainer().set(augmentKey, PersistentDataType.STRING, aug.getId());
                item.setItemMeta(meta);
            }
            inventory.setItem(slots[i], item);
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}