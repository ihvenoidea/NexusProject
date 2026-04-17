package main_plugin.commands;

import main_plugin.NexusCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NexusCommand implements CommandExecutor {
    private final NexusCore plugin;

    public NexusCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. 권한 확인 (OP이거나 nexus.admin 권한이 있어야 함)
        if (!sender.hasPermission("nexus.admin")) {
            sender.sendMessage(ChatColor.RED + "이 명령어를 사용할 권한이 없습니다.");
            return true;
        }

        // 2. 인자가 없는 경우 도움말 출력
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // 3. 하위 명령어 처리
        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reload")) {
            // NexusCore에 구현된 리로드 메서드 호출
            plugin.reloadMarketConfig();
            
            sender.sendMessage(ChatColor.GREEN + "[Nexus] " + ChatColor.WHITE + "모든 설정 파일(config, market)을 성공적으로 리로드했습니다!");
            
            // 콘솔에도 기록 남기기
            if (sender instanceof Player) {
                plugin.getLogger().info(sender.getName() + "님이 플러그인 설정을 리로드했습니다.");
            }
            return true;
        }

        // 알 수 없는 명령어인 경우 도움말 출력
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== [ Nexus Admin Help ] ==========");
        sender.sendMessage(ChatColor.WHITE + "/nexus reload " + ChatColor.GRAY + "- 설정 파일을 다시 불러옵니다.");
        sender.sendMessage(ChatColor.GOLD + "==========================================");
    }
}