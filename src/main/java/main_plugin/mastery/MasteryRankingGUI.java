package main_plugin.mastery;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MasteryRankingGUI implements Listener {

    private final NexusCore plugin;

    public MasteryRankingGUI(NexusCore plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // 1. 명예의 전당 메인 메뉴 오픈
    // ==========================================
    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§8[ 명예의 전당 - 직업 선택 ]");

        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) { paneMeta.setDisplayName(" "); pane.setItemMeta(paneMeta); }
        for (int i = 0; i < 27; i++) gui.setItem(i, pane);

        gui.setItem(10, createIcon(Material.DIAMOND_PICKAXE, "§b[ 광부 랭킹 ]", "MINER"));
        gui.setItem(12, createIcon(Material.GOLDEN_HOE, "§a[ 농부 랭킹 ]", "FARMER"));
        gui.setItem(14, createIcon(Material.FISHING_ROD, "§9[ 어부 랭킹 ]", "FISHER"));
        gui.setItem(16, createIcon(Material.IRON_AXE, "§6[ 벌목꾼 랭킹 ]", "LOGGER"));

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    // ==========================================
    // 2. 특정 직업의 TOP 10 랭킹 GUI (비동기 로드)
    // ==========================================
    private void openJobRanking(Player player, String jobCode) {
        String jobName = plugin.getMasteryManager().getJobDisplayName(jobCode);
        Inventory gui = Bukkit.createInventory(null, 45, "§8[ 명예의 전당 - " + jobName + " ]");

        ItemStack loading = new ItemStack(Material.CLOCK);
        ItemMeta loadingMeta = loading.getItemMeta();
        if (loadingMeta != null) {
            loadingMeta.setDisplayName("§eDB에서 랭킹 데이터를 불러오는 중...");
            loading.setItemMeta(loadingMeta);
        }
        gui.setItem(22, loading);
        player.openInventory(gui);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<RankingData> top10 = fetchTop10FromDB(jobCode);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.getOpenInventory().getTitle().equals("§8[ 명예의 전당 - " + jobName + " ]")) return;

                gui.clear();
                
                ItemStack pane = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
                ItemMeta paneMeta = pane.getItemMeta();
                if (paneMeta != null) { paneMeta.setDisplayName(" "); pane.setItemMeta(paneMeta); }
                for (int i = 0; i < 45; i++) gui.setItem(i, pane);

                int[] slots = {4, 12, 14, 28, 29, 30, 31, 32, 33, 34};
                String[] rankColors = {"§e§l[1위] ", "§7§l[2위] ", "§6§l[3위] ", "§f[4위] ", "§f[5위] ", "§f[6위] ", "§f[7위] ", "§f[8위] ", "§f[9위] ", "§f[10위] "};

                for (int i = 0; i < top10.size(); i++) {
                    if (i >= 10) break;
                    RankingData data = top10.get(i);
                    int level = plugin.getMasteryManager().getLevelFromExp(data.exp);

                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(rankColors[i] + "§a" + data.name);
                        
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(data.uuid));
                        meta.setOwningPlayer(offlinePlayer);

                        List<String> lore = new ArrayList<>();
                        lore.add("");
                        lore.add("§f▪ 숙련도 레벨: §e" + (level >= 100 ? "MAX" : level + "Lv"));
                        lore.add(String.format("§f▪ 누적 경험치: §7%,d EXP", data.exp));
                        lore.add("");
                        meta.setLore(lore);
                        
                        head.setItemMeta(meta);
                    }
                    gui.setItem(slots[i], head);
                }

                ItemStack back = new ItemStack(Material.ARROW);
                ItemMeta backMeta = back.getItemMeta();
                if (backMeta != null) {
                    backMeta.setDisplayName("§c[ 뒤로 가기 ]");
                    back.setItemMeta(backMeta);
                }
                gui.setItem(40, back);
                
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            });
        });
    }

    // ==========================================
    // 3. DB 비동기 조회 메서드 (수정 완료: 커넥션 자동 반납)
    // ==========================================
    private List<RankingData> fetchTop10FromDB(String jobCode) {
        List<RankingData> list = new ArrayList<>();
        String sql = "SELECT uuid, name, job_exp FROM users WHERE job = ? ORDER BY job_exp DESC LIMIT 10";

        // [핵심 수정] Connection 빌려온 후 try-with-resources로 감싸서 자동 반납되게 함
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, jobCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new RankingData(
                            rs.getString("uuid"),
                            rs.getString("name"),
                            rs.getLong("job_exp")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("랭킹 데이터를 불러오는 중 DB 오류 발생!");
            e.printStackTrace();
        }
        return list;
    }

    // ==========================================
    // 4. GUI 클릭 제어 리스너
    // ==========================================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("§8[ 명예의 전당")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
        Player player = (Player) event.getWhoClicked();

        if (title.equals("§8[ 명예의 전당 - 직업 선택 ]")) {
            List<String> lore = event.getCurrentItem().getItemMeta().getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line.startsWith("§0JOB_CODE:")) {
                        String jobCode = line.split(":")[1];
                        openJobRanking(player, jobCode);
                        return;
                    }
                }
            }
        } else {
            if (event.getCurrentItem().getType() == Material.ARROW) {
                openMainMenu(player);
            }
        }
    }

    private static class RankingData {
        String uuid;
        String name;
        long exp;

        public RankingData(String uuid, String name, long exp) {
            this.uuid = uuid;
            this.name = name;
            this.exp = exp;
        }
    }

    private ItemStack createIcon(Material mat, String name, String jobCode) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(
                    "",
                    "§7서버에서 가장 위대한 " + plugin.getMasteryManager().getJobDisplayName(jobCode) + "§7들을",
                    "§7확인할 수 있는 명예의 전당입니다.",
                    "",
                    "§e▶ 클릭하여 랭킹 확인",
                    "§0JOB_CODE:" + jobCode
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}