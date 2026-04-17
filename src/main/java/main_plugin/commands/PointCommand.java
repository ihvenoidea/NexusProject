package main_plugin.commands;

import main_plugin.NexusCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PointCommand implements CommandExecutor {
    private final NexusCore plugin;

    public PointCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. 명령어를 보낸 주체가 플레이어인지 확인
        if (!(sender instanceof Player)) {
            sender.sendMessage("게임 안에서만 사용 가능합니다.");
            return true;
        }

        Player player = (Player) sender;

        // 2. NexusCore를 통해 PointShopManager를 가져와서 상점 열기
        // PointShopManager는 CommandExecutor이기도 하므로 직접 onCommand를 호출하거나 
        // 전용 openShopGui 메서드를 public으로 전환하여 호출할 수 있습니다.
        if (plugin.getPointShopManager() != null) {
            // PointShopManager의 onCommand는 인자들을 내부적으로 처리하여 GUI를 엽니다.
            plugin.getPointShopManager().onCommand(player, command, label, args);
        } else {
            player.sendMessage(ChatColor.RED + "상점 시스템을 로드할 수 없습니다.");
        }
        
        return true;
    }
}