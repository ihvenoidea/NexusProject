package main_plugin.items;

import main_plugin.NexusCore;
import main_plugin.traits.ShardExchangeTrait;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class OminousGachaListener implements Listener {

    private final NexusCore plugin;
    private final Random random = new Random();
    private final Map<Location, String> gachaChests = new HashMap<>();
    private final Map<String, Integer> costs = new HashMap<>();

    public OminousGachaListener(NexusCore plugin) {
        this.plugin = plugin;
        costs.put("SILVER", 1);
        costs.put("GOLD", 5);
        costs.put("PRISM", 10);
        costs.put("MYTHIC", 30);
        loadAllChests();
    }

    public void setChestLoc(String tier, Location loc) {
        gachaChests.put(loc, tier.toUpperCase());
        String path = "gacha-chests." + tier.toLowerCase();
        plugin.getConfig().set(path + ".world", loc.getWorld().getName());
        plugin.getConfig().set(path + ".x", loc.getBlockX());
        plugin.getConfig().set(path + ".y", loc.getBlockY());
        plugin.getConfig().set(path + ".z", loc.getBlockZ());
        plugin.saveConfig();
    }

    private void loadAllChests() {
        if (!plugin.getConfig().contains("gacha-chests")) return;
        for (String tier : plugin.getConfig().getConfigurationSection("gacha-chests").getKeys(false)) {
            String path = "gacha-chests." + tier;
            String world = plugin.getConfig().getString(path + ".world");
            int x = plugin.getConfig().getInt(path + ".x");
            int y = plugin.getConfig().getInt(path + ".y");
            int z = plugin.getConfig().getInt(path + ".z");
            gachaChests.put(new Location(Bukkit.getWorld(world), x, y, z), tier.toUpperCase());
        }
    }

    @EventHandler
    public void onChestClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || !gachaChests.containsKey(block.getLocation())) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        String tier = gachaChests.get(block.getLocation());
        int cost = costs.get(tier);

        if (!hasEnoughShards(player, cost)) {
            player.sendMessage("§c[!] 파편이 부족합니다. (" + tier + " 상자 비용: " + cost + "개)");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        removeShards(player, cost);
        playGachaEffect(block.getLocation(), player, tier);

        // 오직 방어구 4종류만 배열에 남겨 무기/도구가 뽑히지 않도록 제한
        String[] parts = {"투구", "갑옷", "각반", "장화"};
        
        // [업데이트] 등급에 맞는 세트 이름 3개 중 1개를 랜덤으로 가져옴
        String setName = getSetNameByTier(tier);
        ItemStack reward = plugin.getSetItemManager().createSetItem(setName, parts[random.nextInt(parts.length)]);

        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), reward);
        } else {
            player.getInventory().addItem(reward);
        }
        
        player.sendMessage("§d§l[심연] §f" + tier + " 상자에서 §e" + reward.getItemMeta().getDisplayName() + "§f을(를) 획득!");
        
        if (tier.equals("MYTHIC")) {
            Bukkit.broadcastMessage("§d§l[공지] §e" + player.getName() + "§f님이 §5§l신화 상자§f에서 §d" + setName + "§f 방어구를 뽑았습니다!");
        }
    }

    // [업데이트] 등급별로 3개의 세트 중 하나를 무작위로 반환하도록 수정
    private String getSetNameByTier(String tier) {
        String[] silverSets = {"견고", "도약", "재생"};
        String[] goldSets = {"풍요", "탐욕", "화염"};
        String[] prismSets = {"신속", "혹한", "환영"};
        String[] mythicSets = {"권능", "재앙", "불멸"};

        return switch (tier) {
            case "SILVER" -> silverSets[random.nextInt(silverSets.length)];
            case "GOLD" -> goldSets[random.nextInt(goldSets.length)];
            case "PRISM" -> prismSets[random.nextInt(prismSets.length)];
            case "MYTHIC" -> mythicSets[random.nextInt(mythicSets.length)];
            default -> "견고";
        };
    }

    private boolean hasEnoughShards(Player player, int amount) {
        int count = 0;
        NamespacedKey key = new NamespacedKey(plugin, ShardExchangeTrait.SHARD_KEY);
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeShards(Player player, int amount) {
        int toRemove = amount;
        NamespacedKey key = new NamespacedKey(plugin, ShardExchangeTrait.SHARD_KEY);
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                if (item.getAmount() <= toRemove) {
                    toRemove -= item.getAmount();
                    item.setAmount(0);
                } else {
                    item.setAmount(item.getAmount() - toRemove);
                    toRemove = 0;
                }
                if (toRemove <= 0) break;
            }
        }
    }

    private void playGachaEffect(Location loc, Player player, String tier) {
        Location center = loc.clone().add(0.5, 1.0, 0.5);
        Sound sound = switch (tier) {
            case "MYTHIC" -> Sound.UI_TOAST_CHALLENGE_COMPLETE;
            case "PRISM" -> Sound.ENTITY_PLAYER_LEVELUP;
            default -> Sound.BLOCK_AMETHYST_BLOCK_CHIME;
        };
        player.getWorld().playSound(center, sound, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.PORTAL, center, 40, 0.5, 0.5, 0.5, 0.1);
    }
}