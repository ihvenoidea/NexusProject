package main_plugin.commands;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DPCommand implements TabExecutor {
    private final NexusCore plugin;

    public DPCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. 인자가 없는 경우: 본인 포인트 확인 (권한 필요 없음)
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "콘솔에서는 인자 없이 사용할 수 없습니다.");
                return true;
            }
            int currentDP = plugin.getDatabaseManager().getDiscordPoints(player.getUniqueId().toString());
            player.sendMessage(ChatColor.AQUA + "나의 현재 포인트: " + ChatColor.WHITE + String.format("%,d DP", currentDP));
            return true;
        }

        // 2. 인자가 있는 경우: 관리자 권한 확인
        if (!sender.hasPermission("nexus.admin.dp")) {
            sender.sendMessage(ChatColor.RED + "권한이 없습니다.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "사용법: /dp <확인/지급/차감/설정> <유저> [양]");
            return true;
        }

        String action = args[0];
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "해당 유저가 온라인이 아닙니다.");
            return true;
        }

        String uuid = target.getUniqueId().toString();

        try {
            switch (action) {
                case "확인":
                    int current = plugin.getDatabaseManager().getDiscordPoints(uuid);
                    sender.sendMessage(ChatColor.AQUA + targetName + "님의 포인트: " + current + " DP");
                    break;
                case "지급":
                    int addAmt = Integer.parseInt(args[2]);
                    updateDP(uuid, addAmt, targetName, sender);
                    break;
                case "차감":
                    int subAmt = Integer.parseInt(args[2]);
                    plugin.getDatabaseManager().deductDiscordPoints(uuid, subAmt);
                    sender.sendMessage(ChatColor.RED + targetName + "님의 포인트를 " + subAmt + " 차감했습니다.");
                    break;
                case "설정":
                    sender.sendMessage(ChatColor.YELLOW + "포인트 설정 기능은 준비 중입니다.");
                    break;
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "에러 발생: 숫자를 확인하세요.");
        }
        return true;
    }

    private void updateDP(String uuid, int amount, String name, CommandSender sender) {
        plugin.getUserManager().getUser(java.util.UUID.fromString(uuid)).ifPresent(user -> {
            user.setPoints(user.getPoints() + amount);
            plugin.getUserManager().saveUserData(user);
            sender.sendMessage(ChatColor.GREEN + name + "님에게 " + amount + " DP를 지급했습니다.");
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("nexus.admin.dp")) return completions;

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("확인", "지급", "차감", "설정"), completions);
        } else if (args.length == 2) {
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) playerNames.add(p.getName());
            StringUtil.copyPartialMatches(args[1], playerNames, completions);
        }
        Collections.sort(completions);
        return completions;
    }
}