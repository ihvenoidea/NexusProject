package main_plugin.storage;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import main_plugin.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager {

    private final NexusCore plugin;
    public final int MAX_BOXES = 5;
    public final double BASE_COST = 50000;
    
    // [핵심 보안] 저장 중인 유저를 기록하여 중복 오픈(복사 버그)을 막는 락(Lock) 시스템
    private final Set<UUID> lockedPlayers = ConcurrentHashMap.newKeySet();

    public StorageManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    public void openStorageMenu(Player player) {
        Optional<UserData> userOpt = plugin.getUserManager().getUser(player.getUniqueId());
        if (userOpt.isEmpty()) return;

        int unlocked = userOpt.get().getUnlockedBoxes();
        Inventory gui = Bukkit.createInventory(null, 27, "§8[ NEXUS ] 창고 메뉴");

        for (int i = 1; i <= MAX_BOXES; i++) {
            ItemStack icon;
            if (i <= unlocked) {
                icon = createItem(Material.CHEST, "§a§l" + i + "번 창고 박스", "§7클릭하여 창고를 엽니다.");
            } else if (i == unlocked + 1) {
                double cost = (i - 1) * BASE_COST;
                icon = createItem(Material.MINECART, "§e§l" + i + "번 창고 잠금 해제",
                        "§f비용: §e" + String.format("%,.0f원", cost), "§7클릭하여 구매합니다.");
            } else {
                icon = createItem(Material.BARRIER, "§c§l잠긴 박스", "§7이전 박스를 먼저 해금해야 합니다.");
            }
            gui.setItem(10 + i, icon);
        }

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    public void openBox(Player player, int boxIndex) {
        // [복사 버그 원천 차단] 데이터가 저장 중일 때는 창고를 열지 못하게 막습니다.
        if (lockedPlayers.contains(player.getUniqueId())) {
            player.sendMessage("§c[!] 창고 데이터를 안전하게 동기화 중입니다. 잠시 후 다시 열어주세요.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        player.sendMessage("§e창고 데이터를 불러오는 중입니다...");
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String base64Data = null;
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT items FROM user_storage WHERE uuid = ? AND box_index = ?")) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setInt(2, boxIndex);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) base64Data = rs.getString("items");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            final String finalData = base64Data;
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory boxInv = Bukkit.createInventory(null, 54, "§8[ NEXUS ] 창고 - " + boxIndex + "번 박스");
                if (finalData != null && !finalData.isEmpty()) {
                    boxInv.setContents(ItemSerializer.fromBase64(finalData));
                }
                player.openInventory(boxInv);
                player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.0f);
            });
        });
    }

    public void saveBox(Player player, int boxIndex, ItemStack[] contents) {
        // [락(Lock) 활성화] 저장이 시작되면 해당 유저의 창고 접근을 차단합니다.
        lockedPlayers.add(player.getUniqueId());
        
        String base64Data = ItemSerializer.toBase64(contents);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO user_storage (uuid, box_index, items) VALUES (?, ?, ?) "
                       + "ON DUPLICATE KEY UPDATE items = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setInt(2, boxIndex);
                ps.setString(3, base64Data);
                ps.setString(4, base64Data);
                ps.executeUpdate();
                
                // 성공 시
                Bukkit.getScheduler().runTask(plugin, () -> {
                    lockedPlayers.remove(player.getUniqueId()); // [락 해제]
                    player.sendMessage("§a[!] " + boxIndex + "번 창고가 안전하게 저장되었습니다.");
                });
            } catch (SQLException e) {
                e.printStackTrace();
                // 실패 시에도 락은 풀어주어 영원히 못 여는 상황 방지
                Bukkit.getScheduler().runTask(plugin, () -> {
                    lockedPlayers.remove(player.getUniqueId()); 
                    player.sendMessage("§c[!] 창고 저장 중 오류가 발생했습니다.");
                });
            }
        });
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}