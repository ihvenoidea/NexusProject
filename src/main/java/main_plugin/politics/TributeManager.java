package main_plugin.politics;

import dev.lone.itemsadder.api.CustomStack;
import main_plugin.NexusCore;
import main_plugin.mail.MailManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TributeManager {

    private final NexusCore plugin;
    private boolean isProcessing = false;

    public TributeManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    public void donateDP(Player player, int amount) {
        if (isProcessing) {
            player.sendMessage("§c[!] 현재 조공 이벤트를 정산 중입니다. 잠시 후 다시 시도해주세요.");
            return;
        }
        if (amount <= 0) {
            player.sendMessage("§c[조공] 0보다 큰 금액을 입력해주세요.");
            return;
        }
        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            if (user.getPoints() < amount) {
                player.sendMessage("§c[조공] 보유하신 DP가 부족합니다. (현재: " + user.getPoints() + " DP)");
                return;
            }
            user.setPoints(user.getPoints() - amount);
            user.setTotalTribute(user.getTotalTribute() + amount);
            player.sendMessage("§a[조공] 성공적으로 " + amount + " DP를 바쳤습니다!");
            player.sendMessage("§e[정보] 나의 누적 조공량: " + String.format("%,.0f", user.getTotalTribute()) + " DP");
            if (amount >= 500) {
                Bukkit.broadcastMessage("§d§l[NEXUS 조공] §e" + player.getName() + "§f님이 §b" + amount + " DP§f를 자진해서 바쳤습니다!");
            }
            plugin.getUserManager().saveUserData(user);
        });
    }

    public void showLeaderboard(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT name, total_tribute FROM users WHERE total_tribute > 0 ORDER BY total_tribute DESC LIMIT 5";
            List<String> messages = new ArrayList<>();
            messages.add("§6§l--- [ 실시간 DP 조공 순위 TOP 5 ] ---");

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    messages.add("§e" + rank + "위: §f" + rs.getString("name")
                            + " §7- §b" + String.format("%,.0f", rs.getDouble("total_tribute")) + " DP");
                    rank++;
                }
                if (rank == 1) messages.add("§7아직 조공에 참여한 유저가 없습니다.");
            } catch (Exception e) {
                e.printStackTrace();
                messages.add("§c[오류] 랭킹을 불러오는 중 문제가 발생했습니다.");
            }

            Bukkit.getScheduler().runTask(plugin, () -> messages.forEach(player::sendMessage));
        });
    }

    public void rewardTopTributer() {
        if (isProcessing) return;
        isProcessing = true;
        Bukkit.broadcastMessage("§e[시스템] §f조공 이벤트를 정산 중입니다...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String topName = null;
            double topScore = 0;

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT name, total_tribute FROM users WHERE total_tribute > 0 ORDER BY total_tribute DESC LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    topName = rs.getString("name");
                    topScore = rs.getDouble("total_tribute");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 조공량 초기화
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE users SET total_tribute = 0")) {
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }

            final String finalTopName = topName;
            final double finalTopScore = topScore;

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalTopName != null) {
                    CustomStack cs = CustomStack.getInstance("n_items:mythic_augment_ticket");
                    ItemStack rewardItem = (cs != null) ? cs.getItemStack() : new ItemStack(Material.PAPER);
                    if (cs == null) {
                        ItemMeta meta = rewardItem.getItemMeta();
                        meta.setDisplayName("§d§l[신화] 증강권 §c(오류-관리자문의)");
                        rewardItem.setItemMeta(meta);
                    }

                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                            new MailManager(plugin).sendMail(finalTopName, rewardItem, "축하합니다! 조공 이벤트 1위 달성 보상입니다!"));

                    Bukkit.broadcastMessage("§d§l[이벤트 종료] §f조공 1위 §e" + finalTopName
                            + "§f님(" + String.format("%,.0f", finalTopScore) + " DP)에게 §5§l신화 증강권§f이 배송되었습니다!");
                    Bukkit.broadcastMessage("§a(오프라인 상태여도 /우편함 명령어에서 수령할 수 있습니다)");
                } else {
                    Bukkit.broadcastMessage("§7[안내] 조공에 참여한 유저가 없어 보상 지급 없이 이벤트가 종료되었습니다.");
                }

                // 메모리 초기화 및 락 해제
                for (Player p : Bukkit.getOnlinePlayers()) {
                    plugin.getUserManager().getUser(p.getUniqueId()).ifPresent(u -> u.setTotalTribute(0));
                }
                isProcessing = false;
            });
        });
    }
}