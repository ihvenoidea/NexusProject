package main_plugin.commands;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DPCommand implements TabExecutor {
    private final NexusCore plugin;

    public DPCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexus.admin.dp")) {
            sender.sendMessage(ChatColor.RED + "권한이 없습니다.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "사용법: /dp <확인/지급/차감/설정> <유저> [양]");
            return true;
        }

        String action = args[0];
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        
        // 대상이 온라인이 아닐 경우 UUID 확보 로직 필요 (여기선 온라인 기준 예시)
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "해당 유저가 온라인이 아닙니다.");
            return true;
        }

        String uuid = target.getUniqueId().toString();

        try {
            switch (action) {
                case "확인":
                    int current = plugin.getDatabaseManager().getDiscordPoints(uuid);
                    sender.sendMessage(ChatColor.AQUA + targetName + "님의 포인트: " + current + " DP");
                    break;

                case "지급":
                    int addAmt = Integer.parseInt(args[2]);
                    // DB 직접 수정 (addDiscordPoints 메서드 필요)
                    updateDP(uuid, addAmt, targetName, sender);
                    break;

                case "차감":
                    int subAmt = Integer.parseInt(args[2]);
                    plugin.getDatabaseManager().deductDiscordPoints(uuid, subAmt);
                    sender.sendMessage(ChatColor.RED + targetName + "님의 포인트를 " + subAmt + " 차감했습니다.");
                    break;
                    
                case "설정":
                    // 설정 관련 로직이 필요하다면 여기에 추가
                    sender.sendMessage(ChatColor.YELLOW + "포인트 설정 기능은 준비 중입니다.");
                    break;
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "에러 발생: 숫자를 확인하세요.");
        }
        return true;
    }

    // 메모리와 DB를 동시에 업데이트하는 유틸리티 (UserManager 활용 권장)
    private void updateDP(String uuid, int amount, String name, CommandSender sender) {
        plugin.getUserManager().getUser(java.util.UUID.fromString(uuid)).ifPresent(user -> {
            user.setPoints(user.getPoints() + amount);
            plugin.getUserManager().saveUserData(user);
            sender.sendMessage(ChatColor.GREEN + name + "님에게 " + amount + " DP를 지급했습니다.");
        });
    }

    // --- [ Tab 자동완성 로직 ] ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // 권한이 없는 유저에게는 명령어 자동완성을 보여주지 않음
        if (!sender.hasPermission("nexus.admin.dp")) return completions;

        if (args.length == 1) {
            // 첫 번째 인자: 사용할 수 있는 명령어 목록
            List<String> actions = Arrays.asList("확인", "지급", "차감", "설정");
            StringUtil.copyPartialMatches(args[0], actions, completions);
        } else if (args.length == 2) {
            // 두 번째 인자: 온라인 플레이어 이름 자동완성
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerNames.add(p.getName());
            }
            StringUtil.copyPartialMatches(args[1], playerNames, completions);
        } else if (args.length == 3 && !args[0].equals("확인")) {
            // 세 번째 인자: '확인'이 아닐 경우 금액 추천 표시
            StringUtil.copyPartialMatches(args[2], Arrays.asList("<금액(숫자)>", "100", "500", "1000", "5000"), completions);
        }

        // 결과를 알파벳 순서대로 정렬
        Collections.sort(completions);
        return completions;
    }
}