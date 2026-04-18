package main_plugin.collection;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CollectionGUI implements InventoryHolder {

    private final NexusCore plugin;
    private final Inventory inventory;
    private final CollectionData data;
    private final int page;

    private static final List<Material> VANILLA_ITEMS;

    static {
        VANILLA_ITEMS = Arrays.stream(Material.values())
                .filter(Material::isItem)
                .filter(mat -> !mat.isAir())
                .collect(Collectors.toList());
    }

    private static final int ITEMS_PER_PAGE = 16; 

    public CollectionGUI(NexusCore plugin, Player player, int page) {
        this.plugin = plugin;
        this.page = page;
        this.data = plugin.getCollectionManager().getCollectionData(player.getUniqueId());
        
        this.inventory = Bukkit.createInventory(this, 54, "§8[ 넥서스 도감 - " + (page + 1) + "페이지 ]");
        
        setupIcons();
    }

    private void setupIcons() {
        ItemStack pane = createSimpleItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, pane);
        }

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, VANILLA_ITEMS.size());

        for (int i = start; i < end; i++) {
            Material mat = VANILLA_ITEMS.get(i);
            String id = mat.name().toLowerCase();
            String displayName = mat.name().replace("_", " ");
            
            int indexOnPage = i - start;
            int row = indexOnPage / 4;       
            int colPair = indexOnPage % 4;   
            
            int itemSlot = (row * 9) + (colPair * 2); 
            int statusSlot = itemSlot + 1;            

            addCollectionPair(itemSlot, statusSlot, mat, id, displayName);
        }

        inventory.setItem(45, createSimpleItem(Material.PAPER, "§c§l[ 뒤로 가기 ]"));
        
        if (page > 0) {
            inventory.setItem(46, createSimpleItem(Material.ARROW, "§e§l◀ 이전 (" + page + "P)"));
        }
        
        if (end < VANILLA_ITEMS.size()) {
            inventory.setItem(47, createSimpleItem(Material.ARROW, "§e§l다음 (" + (page + 2) + "P) ▶"));
        }

        int maxPage = (int) Math.ceil((double) VANILLA_ITEMS.size() / ITEMS_PER_PAGE);
        inventory.setItem(49, createSimpleItem(Material.BOOK, "§f현재 페이지: §b" + (page + 1) + " / " + maxPage));

        // [수정됨] 맨 끝에 DP 보상량을 파라미터로 추가했습니다.
        setupRewardIcon(48, 100, 1, "§a[ 도감 보상 I ]", Material.COAL_BLOCK, 500);
        setupRewardIcon(50, 500, 2, "§e[ 도감 보상 II ]", Material.IRON_BLOCK, 2000);
        setupRewardIcon(52, 1000, 3, "§6§l[ 도감 보상 III ]", Material.GOLD_BLOCK, 5000);
    }

    private void addCollectionPair(int itemSlot, int statusSlot, Material material, String id, String name) {
        boolean isCollected = (data != null && data.hasCollected(id));
        
        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName((isCollected ? "§b§l" : "§7") + name.toUpperCase());
            List<String> itemLore = new ArrayList<>();
            itemLore.add("§7획득 시 포인트: §e" + (id.contains("nexus") ? 20 : 5) + "pt");
            itemMeta.setLore(itemLore);
            item.setItemMeta(itemMeta);
        }
        inventory.setItem(itemSlot, item);

        Material statusMat = isCollected ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ItemStack status = new ItemStack(statusMat);
        ItemMeta statusMeta = status.getItemMeta();
        if (statusMeta != null) {
            statusMeta.setDisplayName(isCollected ? "§a§l✔ 수집 완료" : "§c§l✘ 미수집");
            List<String> statusLore = new ArrayList<>();
            statusLore.add(isCollected ? "§7도감에 등록된 아이템입니다." : "§7아직 획득하지 못했습니다.");
            statusMeta.setLore(statusLore);
            status.setItemMeta(statusMeta);
        }
        inventory.setItem(statusSlot, status);
    }

    // [수정됨] rewardDP 파라미터가 추가되었고, 로어에 표시됩니다.
    private void setupRewardIcon(int slot, int requiredPoints, int tier, String title, Material iconMat, int rewardDP) {
        int currentPoints = (data != null) ? data.getTotalPoints() : 0;
        int currentTier = (data != null) ? data.getRewardTier() : 0;
        
        boolean canClaim = currentPoints >= requiredPoints && currentTier < tier;
        boolean alreadyClaimed = currentTier >= tier;

        ItemStack item = new ItemStack(alreadyClaimed ? Material.BARRIER : (canClaim ? iconMat : Material.COAL));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(title + (alreadyClaimed ? " §7(수령 완료)" : ""));
            List<String> lore = new ArrayList<>();
            lore.add("§7달성 조건: §f" + requiredPoints + "pt");
            lore.add("§7현재 포인트: §b" + currentPoints + "pt");
            lore.add("§7지급 보상: §b" + String.format("%,d", rewardDP) + " DP"); // 보상 표시
            lore.add("");
            if (alreadyClaimed) lore.add("§c이미 보상을 수령했습니다.");
            else if (canClaim) lore.add("§e§l[!] 클릭하여 보상을 수령하세요!");
            else lore.add("§c포인트가 더 필요합니다.");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private ItemStack createSimpleItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public int getPage() { return page; }
}