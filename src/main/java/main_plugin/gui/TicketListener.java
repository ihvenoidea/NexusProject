package main_plugin.gui;

import dev.lone.itemsadder.api.CustomStack;
import main_plugin.NexusCore;
import main_plugin.augments.AugmentTier;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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
        // 1. 주 사용 손(Main Hand)의 우클릭만 감지 (중복 실행 방지)
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType().isAir()) return;

        // 2. ItemsAdder API를 사용하여 커스텀 아이템 판별
        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null) return;

        String iaId = customStack.getNamespacedID();
        AugmentTier tier = getTierFromTicket(iaId);

        // 3. 티켓이 맞다면 이벤트 취소(블록 설치 방지) 후 로직 실행
        if (tier != null) {
            event.setCancelled(true);

            // 4. 티켓 1장 소모
            item.setAmount(item.getAmount() - 1);

            // 5. 화려한 사운드 재생
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1.0f, 1.2f);

            // 6. 패키지가 수정되어 이제 AugmentSelectorGUI를 정상적으로 엽니다!
            new AugmentSelectorGUI(plugin, player, tier).open();
        }
    }

    /**
     * ItemsAdder ID를 기반으로 해당 티켓의 증강체 티어(등급)를 반환합니다.
     */
    private AugmentTier getTierFromTicket(String iaId) {
        return switch (iaId) {
            case "n_items:silver_augment_ticket" -> AugmentTier.SILVER;
            case "n_items:gold_augment_ticket" -> AugmentTier.GOLD;
            case "n_items:prism_augment_ticket" -> AugmentTier.PRISM;
            case "n_items:mythic_augment_ticket" -> AugmentTier.MYTHIC;
            default -> null; // 티켓이 아니면 null 반환
        };
    }
}