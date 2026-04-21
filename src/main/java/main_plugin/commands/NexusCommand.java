package main_plugin.commands;

import main_plugin.NexusCore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NexusCommand implements CommandExecutor {
    private final NexusCore plugin;

    public NexusCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. 권한 확인 (OP이거나 nexus.admin 권한이 있어야 함)
        if (!sender.hasPermission("nexus.admin")) {
            sender.sendMessage(ChatColor.RED + "이 명령어를 사용할 권한이 없습니다.");
            return true;
        }

        // 2. 인자가 없는 경우 도움말 출력
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // 3. 하위 명령어 처리
        String subCommand = args[0].toLowerCase();

        // --- 플러그인 리로드 ---
        if (subCommand.equals("reload")) {
            // NexusCore에 구현된 리로드 메서드 호출
            plugin.reloadMarketConfig();
            
            sender.sendMessage(ChatColor.GREEN + "[Nexus] " + ChatColor.WHITE + "모든 설정 파일(config, market)을 성공적으로 리로드했습니다!");
            
            // 콘솔에도 기록 남기기
            if (sender instanceof Player player) {
                plugin.getLogger().info(player.getName() + "님이 플러그인 설정을 리로드했습니다.");
            }
            return true;
        }

        // --- [신규] 심연의 상자 지정 ---
        if (subCommand.equals("setgacha")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "이 명령어는 게임 내에서만 사용할 수 있습니다.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§c[!] 사용법: /nexus setgacha <silver|gold|prism|mythic>");
                return true;
            }

            String tier = args[1].toUpperCase();
            
            // 등급 오타 방지용 유효성 검사
            if (!tier.equals("SILVER") && !tier.equals("GOLD") && !tier.equals("PRISM") && !tier.equals("MYTHIC")) {
                player.sendMessage("§c[!] 잘못된 등급입니다. (silver, gold, prism, mythic 중 하나를 입력하세요)");
                return true;
            }

            // 플레이어가 바라보는 블록 (최대 5칸 거리) 가져오기
            Block target = player.getTargetBlockExact(5);
            if (target == null || target.getType() == Material.AIR) {
                player.sendMessage("§c[!] 바라보고 있는 블록이 없습니다. 지정할 상자를 조준하고 명령어를 입력해주세요.");
                return true;
            }
            
            // OminousGachaListener에 상자 위치 및 등급 등록
            plugin.getOminousGachaListener().setChestLoc(tier, target.getLocation());
            player.sendMessage("§d§l[!] §f바라보고 있는 블록을 §e" + tier + " 상자§f로 지정했습니다!");
            return true;
        }

        // 알 수 없는 명령어인 경우 도움말 출력
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== [ Nexus Admin Help ] ==========");
        sender.sendMessage(ChatColor.WHITE + "/nexus reload " + ChatColor.GRAY + "- 설정 파일을 다시 불러옵니다.");
        sender.sendMessage(ChatColor.WHITE + "/nexus setgacha <등급> " + ChatColor.GRAY + "- 바라보는 블록을 뽑기 상자로 지정합니다.");
        sender.sendMessage(ChatColor.GRAY + "  (등급: silver, gold, prism, mythic)");
        sender.sendMessage(ChatColor.GOLD + "==========================================");
    }
}