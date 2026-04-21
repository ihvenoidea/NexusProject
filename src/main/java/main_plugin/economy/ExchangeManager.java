package main_plugin.economy;

import main_plugin.NexusCore;
import main_plugin.mail.MailManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ExchangeManager {

    private final NexusCore plugin;
    private final MailManager mailManager;

    public ExchangeManager(NexusCore plugin) {
        this.plugin = plugin;
        this.mailManager = new MailManager(plugin);
        createTable();
    }

    // 1. 거래소 전용 DB 테이블 자동 생성
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS market_listings (" +
                     "id INT AUTO_INCREMENT PRIMARY KEY, " +
                     "seller_uuid VARCHAR(36) NOT NULL, " +
                     "seller_name VARCHAR(16) NOT NULL, " +
                     "item_data LONGTEXT NOT NULL, " +
                     "price DOUBLE NOT NULL, " +
                     "currency VARCHAR(10) NOT NULL, " + // "MONEY" 또는 "DP"
                     "list_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                     ");";
        try (Statement stmt = plugin.getDatabaseManager().getConnection().createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // 2. 아이템 등록 로직 (전체 공지 기능 추가됨)
    public void listItem(Player player, ItemStack item, double price, String currency) {
        String base64 = MailManager.itemStackToBase64(item);
        String sql = "INSERT INTO market_listings (seller_uuid, seller_name, item_data, price, currency) VALUES (?, ?, ?, ?, ?)";
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setString(2, player.getName());
                ps.setString(3, base64);
                ps.setDouble(4, price);
                ps.setString(5, currency.toUpperCase());
                ps.executeUpdate();
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.GREEN + "[거래소] 성공적으로 아이템을 등록했습니다!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    
                    // 손에 든 아이템 제거
                    player.getInventory().setItemInMainHand(null);

                    // ==========================================
                    // [신규] 거래소 아이템 등록 자동 전체 공지
                    // ==========================================
                    String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? 
                                      item.getItemMeta().getDisplayName() : 
                                      "§f" + item.getType().name().replace("_", " ");
                    int amount = item.getAmount();
                    String priceStr = currency.equalsIgnoreCase("DP") ? String.format("%,.0f DP", price) : String.format("%,.0f원", price);

                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§b§l[ 유저 거래소 ] §e" + player.getName() + "§f님이 새로운 상품을 등록했습니다!");
                    Bukkit.broadcastMessage("§f▶ 상품: §a" + itemName + " §f(" + amount + "개)");
                    Bukkit.broadcastMessage("§f▶ 가격: §e" + priceStr);
                    Bukkit.broadcastMessage("§7( 중앙 시장의 거래소 중개인을 통해 확인하세요! )");
                    Bukkit.broadcastMessage("");
                    
                    // 모든 유저에게 가벼운 알림음 재생
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.5f);
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // 3. 아이템 구매 로직 (동시성 방지 완벽 적용)
    public void buyItem(Player buyer, int marketId, double price, String currency, String sellerUuid, String sellerName) {
        // [1] 재화 확인 및 차감 (메인 스레드)
        boolean canAfford = false;
        
        if (currency.equals("MONEY")) {
            if (NexusCore.getEconomy().getBalance(buyer) >= price) {
                NexusCore.getEconomy().withdrawPlayer(buyer, price);
                canAfford = true;
            }
        } else if (currency.equals("DP")) {
            int currentDP = plugin.getUserManager().getUser(buyer.getUniqueId()).get().getPoints();
            if (currentDP >= price) {
                plugin.getUserManager().getUser(buyer.getUniqueId()).ifPresent(u -> {
                    u.setPoints(u.getPoints() - (int)price);
                    plugin.getUserManager().saveUserData(u);
                });
                canAfford = true;
            }
        }

        if (!canAfford) {
            buyer.sendMessage(ChatColor.RED + "[!] 잔액이 부족합니다.");
            return;
        }

        // [2] DB에서 상품 삭제 시도 (비동기)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sqlSelect = "SELECT item_data FROM market_listings WHERE id = ?";
            String sqlDelete = "DELETE FROM market_listings WHERE id = ?";
            
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                // 아이템 데이터 복사
                ItemStack boughtItem = null;
                try (PreparedStatement psSel = conn.prepareStatement(sqlSelect)) {
                    psSel.setInt(1, marketId);
                    ResultSet rs = psSel.executeQuery();
                    if (rs.next()) boughtItem = MailManager.itemStackFromBase64(rs.getString("item_data"));
                }

                // 상품 삭제 시도
                int rowsAffected = 0;
                try (PreparedStatement psDel = conn.prepareStatement(sqlDelete)) {
                    psDel.setInt(1, marketId);
                    rowsAffected = psDel.executeUpdate();
                }

                final ItemStack finalItem = boughtItem;
                final int finalRows = rowsAffected;

                // [3] 결과 처리 (메인 스레드)
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (finalRows == 1 && finalItem != null) { // 성공
                        // 1. 구매자에게 우편으로 배송
                        mailManager.sendMail(buyer.getName(), finalItem, "거래소에서 구매하신 상품입니다.");
                        buyer.sendMessage(ChatColor.AQUA + "[거래소] 구매 완료! 개인 우편함(/우편함)을 확인해주세요.");
                        buyer.playSound(buyer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);

                        // 2. 판매자에게 대금 지급
                        paySeller(sellerUuid, price, currency);

                    } else { // 누군가 먼저 샀음 (환불)
                        refundBuyer(buyer, price, currency);
                        buyer.sendMessage(ChatColor.RED + "[!] 누군가 먼저 구매한 상품입니다. 결제 금액이 환불되었습니다.");
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // 4. 판매 취소 로직
    public void cancelListing(Player seller, int marketId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sqlSelect = "SELECT item_data FROM market_listings WHERE id = ? AND seller_uuid = ?";
            String sqlDelete = "DELETE FROM market_listings WHERE id = ?";
            
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                ItemStack item = null;
                try (PreparedStatement psSel = conn.prepareStatement(sqlSelect)) {
                    psSel.setInt(1, marketId);
                    psSel.setString(2, seller.getUniqueId().toString());
                    ResultSet rs = psSel.executeQuery();
                    if (rs.next()) item = MailManager.itemStackFromBase64(rs.getString("item_data"));
                }

                if (item != null) {
                    try (PreparedStatement psDel = conn.prepareStatement(sqlDelete)) {
                        psDel.setInt(1, marketId);
                        psDel.executeUpdate();
                    }
                    final ItemStack finalItem = item;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        mailManager.sendMail(seller.getName(), finalItem, "거래소 등록이 취소되어 반환된 아이템입니다.");
                        seller.sendMessage(ChatColor.GREEN + "[!] 판매 취소 완료. 우편함으로 아이템이 반환되었습니다.");
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void paySeller(String sellerUuid, double price, String currency) {
        if (currency.equals("MONEY")) {
            plugin.getDatabaseManager().addMoney(sellerUuid, price);
        } else {
            String sql = "UPDATE users SET points = points + ? WHERE uuid = ?";
            try (PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {
                ps.setInt(1, (int)price);
                ps.setString(2, sellerUuid);
                ps.executeUpdate();
            } catch (Exception e) {}
            // 온라인 유저면 메모리도 업데이트
            plugin.getUserManager().getUser(java.util.UUID.fromString(sellerUuid)).ifPresent(u -> u.setPoints(u.getPoints() + (int)price));
        }
    }

    private void refundBuyer(Player buyer, double price, String currency) {
        if (currency.equals("MONEY")) NexusCore.getEconomy().depositPlayer(buyer, price);
        else plugin.getUserManager().getUser(buyer.getUniqueId()).ifPresent(u -> {
            u.setPoints(u.getPoints() + (int)price);
            plugin.getUserManager().saveUserData(u);
        });
    }
}