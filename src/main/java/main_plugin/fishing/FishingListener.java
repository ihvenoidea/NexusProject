package main_plugin.fishing; // 패키지 경로 수정 완료

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class FishingListener implements Listener {

    private final NexusCore plugin;
    private final Random random = new Random();

    public FishingListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        Optional<UserData> userOpt = plugin.getUserManager().getUser(player.getUniqueId());
        if (userOpt.isEmpty()) return;
        UserData user = userOpt.get();

        String job = user.getJob();
        int fisherLevel = 0;

        if (job.equals("FISHER")) {
            long currentExp = user.getJobExp();
            fisherLevel = plugin.getMasteryManager().getLevelFromExp(currentExp);

            addJobExp(player, user, random.nextInt(21) + 15, fisherLevel);
            rollSpecialItem(player, user, fisherLevel, "가라앉은 흉조의 열쇠", Material.TRIPWIRE_HOOK);
        }

        if (random.nextInt(100) < 20) {
            double jackpotChance = 5.0; 
            if (job.equals("FISHER")) {
                jackpotChance += (15.0 * (fisherLevel / 100.0));
            }

            boolean isJackpot = (random.nextDouble() * 100.0) < jackpotChance;
            int dpAmount = isJackpot ? (random.nextInt(11) + 10) : (random.nextInt(4) + 2);

            user.setPoints(user.getPoints() + dpAmount);

            // [오류 수정] user.getUuid() 로 변경
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().saveUserData(user.getUuid());
            });

            if (isJackpot) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§9§l[ 월척! ] §f그물에 걸린 보따리에서 §b" + dpAmount + " DP§f를 획득했습니다!"));
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§9[ 낚시 ] §b" + dpAmount + " DP§f를 건져올렸습니다."));
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
                        "§7심해의 불길한 기운을 머금은 녹슨 열쇠입니다.",
                        "§e▶ 흉조 금고 오픈 시 상위 장비 획득 확률을 대폭 상승시킵니다."
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
            Bukkit.broadcastMessage("§d§l[기적] §e" + player.getName() + "§f님이 낚시 도중 심해에서 §d§l" + itemName + "§f을(를) 건져올렸습니다!!");
        }
    }
}