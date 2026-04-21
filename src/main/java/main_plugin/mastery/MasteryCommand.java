package main_plugin.mastery;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
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

        // /숙련도 (인자 없을 시 내 정보 출력)
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
                // TODO: 다음에 만들 랭킹 GUI 오픈 메서드 연결
                // plugin.getMasteryRankingManager().openRankingGUI(player);
                player.sendMessage("§e[!] 직업별 명예의 전당(랭킹) 시스템은 곧 오픈됩니다!");
                return true;
            }
            // ==========================================
            // [관리자 전용] 테스트를 위한 경험치 조작 명령어
            // ==========================================
            case "경험치" -> {
                if (!player.isOp()) {
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
                        player.sendMessage("§a[!] " + target.getName() + "님의 경험치를 " + amount + "만큼 올렸습니다.");
                    });
                } catch (NumberFormatException e) {
                    player.sendMessage("§c[!] 숫자를 입력해주세요.");
                }
                return true;
            }
            case "초기화" -> {
                if (!player.isOp()) return true;
                if (args.length < 2) return true;
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    plugin.getUserManager().getUser(target.getUniqueId()).ifPresent(u -> {
                        u.setJob("NONE");
                        u.setJobExp(0);
                        player.sendMessage("§a[!] " + target.getName() + "님의 직업을 초기화했습니다.");
                    });
                }
                return true;
            }
            default -> {
                player.sendMessage("§c[!] 알 수 없는 명령어입니다. (/숙련도, /숙련도 전직, /숙련도 랭킹)");
                return true;
            }
        }
    }

    // ==========================================
    // 내 숙련도 정보 예쁘게 출력하는 로직
    // ==========================================
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

        // 현재 레벨에 들어오기까지 쓴 누적 경험치를 빼서, '이번 레벨'만의 진행도를 구함
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

    // 시각적 진행도 바 (Progress Bar) 생성기
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
            if (sender.isOp()) {
                subCommands.add("경험치");
                subCommands.add("초기화");
            }
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length == 2 && (args[0].equals("경험치") || args[0].equals("초기화")) && sender.isOp()) {
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) playerNames.add(p.getName());
            StringUtil.copyPartialMatches(args[1], playerNames, completions);
        }
        Collections.sort(completions);
        return completions;
    }
}