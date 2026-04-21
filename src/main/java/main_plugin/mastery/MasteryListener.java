package main_plugin.mastery;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class MasteryListener implements Listener {

    private final NexusCore plugin;
    private final Random random = new Random();

    public MasteryListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // 1. 직업 선택 GUI 클릭 이벤트
    // ==========================================
    @EventHandler
    public void onJobSelectionClick(InventoryClickEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (!title.equals("[ 넥서스 직업 선택소 ]")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        List<String> lore = event.getCurrentItem().getItemMeta().getLore();
        if (lore != null) {
            for (String line : lore) {
                if (line.startsWith("§0JOB_CODE:")) {
                    String jobCode = line.split(":")[1];
                    plugin.getMasteryManager().selectJob(player, jobCode);
                    return;
                }
            }
        }
    }

    // ==========================================
    // 2. 어뷰징 방지 (유저가 설치한 블록은 경험치/보상 제외)
    // ==========================================
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        String type = block.getType().toString();
        
        // 광물이나 원목을 설치할 경우 메타데이터 표식을 남김
        if (type.contains("ORE") || type.contains("LOG") || type.contains("STEM")) {
            block.setMetadata("player_placed", new FixedMetadataValue(plugin, true));
        }
    }

    // ==========================================
    // 3. 광부 & 벌목꾼 실제 행동 감지 및 보상 로직
    // ==========================================
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // 유저가 직접 설치한 블록이면 무시 (어뷰징 방지)
        if (block.hasMetadata("player_placed")) return;

        Optional<UserData> userOpt = plugin.getUserManager().getUser(player.getUniqueId());
        if (userOpt.isEmpty()) return;
        UserData user = userOpt.get();

        String job = user.getJob();
        String blockType = block.getType().toString();

        // [ 광부 (MINER) 로직 ]
        if (job.equals("MINER") && (blockType.contains("ORE") || blockType.contains("ANCIENT_DEBRIS"))) {
            // 경험치 획득 (광물 종류에 따라 다르게 설정 가능, 기본 10~30)
            int expGain = random.nextInt(21) + 10;
            addJobExp(player, user, expGain);

            // 특수 아이템 드롭 (대지의 파편)
            rollSpecialItem(player, user, "대지의 파편", Material.PRISMARINE_CRYSTALS);
        }

        // [ 벌목꾼 (LOGGER) 로직 ]
        else if (job.equals("LOGGER") && (blockType.contains("LOG") || blockType.contains("STEM"))) {
            // 경험치 획득 (원목 1개당 5~15)
            int expGain = random.nextInt(11) + 5;
            addJobExp(player, user, expGain);

            // 특수 아이템 드롭 (세계수의 토템)
            rollSpecialItem(player, user, "세계수의 토템", Material.TOTEM_OF_UNDYING);

            // [벌목꾼 전용] DP 획득 시스템 (농사 시스템과 유사한 밸런스)
            if (random.nextInt(100) < 10) { // 10% 확률로 발견
                boolean isJackpot = random.nextInt(100) < 5; // 그 중 5%는 잭팟
                int dpAmount = isJackpot ? (random.nextInt(6) + 5) : (random.nextInt(3) + 1);

                user.setPoints(user.getPoints() + dpAmount);

                if (isJackpot) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§6§l[ 잭팟! ] §f나뭇잎 사이로 §b" + dpAmount + " DP§f를 발견했습니다!"));
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§a[ 벌목 ] §b" + dpAmount + " DP§f를 얻었습니다."));
                }
            }
        }
    }

    // ==========================================
    // 4. 경험치 추가 및 레벨업 처리 메서드
    // ==========================================
    private void addJobExp(Player player, UserData user, int amount) {
        long currentExp = user.getJobExp();
        int currentLevel = plugin.getMasteryManager().getLevelFromExp(currentExp);

        if (currentLevel >= 100) return; // 만렙 달성 시 경험치 획득 중단

        long newExp = currentExp + amount;
        user.setJobExp(newExp);

        int newLevel = plugin.getMasteryManager().getLevelFromExp(newExp);

        // 레벨업 감지
        if (newLevel > currentLevel) {
            player.sendTitle("§a§lLEVEL UP!", "§f생활 숙련도가 §e" + newLevel + "레벨§f이 되었습니다!", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            
            if (newLevel % 10 == 0) { // 10레벨 단위 전체 공지
                Bukkit.broadcastMessage("§6§l[숙련도] §e" + player.getName() + "§f님이 " + 
                        plugin.getMasteryManager().getJobDisplayName(user.getJob()) + " §e" + newLevel + "레벨§f을 달성했습니다!");
            }
        }

        // [컴파일 오류 수정 완료] user 객체 대신 UUID를 꺼내서 전달
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().saveUserData(user.getUuid());
        });
    }

    // ==========================================
    // 5. 극악 확률 특수 아이템 드롭 로직 (10배 상향 반영)
    // ==========================================
    private void rollSpecialItem(Player player, UserData user, String itemName, Material material) {
        int currentLevel = plugin.getMasteryManager().getLevelFromExp(user.getJobExp());

        // 기본 0.01% (0.0001) ~ 만렙 시 0.05% (0.0005)
        // 레벨당 0.0004%씩 확률이 증가합니다.
        double dropChance = 0.0001 + (currentLevel * 0.000004); 

        if (random.nextDouble() <= dropChance) {
            ItemStack specialItem = new ItemStack(material);
            ItemMeta meta = specialItem.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName("§d§l[ " + itemName + " ]");
                meta.setLore(List.of(
                        "§7숙련된 자의 노력 끝에 발견된 진귀한 보물입니다.",
                        "§e▶ 특별한 용도로 사용되거나 거래소에서 비싸게 팔립니다."
                ));
                
                // 특수 아이템 식별 키 삽입
                NamespacedKey key = new NamespacedKey(plugin, "mastery_special_item");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, itemName);
                specialItem.setItemMeta(meta);
            }

            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), specialItem);
            } else {
                player.getInventory().addItem(specialItem);
            }

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
            Bukkit.broadcastMessage("§d§l[기적] §e" + player.getName() + "§f님이 작업 도중 기적적으로 §d§l" + itemName + "§f을(를) 발견했습니다!!");
        }
    }
}