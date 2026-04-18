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

    /**
     * NPC를 우클릭했을 때 실행되는 이벤트입니다.
     */
    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        // 1. 클릭한 NPC가 이 Trait(vanilla_shop)을 가지고 있는지 확인합니다.
        if (event.getNPC() != this.getNPC()) return;

        Player player = event.getClicker();
        
        // 2. NexusCore 인스턴스를 통해 VanillaShopManager를 가져옵니다.
        if (NexusCore.getInstance().getVanillaShopManager() != null) {
            /* * [핵심 수정 사항]
             * 기존: openShop(player, "default") -> market.yml에 "default" 섹션이 없어 작동 안 함.
             * 수정: openCategoryMenu(player) -> market.yml의 'categories' 목록을 보여주는 메인 메뉴 오픈.
             */
            NexusCore.getInstance().getVanillaShopManager().openCategoryMenu(player);
        } else {
            player.sendMessage("§c[시스템] 상점 매니저를 찾을 수 없습니다. 관리자에게 문의하세요.");
        }
    }
}