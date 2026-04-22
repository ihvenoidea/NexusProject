package main_plugin.commands;

import main_plugin.NexusCore;
import main_plugin.town.TownData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 타운(부동산) 시스템 명령어를 처리하고 탭 자동완성을 제공합니다.
 */
public class TownCommand implements TabExecutor {

    private final NexusCore plugin;

    public TownCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[!] 게임 내에서만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§6=== [ 넥서스 타운 시스템 ] ===");
            player.sendMessage("§f/타운 생성 §7- 현재 위치에 내 타운을 만듭니다. (1청크 무료)");
            player.sendMessage("§f/타운 확장 §7- 현재 밟고 있는 청크를 구매하여 영토를 넓힙니다.");
            player.sendMessage("§f/타운 이동 §7- 5초 대기 후 내 타운으로 텔레포트합니다.");
            player.sendMessage("§a/타운 시각화 §7- 내 타운의 영토 경계선을 파티클로 켜고 끕니다.");
            player.sendMessage("§a/타운 스폰설정 §7- 내 영토 내에서 타운의 스폰 좌표를 변경합니다.");
            
            if (player.hasPermission("nexus.admin")) {
                player.sendMessage("");
                player.sendMessage("§c[관리자 전용 명령어]");
                player.sendMessage("§c/타운 관리자 정보 <유저명> §7- 타운 정보를 조회합니다.");
                player.sendMessage("§c/타운 관리자 삭제 <유저명> §7- 타운을 강제 철거합니다.");
            }
            return true;
        }

        switch (args[0]) {
            case "생성" -> plugin.getTownManager().createTown(player);
            case "확장" -> plugin.getTownManager().claimChunk(player);
            case "이동" -> plugin.getTownManager().teleportToTown(player);
            case "시각화" -> plugin.getTownManager().toggleVisualization(player);
            case "스폰설정" -> plugin.getTownManager().setSpawn(player);
            case "관리자" -> handleAdminCommand(player, args);
            default -> player.sendMessage("§c[!] 알 수 없는 명령어입니다. '/타운'을 입력하여 도움말을 확인하세요.");
        }
        return true;
    }

    private void handleAdminCommand(Player player, String[] args) {
        if (!player.hasPermission("nexus.admin")) {
            player.sendMessage("§c[!] 이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§c[!] 사용법: /타운 관리자 <정보|삭제> <유저명>");
            return;
        }

        String action = args[1];
        String targetName = args[2];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();

        if (action.equals("삭제")) {
            if (plugin.getTownManager().deleteTown(targetUUID)) {
                player.sendMessage("§a[!] 성공적으로 §e" + targetName + "§a님의 타운을 강제 철거했습니다.");
            } else {
                player.sendMessage("§c[!] 해당 유저는 타운을 소유하고 있지 않습니다.");
            }
        } else if (action.equals("정보")) {
            TownData town = plugin.getTownManager().getTown(targetUUID);
            if (town == null) {
                player.sendMessage("§c[!] 해당 유저는 타운을 소유하고 있지 않습니다.");
                return;
            }
            Location loc = town.getSpawnLoc();
            player.sendMessage("§6=== [ " + targetName + "님의 타운 정보 ] ===");
            player.sendMessage("§f소유 청크 수: §e" + town.getClaimedChunks().size() + "개");
            player.sendMessage("§f스폰 위치: §7" + loc.getWorld().getName() + " (X: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ() + ")");
        } else {
            player.sendMessage("§c[!] 알 수 없는 관리자 명령어입니다.");
        }
    }

    // --- [ Tab 자동완성 로직 ] ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 첫 번째 인자: 기본 명령어 목록
            List<String> subCommands = new ArrayList<>(Arrays.asList("생성", "확장", "이동", "시각화", "스폰설정"));
            if (sender.hasPermission("nexus.admin")) {
                subCommands.add("관리자");
            }
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } 
        else if (args.length == 2 && args[0].equals("관리자") && sender.hasPermission("nexus.admin")) {
            // 두 번째 인자: /타운 관리자 <탭>
            StringUtil.copyPartialMatches(args[1], Arrays.asList("정보", "삭제"), completions);
        } 
        else if (args.length == 3 && args[0].equals("관리자") && sender.hasPermission("nexus.admin")) {
            // 세 번째 인자: /타운 관리자 정보/삭제 <탭> (온라인 플레이어 목록)
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerNames.add(p.getName());
            }
            StringUtil.copyPartialMatches(args[2], playerNames, completions);
        }

        Collections.sort(completions);
        return completions;
    }
}