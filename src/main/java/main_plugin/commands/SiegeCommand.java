package main_plugin.commands;

import main_plugin.NexusCore;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SiegeCommand implements CommandExecutor {

    private final NexusCore plugin;

    public SiegeCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("nexus.admin")) return true;

        if (args.length == 0) {
            player.sendMessage("§d§l[ 레이드 관리 ]");
            player.sendMessage("§f/공성전 코어설정 §7- 코어 위치 지정");
            player.sendMessage("§f/공성전 시작 §7- 레이드 시작 (무제한)");
            player.sendMessage("§f/공성전 종료 §7- 레이드 강제 중단");
            return true;
        }

        switch (args[0]) {
            case "코어설정" -> {
                Block target = player.getTargetBlockExact(5);
                if (target == null) return true;
                plugin.getSiegeManager().setCoreLocation(target.getLocation());
                player.sendMessage("§a[!] 코어 위치가 설정되었습니다.");
            }
            case "시작" -> plugin.getSiegeManager().startSiege();
            case "종료" -> plugin.getSiegeManager().endSiege(null);
        }
        return true;
    }
}