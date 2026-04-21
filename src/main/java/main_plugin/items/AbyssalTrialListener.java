package main_plugin.items;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

public class AbyssalTrialListener implements Listener {

    private final NexusCore plugin;
    private final Random random = new Random();

    // 등급별 3종 세트
    private final String[] silverSets = {"견고", "도약", "재생"};
    private final String[] goldSets = {"풍요", "탐욕", "화염"};
    private final String[] prismSets = {"신속", "혹한", "환영"};
    private final String[] mythicSets = {"권능", "재앙", "불멸"};
    
    // 등장 가능한 장비 파츠
    private final String[] parts = {"투구", "갑옷", "각반", "장화", "검", "활", "곡괭이", "도끼", "삽"};

    public AbyssalTrialListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVaultInteract(PlayerInteractEvent event) {
        // 메인 손 우클릭만 감지 (중복 실행 방지)
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        // [참고] 현재 서버 환경에 맞게 금고 블록 판별 (예시: 마인크래프트 1.21 VAULT 블록 또는 ENDER_CHEST)
        if (block.getType() != Material.VAULT && block.getType() != Material.ENDER_CHEST) return;
        
        // (필요 시 특정 좌표나 블록 메타데이터 검사 로직을 이곳에 추가합니다)

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        boolean isKeyUsed = false;
        
        // 1. 손에 든 아이템이 어부 특수 아이템 '가라앉은 흉조의 열쇠'인지 NBT 태그로 확인
        if (itemInHand != null && itemInHand.hasItemMeta()) {
            NamespacedKey key = new NamespacedKey(plugin, "mastery_special_item");
            String specialItemType = itemInHand.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            
            if ("가라앉은 흉조의 열쇠".equals(specialItemType)) {
                isKeyUsed = true;
            }
        }

        // 2. 확률 굴림 로직 (0.0 ~ 100.0)
        double chance = random.nextDouble() * 100.0;
        String selectedTier = "";
        String setName = "";

        if (isKeyUsed) {
            // [열쇠 사용 시 파격적인 확률] 
            // 실버 대폭 감소(30%), 골드(50%), 프리즘(15%), 신화(5%) 증가
            if (chance < 30.0) { selectedTier = "SILVER"; }
            else if (chance < 80.0) { selectedTier = "GOLD"; }
            else if (chance < 95.0) { selectedTier = "PRISM"; }
            else { selectedTier = "MYTHIC"; }
            
            // 열쇠 1개 소모
            itemInHand.setAmount(itemInHand.getAmount() - 1);
            player.sendMessage("§d§l[공명] §f가라앉은 흉조의 열쇠가 금고의 기운을 강력하게 증폭시킵니다!");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
        } else {
            // [일반 개봉 시 기본 확률]
            // 실버(70%), 골드(24%), 프리즘(5%), 신화(1%)
            if (chance < 70.0) { selectedTier = "SILVER"; }
            else if (chance < 94.0) { selectedTier = "GOLD"; }
            else if (chance < 99.0) { selectedTier = "PRISM"; }
            else { selectedTier = "MYTHIC"; }
        }

        // 3. 결정된 등급에서 세트 종류 무작위 선택
        switch (selectedTier) {
            case "SILVER" -> setName = silverSets[random.nextInt(silverSets.length)];
            case "GOLD" -> setName = goldSets[random.nextInt(goldSets.length)];
            case "PRISM" -> setName = prismSets[random.nextInt(prismSets.length)];
            case "MYTHIC" -> setName = mythicSets[random.nextInt(mythicSets.length)];
        }

        // 4. 부위 무작위 선택
        String part = parts[random.nextInt(parts.length)];

        // 5. 아이템 생성 및 지급
        ItemStack reward = plugin.getSetItemManager().createSetItem(setName, part);
        
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), reward);
        } else {
            player.getInventory().addItem(reward);
        }

        // 6. 시각적 연출 및 마무리
        event.setCancelled(true); // 바닐라 금고/엔더상자 열림 모션 취소
        
        player.playSound(player.getLocation(), Sound.BLOCK_VAULT_ACTIVATE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, block.getLocation().add(0.5, 1.2, 0.5), 30, 0.3, 0.3, 0.3, 0.1);

        String message = "§6§l[흉조 금고] §f어둠 속에서 §e" + setName + " " + part + "§f을(를) 획득했습니다!";
        if (isKeyUsed) {
            message = "§d§l[증폭된 금고] §f열쇠의 힘으로 §e" + setName + " " + part + "§f을(를) 획득했습니다!";
        }
        player.sendMessage(message);

        // 신화 등급 획득 시 서버 전체에 화려한 공지
        if ("MYTHIC".equals(selectedTier)) {
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§5§l[전설] §d" + player.getName() + "§f님이 흉조 금고에서 §5§l신화 장비(" + setName + " " + part + ")§f를 뽑았습니다!!");
            Bukkit.broadcastMessage("");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.5f);
            player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 2);
        }
    }
}