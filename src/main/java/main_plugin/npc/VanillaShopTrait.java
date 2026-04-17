package main_plugin.npc;

import main_plugin.NexusCore;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class VanillaShopTrait extends Trait {
    public VanillaShopTrait() {
        super("vanilla_shop");
    }

    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        // 클릭된 NPC가 이 특성을 가지고 있는지 확인
        if (event.getNPC() != this.getNPC()) return;
        
        // NexusCore 플러그인 인스턴스 가져오기
        NexusCore plugin = JavaPlugin.getPlugin(NexusCore.class);
        
        // [수정됨] openShop 대신 카테고리 메뉴(메인 화면)를 먼저 엽니다.
        Player player = event.getClicker();
        plugin.getVanillaShopManager().openCategoryMenu(player);
    }
}