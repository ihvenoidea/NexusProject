package main_plugin.mastery;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MasteryManager {

    private final NexusCore plugin;

    public MasteryManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    public long getExpRequiredForNextLevel(int currentLevel) {
        if (currentLevel >= 100) return 0; 
        return 1000L + ((long) Math.pow(currentLevel, 3) * 5L);
    }

    public int getLevelFromExp(long totalExp) {
        int level = 1;
        long expLeft = totalExp;

        while (level < 100) {
            long req = getExpRequiredForNextLevel(level);
            if (expLeft >= req) {
                expLeft -= req;
                level++;
            } else {
                break;
            }
        }
        return level;
    }

    public double getLevelProgressPercent(long totalExp) {
        int level = 1;
        long expLeft = totalExp;

        while (level < 100) {
            long req = getExpRequiredForNextLevel(level);
            if (expLeft >= req) {
                expLeft -= req;
                level++;
            } else {
                return ((double) expLeft / req) * 100.0;
            }
        }
        return 100.0; 
    }

    public void openJobSelectionGUI(Player player) {
        Optional<UserData> userOpt = plugin.getUserManager().getUser(player.getUniqueId());
        if (userOpt.isEmpty()) return;
        
        if (!userOpt.get().getJob().equals("NONE")) {
            player.sendMessage("§c[!] 이미 직업을 선택하셨습니다. (현재 직업: " + getJobDisplayName(userOpt.get().getJob()) + ")");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 27, "§8[ 넥서스 직업 선택소 ]");

        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) { paneMeta.setDisplayName(" "); pane.setItemMeta(paneMeta); }
        for (int i = 0; i < 27; i++) gui.setItem(i, pane);

        gui.setItem(10, createIcon(Material.DIAMOND_PICKAXE, "§b[ 광부 ]", "MINER", 
                "§f블록 채굴 시 희귀 광물 획득 확률 상승.", "§f만렙 혜택: 초월 강화 재료인 §d[대지의 파편] §f발견 확률 증가."));
        gui.setItem(12, createIcon(Material.GOLDEN_HOE, "§a[ 농부 ]", "FARMER", 
                "§f농작물 수확 시 DP 잭팟 확률 극대화.", "§f만렙 혜택: 도핑 영약 재료인 §d[생명의 정수] §f발견 확률 증가."));
        gui.setItem(14, createIcon(Material.FISHING_ROD, "§9[ 어부 ]", "FISHER", 
                "§f낚시 속도 및 잭팟 시 DP 획득량 증가.", "§f만렙 혜택: 확률업 열쇠인 §d[가라앉은 흉조의 열쇠] §f발견 확률 증가."));
        gui.setItem(16, createIcon(Material.IRON_AXE, "§6[ 벌목꾼 ]", "LOGGER", 
                "§f나무 벌목 시 추가 원목 및 DP 획득 가능.", "§f만렙 혜택: 타운 버프 토템인 §d[세계수의 토템] §f발견 확률 증가."));

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
    }

    public void selectJob(Player player, String jobCode) {
        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            if (!user.getJob().equals("NONE")) return; 

            user.setJob(jobCode);
            user.setJobExp(0L); 

            // [오류 수정] user.getUuid() 로 변경
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().saveUserData(user.getUuid()); 
            });

            player.closeInventory();
            player.sendTitle("§e§l전직 완료", "§f당신은 이제 " + getJobDisplayName(jobCode) + "입니다!", 10, 70, 20);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            
            Bukkit.broadcastMessage("§6§l[전직] §e" + player.getName() + "§f님이 " + getJobDisplayName(jobCode) + "§f(으)로 전직하셨습니다!");
        });
    }

    public String getJobDisplayName(String jobCode) {
        return switch (jobCode) {
            case "MINER" -> "§b광부";
            case "FARMER" -> "§a농부";
            case "FISHER" -> "§9어부";
            case "LOGGER" -> "§6벌목꾼";
            default -> "§7무직";
        };
    }

    private ItemStack createIcon(Material mat, String name, String jobCode, String... desc) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new java.util.ArrayList<>();
            lore.add("");
            lore.addAll(Arrays.asList(desc));
            lore.add("");
            lore.add("§c[!] 한 번 선택하면 절대 바꿀 수 없습니다.");
            lore.add("§e▶ 클릭하여 전직하기");
            lore.add("§0JOB_CODE:" + jobCode);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}