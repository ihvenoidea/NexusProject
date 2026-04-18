package main_plugin.gui;

import dev.lone.itemsadder.api.CustomStack;
import main_plugin.NexusCore;
import main_plugin.user.UserData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * DP 상점 통합 뽑기 시스템
 * [초비상] 증강권(1%), 무기 상자(1%), 바닐라 꿀템(98%) 확률 조정본
 */
public class PointShopManager implements Listener {

    private final NexusCore plugin;
    private final String MAIN_TITLE = "§8[ Nexus 특별 조공 뽑기 ]";
    private final String PREVIEW_PREFIX = "§8미리보기: ";
    private final Random random = new Random();

    public PointShopManager(NexusCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    enum GachaTier {
        SILVER("실버", "§7", 500, 10, "n_items:silver_augment_ticket", "n_items:silver_weapon_box", 
                Arrays.asList("폭죽 64개", "경험치 병 32개", "황금 사과 1개")),
        
        GOLD("골드", "§e", 1500, 12, "n_items:gold_augment_ticket", "n_items:gold_weapon_box", 
                Arrays.asList("셜커 껍데기 2개", "경험치 병 64개", "스펀지 16개")),
        
        PRISM("프리즘", "§b", 4000, 14, "n_items:prism_augment_ticket", "n_items:prism_weapon_box", 
                Arrays.asList("불사의 토템 1개", "인챈트된 황금 사과 1개", "겉날개 1개", "불길한 열쇠 1개")),
        
        MYTHIC("신화", "§d", 10000, 16, "n_items:mythic_augment_ticket", "n_items:mythic_weapon_box", 
                Arrays.asList("무거운 코어 1개", "삼지창 1개", "신호기 1개", "불길한 열쇠 1개"));

        final String name, color;
        final int price, slot;
        final String ticketId, weaponBoxId;
        final List<String> vanillaList;

        GachaTier(String name, String color, int price, int slot, String ticketId, String weaponBoxId, List<String> vanillaList) {
            this.name = name;
            this.color = color;
            this.price = price;
            this.slot = slot;
            this.ticketId = ticketId;
            this.weaponBoxId = weaponBoxId;
            this.vanillaList = vanillaList;
        }
    }

    private static class VanillaReward {
        final ItemStack item;
        final String displayName;

        public VanillaReward(Material material, int amount, String displayName) {
            this.item = new ItemStack(material, amount);
            this.displayName = displayName;
        }
    }

    public void openShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_TITLE);
        
        ItemStack pane = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) { paneMeta.setDisplayName(" "); pane.setItemMeta(paneMeta); }
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        for (GachaTier tier : GachaTier.values()) {
            inv.setItem(tier.slot, createGachaDisplayItem(tier));
        }
        
        player.openInventory(inv);
    }

    private ItemStack createGachaDisplayItem(GachaTier tier) {
        ItemStack item = new ItemStack(Material.CHEST_MINECART);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tier.color + "§l[" + tier.name + "] §f종합 뽑기 상자");
            List<String> lore = new ArrayList<>();
            lore.add("§7가격: §b" + String.format("%,d", tier.price) + " DP");
            lore.add("");
            lore.add("§e[좌클릭] §f즉시 뽑기 실행");
            lore.add("§b[우클릭] §f구성품 미리보기");
            lore.add("");
            lore.add("§f[ 등장 가능 보상 요약 ]");
            // [업데이트] UI 확률 표시 1%로 수정
            lore.add("§7▶ " + tier.color + tier.name + " 증강 뽑기권 §d(1%)");
            lore.add("§7▶ " + tier.color + tier.name + " 무기 & 도구 상자 §d(1%)");
            lore.add("§7▶ 특별 보상 §e(98% 확률로 아래 중 1개 획득)");
            
            for (String vName : tier.vanillaList) {
                lore.add("  §8- " + vName);
            }
            
            lore.add("");
            lore.add("§e[클릭] §f즉시 " + String.format("%,d", tier.price) + " DP를 소모하여 뽑습니다!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openPreview(Player player, GachaTier tier) {
        Inventory previewInv = Bukkit.createInventory(null, 27, PREVIEW_PREFIX + tier.color + tier.name);
        
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) { paneMeta.setDisplayName(" "); pane.setItemMeta(paneMeta); }
        for (int i = 0; i < 27; i++) previewInv.setItem(i, pane);

        ItemStack ticket = getIAItem(tier.ticketId);
        previewInv.setItem(11, ticket);

        ItemStack weaponBox = getIAItem(tier.weaponBoxId);
        previewInv.setItem(13, weaponBox);

        ItemStack vanillaBook = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = vanillaBook.getItemMeta();
        if (bookMeta != null) {
            bookMeta.setDisplayName("§d특별 보상 후보 리스트");
            List<String> bookLore = new ArrayList<>();
            bookLore.add("§798% 확률로 아래 중 하나가 나옵니다:");
            for (String vName : tier.vanillaList) bookLore.add("§f - " + vName);
            bookMeta.setLore(bookLore);
            vanillaBook.setItemMeta(bookMeta);
        }
        previewInv.setItem(15, vanillaBook);
        
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        if (bMeta != null) { bMeta.setDisplayName("§c[ 돌아가기 ]"); back.setItemMeta(bMeta); }
        previewInv.setItem(26, back);

        player.openInventory(previewInv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1.2f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = ChatColor.stripColor(event.getView().getTitle());
        String mainTitleStripped = ChatColor.stripColor(MAIN_TITLE);

        if (title.equals(mainTitleStripped)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            for (GachaTier tier : GachaTier.values()) {
                if (tier.slot == slot) {
                    if (event.getClick() == ClickType.RIGHT) {
                        openPreview(player, tier);
                    } else {
                        processGachaPull(player, tier);
                    }
                    return;
                }
            }
        }
        else if (title.startsWith(ChatColor.stripColor(PREVIEW_PREFIX))) {
            event.setCancelled(true);
            if (event.getRawSlot() == 26) {
                openShop(player);
            }
        }
    }

    private void processGachaPull(Player player, GachaTier tier) {
        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            if (user.getPoints() >= tier.price) {
                user.setPoints(user.getPoints() - tier.price);
                plugin.getUserManager().saveUserData(user); 
                
                int roll = random.nextInt(100) + 1; // 1~100 랜덤값
                ItemStack reward;
                String rewardName;

                // [핵심] 확률 조건 1%로 대폭 하향
                if (roll == 1) {
                    // 딱 1이 나올 확률 (1%)
                    reward = getIAItem(tier.ticketId);
                    rewardName = tier.color + tier.name + " 증강 뽑기권";
                } else if (roll == 2) {
                    // 딱 2가 나올 확률 (1%)
                    reward = getIAItem(tier.weaponBoxId);
                    rewardName = tier.color + tier.name + " 무기 & 도구 상자";
                } else {
                    // 나머지 3~100 (98%)
                    VanillaReward vReward = getRandomVanillaReward(tier);
                    reward = vReward.item;
                    rewardName = "§d" + vReward.displayName;
                }

                player.getInventory().addItem(reward);
                player.sendMessage("§a§l[뽑기 성공] §f축하합니다! §e" + rewardName + "§f을(를) 획득했습니다!");
                
                // 1% 당첨 시에는 더 화려한 소리!
                if (roll <= 2) {
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.0f);
                    Bukkit.broadcastMessage("§6§l[!] §f" + player.getName() + "님이 " + tier.color + tier.name + " 뽑기§f에서 §b§l극악의 확률§f을 뚫고 §e" + rewardName + "§f을(를) 획득했습니다!");
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                }
                
            } else {
                player.sendMessage("§c§l[!] §fDP가 부족합니다! (현재: " + user.getPoints() + " DP)");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        });
    }

    private VanillaReward getRandomVanillaReward(GachaTier tier) {
        List<VanillaReward> list = new ArrayList<>();
        switch (tier) {
            case SILVER -> {
                list.add(new VanillaReward(Material.FIREWORK_ROCKET, 64, "폭죽 64개"));
                list.add(new VanillaReward(Material.EXPERIENCE_BOTTLE, 32, "경험치 병 32개"));
                list.add(new VanillaReward(Material.GOLDEN_APPLE, 1, "황금 사과 1개"));
            }
            case GOLD -> {
                list.add(new VanillaReward(Material.SHULKER_SHELL, 2, "셜커 껍데기 2개"));
                list.add(new VanillaReward(Material.EXPERIENCE_BOTTLE, 64, "경험치 병 64개"));
                list.add(new VanillaReward(Material.SPONGE, 16, "스펀지 16개"));
            }
            case PRISM -> {
                list.add(new VanillaReward(Material.TOTEM_OF_UNDYING, 1, "불사의 토템 1개"));
                list.add(new VanillaReward(Material.ENCHANTED_GOLDEN_APPLE, 1, "인챈트된 황금 사과 1개"));
                list.add(new VanillaReward(Material.ELYTRA, 1, "겉날개 1개"));
                list.add(new VanillaReward(Material.OMINOUS_TRIAL_KEY, 1, "불길한 열쇠 1개"));
            }
            case MYTHIC -> {
                list.add(new VanillaReward(Material.HEAVY_CORE, 1, "무거운 코어 1개"));
                list.add(new VanillaReward(Material.TRIDENT, 1, "삼지창 1개"));
                list.add(new VanillaReward(Material.BEACON, 1, "신호기 1개"));
                list.add(new VanillaReward(Material.OMINOUS_TRIAL_KEY, 1, "불길한 열쇠 1개"));
            }
        }
        return list.get(random.nextInt(list.size()));
    }

    private ItemStack getIAItem(String id) {
        CustomStack cs = CustomStack.getInstance(id);
        if (cs != null) return cs.getItemStack();
        
        ItemStack errorItem = new ItemStack(Material.PAPER);
        ItemMeta meta = errorItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c[오류] " + id + " 로드 실패");
            errorItem.setItemMeta(meta);
        }
        return errorItem;
    }
}