package main_plugin.mastery;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MasteryCommand implements TabExecutor {

    private final NexusCore plugin;

    public MasteryCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[!] 플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        if (args.length == 0) {
            showPlayerMasteryInfo(player);
            return true;
        }

        switch (args[0]) {
            case "전직" -> {
                plugin.getMasteryManager().openJobSelectionGUI(player);
                return true;
            }
            case "랭킹" -> {
                plugin.getMasteryRankingGUI().openMainMenu(player);
                return true;
            }
            case "경험치" -> {
                if (!player.hasPermission("nexus.admin")) {
                    player.sendMessage("§c[!] 권한이 없습니다.");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage("§c[!] 사용법: /숙련도 경험치 <닉네임> <량>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage("§c[!] 해당 플레이어를 찾을 수 없습니다.");
                    return true;
                }
                try {
                    long amount = Long.parseLong(args[2]);
                    plugin.getUserManager().getUser(target.getUniqueId()).ifPresent(u -> {
                        u.setJobExp(u.getJobExp() + amount);
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            plugin.getDatabaseManager().saveUserData(u.getUuid());
                        });
                        player.sendMessage("§a[!] " + target.getName() + "님의 경험치를 " + amount + "만큼 올렸습니다.");
                    });
                } catch (NumberFormatException e) {
                    player.sendMessage("§c[!] 숫자를 입력해주세요.");
                }
                return true;
            }
            case "초기화" -> {
                if (!player.hasPermission("nexus.admin")) return true;
                if (args.length < 2) return true;
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    plugin.getUserManager().getUser(target.getUniqueId()).ifPresent(u -> {
                        u.setJob("NONE");
                        u.setJobExp(0);
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            plugin.getDatabaseManager().saveUserData(u.getUuid());
                        });
                        player.sendMessage("§a[!] " + target.getName() + "님의 직업을 초기화했습니다.");
                    });
                }
                return true;
            }
            case "아이템" -> {
                if (!player.hasPermission("nexus.admin")) {
                    player.sendMessage("§c[!] 권한이 없습니다.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§c[!] 사용법: /숙련도 아이템 <대지의파편|생명의정수|흉조의열쇠|세계수의토템>");
                    return true;
                }

                ItemStack item = null;
                NamespacedKey key = new NamespacedKey(plugin, "mastery_special_item");

                switch (args[1]) {
                    case "대지의파편" -> item = createSpecialItem(key, Material.PRISMARINE_CRYSTALS, "대지의 파편", 
                            "§7숙련된 자의 노력 끝에 발견된 진귀한 보물입니다.", "§e▶ 특별한 용도로 사용되거나 거래소에서 비싸게 팔립니다.");
                    case "생명의정수" -> item = createSpecialItem(key, Material.GHAST_TEAR, "생명의 정수", 
                            "§7대지의 생명력이 응집된 신비로운 정수입니다.", "§e▶ 넥서스 영약을 제조하거나 거래소에서 비싸게 팔립니다.");
                    // [버그 픽스] 철사 덫 갈고리 -> 진짜 흉조의 열쇠로 변경
                    case "흉조의열쇠" -> item = createSpecialItem(key, Material.OMINOUS_TRIAL_KEY, "가라앉은 흉조의 열쇠", 
                            "§7심해의 불길한 기운을 머금은 열쇠입니다.", "§e▶ 흉조 금고 오픈 시 상위 장비 획득 확률을 대폭 상승시킵니다.");
                    case "세계수의토템" -> item = createSpecialItem(key, Material.TOTEM_OF_UNDYING, "세계수의 토템", 
                            "§7숙련된 자의 노력 끝에 발견된 진귀한 보물입니다.", "§e▶ 특별한 용도로 사용되거나 거래소에서 비싸게 팔립니다.");
                    default -> player.sendMessage("§c[!] 알 수 없는 아이템입니다.");
                }

                if (item != null) {
                    player.getInventory().addItem(item);
                    player.sendMessage("§d§l[!] §f성공적으로 §e" + args[1] + " §f아이템을 소환했습니다!");
                }
                return true;
            }
            default -> {
                player.sendMessage("§c[!] 알 수 없는 명령어입니다. (/숙련도, /숙련도 전직, /숙련도 랭킹)");
                return true;
            }
        }
    }

    private ItemStack createSpecialItem(NamespacedKey key, Material mat, String name, String lore1, String lore2) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d§l[ " + name + " ]");
            meta.setLore(List.of(lore1, lore2));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void showPlayerMasteryInfo(Player player) {
        Optional<UserData> userOpt = plugin.getUserManager().getUser(player.getUniqueId());
        if (userOpt.isEmpty()) return;
        UserData user = userOpt.get();

        String jobCode = user.getJob();
        String jobName = plugin.getMasteryManager().getJobDisplayName(jobCode);

        player.sendMessage("");
        player.sendMessage("§8====================================");
        player.sendMessage("§6§l[ 내 생활 숙련도 정보 ]");
        
        if (jobCode.equals("NONE")) {
            player.sendMessage("");
            player.sendMessage("§7아직 직업을 선택하지 않았습니다.");
            player.sendMessage("§e▶ §f/숙련도 전직 §e명령어를 통해 전직해주세요!");
            player.sendMessage("§8====================================");
            player.sendMessage("");
            return;
        }

        long currentExp = user.getJobExp();
        int level = plugin.getMasteryManager().getLevelFromExp(currentExp);
        long requiredForNext = plugin.getMasteryManager().getExpRequiredForNextLevel(level);
        double percent = plugin.getMasteryManager().getLevelProgressPercent(currentExp);

        long expInThisLevel = currentExp;
        for (int i = 1; i < level; i++) {
            expInThisLevel -= plugin.getMasteryManager().getExpRequiredForNextLevel(i);
        }

        player.sendMessage("§f▪ 직업: " + jobName);
        if (level >= 100) {
            player.sendMessage("§f▪ 레벨: §d§lMAX (100Lv)");
            player.sendMessage("§f▪ 진행도: §d[■■■■■■■■■■] 100%");
            player.sendMessage("§7(더 이상 오를 경지가 없습니다.)");
        } else {
            player.sendMessage("§f▪ 레벨: §e" + level + "Lv §7(다음 혜택까지 " + (level+1) + "Lv)");
            player.sendMessage(String.format("§f▪ 누적 EXP: §7%,d", currentExp));
            player.sendMessage(String.format("§f▪ 진행도: §a%s §7(%,d / %,d) [%.2f%%]", 
                    createProgressBar(percent), expInThisLevel, requiredForNext, percent));
        }
        player.sendMessage("§8====================================");
        player.sendMessage("");
    }

    private String createProgressBar(double percent) {
        int totalBars = 10;
        int filledBars = (int) (totalBars * (percent / 100.0));
        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < totalBars; i++) {
            if (i == filledBars) bar.append("§7");
            bar.append("■");
        }
        return bar.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("전직", "랭킹"));
            if (sender.hasPermission("nexus.admin")) {
                subCommands.add("경험치");
                subCommands.add("초기화");
                subCommands.add("아이템"); 
            }
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } 
        else if (args.length == 2 && sender.hasPermission("nexus.admin")) {
            if (args[0].equals("경험치") || args[0].equals("초기화")) {
                List<String> playerNames = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) playerNames.add(p.getName());
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
            } 
            else if (args[0].equals("아이템")) {
                StringUtil.copyPartialMatches(args[1], Arrays.asList("대지의파편", "생명의정수", "흉조의열쇠", "세계수의토템"), completions);
            }
        }
        
        Collections.sort(completions);
        return completions;
    }
}