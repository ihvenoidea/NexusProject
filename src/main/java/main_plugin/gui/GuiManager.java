package main_plugin.gui;

import main_plugin.NexusCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class GuiManager implements Listener {

    private final NexusCore plugin;
    private final String menuTitle = "§8[ Nexus Codex ]";

    public GuiManager(NexusCore plugin) {
        this.plugin = plugin;
        // 이벤트 리스너 등록
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 메인 도감 메뉴를 엽니다.
     */
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(menuTitle));

        // 배경 유리판 설치 (장식)
        ItemStack pane = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }

        // 도감 아이콘 (책)
        inv.setItem(11, createItem(Material.BOOK, "§e§l증강체 도감", "§7수집한 증강체 목록을 확인합니다."));
        
        // 조공 현황 아이콘 (에메랄드)
        inv.setItem(13, createItem(Material.EMERALD, "§a§l조공 현황", "§7현재 성주에게 바친 조공액을 확인합니다."));
        
        // 닫기 버튼 (배리어)
        inv.setItem(15, createItem(Material.BARRIER, "§c§l닫기"));

        player.openInventory(inv);
    }

    /**
     * GUI 클릭 이벤트 처리
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 우리가 만든 GUI 타이틀인지 확인
        if (!event.getView().title().equals(Component.text(menuTitle))) return;

        event.setCancelled(true); // 아이템 꺼내기 방지

        if (event.getCurrentItem() == null) return;
        Player player = (Player) event.getWhoClicked();
        Material type = event.getCurrentItem().getType();

        switch (type) {
            case BOOK -> {
                player.sendMessage(Component.text("도감 페이지를 불러오는 중입니다...", NamedTextColor.YELLOW));
                // 여기서 도감 상세 페이지 오픈 로직 호출
            }
            case EMERALD -> {
                player.sendMessage(Component.text("조공 현황창을 엽니다.", NamedTextColor.GREEN));
            }
            case BARRIER -> player.closeInventory();
        }
    }

    /**
     * 아이템 생성을 도와주는 유틸리티 메서드
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            if (lore.length > 0) {
                meta.lore(Collections.singletonList(Component.text(lore[0]))); // 간단하게 한 줄만 예시
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}