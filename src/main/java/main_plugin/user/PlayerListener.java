package main_plugin.user;

import main_plugin.NexusCore;
import main_plugin.mail.MailManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Bukkit;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final NexusCore plugin;

    public PlayerListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            
            plugin.getDatabaseManager().loadUserData(player.getUniqueId());
            
            if (plugin.getDatabaseManager().claimNewbiePackage(player.getUniqueId())) {
                sendNewbiePackage(player);
            }

            int mailCount = plugin.getDatabaseManager().getMailCount(name);
            if (mailCount > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage("");
                        player.sendMessage("§e§l[!] §f수령하지 않은 우편이 §b" + mailCount + "개 §f있습니다!");
                        player.sendMessage("§7▶ §b/우편함 §f명령어로 아이템을 확인하세요.");
                        player.sendMessage("");
                        player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.0f);
                    }
                }, 80L);
            }
        });
    }

    private void sendNewbiePackage(Player player) {
        MailManager mailManager = new MailManager(plugin);
        String msg = "환영합니다! 뉴비 정착 지원 패키지입니다.";

        mailManager.sendMail(player.getName(), createItem(Material.CHAINMAIL_HELMET, "§f초보자의 사슬 투구"), msg);
        mailManager.sendMail(player.getName(), createItem(Material.CHAINMAIL_CHESTPLATE, "§f초보자의 사슬 갑옷"), msg);
        mailManager.sendMail(player.getName(), createItem(Material.CHAINMAIL_LEGGINGS, "§f초보자의 사슬 각반"), msg);
        mailManager.sendMail(player.getName(), createItem(Material.CHAINMAIL_BOOTS, "§f초보자의 사슬 장화"), msg);
        mailManager.sendMail(player.getName(), createItem(Material.IRON_PICKAXE, "§f초보자의 철 곡괭이"), msg);
        mailManager.sendMail(player.getName(), createItem(Material.IRON_SWORD, "§f초보자의 철 검"), msg); 
        
        mailManager.sendMail(player.getName(), new ItemStack(Material.VILLAGER_SPAWN_EGG, 1), msg);
        mailManager.sendMail(player.getName(), new ItemStack(Material.COOKED_BEEF, 32), msg);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.sendMessage("§a§l[환영합니다!] §f서버에 처음 오신 것을 환영합니다!");
                player.sendMessage("§e▶ §f정착 지원품 8종이 §d우편함§f으로 배송되었습니다.");
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
            }
        });
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    // [핵심 수정] 유저 퇴장 시 발생하는 렉 제거
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        
        if (plugin.getUserManager() != null) {
            plugin.getUserManager().getUser(uuid).ifPresent(user -> {
                
                // 1. 서버가 멈추지 않게 DB 저장은 비동기로 던집니다.
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getDatabaseManager().saveUserData(user.getUuid());
                });
                
                // 2. 메모리에서 유저 데이터 지우는 건 즉시 실행합니다.
                plugin.getUserManager().removeUser(uuid);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(plugin.getSpawnLocation());
    }
}