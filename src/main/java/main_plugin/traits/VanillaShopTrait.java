package main_plugin.traits;

import main_plugin.NexusCore;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class VanillaShopTrait extends Trait {

    public VanillaShopTrait() {
        super("vanilla_shop");
    }

    // NPC를 우클릭했을 때 실행되는 이벤트
    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        // 클릭한 NPC가 이 Trait을 가지고 있는지 확인
        if (event.getNPC() != this.getNPC()) return;

        Player player = event.getClicker();
        
        // NexusCore를 통해 일반 상점 GUI를 엽니다.
        if (NexusCore.getInstance().getVanillaShopManager() != null) {
            NexusCore.getInstance().getVanillaShopManager().openShop(player);
        } else {
            player.sendMessage("§c상점 시스템을 불러올 수 없습니다.");
        }
    }

    // NPC가 스폰될 때 호출 (필요 시)
    @Override
    public void onSpawn() {
        // getNPC().getEntity().setCustomNameVisible(true);
    }
}