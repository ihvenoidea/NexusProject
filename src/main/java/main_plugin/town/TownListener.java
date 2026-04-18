package main_plugin.town;

import main_plugin.NexusCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class TownListener implements Listener {

    private final NexusCore plugin;

    public TownListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return; // 관리자 예외

        if (!plugin.getTownManager().canBuild(player, event.getBlock().getLocation())) {
            player.sendMessage("§c[!] 남의 타운에서는 블록을 파괴할 수 없습니다.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;

        if (!plugin.getTownManager().canBuild(player, event.getBlock().getLocation())) {
            player.sendMessage("§c[!] 남의 타운에서는 블록을 설치할 수 없습니다.");
            event.setCancelled(true);
        }
    }
}