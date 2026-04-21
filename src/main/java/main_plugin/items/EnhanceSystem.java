package main_plugin.items;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class EnhanceSystem implements CommandExecutor, Listener {

    private final NexusCore plugin;
    private final NamespacedKey levelKey;
    private final NamespacedKey nameKey; 
    private final String GUI_TITLE = "§8[ 넥서스 대장간 - 방어구 강화 ]";
    private final Random random = new Random();

    public EnhanceSystem(NexusCore plugin) {
        this.plugin = plugin;
        this.levelKey = new NamespacedKey(plugin, "enhance_level");
        this.nameKey = plugin.getSetItemManager().getNameKey();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("nexus.admin")) {
            player.sendMessage("§c[!] 넥서스 대장장이 NPC를 통해서만 장비를 강화할 수 있습니다.");
            return true;
        }

        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
        
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        paneMeta.setDisplayName(" "); pane.setItemMeta(paneMeta);
        
        for (int i = 0; i < 27; i++) {
            if (i != 13 && i != 22) inv.setItem(i, pane); 
        }

        ItemStack enhanceBtn = new ItemStack(Material.ANVIL);
        inv.setItem(22, enhanceBtn);
        
        updateAnvilButton(inv); 
        player.openInventory(inv);
        return true;
    }

    private void updateAnvilButton(Inventory inv) {
        ItemStack targetItem = inv.getItem(13);
        ItemStack enhanceBtn = inv.getItem(22);
        
        if (enhanceBtn == null || enhanceBtn.getType() != Material.ANVIL) return;

        ItemMeta btnMeta = enhanceBtn.getItemMeta();
        btnMeta.setDisplayName("§e§l[ 방어구 강화 연마 ]");
        List<String> lore = new ArrayList<>();

        if (targetItem == null || targetItem.getType() == Material.AIR) {
            lore.add("§7중앙 빈 칸에 넥서스 §a방어구§7를 올려주세요.");
            lore.add("§7장비를 올리면 비용과 확률이 표시됩니다.");
        } else {
            ItemMeta targetMeta = targetItem.getItemMeta();
            if (targetMeta == null || !targetMeta.getPersistentDataContainer().has(nameKey, PersistentDataType.STRING)) {
                lore.add("§c[!] 넥서스 세트 방어구만 강화할 수 있습니다.");
            } else {
                String partName = ChatColor.stripColor(targetMeta.getDisplayName());
                if (!partName.contains("투구") && !partName.contains("갑옷") && !partName.contains("각반") && !partName.contains("장화")) {
                    lore.add("§c[!] 무기나 도구는 강화할 수 없습니다.");
                } else {
                    int currentLevel = targetMeta.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.INTEGER, 0);
                    
                    if (currentLevel >= 10) {
                        lore.add("§a[!] 이미 최고 레벨(+10)에 도달한 방어구입니다.");
                    } else {
                        double cost = (currentLevel + 1) * 15000.0;
                        lore.add("§f현재 장비: §e" + targetMeta.getDisplayName());
                        lore.add("§f강화 비용: §e" + String.format("%,.0f원", cost));
                        lore.add("");
                        lore.add("§f[ 강화 확률 ]");
                        
                        if (currentLevel < 3) { lore.add("§a▶ 성공: 100%"); } 
                        else if (currentLevel < 5) { lore.add("§a▶ 성공: 70%"); lore.add("§c▶ 실패 (유지): 30%"); } 
                        else if (currentLevel < 7) { lore.add("§a▶ 성공: 50%"); lore.add("§c▶ 실패 (유지): 40%"); lore.add("§4▶ 하락 (-1단계): 10%"); } 
                        else if (currentLevel < 9) { lore.add("§a▶ 성공: 30%"); lore.add("§c▶ 실패 (유지): 40%"); lore.add("§4▶ 하락 (-1단계): 30%"); } 
                        else { lore.add("§a▶ 성공: 10%"); lore.add("§c▶ 실패 (유지): 40%"); lore.add("§4▶ 하락 (-1단계): 50%"); }
                        lore.add(""); lore.add("§e[ 클릭하여 강화 시도 ]");
                    }
                }
            }
        }
        btnMeta.setLore(lore);
        enhanceBtn.setItemMeta(btnMeta);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        if (event.getInventory().getHolder() != null) return; 

        int slot = event.getRawSlot();
        if (slot >= 0 && slot <= 26 && slot != 13) {
            event.setCancelled(true);
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();

        Bukkit.getScheduler().runTask(plugin, () -> updateAnvilButton(inv));

        if (slot == 22) {
            ItemStack targetItem = inv.getItem(13);
            if (targetItem == null || targetItem.getType() == Material.AIR) { player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f); return; }

            ItemMeta meta = targetItem.getItemMeta();
            if (meta == null || !meta.getPersistentDataContainer().has(nameKey, PersistentDataType.STRING)) { player.sendMessage("§c[!] 넥서스 세트 방어구만 강화할 수 있습니다."); return; }

            String partName = ChatColor.stripColor(meta.getDisplayName());
            if (!partName.contains("투구") && !partName.contains("갑옷") && !partName.contains("각반") && !partName.contains("장화")) { player.sendMessage("§c[!] 방어구만 올려주세요."); return; }

            int currentLevel = meta.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.INTEGER, 0);
            if (currentLevel >= 10) { player.sendMessage("§c[!] 이미 최고 레벨(+10)에 도달한 방어구입니다."); return; }

            double cost = (currentLevel + 1) * 15000.0;
            if (!NexusCore.getEconomy().has(player, cost)) { player.sendMessage("§c[!] 골드가 부족합니다. (필요: " + String.format("%,.0f원", cost) + ")"); return; }

            NexusCore.getEconomy().withdrawPlayer(player, cost);

            double roll = random.nextDouble() * 100.0;
            String result = calculateResult(currentLevel, roll);

            if (result.equals("SUCCESS")) {
                applyEnhancement(targetItem, currentLevel + 1);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
                player.sendMessage("§a§l[강화 성공] §f축하합니다! 방어구가 §e+" + (currentLevel + 1) + "§f(으)로 강화되어 최대 체력이 증가했습니다!");
                if (currentLevel + 1 >= 7) Bukkit.broadcastMessage("§6§l[서버 공지] §f" + player.getName() + "님이 방어구를 §c+" + (currentLevel + 1) + "§f강까지 강화하는 데 성공했습니다!");
            } 
            else if (result.equals("FAIL")) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 0.5f);
                player.sendMessage("§e[강화 실패] §f아쉽게도 강화에 실패했습니다. (변화 없음)");
            } 
            else if (result.equals("DOWNGRADE")) {
                applyEnhancement(targetItem, currentLevel - 1);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.8f);
                player.sendMessage("§c§l[강화 하락] §f강화의 기운이 흩어져 단계가 하락했습니다... (+" + (currentLevel - 1) + ")");
            } 
            updateAnvilButton(inv);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE) && event.getInventory().getHolder() == null) {
            Bukkit.getScheduler().runTask(plugin, () -> updateAnvilButton(event.getInventory()));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            if (event.getInventory().getHolder() != null) return; 

            ItemStack item = event.getInventory().getItem(13);
            if (item != null && item.getType() != Material.AIR) {
                event.getInventory().setItem(13, null); 
                
                Player player = (Player) event.getPlayer();
                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItem(player.getLocation(), item);
                } else {
                    player.getInventory().addItem(item);
                }
            }
        }
    }

    private String calculateResult(int level, double roll) {
        if (level < 3) return "SUCCESS"; 
        if (level < 5) return roll < 70 ? "SUCCESS" : "FAIL";
        if (level < 7) { if (roll < 50) return "SUCCESS"; if (roll < 90) return "FAIL"; return "DOWNGRADE"; }
        if (level < 9) { if (roll < 30) return "SUCCESS"; if (roll < 70) return "FAIL"; return "DOWNGRADE"; }
        if (roll < 10) return "SUCCESS"; if (roll < 50) return "FAIL"; return "DOWNGRADE";
    }

    // ==============================================================
    // [버그 픽스] 네더라이트 방어구 기본 스탯 복구 로직 적용
    // ==============================================================
    private void applyEnhancement(ItemStack item, int newLevel) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, newLevel);
        String originalName = meta.getDisplayName().replaceAll(" §c\\[\\+[0-9]+\\]", "");
        if (newLevel > 0) meta.setDisplayName(originalName + " §c[+" + newLevel + "]");
        else meta.setDisplayName(originalName);

        // 기존 모든 커스텀 속성 초기화 (버그 방지)
        meta.removeAttributeModifier(Attribute.GENERIC_MAX_HEALTH);
        meta.removeAttributeModifier(Attribute.GENERIC_ARMOR);
        meta.removeAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS);
        meta.removeAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE);

        if (newLevel > 0) {
            String part = ChatColor.stripColor(originalName);
            EquipmentSlotGroup slotGroup = part.contains("투구") ? EquipmentSlotGroup.HEAD : 
                                           part.contains("갑옷") ? EquipmentSlotGroup.CHEST : 
                                           part.contains("각반") ? EquipmentSlotGroup.LEGS : 
                                           EquipmentSlotGroup.FEET;
                                           
            // 1. 커스텀 체력 증가
            meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(
                    new NamespacedKey(plugin, "enhance_health_" + UUID.randomUUID()), newLevel * 1.0, AttributeModifier.Operation.ADD_NUMBER, slotGroup));

            // 2. 날아간 네더라이트 기본 방어력 강제 복구
            double armor = part.contains("갑옷") ? 8.0 : part.contains("각반") ? 6.0 : 3.0;
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                    new NamespacedKey(plugin, "base_armor_" + UUID.randomUUID()), armor, AttributeModifier.Operation.ADD_NUMBER, slotGroup));
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(
                    new NamespacedKey(plugin, "base_tough_" + UUID.randomUUID()), 3.0, AttributeModifier.Operation.ADD_NUMBER, slotGroup));
            meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(
                    new NamespacedKey(plugin, "base_kb_" + UUID.randomUUID()), 0.1, AttributeModifier.Operation.ADD_NUMBER, slotGroup));
        }
        item.setItemMeta(meta);
    }
}