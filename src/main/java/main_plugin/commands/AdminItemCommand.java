package main_plugin.commands;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 관리자가 세트 아이템을 지급하는 명령어를 처리합니다.
 * 사용법: /setitem 지급 <플레이어> <세트이름> <부위>
 */
public class AdminItemCommand implements TabExecutor {

    private final NexusCore plugin;
    
    // 유효한 세트 이름 및 부위 리스트 (오타 방지 및 탭 자동완성용)
    private final List<String> validNames = Arrays.asList("신속", "체력", "방어", "격류", "흡혈", "황금");
    private final List<String> validParts = Arrays.asList("투구", "갑옷", "각반", "장화", "검", "곡괭이", "도끼", "삽", "활");

    public AdminItemCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. 관리자 권한 확인
        if (!sender.hasPermission("nexus.admin")) {
            sender.sendMessage("§c[!] 해당 명령어를 사용할 권한이 없습니다.");
            return true;
        }

        // 2. 명령어 인자 확인 (지급, 유저, 세트이름, 부위 총 4개 필요)
        if (args.length < 4 || !args[0].equals("지급")) {
            showHelp(sender);
            return true;
        }

        // 3. 대상 플레이어 확인
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[!] 플레이어 §e" + args[1] + "§c님을 찾을 수 없습니다.");
            return true;
        }

        String setName = args[2];
        String part = args[3];

        // 4. 입력값 유효성 검사
        if (!validNames.contains(setName)) {
            sender.sendMessage("§c[!] 존재하지 않는 세트 이름입니다: §e" + setName);
            sender.sendMessage("§7(사용 가능: 신속, 체력, 방어, 격류, 흡혈, 황금)");
            return true;
        }

        if (!validParts.contains(part)) {
            sender.sendMessage("§c[!] 잘못된 부위 명칭입니다: §e" + part);
            sender.sendMessage("§7(사용 가능: 투구, 갑옷, 각반, 장화, 검, 활 등)");
            return true;
        }

        // 5. SetItemManager를 통해 아이템 생성 및 지급
        ItemStack item = plugin.getSetItemManager().createSetItem(setName, part);
        
        target.getInventory().addItem(item);

        // 6. 성공 메시지 출력
        sender.sendMessage("§a[!] §e" + target.getName() + "§a님에게 §f" + setName + " " + part + "§a을(를) 지급했습니다.");
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§6§l[ 넥서스 세트 아이템 관리 ]");
        sender.sendMessage("§f/setitem 지급 <플레이어> <세트이름> <부위>");
        sender.sendMessage("§7- 세트이름: 신속, 체력, 방어, 격류, 흡혈, 황금");
        sender.sendMessage("§7- 부위: 투구, 갑옷, 각반, 장화, 검, 활 등");
        sender.sendMessage("");
    }

    // --- [ Tab 자동완성 로직 ] ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // 권한이 없으면 자동완성을 보여주지 않음
        if (!sender.hasPermission("nexus.admin")) return completions;

        if (args.length == 1) {
            // 첫 번째 인자: "지급" 자동완성
            StringUtil.copyPartialMatches(args[0], Collections.singletonList("지급"), completions);
        } else if (args.length == 2) {
            // 두 번째 인자: 온라인 플레이어 이름 자동완성
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerNames.add(p.getName());
            }
            StringUtil.copyPartialMatches(args[1], playerNames, completions);
        } else if (args.length == 3) {
            // 세 번째 인자: 세트 이름 자동완성
            StringUtil.copyPartialMatches(args[2], validNames, completions);
        } else if (args.length == 4) {
            // 네 번째 인자: 부위 이름 자동완성
            StringUtil.copyPartialMatches(args[3], validParts, completions);
        }
        
        // 결과를 알파벳 순서대로 정렬하여 반환
        Collections.sort(completions);
        return completions;
    }
}