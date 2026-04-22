package main_plugin.town;

import main_plugin.NexusCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class TownListener implements Listener {

    private final NexusCore plugin;

    public TownListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    // 1. 블록 파괴 방지
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return; // 관리자 예외

        if (!plugin.getTownManager().canBuild(player, event.getBlock().getLocation())) {
            player.sendMessage("§c[!] 남의 타운에서는 블록을 파괴할 수 없습니다.");
            event.setCancelled(true);
        }
    }

    // 2. 블록 설치 방지
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;

        if (!plugin.getTownManager().canBuild(player, event.getBlock().getLocation())) {
            player.sendMessage("§c[!] 남의 타운에서는 블록을 설치할 수 없습니다.");
            event.setCancelled(true);
        }
    }

    // 3. [보안] 상호작용 차단 (상자 도둑질, 문 열기, 버튼/레버 조작 방지)
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;
        
        // 블록에 우클릭을 한 경우만 체크합니다.
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        if (!plugin.getTownManager().canBuild(player, event.getClickedBlock().getLocation())) {
            player.sendMessage("§c[!] 남의 타운에서는 상호작용(상자 열기 등)을 할 수 없습니다.");
            event.setCancelled(true);
        }
    }

    // 4. [보안] 양동이 비우기 차단 (용암, 물 테러 방지)
    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;

        if (!plugin.getTownManager().canBuild(player, event.getBlock().getLocation())) {
            player.sendMessage("§c[!] 남의 타운에서는 양동이를 비울 수 없습니다.");
            event.setCancelled(true);
        }
    }

    // 5. [보안] 양동이 채우기 차단 (남의 타운 물/용암 퍼가기 방지)
    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;

        if (!plugin.getTownManager().canBuild(player, event.getBlock().getLocation())) {
            player.sendMessage("§c[!] 남의 타운에서는 액체를 퍼갈 수 없습니다.");
            event.setCancelled(true);
        }
    }
}