package main_plugin.gui;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Optional;

/**
 * Shift + F 키 또는 Shift + 맨손 허공 우클릭으로 열 수 있는 메인 메뉴 시스템입니다.
 */
public class MainMenuListener implements Listener {

    private final NexusCore plugin;

    public MainMenuListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // [1] Shift + F (웅크리기 + 양손 바꾸기) 이벤트 감지
    // ==========================================
    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            event.setCancelled(true); 
            openMainMenu(player);
        }
    }

    // ==========================================
    // [2] Shift + 맨손 우클릭 감지 (허공에만 작동하도록 수정!)
    // ==========================================
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // 오른손 상호작용만 감지 (중복 방지)
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        // [수정됨] Action.RIGHT_CLICK_AIR (허공)일 때만 작동하게 하여 블록 클릭 간섭 차단
        if (player.isSneaking() && event.getAction() == Action.RIGHT_CLICK_AIR) {
            // 맨손일 때만 작동
            if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                event.setCancelled(true);
                openMainMenu(player);
            }
        }
    }

    // ==========================================
    // 메인 메뉴 GUI 생성 및 오픈
    // ==========================================
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "넥서스 퀵 메뉴");

        inv.setItem(10, createMenuItem(Material.BOOK, ChatColor.GOLD + "📚 전체 도감 열기", "클릭 시 즉시 도감 창이 열립니다."));
        inv.setItem(11, createMenuItem(Material.PAPER, ChatColor.LIGHT_PURPLE + "📩 개인 우편함", "클릭 시 즉시 우편함이 열립니다."));
        inv.setItem(12, createMenuItem(Material.ENDER_CHEST, ChatColor.GOLD + "📦 개인 창고", "안전하게 아이템을 보관합니다."));
        inv.setItem(13, createMenuItem(Material.EMERALD, ChatColor.GREEN + "🔄 재화 확인 및 갱신", "디스코드 등에서 변동된 재화를 실시간으로 불러옵니다."));
        inv.setItem(14, createMenuItem(Material.COMPASS, ChatColor.AQUA + "🌍 스폰으로 귀환", "5초 후 공식 스폰 지역으로 이동합니다."));
        inv.setItem(15, createMenuItem(Material.ENDER_PEARL, ChatColor.GREEN + "🏠 내 타운으로 귀환", "클릭 시 즉시 내 타운으로 이동합니다."));
        inv.setItem(16, createMenuItem(Material.CAULDRON, ChatColor.RED + "🗑️ 쓰레기통 열기", "필요 없는 아이템을 영구 삭제합니다."));

        player.openInventory(inv);
    }

    private ItemStack createMenuItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(ChatColor.GRAY + lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    // ==========================================
    // 메인 메뉴 클릭 이벤트 처리
    // ==========================================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals(ChatColor.DARK_GRAY + "넥서스 퀵 메뉴")) {
            event.setCancelled(true); 
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            Material type = clickedItem.getType();
            player.closeInventory(); 

            if (type == Material.BOOK) {
                player.performCommand("도감");
            } 
            else if (type == Material.PAPER) {
                player.performCommand("우편함");
            }
            else if (type == Material.ENDER_CHEST) {
                plugin.getStorageManager().openStorageMenu(player);
            }
            else if (type == Material.EMERALD) {
                player.sendMessage("§e[!] DB에서 최신 재화 정보를 불러오는 중...");
                
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getDatabaseManager().syncBalanceFromDB(player.getUniqueId());
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Optional<UserData> userOpt = plugin.getUserManager().getUser(player.getUniqueId());
                        if (userOpt.isPresent()) {
                            UserData user = userOpt.get();
                            player.sendMessage("§a[!] 새로고침 완료! 현재 자산 정보:");
                            player.sendMessage("§f - 돈(Money): §e" + String.format("%,.0f원", user.getMoney()));
                            player.sendMessage("§f - 포인트(DP): §b" + String.format("%,d DP", user.getPoints()));
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
                        }
                    });
                });
            }
            else if (type == Material.ENDER_PEARL) {
                player.performCommand("타운 이동");
            }
            else if (type == Material.CAULDRON) {
                Inventory trashInv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "쓰레기통");
                player.openInventory(trashInv);
            }
            else if (type == Material.COMPASS) {
                player.sendMessage(ChatColor.AQUA + "5초 뒤 스폰으로 텔레포트합니다. 움직이지 마세요!");
                
                Location configSpawn = plugin.getSpawnLocation(); 
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && !player.isDead()) {
                        player.teleport(configSpawn);
                        player.sendMessage(ChatColor.GREEN + "성공적으로 스폰에 도착했습니다.");
                    }
                }, 100L);
            }
        }
    }
}