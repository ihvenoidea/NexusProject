package main_plugin.gui;

import dev.lone.itemsadder.api.CustomStack;
import main_plugin.NexusCore;
import main_plugin.augments.Augment;
import main_plugin.augments.AugmentTier;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 유저가 증강체 티켓을 우클릭했을 때 감지하고,
 * 티켓을 소모하여 증강체 선택 GUI를 열어주는 리스너입니다.
 */
public class TicketListener implements Listener {

    private final NexusCore plugin;

    public TicketListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTicketUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType().isAir()) return;

        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null) return;

        String iaId = customStack.getNamespacedID();
        AugmentTier tier = getTierFromTicket(iaId);

        if (tier != null) {
            event.setCancelled(true);

            // [에러 해결 핵심] 해당 티어의 증강체 목록을 랜덤하게 3개 뽑아서 넘깁니다.
            List<Augment> allAugments = plugin.getAugmentManager().getRegisteredAugments().values().stream()
                    .filter(a -> a.getTier() == tier)
                    .collect(Collectors.toList());

            if (allAugments.isEmpty()) {
                player.sendMessage("§c[!] 해당 등급의 증강체가 등록되어 있지 않습니다.");
                return;
            }

            Collections.shuffle(allAugments);
            List<Augment> options = allAugments.subList(0, Math.min(3, allAugments.size()));

            // 티켓 1장 소모
            item.setAmount(item.getAmount() - 1);

            // 효과음
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

            // 수정된 생성자 (plugin, tier, options) 형식에 맞게 호출
            AugmentSelectorGUI gui = new AugmentSelectorGUI(plugin, tier, options);
            player.openInventory(gui.getInventory());
        }
    }

    private AugmentTier getTierFromTicket(String iaId) {
        return switch (iaId) {
            case "n_items:silver_augment_ticket" -> AugmentTier.SILVER;
            case "n_items:gold_augment_ticket" -> AugmentTier.GOLD;
            case "n_items:prism_augment_ticket" -> AugmentTier.PRISM;
            case "n_items:mythic_augment_ticket" -> AugmentTier.MYTHIC;
            default -> null;
        };
    }
}