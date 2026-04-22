package main_plugin.economy;

import main_plugin.NexusCore;
import main_plugin.mail.MailManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExchangeCommand implements CommandExecutor, Listener {

    private final NexusCore plugin;
    private final ExchangeManager exchangeManager;
    private final int PAGE_SIZE = 36;

    public ExchangeCommand(NexusCore plugin, ExchangeManager exchangeManager) {
        this.plugin = plugin;
        this.exchangeManager = exchangeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        if (label.equalsIgnoreCase("거래소")) {
            if (!player.hasPermission("nexus.admin")) {
                player.sendMessage("§c[!] 거래소 중개인 NPC를 통해서만 열 수 있습니다.");
                return true;
            }
            openExchangeGui(player, 1);
            return true;
        }

        if (label.equalsIgnoreCase("판매")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "사용법: /판매 <금액> <돈/dp>");
                return true;
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "[!] 공기를 팔 수는 없습니다.");
                return true;
            }
            try {
                double price = Double.parseDouble(args[0]);
                if (price <= 0) { player.sendMessage("§c0원 이상으로 올려주세요."); return true; }
                String currency = args[1].toLowerCase().equals("dp") ? "DP" : "MONEY";
                exchangeManager.listItem(player, item, price, currency);
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "[!] 금액은 숫자로 입력해주세요.");
            }
            return true;
        }
        return false;
    }

    public void openExchangeGui(Player player, int page) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int totalItems = 0;
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement psCount = conn.prepareStatement("SELECT COUNT(*) FROM market_listings");
                 ResultSet rsCount = psCount.executeQuery()) {
                if (rsCount.next()) totalItems = rsCount.getInt(1);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Inventory inv = Bukkit.createInventory(null, 54, "§8[ 유저 거래소 - " + page + "페이지 ]");
            ItemStack border = createGuiItem(Material.CYAN_STAINED_GLASS_PANE, " ");
            for (int i = 0; i < 9; i++) inv.setItem(i, border);
            for (int i = 45; i < 54; i++) inv.setItem(i, border);

            if (page > 1) inv.setItem(45, createGuiItem(Material.ARROW, "§e◀ 이전 페이지"));
            final int finalTotal = totalItems;
            if (finalTotal > page * PAGE_SIZE) inv.setItem(53, createGuiItem(Material.ARROW, "§e다음 페이지 ▶"));

            ItemStack info = createGuiItem(Material.EMERALD, "§a거래소 이용 안내");
            ItemMeta im = info.getItemMeta();
            im.setLore(Arrays.asList(
                    "§f/판매 <금액> <돈/dp> 로 아이템을 등록하세요.",
                    "§f구매 및 취소된 아이템은 §d우편함§f으로 배송됩니다."));
            info.setItemMeta(im);
            inv.setItem(49, info);

            int offset = (page - 1) * PAGE_SIZE;
            String sql = "SELECT * FROM market_listings ORDER BY id DESC LIMIT ? OFFSET ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, PAGE_SIZE);
                ps.setInt(2, offset);
                ResultSet rs = ps.executeQuery();
                int slot = 9;
                while (rs.next() && slot < 45) {
                    int id = rs.getInt("id");
                    String sellerUuid = rs.getString("seller_uuid");
                    String sellerName = rs.getString("seller_name");
                    double price = rs.getDouble("price");
                    String cur = rs.getString("currency");
                    ItemStack item = MailManager.itemStackFromBase64(rs.getString("item_data"));
                    if (item != null) {
                        ItemMeta meta = item.getItemMeta();
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        lore.add("§8-------------------------");
                        lore.add("§7판매자: §f" + sellerName);
                        String priceText = cur.equals("DP")
                                ? String.format("%,.0f DP", price)
                                : String.format("%,.0f원", price);
                        lore.add("§7가격: §e" + priceText);
                        lore.add("§8-------------------------");
                        if (player.getUniqueId().toString().equals(sellerUuid)) {
                            lore.add("§c[Shift+우클릭] 판매 취소 및 회수");
                        } else {
                            lore.add("§a[좌클릭] 즉시 구매");
                        }
                        lore.add("§0MID:" + id + ":" + price + ":" + cur + ":" + sellerUuid + ":" + sellerName);
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                        inv.setItem(slot++, item);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
        });
    }

    private ItemStack createGuiItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != null) return;
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (!title.startsWith("[ 유저 거래소")) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        Player player = (Player) event.getWhoClicked();
        Material type = clicked.getType();

        int currentPage = 1;
        try { currentPage = Integer.parseInt(title.split("- ")[1].replace("페이지 ]", "").trim()); } catch (Exception ignored) {}

        if (type == Material.ARROW) {
            String btn = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            if (btn.contains("이전")) openExchangeGui(player, currentPage - 1);
            else if (btn.contains("다음")) openExchangeGui(player, currentPage + 1);
            return;
        }

        if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
            List<String> lore = clicked.getItemMeta().getLore();
            String lastLore = lore.get(lore.size() - 1);
            if (lastLore.startsWith("§0MID:")) {
                String[] data = lastLore.substring(6).split(":");
                int marketId = Integer.parseInt(data[0]);
                double price = Double.parseDouble(data[1]);
                String cur = data[2];
                String sellerUuid = data[3];
                String sellerName = data[4];
                boolean isSeller = player.getUniqueId().toString().equals(sellerUuid);
                if (isSeller && event.getClick().isShiftClick() && event.getClick().isRightClick()) {
                    player.closeInventory();
                    exchangeManager.cancelListing(player, marketId);
                } else if (!isSeller && event.getClick().isLeftClick()) {
                    player.closeInventory();
                    exchangeManager.buyItem(player, marketId, price, cur, sellerUuid, sellerName);
                }
            }
        }
    }
}