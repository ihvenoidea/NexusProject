package main_plugin.commands;

import main_plugin.NexusCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TributeCommand implements CommandExecutor {

    private final NexusCore plugin;

    public TributeCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 플레이어 여부 확인
        if (!(sender instanceof Player player)) {
            sender.sendMessage("이 명령어는 게임 내 플레이어만 사용할 수 있습니다.");
            return true;
        }

        // 1. /조공 <금액> 처리
        if (label.equalsIgnoreCase("조공")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "사용법: /조공 <금액>");
                return true;
            }

            try {
                int amount = Integer.parseInt(args[0]);
                // TributeManager의 자발적 조공 메서드 호출
                plugin.getTributeManager().donateDP(player, amount);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "금액은 숫자로 입력해야 합니다.");
            }
            return true;
        }

        // 2. /조공순위 처리
        if (label.equalsIgnoreCase("조공순위")) {
            plugin.getTributeManager().showLeaderboard(player);
            return true;
        }

        // 3. /조공종료 처리 (관리자용)
        if (label.equalsIgnoreCase("조공종료")) {
            if (!player.hasPermission("nexus.admin")) {
                player.sendMessage(ChatColor.RED + "권한이 없습니다.");
                return true;
            }
            
            player.sendMessage(ChatColor.YELLOW + "이벤트를 종료하고 1등 보상을 정산합니다...");
            plugin.getTributeManager().rewardTopTributer();
            return true;
        }

        return false;
    }
}