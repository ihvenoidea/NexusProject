package main_plugin.collection;

import main_plugin.NexusCore;
import main_plugin.augments.Augment;
import main_plugin.user.UserData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CollectionManager {

    private final NexusCore plugin;
    private final Map<UUID, CollectionData> collectionDataMap = new HashMap<>();

    public CollectionManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    public void loadUserData(UUID uuid, String collectionString, int totalPoints, int level) {
        collectionDataMap.put(uuid, new CollectionData(collectionString, totalPoints, level));
    }

    public CollectionData getCollectionData(UUID uuid) {
        return collectionDataMap.getOrDefault(uuid, new CollectionData("", 0, 1));
    }

    // ==========================================
    // [ 핵심 버그 수정 ] 도감 데이터 실제 저장 및 비동기(DB 렉 제거) 로직 적용
    // ==========================================
    public void registerEntry(Player player, String id, String grade, String displayName) {
        CollectionData data = getCollectionData(player.getUniqueId());
        if (data == null) return;

        // addUnlock 내부에서 중복 검사를 하고, 처음 획득한 거면 점수를 올려주고 true를 반환합니다.
        if (data.addUnlock(id, grade)) {
            
            // [수정됨] DB 저장 시 서버가 멈칫하며 클릭이 씹히는 현상을 막기 위해 비동기(백그라운드)로 돌립니다.
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
                    plugin.getUserManager().saveUserData(user); // DB에 덮어쓰기
                });
            });

            // 플레이어에게 알림 및 효과음
            player.sendMessage("§a§l[도감] §f새로운 항목 §e" + displayName + " §f수집 완료!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
            plugin.getLogger().info("[도감] " + player.getName() + "님이 " + displayName + " 수집!");
        }
    }

    // ==========================================
    // [ UI ] 증강체 콜렉션 GUI 열기
    // ==========================================
    public void openAugmentCollection(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8[ NEXUS ] 증강체 콜렉션");
        List<String> playerAugments = plugin.getUserManager().getUser(player.getUniqueId())
                .map(UserData::getAugments).orElse(new ArrayList<>());

        fillBackground(gui);

        setupSynergySection(gui, "IRONCLAD", "§f🛡 §l철갑 시너지", 10, playerAugments, Arrays.asList(
            "§7생존력과 방어력에 특화된 시너지입니다.", "", "§e§l[ 3세트 달성 효과 ]", "§a상시 받는 피해량 10% 감소"
        ));
        
        setupSynergySection(gui, "WARLORD", "§f⚔ §l패왕 시너지", 19, playerAugments, Arrays.asList(
            "§7공격력과 적 처치에 특화된 시너지입니다.", "", "§e§l[ 3세트 달성 효과 ]", "§c상시 가하는 피해량 10% 증가"
        ));
        
        setupSynergySection(gui, "TOXIC", "§f🧪 §l맹독 시너지", 28, playerAugments, Arrays.asList(
            "§7디버프 부여와 하이리스크에 특화된 시너지입니다.", "", "§e§l[ 3세트 달성 효과 ]", "§2공격 시 5% 확률로 적에게 위더 효과"
        ));
        
        setupSynergySection(gui, "BOMB", "§f💣 §l폭탄 시너지", 37, playerAugments, Arrays.asList(
            "§7광역 폭발과 변수 창출에 특화된 시너지입니다.", "", "§e§l[ 3세트 달성 효과 ]", "§c자신이 일으키는 폭발 위력 2배 증폭"
        ));

        setupSummaryIcon(gui, playerAugments);

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.2f);
    }

    private void setupSynergySection(Inventory gui, String tag, String title, int startSlot, List<String> playerOwned, List<String> lore) {
        ItemStack titleItem = new ItemStack(Material.PAPER);
        ItemMeta tMeta = titleItem.getItemMeta();
        tMeta.setDisplayName(title);
        tMeta.setLore(lore);
        titleItem.setItemMeta(tMeta);
        gui.setItem(startSlot - 1, titleItem);

        int currentSlot = startSlot;
        for (Augment aug : plugin.getAugmentManager().getRegisteredAugments().values()) {
            if (aug.getTags().contains(tag)) {
                boolean owned = playerOwned.contains(aug.getId());
                gui.setItem(currentSlot++, createAugmentIcon(aug, owned));
            }
        }
    }

    private ItemStack createAugmentIcon(Augment aug, boolean owned) {
        if (owned) {
            ItemStack item = new ItemStack(aug.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(aug.getTier().getColor() + "§l" + aug.getName());
            List<String> lore = new ArrayList<>();
            lore.add("§8" + aug.getTier().getDisplayName() + " 등급");
            lore.add("");
            lore.addAll(aug.getDescription());
            lore.add("");
            lore.add("§a✔ 수집 완료");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        } else {
            ItemStack item = new ItemStack(Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§7???");
            List<String> lore = new ArrayList<>();
            lore.add("§c미해금 증강체");
            lore.add("§8획득 시 정보가 공개됩니다.");
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }
    }

    private void setupSummaryIcon(Inventory gui, List<String> playerOwned) {
        int total = plugin.getAugmentManager().getRegisteredAugments().size();
        int collected = playerOwned.size();
        double percent = (total == 0) ? 0 : ((double) collected / total) * 100;

        ItemStack summary = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = summary.getItemMeta();
        meta.setDisplayName("§e§l전체 수집 현황");
        List<String> lore = new ArrayList<>();
        lore.add("§f진행률: §b" + String.format("%.1f", percent) + "%");
        lore.add(createProgressBar(collected, total));
        lore.add("");
        lore.add("§7전체 증강체: §f" + total + "개");
        lore.add("§7수집한 증강체: §e" + collected + "개");
        meta.setLore(lore);
        summary.setItemMeta(meta);
        gui.setItem(49, summary);
    }

    private String createProgressBar(int current, int total) {
        if (total == 0) return "§8[§7□□□□□□□□□□§8]";
        StringBuilder bar = new StringBuilder("§8[");
        int progress = (int) (((double) current / total) * 10);
        for (int i = 0; i < 10; i++) {
            if (i < progress) bar.append("§a■");
            else bar.append("§7□");
        }
        bar.append("§8]");
        return bar.toString();
    }

    private void fillBackground(Inventory gui) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        for (int i = 0; i < 54; i++) gui.setItem(i, pane);
    }
}