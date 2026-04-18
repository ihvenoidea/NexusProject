package main_plugin.items;

import dev.lone.itemsadder.api.CustomStack;
import main_plugin.NexusCore;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * DP 상점에서 구매한 '등급별 무기 및 도구 상자'를 
 * 우클릭했을 때의 이벤트를 처리합니다.
 */
public class WeaponBoxListener implements Listener {

    private final NexusCore plugin;
    private final Random random = new Random();

    public WeaponBoxListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBoxUse(PlayerInteractEvent event) {
        // 1. 주 사용 손(Main Hand)의 우클릭만 감지 (중복 실행 방지)
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) return;

        // 2. ItemsAdder API를 이용해 커스텀 상자인지 판별
        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null) return; // IA 아이템이 아니면 무시

        String iaId = customStack.getNamespacedID();
        String setName = null;

        // 3. ItemsAdder ID를 기반으로 확정 지급할 세트 이름(등급) 결정
        switch (iaId) {
            case "n_items:silver_weapon_box" -> setName = "신속"; // 실버 등급
            case "n_items:gold_weapon_box" -> setName = "방어";  // 골드 등급
            case "n_items:prism_weapon_box" -> setName = "격류"; // 프리즘 등급
            case "n_items:mythic_weapon_box" -> setName = "황금"; // 신화 등급
        }

        // 4. 무기 상자가 맞다면 아이템 지급 로직 실행
        if (setName != null) {
            event.setCancelled(true); // 블록 설치 등 바닐라 이벤트 취소
            
            // 상자 1개 소모
            item.setAmount(item.getAmount() - 1);

            // 부위만 랜덤으로 결정 (등급은 상자 등급으로 고정됨)
            String[] parts = {"검", "활", "곡괭이", "도끼", "삽"};
            String part = parts[random.nextInt(parts.length)];

            // 아이템 생성 및 인벤토리에 지급
            ItemStack reward = plugin.getSetItemManager().createSetItem(setName, part);
            player.getInventory().addItem(reward);

            // 화려한 연출 및 메시지 출력
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

            player.sendMessage("§6§l[상자 오픈] §f" + customStack.getDisplayName() + "§f에서 §e" + part + "§f을(를) 획득했습니다!");
        }
    }
}