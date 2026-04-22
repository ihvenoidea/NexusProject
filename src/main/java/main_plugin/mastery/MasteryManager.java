package main_plugin.mastery;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MasteryManager {

    private final NexusCore plugin;
    
    // [최적화 핵심] 매번 반복문으로 계산하지 않고, 메모리에 배열로 미리 올려둡니다.
    private final long[] expReqCache = new long[101];    // 각 레벨별 다음 레벨로 가기 위한 요구량
    private final long[] expTotalCache = new long[101];  // 해당 레벨에 도달하기 위한 총 누적 요구량

    public MasteryManager(NexusCore plugin) {
        this.plugin = plugin;
        precalculateExp(); // 서버가 켜질 때 단 한 번만 계산
    }

    /**
     * 레벨업 공식을 적용하여 1~100레벨까지의 경험치 요구량을 미리 배열에 캐싱합니다.
     */
    private void precalculateExp() {
        long total = 0;
        expTotalCache[1] = 0; // 1레벨 도달 요구치는 0

        for (int i = 1; i < 100; i++) {
            long req = 1000L + ((long) Math.pow(i, 3) * 5L);
            expReqCache[i] = req;
            
            total += req;
            expTotalCache[i + 1] = total;
        }
        expTotalCache[100] = total; // 100레벨 누적치 고정
    }

    // ==========================================
    // 1. 경험치 및 레벨 계산 (최적화 완료)
    // ==========================================
    
    /**
     * 다음 레벨로 가기 위한 필요 경험치를 O(1) 속도로 즉시 반환합니다.
     */
    public long getExpRequiredForNextLevel(int currentLevel) {
        if (currentLevel >= 100 || currentLevel < 1) return 0; 
        return expReqCache[currentLevel];
    }

    /**
     * 누적된 총 경험치(jobExp)를 기반으로 현재 레벨(1~100)을 역산합니다.
     * [성능 개선] O(n) while문 대신 O(logN) 이진 탐색(Binary Search)을 사용하여 속도를 극대화했습니다.
     */
    public int getLevelFromExp(long totalExp) {
        if (totalExp <= 0) return 1;
        if (totalExp >= expTotalCache[100]) return 100;

        int low = 1, high = 100;
        while (low <= high) {
            int mid = (low + high) / 2;
            
            // 현재 mid 레벨의 누적치보다 totalExp가 크거나 같고, 다음 레벨 누적치보다는 작다면 현재 레벨은 mid
            if (expTotalCache[mid] <= totalExp && (mid == 100 || expTotalCache[mid + 1] > totalExp)) {
                return mid;
            } else if (expTotalCache[mid] > totalExp) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return 1;
    }

    /**
     * 현재 레벨 기준, 다음 레벨업까지 현재 얼마나 경험치를 채웠는지(%) O(1) 속도로 즉시 반환합니다.
     */
    public double getLevelProgressPercent(long totalExp) {
        int level = getLevelFromExp(totalExp);
        if (level >= 100) return 100.0;

        long expInCurrentLevel = totalExp - expTotalCache[level]; // 현재 레벨에서 올린 순수 경험치
        long reqForNext = expReqCache[level];                     // 이번 레벨의 총 요구량

        return ((double) expInCurrentLevel / reqForNext) * 100.0;
    }

    // ==========================================
    // 2. 직업 선택 GUI 시스템 (1인 1직업)
    // ==========================================
    public void openJobSelectionGUI(Player player) {
        Optional<UserData> userOpt = plugin.getUserManager().getUser(player.getUniqueId());
        if (userOpt.isEmpty()) return;
        
        // 이미 직업이 있는 경우 (NONE이 아닌 경우) 접근 차단
        if (!userOpt.get().getJob().equals("NONE")) {
            player.sendMessage("§c[!] 이미 직업을 선택하셨습니다. (현재 직업: " + getJobDisplayName(userOpt.get().getJob()) + ")");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 27, "§8[ 넥서스 직업 선택소 ]");

        // 배경 유리판 세팅
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) { paneMeta.setDisplayName(" "); pane.setItemMeta(paneMeta); }
        for (int i = 0; i < 27; i++) gui.setItem(i, pane);

        gui.setItem(10, createIcon(Material.DIAMOND_PICKAXE, "§b[ 광부 ]", "MINER", 
                "§f블록 채굴 시 희귀 광물 획득 확률 상승.", "§f만렙 혜택: 초월 강화 재료인 §d[대지의 파편] §f발견 확률 증가."));
        gui.setItem(12, createIcon(Material.GOLDEN_HOE, "§a[ 농부 ]", "FARMER", 
                "§f농작물 수확 시 DP 잭팟 확률 극대화.", "§f만렙 혜택: 도핑 영약 재료인 §d[생명의 정수] §f발견 확률 증가."));
        gui.setItem(14, createIcon(Material.FISHING_ROD, "§9[ 어부 ]", "FISHER", 
                "§f낚시 속도 및 잭팟 시 DP 획득량 증가.", "§f만렙 혜택: 확률업 열쇠인 §d[가라앉은 흉조의 열쇠] §f발견 확률 증가."));
        gui.setItem(16, createIcon(Material.IRON_AXE, "§6[ 벌목꾼 ]", "LOGGER", 
                "§f나무 벌목 시 추가 원목 및 DP 획득 가능.", "§f만렙 혜택: 타운 버프 토템인 §d[세계수의 토템] §f발견 확률 증가."));

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
    }

    public void selectJob(Player player, String jobCode) {
        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            if (!user.getJob().equals("NONE")) return; // 중복 선택 완벽 방지

            user.setJob(jobCode);
            user.setJobExp(0L); // 경험치 0으로 초기화

            // 비동기로 DB에 즉시 저장 ([오류 수정 완료] user.getUuid() 사용)
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().saveUserData(user.getUuid()); 
            });

            player.closeInventory();
            player.sendTitle("§e§l전직 완료", "§f당신은 이제 " + getJobDisplayName(jobCode) + "입니다!", 10, 70, 20);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            
            Bukkit.broadcastMessage("§6§l[전직] §e" + player.getName() + "§f님이 " + getJobDisplayName(jobCode) + "§f(으)로 전직하셨습니다!");
        });
    }

    // 직업 코드에 따른 한글 표시 이름 반환
    public String getJobDisplayName(String jobCode) {
        return switch (jobCode) {
            case "MINER" -> "§b광부";
            case "FARMER" -> "§a농부";
            case "FISHER" -> "§9어부";
            case "LOGGER" -> "§6벌목꾼";
            default -> "§7무직";
        };
    }

    private ItemStack createIcon(Material mat, String name, String jobCode, String... desc) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new java.util.ArrayList<>();
            lore.add("");
            lore.addAll(Arrays.asList(desc));
            lore.add("");
            lore.add("§c[!] 한 번 선택하면 절대 바꿀 수 없습니다.");
            lore.add("§e▶ 클릭하여 전직하기");
            // 클릭 이벤트를 위한 숨겨진 직업 코드 식별자
            lore.add("§0JOB_CODE:" + jobCode);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}