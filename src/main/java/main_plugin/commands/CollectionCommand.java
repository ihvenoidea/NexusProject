// 파일: src/main/java/main_plugin/commands/CollectionCommand.java
package main_plugin.commands;

import main_plugin.NexusCore;
import main_plugin.collection.CollectionMenuGUI;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CollectionCommand implements CommandExecutor {

    private final NexusCore plugin;

    public CollectionCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[!] 이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        try {
            // 증강체 도감 바로가기가 아닌, 새로 만든 도감 메인 메뉴를 오픈합니다.
            player.openInventory(new CollectionMenuGUI().getInventory());
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.2f);
            
        } catch (Exception e) {
            player.sendMessage("§c[!] 도감을 여는 도중 오류가 발생했습니다. 관리자에게 문의하세요.");
            e.printStackTrace();
        }

        return true;
    }
}