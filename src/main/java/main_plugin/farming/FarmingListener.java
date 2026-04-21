package main_plugin.farming; // 패키지 경로 수정 완료

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class FarmingListener implements Listener {

    private final NexusCore plugin;
    private final Random random = new Random();

    public FarmingListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCropHarvest(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        if (ageable.getAge() != ageable.getMaximumAge()) return; 

        Player player = event.getPlayer();
        Optional<UserData> userOpt = plugin.getUserManager().getUser(player.getUniqueId());
        if (userOpt.isEmpty()) return;
        UserData user = userOpt.get();

        String job = user.getJob();
        int farmerLevel = 0;

        if (job.equals("FARMER")) {
            long currentExp = user.getJobExp();
            farmerLevel = plugin.getMasteryManager().getLevelFromExp(currentExp);

            addJobExp(player, user, random.nextInt(3) + 1, farmerLevel);
            rollSpecialItem(player, user, farmerLevel, "생명의 정수", Material.GHAST_TEAR);
        }

        if (random.nextInt(100) < 10) {
            double jackpotChance = 5.0; 
            if (job.equals("FARMER")) {
                jackpotChance += (10.0 * (farmerLevel / 100.0)); 
            }

            boolean isJackpot = (random.nextDouble() * 100.0) < jackpotChance;
            int dpAmount = isJackpot ? (random.nextInt(6) + 5) : (random.nextInt(3) + 1);

            user.setPoints(user.getPoints() + dpAmount);

            // [오류 수정] user.getUuid() 로 변경
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().saveUserData(user.getUuid());
            });

            if (isJackpot) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§a§l[ 대풍년! ] §f싱싱한 작물 사이로 §b" + dpAmount + " DP§f를 발견했습니다!"));
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§a[ 농사 ] §b" + dpAmount + " DP§f를 얻었습니다."));
            }
        }
    }

    private void addJobExp(Player player, UserData user, int amount, int currentLevel) {
        if (currentLevel >= 100) return; 

        long newExp = user.getJobExp() + amount;
        user.setJobExp(newExp);

        int newLevel = plugin.getMasteryManager().getLevelFromExp(newExp);

        if (newLevel > currentLevel) {
            player.sendTitle("§a§lLEVEL UP!", "§f생활 숙련도가 §e" + newLevel + "레벨§f이 되었습니다!", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            
            if (newLevel % 10 == 0) {
                Bukkit.broadcastMessage("§6§l[숙련도] §e" + player.getName() + "§f님이 " + 
                        plugin.getMasteryManager().getJobDisplayName(user.getJob()) + " §e" + newLevel + "레벨§f을 달성했습니다!");
            }
        }
    }

    private void rollSpecialItem(Player player, UserData user, int currentLevel, String itemName, Material material) {
        double dropChance = 0.0001 + (currentLevel * 0.000004); 

        if (random.nextDouble() <= dropChance) {
            ItemStack specialItem = new ItemStack(material);
            ItemMeta meta = specialItem.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName("§d§l[ " + itemName + " ]");
                meta.setLore(List.of(
                        "§7대지의 생명력이 응집된 신비로운 정수입니다.",
                        "§e▶ 넥서스 영약을 제조하거나 거래소에서 비싸게 팔립니다."
                ));
                
                NamespacedKey key = new NamespacedKey(plugin, "mastery_special_item");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, itemName);
                specialItem.setItemMeta(meta);
            }

            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), specialItem);
            } else {
                player.getInventory().addItem(specialItem);
            }

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
            Bukkit.broadcastMessage("§d§l[기적] §e" + player.getName() + "§f님이 밭을 매던 중 기적적으로 §d§l" + itemName + "§f을(를) 발견했습니다!!");
        }
    }
}