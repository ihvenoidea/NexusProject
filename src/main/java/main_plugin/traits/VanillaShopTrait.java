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
        
        // NexusCore를 통해 일반 상점 매니저를 가져옵니다.
        if (NexusCore.getInstance().getVanillaShopManager() != null) {
            /* * [수정 포인트] 
             * VanillaShopManager.java의 openShop 메서드는 (Player, String)을 인자로 받습니다.
             * 여기서 두 번째 인자는 market.yml 등에 정의된 상점의 ID(카테고리)입니다.
             * 기본적으로 "default" 또는 "main" 등을 사용하도록 수정했습니다.
             */
            NexusCore.getInstance().getVanillaShopManager().openShop(player, "default");
        } else {
            player.sendMessage("§c[시스템] 상점 시스템을 불러올 수 없습니다.");
        }
    }
}