package main_plugin.commands;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MoneyCommand implements CommandExecutor {
    private final NexusCore plugin;

    public MoneyCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. 인자가 없는 경우: 본인 잔액 확인 (권한 필요 없음)
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "콘솔에서는 인자 없이 사용할 수 없습니다.");
                return true;
            }
            double balance = plugin.getDatabaseManager().getMoney(player.getUniqueId().toString());
            player.sendMessage(ChatColor.YELLOW + "--------------------------------");
            player.sendMessage(ChatColor.WHITE + "나의 현재 잔액: " + ChatColor.GOLD + String.format("%,.0f원", balance));
            player.sendMessage(ChatColor.YELLOW + "--------------------------------");
            return true;
        }

        // 2. 인자가 있는 경우: 관리자 권한 확인
        if (!sender.hasPermission("nexus.admin.money")) {
            sender.sendMessage(ChatColor.RED + "이 명령어를 사용할 권한이 없습니다.");
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String action = args[0];
        String targetName = args[1];

        // 대상 유저 UUID 확보
        UUID targetUUID;
        Player onlineTarget = Bukkit.getPlayer(targetName);
        if (onlineTarget != null) {
            targetUUID = onlineTarget.getUniqueId();
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline()) {
                sender.sendMessage(ChatColor.RED + "해당 유저(" + targetName + ")는 서버에 접속한 기록이 없습니다.");
                return true;
            }
            targetUUID = offlineTarget.getUniqueId();
        }

        String uuidStr = targetUUID.toString();

        try {
            switch (action) {
                case "확인":
                    double bal = plugin.getDatabaseManager().getMoney(uuidStr);
                    sender.sendMessage(ChatColor.YELLOW + "--------------------------------");
                    sender.sendMessage(ChatColor.WHITE + targetName + "님의 잔액: " + ChatColor.GOLD + String.format("%,.0f원", bal));
                    sender.sendMessage(ChatColor.YELLOW + "--------------------------------");
                    break;
                case "지급":
                    if (args.length < 3) { sender.sendMessage(ChatColor.RED + "지급할 금액을 입력하세요."); return true; }
                    double addAmt = Double.parseDouble(args[2]);
                    if (plugin.getDatabaseManager().addMoney(uuidStr, addAmt)) {
                        sender.sendMessage(ChatColor.GREEN + targetName + "님에게 " + String.format("%,.0f원", addAmt) + "을 지급했습니다.");
                        if (onlineTarget != null) onlineTarget.sendMessage(ChatColor.GOLD + "[!] " + ChatColor.WHITE + "관리자로부터 " + ChatColor.YELLOW + String.format("%,.0f원", addAmt) + ChatColor.WHITE + "을 지급받았습니다.");
                    }
                    break;
                case "차감":
                    if (args.length < 3) { sender.sendMessage(ChatColor.RED + "차감할 금액을 입력하세요."); return true; }
                    double subAmt = Double.parseDouble(args[2]);
                    if (plugin.getDatabaseManager().deductMoney(uuidStr, subAmt)) sender.sendMessage(ChatColor.RED + targetName + "님의 돈을 " + String.format("%,.0f원", subAmt) + "원 차감했습니다.");
                    break;
                case "설정":
                    if (args.length < 3) { sender.sendMessage(ChatColor.RED + "설정할 금액을 입력하세요."); return true; }
                    double setAmt = Double.parseDouble(args[2]);
                    if (plugin.getDatabaseManager().setMoney(uuidStr, setAmt)) sender.sendMessage(ChatColor.YELLOW + targetName + "님의 돈을 " + String.format("%,.0f원", setAmt) + "원으로 설정했습니다.");
                    break;
                default:
                    sendUsage(sender);
                    break;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "금액은 숫자로 입력해야 합니다.");
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--------- [ Economy Help ] ---------");
        sender.sendMessage(ChatColor.WHITE + "/money 확인 <유저>");
        sender.sendMessage(ChatColor.WHITE + "/money 지급 <유저> <금액>");
        sender.sendMessage(ChatColor.WHITE + "/money 차감 <유저> <금액>");
        sender.sendMessage(ChatColor.WHITE + "/money 설정 <유저> <금액>");
        sender.sendMessage(ChatColor.GOLD + "----------------------------------");
    }
}