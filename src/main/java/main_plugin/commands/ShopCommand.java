package main_plugin.commands;

import main_plugin.NexusCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final NexusCore plugin;

    public ShopCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("게임 내에서만 사용할 수 있습니다.");
            return true;
        }

        // [핵심] 일반 유저가 /상점 명령어를 치면 차단!
        if (!player.hasPermission("nexus.admin")) {
            player.sendMessage("§c[!] 중앙 시장 상인 NPC를 통해서만 상점을 열 수 있습니다.");
            return true;
        }

        // 바닐라 상점 메인 메뉴 열기
        if (plugin.getVanillaShopManager() != null) {
            plugin.getVanillaShopManager().openCategoryMenu(player);
        } else {
            player.sendMessage("§c[!] 상점 시스템을 불러올 수 없습니다.");
        }

        return true;
    }
}