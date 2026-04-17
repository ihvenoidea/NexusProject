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
        if (!(sender instanceof Player)) {
            sender.sendMessage("게임 안에서만 사용 가능합니다.");
            return true;
        }

        Player player = (Player) sender;
        // 포인트 상점(조공 상점) GUI를 엽니다.
        // plugin.getPointShopManager().openShop(player); 
        // (현재 구현된 포인트 상점 호출 메서드에 맞춰 수정하세요)
        
        player.sendMessage(ChatColor.GREEN + "포인트 상점을 엽니다.");
        return true;
    }
}