package main_plugin.npc;

import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.Bukkit;

public class ShopTrait extends Trait {
    public ShopTrait() {
        super("shop_npc");
    }

    // NPC를 우클릭했을 때 발생하는 이벤트
    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        // 이 NPC가 이 특성을 가지고 있는지 확인
        if (event.getNPC() != this.getNPC()) return;

        // 클릭한 플레이어에게 "/포인트" 명령어를 강제로 실행하게 함
        // (직접 PointShopManager의 메서드를 호출해도 되지만, 이게 가장 간단하고 확실합니다)
        event.getClicker().performCommand("포인트");
    }
}