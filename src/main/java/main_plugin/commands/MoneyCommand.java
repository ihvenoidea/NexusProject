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
        // 1. 권한 확인
        if (!sender.hasPermission("nexus.admin.money")) {
            sender.sendMessage(ChatColor.RED + "이 명령어를 사용할 권한이 없습니다.");
            return true;
        }

        // 2. 기본 사용법 안내
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String action = args[0];
        String targetName = args[1];

        // 3. 대상 유저 UUID 확보 (온라인 유저 우선 검색)
        UUID targetUUID;
        Player onlineTarget = Bukkit.getPlayer(targetName);
        if (onlineTarget != null) {
            targetUUID = onlineTarget.getUniqueId();
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            // 접속 기록이 없는 유저는 처리하지 않음
            if (!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline()) {
                sender.sendMessage(ChatColor.RED + "해당 유저(" + targetName + ")는 서버에 접속한 기록이 없습니다.");
                return true;
            }
            targetUUID = offlineTarget.getUniqueId();
        }

        String uuidStr = targetUUID.toString();

        // 4. 서브 명령어 처리
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
                        if (onlineTarget != null) {
                            onlineTarget.sendMessage(ChatColor.GOLD + "[!] " + ChatColor.WHITE + "관리자로부터 " + ChatColor.YELLOW + String.format("%,.0f원", addAmt) + ChatColor.WHITE + "을 지급받았습니다.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "지급 실패: DB 연결이나 유저 데이터를 확인하세요.");
                    }
                    break;

                case "차감":
                    if (args.length < 3) { sender.sendMessage(ChatColor.RED + "차감할 금액을 입력하세요."); return true; }
                    double subAmt = Double.parseDouble(args[2]);
                    if (plugin.getDatabaseManager().deductMoney(uuidStr, subAmt)) {
                        sender.sendMessage(ChatColor.RED + targetName + "님의 돈을 " + String.format("%,.0f원", subAmt) + "원 차감했습니다.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "차감 실패: 잔액이 부족하거나 DB 오류입니다.");
                    }
                    break;

                case "설정":
                    if (args.length < 3) { sender.sendMessage(ChatColor.RED + "설정할 금액을 입력하세요."); return true; }
                    double setAmt = Double.parseDouble(args[2]);
                    if (plugin.getDatabaseManager().setMoney(uuidStr, setAmt)) {
                        sender.sendMessage(ChatColor.YELLOW + targetName + "님의 돈을 " + String.format("%,.0f원", setAmt) + "원으로 설정했습니다.");
                    }
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