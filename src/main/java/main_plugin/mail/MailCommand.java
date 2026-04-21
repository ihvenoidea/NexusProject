package main_plugin.mail;

import main_plugin.NexusCore;
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MailCommand implements CommandExecutor, Listener {

    private final NexusCore plugin;
    private final MailManager mailManager;
    private final int PAGE_SIZE = 45; // 한 페이지에 들어갈 우편물 개수 (0~44슬롯)

    // [보안 최적화] 현재 수령 처리 중인 메일 ID를 저장하여 중복 클릭(아이템 복사)을 방지합니다.
    private final Set<Integer> processingMails = new HashSet<>();

    public MailCommand(NexusCore plugin) {
        this.plugin = plugin;
        this.mailManager = new MailManager(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        if (label.equalsIgnoreCase("우편함")) {
            openMailBox(player, 1);
            return true;
        }

        if (label.equalsIgnoreCase("우편보내기") && player.isOp()) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "사용법: /우편보내기 <닉네임 또는 all> <메시지>");
                return true;
            }

            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem == null || handItem.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "손에 보낼 아이템을 들고 있어야 합니다!");
                return true;
            }

            String target = args[0];
            StringBuilder msgBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) msgBuilder.append(args[i]).append(" ");
            String message = msgBuilder.toString().trim();

            if (target.equalsIgnoreCase("all")) {
                mailManager.sendMailToAll(handItem, message);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[공지] " + ChatColor.WHITE + "서버의 모든 유저에게 우편(" + message + ")이 발송되었습니다!");
            } else {
                mailManager.sendMail(target, handItem, message);
                player.sendMessage(ChatColor.GREEN + target + "님에게 우편을 보냈습니다.");
            }
            return true;
        }
        return false;
    }

    public void openMailBox(Player player, int page) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int totalMails = 0;
            String countSql = "SELECT COUNT(*) FROM user_mail WHERE receiver = ?";
            try (PreparedStatement psCount = plugin.getDatabaseManager().getConnection().prepareStatement(countSql)) {
                psCount.setString(1, player.getName());
                ResultSet rsCount = psCount.executeQuery();
                if (rsCount.next()) totalMails = rsCount.getInt(1);
            } catch (Exception e) { e.printStackTrace(); }

            Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_BLUE + "우편함 - " + page + "페이지");

            ItemStack bgPane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
            for (int i = 0; i < 45; i++) inv.setItem(i, bgPane);

            ItemStack bottomPane = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
            for (int i = 45; i < 54; i++) inv.setItem(i, bottomPane);

            if (page > 1) {
                inv.setItem(45, createGuiItem(Material.ARROW, ChatColor.YELLOW + "◀ 이전 페이지"));
            }
            if (totalMails > page * PAGE_SIZE) {
                inv.setItem(53, createGuiItem(Material.ARROW, ChatColor.YELLOW + "다음 페이지 ▶"));
            }
            
            ItemStack info = createGuiItem(Material.BOOK, ChatColor.WHITE + "우편함 정보");
            ItemMeta infoMeta = info.getItemMeta();
            infoMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "현재 페이지: " + ChatColor.AQUA + page,
                ChatColor.GRAY + "도착한 우편: " + ChatColor.AQUA + totalMails + "개"
            ));
            info.setItemMeta(infoMeta);
            inv.setItem(49, info);

            int offset = (page - 1) * PAGE_SIZE;
            String sql = "SELECT * FROM user_mail WHERE receiver = ? ORDER BY id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {
                ps.setString(1, player.getName());
                ps.setInt(2, PAGE_SIZE);
                ps.setInt(3, offset);
                ResultSet rs = ps.executeQuery();
                
                int slot = 0;
                while (rs.next() && slot < PAGE_SIZE) {
                    int mailId = rs.getInt("id");
                    String msg = rs.getString("message");
                    ItemStack item = MailManager.itemStackFromBase64(rs.getString("item_data"));
                    
                    if (item != null) {
                        ItemMeta meta = item.getItemMeta();
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        lore.add("");
                        lore.add(ChatColor.YELLOW + "📩 메시지: " + ChatColor.WHITE + msg);
                        lore.add(ChatColor.GREEN + "[클릭하여 수령하기]");
                        lore.add(ChatColor.BLACK + "MailID:" + mailId); 
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                        
                        inv.setItem(slot++, item);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
            
            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
        });
    }

    private ItemStack createGuiItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onMailClick(InventoryClickEvent event) {
        // [복사 꼼수 완벽 차단] 유저가 실제 설치한 상자의 이름을 바꿔서 연 경우를 원천 차단합니다.
        if (event.getInventory().getHolder() != null) return; 

        String title = event.getView().getTitle();
        String rawTitle = ChatColor.stripColor(title);
        
        if (!rawTitle.startsWith("우편함")) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        Material type = clicked.getType();

        int currentPage = 1;
        if (rawTitle.contains(" - ")) {
            try {
                String pageStr = rawTitle.split("- ")[1].replace("페이지", "").trim();
                currentPage = Integer.parseInt(pageStr);
            } catch (Exception ignored) {}
        }

        if (type == Material.GRAY_STAINED_GLASS_PANE || 
            type == Material.BLACK_STAINED_GLASS_PANE || 
            type == Material.BOOK) return;

        if (type == Material.ARROW) {
            String btnName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            if (btnName.contains("이전")) openMailBox(player, currentPage - 1);
            else if (btnName.contains("다음")) openMailBox(player, currentPage + 1);
            return;
        }

        if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
            List<String> lore = clicked.getItemMeta().getLore();
            String lastLore = lore.get(lore.size() - 1);
            
            if (lastLore.startsWith(ChatColor.BLACK + "MailID:")) {
                int mailId = Integer.parseInt(lastLore.split(":")[1]);
                
                // [수정: 복사 버그 방지] 이미 수령 처리 중인 메일이라면 무시합니다.
                if (processingMails.contains(mailId)) {
                    return;
                }
                processingMails.add(mailId); // 수령 잠금 설정

                // 로어 원상복구
                lore.remove(lore.size() - 1); 
                lore.remove(lore.size() - 1); 
                lore.remove(lore.size() - 1); 
                lore.remove(lore.size() - 1); 
                
                ItemMeta meta = clicked.getItemMeta();
                meta.setLore(lore);
                clicked.setItemMeta(meta);

                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage(ChatColor.RED + "인벤토리가 가득 찼습니다!");
                    processingMails.remove(mailId); // 인벤토리가 꽉 차면 잠금 해제
                    return;
                }
                
                player.getInventory().addItem(clicked);
                player.sendMessage(ChatColor.GREEN + "우편 아이템을 수령했습니다!");

                final int refreshPage = currentPage;
                // 비동기로 DB에서 삭제 후, 잠금을 해제하고 GUI를 새로고침합니다.
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    mailManager.deleteMail(mailId);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        processingMails.remove(mailId); // 수령 잠금 해제
                        openMailBox(player, refreshPage);
                    });
                });
            }
        }
    }
}