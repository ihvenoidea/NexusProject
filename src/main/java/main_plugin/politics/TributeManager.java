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
    // 정산 중 명령어 연타 방지 및 데이터 꼬임 방지용 자물쇠
    private boolean isProcessing = false; 

    public TributeManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 유저가 디스코드 포인트(DP)를 자발적으로 조공합니다.
     */
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

            // 자발적 소모 및 누적 조공량 가산
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

    /**
     * 조공 순위 TOP 5를 출력합니다.
     */
    public void showLeaderboard(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT name, total_tribute FROM users WHERE total_tribute > 0 ORDER BY total_tribute DESC LIMIT 5";
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // [버그 수정] 글로벌 DB 연결(conn)이 닫히지 않도록 안전하게 분리
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                List<String> messages = new ArrayList<>();
                messages.add("§6§l--- [ 실시간 DP 조공 순위 TOP 5 ] ---");

                int rank = 1;
                while (rs.next()) {
                    String name = rs.getString("name");
                    double tribute = rs.getDouble("total_tribute");
                    messages.add("§e" + rank + "위: §f" + name + " §7- §b" + String.format("%,.0f", tribute) + " DP");
                    rank++;
                }

                if (rank == 1) {
                    messages.add("§7아직 조공에 참여한 유저가 없습니다.");
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (String msg : messages) {
                        player.sendMessage(msg);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                player.sendMessage("§c[오류] 랭킹을 불러오는 중 문제가 발생했습니다.");
            }
        });
    }

    /**
     * 이벤트를 종료하고 1등에게 우편 보상을 지급한 뒤 데이터를 안전하게 초기화합니다.
     */
    public void rewardTopTributer() {
        if (isProcessing) return; // 중복 실행(연타) 완벽 차단
        isProcessing = true;

        Bukkit.broadcastMessage("§e[시스템] §f조공 이벤트를 정산 중입니다...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                
                // 1. 오프라인 유저를 포함하여 DB에서 전체 1등 찾기
                String topSql = "SELECT name, total_tribute FROM users WHERE total_tribute > 0 ORDER BY total_tribute DESC LIMIT 1";
                String topName = null;
                double topScore = 0; 

                // [버그 수정] 글로벌 DB 연결(conn)이 닫히지 않도록 안전하게 분리
                try (PreparedStatement ps = conn.prepareStatement(topSql);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        topName = rs.getString("name");
                        topScore = rs.getDouble("total_tribute");
                    }
                }

                final String finalTopName = topName;
                final double finalTopScore = topScore;
                
                // 버킷 아이템 생성은 메인 스레드에서 실행
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (finalTopName != null) {
                        CustomStack cs = CustomStack.getInstance("n_items:mythic_augment_ticket");
                        ItemStack rewardItem = (cs != null) ? cs.getItemStack() : new ItemStack(Material.PAPER);
                        
                        if (cs == null) {
                            ItemMeta meta = rewardItem.getItemMeta();
                            meta.setDisplayName("§d§l[신화] 증강권 §c(오류-관리자문의)");
                            rewardItem.setItemMeta(meta);
                        }

                        // 다시 비동기로 우편 발송 처리
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            MailManager mailManager = new MailManager(plugin);
                            mailManager.sendMail(finalTopName, rewardItem, "축하합니다! 조공 이벤트 1위 달성 보상입니다!");
                        });

                        // [연출 강화] 1등의 점수까지 함께 방송합니다!
                        Bukkit.broadcastMessage("§d§l[이벤트 종료] §f조공 1위 §e" + finalTopName + "§f님(" + String.format("%,.0f", finalTopScore) + " DP)에게 §5§l신화 증강권§f이 배송되었습니다!");
                        Bukkit.broadcastMessage("§a(오프라인 상태여도 /우편함 명령어에서 수령할 수 있습니다)");
                    } else {
                        Bukkit.broadcastMessage("§7[안내] 조공에 참여한 유저가 없어 보상 지급 없이 이벤트가 종료되었습니다.");
                    }
                });

                // 2. 모든 유저의 누적 조공량을 0으로 초기화
                String resetSql = "UPDATE users SET total_tribute = 0";
                try (PreparedStatement psReset = conn.prepareStatement(resetSql)) {
                    psReset.executeUpdate();
                }

            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getLogger().warning("[TributeManager] 정산 중 데이터베이스 에러 발생!");
            } finally {
                // 3. 메인 스레드에서 메모리 데이터 동기화 및 락(Lock) 해제
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        plugin.getUserManager().getUser(p.getUniqueId()).ifPresent(u -> u.setTotalTribute(0));
                    }
                    isProcessing = false; // 정산이 모두 끝난 후 자물쇠 해제
                });
            }
        });
    }
}