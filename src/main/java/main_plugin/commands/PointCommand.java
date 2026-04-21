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

        // [추가됨] 관리자(OP) 또는 NPC 우클릭(명령어 강제 실행)이 아니면 사용 차단
        if (!player.hasPermission("nexus.admin")) {
            player.sendMessage("§c[!] 조공 관리인 NPC를 통해서만 상점을 열 수 있습니다.");
            return true;
        }

        // 2. PointShopManager의 openShop 메서드를 직접 호출합니다.
        if (plugin.getPointShopManager() != null) {
            plugin.getPointShopManager().openShop(player);
        } else {
            player.sendMessage(ChatColor.RED + "상점 시스템을 로드할 수 없습니다.");
        }
        
        return true;
    }
}