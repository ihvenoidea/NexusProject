package main_plugin.town;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class TownTotemListener implements Listener {

    private final NexusCore plugin;
    private final NamespacedKey key;

    // 타운 단계별 요구 토템 개수 상수
    private final int TIER_1_REQ = 1;
    private final int TIER_2_REQ = 3;
    private final int TIER_3_REQ = 7;
    private final int TIER_MAX_REQ = 15;

    public TownTotemListener(NexusCore plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "mastery_special_item");
        
        // 서버 켜질 때 타운 버프 스케줄러 자동 실행
        startTotemBuffTask();
    }

    // ==========================================
    // 1. 세계수의 토템 설치(누적) 이벤트
    // ==========================================
    @EventHandler
    public void onTotemInstall(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING || !item.hasItemMeta()) return;

        String specialItemType = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (!"세계수의 토템".equals(specialItemType)) return;

        event.setCancelled(true); // 불사의 토템이 손에서 터지는 기본 기능 막기

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        // [주의] 아래 코드는 현재 서버의 타운 매니저 구조에 맞게 메서드명을 수정해주세요.
        /*
        TownData town = plugin.getTownManager().getTownAt(clickedBlock.getLocation());
        
        if (town == null) {
            player.sendMessage("§c[!] 타운 영토 내에서만 세계수의 토템을 설치할 수 있습니다.");
            return;
        }
        
        if (!town.isMember(player.getUniqueId())) {
            player.sendMessage("§c[!] 소속된 타운의 영토에만 설치할 수 있습니다.");
            return;
        }

        int currentTotems = town.getTotemCount();
        if (currentTotems >= TIER_MAX_REQ) {
            player.sendMessage("§c[!] 이 타운은 이미 세계수의 기운이 최고조에 달했습니다! (MAX)");
            return;
        }

        // 토템 개수 누적 및 DB 저장
        int newTotemCount = currentTotems + 1;
        town.setTotemCount(newTotemCount);
        plugin.getTownManager().saveTown(town);
        */

        // 테스트용 임시 변수 (타운 매니저 연동 후 지우세요)
        int newTotemCount = 1; // 임시

        // 아이템 소모
        item.setAmount(item.getAmount() - 1);

        // 화려한 설치 이펙트
        Location loc = clickedBlock.getLocation().add(0.5, 1.5, 0.5);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 150, 0.5, 0.5, 0.5, 0.3);
        player.getWorld().spawnParticle(Particle.COMPOSTER, loc, 80, 1.0, 1.0, 1.0, 0.1);
        player.playSound(loc, Sound.ITEM_TOTEM_USE, 1.0f, 1.2f);
        player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 2.0f);

        // 단계 상승(레벨업) 체크 및 메시지 출력
        player.sendMessage("§a[타운 성장] §f세계수의 토템을 주입했습니다! §7(" + newTotemCount + "/" + TIER_MAX_REQ + ")");

        if (newTotemCount == TIER_1_REQ) {
            Bukkit.broadcastMessage("§a§l[대자연의 축복] §e" + player.getName() + "§f님의 타운이 §aLv.1 축복§f을 받았습니다! (성급함 I)");
        } else if (newTotemCount == TIER_2_REQ) {
            Bukkit.broadcastMessage("§a§l[대자연의 축복] §e" + player.getName() + "§f님의 타운이 §aLv.2 축복§f으로 승급했습니다! (재생 I 추가)");
        } else if (newTotemCount == TIER_3_REQ) {
            Bukkit.broadcastMessage("§a§l[대자연의 축복] §e" + player.getName() + "§f님의 타운이 §aLv.3 축복§f으로 승급했습니다! (이속 증가 I, 성급함 II)");
        } else if (newTotemCount == TIER_MAX_REQ) {
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§6§l[세계수의 완성] §e" + player.getName() + "§f님의 타운이 §6§l최종 축복(MAX)§f에 도달했습니다!!");
            Bukkit.broadcastMessage("§f▶ 이제 해당 타운 영토 내에서는 §c적대적 몬스터가 스폰되지 않습니다§f.");
            Bukkit.broadcastMessage("");
        }
    }

    // ==========================================
    // 2. 타운 영토 내 유저 상시 버프 스케줄러 (2초마다 확인)
    // ==========================================
    private void startTotemBuffTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Location loc = player.getLocation();
                    
                    // [주의] TownManager 연동 부분
                    /*
                    TownData town = plugin.getTownManager().getTownAt(loc);
                    if (town == null) continue;
                    
                    int count = town.getTotemCount();
                    if (count >= TIER_MAX_REQ) { // Lv MAX (15개 이상)
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 1, false, false, true)); // 성급함 II
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1, false, false, true)); // 재생 II
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false, true)); // 이속 II
                    } else if (count >= TIER_3_REQ) { // Lv 3 (7개 이상)
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 1, false, false, true)); // 성급함 II
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false, true)); // 재생 I
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false, true)); // 이속 I
                    } else if (count >= TIER_2_REQ) { // Lv 2 (3개 이상)
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 0, false, false, true)); // 성급함 I
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false, true)); // 재생 I
                    } else if (count >= TIER_1_REQ) { // Lv 1 (1개 이상)
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 0, false, false, true)); // 성급함 I
                    }
                    */
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // 40틱(2초)마다 반복 실행
    }

    // ==========================================
    // 3. (MAX 레벨 한정) 타운 영토 내 적대적 몹 스폰 억제
    // ==========================================
    @EventHandler
    public void onHostileMobSpawn(CreatureSpawnEvent event) {
        // 적대적 몬스터인지 확인 (동물 등은 정상적으로 스폰됨)
        if (!(event.getEntity() instanceof Monster)) return;
        
        Location loc = event.getLocation();

        // [주의] TownManager 연동 부분
        /*
        TownData town = plugin.getTownManager().getTownAt(loc);
        if (town != null && town.getTotemCount() >= TIER_MAX_REQ) {
            // 스폰 취소 (스포너, 자연스폰 등 모두 차단)
            event.setCancelled(true);
        }
        */
    }
}