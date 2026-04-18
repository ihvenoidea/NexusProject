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

        // 2. [에러 해결] PointShopManager의 openShop 메서드를 직접 호출합니다.
        if (plugin.getPointShopManager() != null) {
            plugin.getPointShopManager().openShop(player);
        } else {
            player.sendMessage(ChatColor.RED + "상점 시스템을 로드할 수 없습니다.");
        }
        
        return true;
    }
}