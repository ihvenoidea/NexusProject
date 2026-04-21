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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class StorageManager {

    private final NexusCore plugin;
    public final int MAX_BOXES = 5; // 최대 창고 개수
    public final double BASE_COST = 50000; // 창고 해금 기본 비용

    public StorageManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 유저의 창고 메인 메뉴 GUI를 엽니다.
     */
    public void openStorageMenu(Player player) {
        Optional<UserData> userOpt = plugin.getUserManager().getUser(player.getUniqueId());
        if (userOpt.isEmpty()) return;

        int unlocked = userOpt.get().getUnlockedBoxes();
        Inventory gui = Bukkit.createInventory(null, 27, "§8[ NEXUS ] 창고 메뉴");

        for (int i = 1; i <= MAX_BOXES; i++) {
            ItemStack icon;
            if (i <= unlocked) {
                // 이미 해금된 창고
                icon = createItem(Material.CHEST, "§a§l" + i + "번 창고 박스", "§7클릭하여 창고를 엽니다.");
            } else if (i == unlocked + 1) {
                // 해금 가능한 다음 창고
                double cost = (i - 1) * BASE_COST;
                icon = createItem(Material.MINECART, "§e§l" + i + "번 창고 잠금 해제", 
                        "§f비용: §e" + String.format("%,.0f원", cost), "§7클릭하여 구매합니다.");
            } else {
                // 아직 잠겨있는 창고
                icon = createItem(Material.BARRIER, "§c§l잠긴 박스", "§7이전 박스를 먼저 해금해야 합니다.");
            }
            gui.setItem(10 + i, icon);
        }

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    /**
     * DB에서 특정 창고의 아이템 데이터를 비동기로 불러와 엽니다.
     */
    public void openBox(Player player, int boxIndex) {
        player.sendMessage("§e창고 데이터를 불러오는 중입니다...");
        
        // 메인 스레드 렉 방지를 위해 비동기 처리
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String base64Data = null;
            
            // [버그 해결] Connection 객체를 try 괄호 안에 넣지 않아서 글로벌 연결이 끊어지는 것을 막음!
            try (PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement("SELECT items FROM user_storage WHERE uuid = ? AND box_index = ?")) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setInt(2, boxIndex);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    base64Data = rs.getString("items");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            final String finalData = base64Data;
            // 인벤토리 생성 및 열기는 반드시 메인 스레드에서 수행
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

    /**
     * 창고의 아이템 데이터를 DB에 비동기로 저장합니다.
     */
    public void saveBox(Player player, int boxIndex, ItemStack[] contents) {
        String base64Data = ItemSerializer.toBase64(contents);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO user_storage (uuid, box_index, items) VALUES (?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE items = ?";
                         
            // [버그 해결] Connection 객체를 try 괄호 안에 넣지 않아서 글로벌 연결이 끊어지는 것을 막음!
            try (PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setInt(2, boxIndex);
                ps.setString(3, base64Data);
                ps.setString(4, base64Data);
                ps.executeUpdate();
                
                player.sendMessage("§a[!] " + boxIndex + "번 창고가 안전하게 저장되었습니다.");
            } catch (SQLException e) {
                e.printStackTrace();
                player.sendMessage("§c[!] 창고 저장 중 오류가 발생했습니다.");
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