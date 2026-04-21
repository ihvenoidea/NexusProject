package main_plugin.traits;

import main_plugin.NexusCore;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class SetItemExchangeTrait extends Trait {

    public SetItemExchangeTrait() {
        super("set_exchange");
    }

    // [중요] 우선순위를 HIGHEST로 설정하여 방어구 착용보다 먼저 계산하게 합니다.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRightClick(NPCRightClickEvent event) {
        if (event.getNPC() != this.getNPC()) return;

        Player player = event.getClicker();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // 1. 아이템 체크
        if (itemInHand == null || itemInHand.getType().isAir()) {
            player.sendMessage("§c[!] 교환할 넥서스 세트 장비를 손에 들고 우클릭해주세요.");
            return;
        }

        ItemMeta meta = itemInHand.getItemMeta();
        if (meta == null) return;

        NexusCore plugin = NexusCore.getInstance();
        NamespacedKey nameKey = plugin.getSetItemManager().getNameKey();

        // 2. 넥서스 세트 장비인지 확인
        if (!meta.getPersistentDataContainer().has(nameKey, PersistentDataType.STRING)) {
            return; // 일반 아이템이면 무시 (평소처럼 입어지게 둠)
        }

        // ==============================================================
        // [버그 해결 핵심] 방어구가 자동으로 입혀지는 것을 방지
        // ==============================================================
        // 넥서스 장비라면 일단 NPC 클릭 시 방어구 착용 이벤트를 차단해야 합니다.
        // Citizens의 NPCRightClickEvent에서 처리가 안 될 경우를 대비해 
        // 웅크린 상태(Shift)에서만 교환이 되도록 가이드하는 것이 가장 깔끔합니다.
        if (!player.isSneaking()) {
            player.sendMessage("§e[!] 장비를 입지 않고 교환하려면 §f§l[Shift + 우클릭]§e 하세요.");
            return;
        }

        // 3. 내구도 체크
        if (meta instanceof Damageable damageable) {
            if (damageable.getDamage() > 0) {
                player.sendMessage("§c[!] 내구도가 닳은 장비는 교환할 수 없습니다. (수리 후 가져오세요)");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }

        // 4. 등급별 보상 설정
        String setName = meta.getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        int dpReward = 0;
        String tierName = "";

        if (setName != null) {
            switch (setName) {
                case "견고", "도약", "재생": 
                    dpReward = 100; tierName = "§7[실버]"; break;
                case "풍요", "탐욕", "화염": 
                    dpReward = 500; tierName = "§e[골드]"; break;
                case "신속", "혹한", "환영": 
                    dpReward = 2000; tierName = "§b[프리즘]"; break;
                case "권능", "재앙", "불멸": 
                    dpReward = 5000; tierName = "§d[신화]"; break;
            }
        }

        if (dpReward == 0) return;

        // 5. 아이템 회수 및 포인트 지급
        itemInHand.setAmount(itemInHand.getAmount() - 1);
        
        final int finalReward = dpReward;
        plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
            user.setPoints(user.getPoints() + finalReward);
            plugin.getUserManager().saveUserData(user);
        });

        // 6. 피드백
        player.sendMessage("§a§l[장비 교환] " + tierName + " §f장비를 반납하고 §b" + String.format("%,d DP", dpReward) + "§f를 얻었습니다!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }
}