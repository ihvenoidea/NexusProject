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

public class AdminItemCommand implements TabExecutor {

    private final NexusCore plugin;
    
    // [업데이트] 총 12개의 세트 이름으로 확장하여 자동완성 및 검증에 반영
    private final List<String> validNames = Arrays.asList(
            "견고", "도약", "재생", // 실버 등급
            "풍요", "탐욕", "화염", // 골드 등급
            "신속", "혹한", "환영", // 프리즘 등급
            "권능", "재앙", "불멸"  // 신화 등급
    );
    private final List<String> validParts = Arrays.asList("투구", "갑옷", "각반", "장화", "검", "곡괭이", "도끼", "삽", "활");

    public AdminItemCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexus.admin")) {
            sender.sendMessage("§c[!] 해당 명령어를 사용할 권한이 없습니다.");
            return true;
        }

        if (args.length < 4 || !args[0].equals("지급")) {
            showHelp(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[!] 플레이어 §e" + args[1] + "§c님을 찾을 수 없습니다.");
            return true;
        }

        String setName = args[2];
        String part = args[3];

        if (!validNames.contains(setName)) {
            sender.sendMessage("§c[!] 존재하지 않는 세트 이름입니다: §e" + setName);
            sender.sendMessage("§7(사용 가능: " + String.join(", ", validNames) + ")");
            return true;
        }

        if (!validParts.contains(part)) {
            sender.sendMessage("§c[!] 잘못된 부위 명칭입니다: §e" + part);
            return true;
        }

        ItemStack item = plugin.getSetItemManager().createSetItem(setName, part);
        target.getInventory().addItem(item);

        sender.sendMessage("§a[!] §e" + target.getName() + "§a님에게 §f" + setName + " " + part + "§a을(를) 지급했습니다.");
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§6§l[ 넥서스 세트 아이템 관리 ]");
        sender.sendMessage("§f/setitem 지급 <플레이어> <세트이름> <부위>");
        sender.sendMessage("§7- 실버: 견고, 도약, 재생");
        sender.sendMessage("§7- 골드: 풍요, 탐욕, 화염");
        sender.sendMessage("§7- 프리즘: 신속, 혹한, 환영");
        sender.sendMessage("§7- 신화: 권능, 재앙, 불멸");
        sender.sendMessage("§7- 부위: 투구, 갑옷, 각반, 장화, 검, 곡괭이, 도끼, 삽, 활");
        sender.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("nexus.admin")) return completions;

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Collections.singletonList("지급"), completions);
        } else if (args.length == 2) {
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) playerNames.add(p.getName());
            StringUtil.copyPartialMatches(args[1], playerNames, completions);
        } else if (args.length == 3) {
            // 이제 12종의 모든 세트 이름이 탭 자동완성에 나타납니다.
            StringUtil.copyPartialMatches(args[2], validNames, completions);
        } else if (args.length == 4) {
            StringUtil.copyPartialMatches(args[3], validParts, completions);
        }
        
        Collections.sort(completions);
        return completions;
    }
}