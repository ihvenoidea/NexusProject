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

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS market_listings ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "seller_uuid VARCHAR(36) NOT NULL, "
                + "seller_name VARCHAR(16) NOT NULL, "
                + "item_data LONGTEXT NOT NULL, "
                + "price DOUBLE NOT NULL, "
                + "currency VARCHAR(10) NOT NULL, "
                + "list_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ");";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void listItem(Player player, ItemStack item, double price, String currency) {
        String base64 = MailManager.itemStackToBase64(item);
        String sql = "INSERT INTO market_listings (seller_uuid, seller_name, item_data, price, currency) VALUES (?, ?, ?, ?, ?)";
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setString(2, player.getName());
                ps.setString(3, base64);
                ps.setDouble(4, price);
                ps.setString(5, currency.toUpperCase());
                ps.executeUpdate();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.GREEN + "[거래소] 성공적으로 아이템을 등록했습니다!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    player.getInventory().setItemInMainHand(null);

                    String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                            ? item.getItemMeta().getDisplayName()
                            : "§f" + item.getType().name().replace("_", " ");
                    String priceStr = currency.equalsIgnoreCase("DP")
                            ? String.format("%,.0f DP", price)
                            : String.format("%,.0f원", price);

                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§b§l[ 유저 거래소 ] §e" + player.getName() + "§f님이 새로운 상품을 등록했습니다!");
                    Bukkit.broadcastMessage("§f▶ 상품: §a" + itemName + " §f(" + item.getAmount() + "개)");
                    Bukkit.broadcastMessage("§f▶ 가격: §e" + priceStr);
                    Bukkit.broadcastMessage("§7( 중앙 시장의 거래소 중개인을 통해 확인하세요! )");
                    Bukkit.broadcastMessage("");
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.5f);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void buyItem(Player buyer, int marketId, double price, String currency, String sellerUuid, String sellerName) {
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
                    u.setPoints(u.getPoints() - (int) price);
                    plugin.getUserManager().saveUserData(u);
                });
                canAfford = true;
            }
        }
        if (!canAfford) {
            buyer.sendMessage(ChatColor.RED + "[!] 잔액이 부족합니다.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ItemStack boughtItem = null;
            int rowsAffected = 0;
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                try (PreparedStatement psSel = conn.prepareStatement(
                        "SELECT item_data FROM market_listings WHERE id = ?")) {
                    psSel.setInt(1, marketId);
                    ResultSet rs = psSel.executeQuery();
                    if (rs.next()) boughtItem = MailManager.itemStackFromBase64(rs.getString("item_data"));
                }
                try (PreparedStatement psDel = conn.prepareStatement(
                        "DELETE FROM market_listings WHERE id = ?")) {
                    psDel.setInt(1, marketId);
                    rowsAffected = psDel.executeUpdate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final ItemStack finalItem = boughtItem;
            final int finalRows = rowsAffected;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalRows == 1 && finalItem != null) {
                    mailManager.sendMail(buyer.getName(), finalItem, "거래소에서 구매하신 상품입니다.");
                    buyer.sendMessage(ChatColor.AQUA + "[거래소] 구매 완료! 개인 우편함(/우편함)을 확인해주세요.");
                    buyer.playSound(buyer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
                    paySeller(sellerUuid, price, currency);
                } else {
                    refundBuyer(buyer, price, currency);
                    buyer.sendMessage(ChatColor.RED + "[!] 누군가 먼저 구매한 상품입니다. 결제 금액이 환불되었습니다.");
                }
            });
        });
    }

    public void cancelListing(Player seller, int marketId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                ItemStack item = null;
                try (PreparedStatement psSel = conn.prepareStatement(
                        "SELECT item_data FROM market_listings WHERE id = ? AND seller_uuid = ?")) {
                    psSel.setInt(1, marketId);
                    psSel.setString(2, seller.getUniqueId().toString());
                    ResultSet rs = psSel.executeQuery();
                    if (rs.next()) item = MailManager.itemStackFromBase64(rs.getString("item_data"));
                }
                if (item != null) {
                    try (PreparedStatement psDel = conn.prepareStatement(
                            "DELETE FROM market_listings WHERE id = ?")) {
                        psDel.setInt(1, marketId);
                        psDel.executeUpdate();
                    }
                    final ItemStack finalItem = item;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        mailManager.sendMail(seller.getName(), finalItem, "거래소 등록이 취소되어 반환된 아이템입니다.");
                        seller.sendMessage(ChatColor.GREEN + "[!] 판매 취소 완료. 우편함으로 아이템이 반환되었습니다.");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void paySeller(String sellerUuid, double price, String currency) {
        if (currency.equals("MONEY")) {
            plugin.getDatabaseManager().addMoney(sellerUuid, price);
        } else {
            String sql = "UPDATE users SET points = points + ? WHERE uuid = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, (int) price);
                ps.setString(2, sellerUuid);
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
            plugin.getUserManager().getUser(java.util.UUID.fromString(sellerUuid))
                    .ifPresent(u -> u.setPoints(u.getPoints() + (int) price));
        }
    }

    private void refundBuyer(Player buyer, double price, String currency) {
        if (currency.equals("MONEY")) {
            NexusCore.getEconomy().depositPlayer(buyer, price);
        } else {
            plugin.getUserManager().getUser(buyer.getUniqueId()).ifPresent(u -> {
                u.setPoints(u.getPoints() + (int) price);
                plugin.getUserManager().saveUserData(u);
            });
        }
    }
}