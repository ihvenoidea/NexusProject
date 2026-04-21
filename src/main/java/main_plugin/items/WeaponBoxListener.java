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

public class WeaponBoxListener implements Listener {

    private final NexusCore plugin;
    private final Random random = new Random();

    public WeaponBoxListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBoxUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) return;

        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null) return;

        String iaId = customStack.getNamespacedID();
        String setName = null;

        // [업데이트] 신규 12종 세트 풀 (각 등급당 3개)
        String[] silverSets = {"견고", "도약", "재생"};
        String[] goldSets = {"풍요", "탐욕", "화염"};
        String[] prismSets = {"신속", "혹한", "환영"};
        String[] mythicSets = {"권능", "재앙", "불멸"};

        // 리뉴얼된 세트 확률 매칭
        switch (iaId) {
            case "n_items:silver_weapon_box" -> setName = silverSets[random.nextInt(silverSets.length)]; // 실버
            case "n_items:gold_weapon_box" -> setName = goldSets[random.nextInt(goldSets.length)];  // 골드
            case "n_items:prism_weapon_box" -> setName = prismSets[random.nextInt(prismSets.length)]; // 프리즘
            case "n_items:mythic_weapon_box" -> setName = mythicSets[random.nextInt(mythicSets.length)]; // 신화
        }

        if (setName != null) {
            event.setCancelled(true);
            item.setAmount(item.getAmount() - 1);

            String[] parts = {"검", "활", "곡괭이", "도끼", "삽"};
            String part = parts[random.nextInt(parts.length)];

            ItemStack reward = plugin.getSetItemManager().createSetItem(setName, part);
            player.getInventory().addItem(reward);

            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

            // [업데이트] 어떤 세트가 나왔는지 정확히 메시지에 출력해줍니다!
            player.sendMessage("§6§l[상자 오픈] §f" + customStack.getDisplayName() + "§f에서 §e" + setName + " " + part + "§f을(를) 획득했습니다!");
        }
    }
}