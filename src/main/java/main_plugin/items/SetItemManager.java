package main_plugin.items;

import main_plugin.NexusCore;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 세트 아이템(장비, 도구, 무기)의 생성 및 데이터 관리를 담당합니다.
 * 증강체와 완전히 분리되어 순수하게 아이템 본연의 기능만 표시합니다.
 */
public class SetItemManager {

    private final NexusCore plugin;
    public static final String SET_NAME_TAG = "nexus_set_name";
    private final NamespacedKey nameKey;

    public SetItemManager(NexusCore plugin) {
        this.plugin = plugin;
        this.nameKey = new NamespacedKey(plugin, SET_NAME_TAG);
    }

    public ItemStack createSetItem(String setName, String part) {
        Material mat = getMaterial(part); // 부위에 맞는 재질 가져오기 (갑옷, 검, 활 등)
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String tier = getFixedTier(setName);
            String color = getTierColor(tier);

            // 아이템 이름 설정 (예: §d§l[신화] §f황금 검)
            meta.setDisplayName(color + "§l[" + tier + "] §f" + setName + " " + part);

            List<String> lore = new ArrayList<>();
            lore.add("§7아이템 등급: " + color + tier);
            lore.add("");
            
            // 갑옷(방어구)인지, 도구/무기인지에 따라 로어(기능 설명) 분리
            if (isArmor(part)) {
                lore.add("§e[세트 고유 기능] §7(2부위 이상 착용 시)");
                addArmorLore(setName, lore);
            } else {
                lore.add("§e[무기/도구 고유 기능]");
                addToolWeaponLore(setName, part, lore);
            }
            
            meta.setLore(lore);
            // 시스템(리스너, 스크립트)이 인식할 수 있도록 PDC에 세트 이름 각인
            meta.getPersistentDataContainer().set(nameKey, PersistentDataType.STRING, setName);
            
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 세트 이름에 매칭되는 등급 반환
     */
    private String getFixedTier(String setName) {
        return switch (setName) {
            case "신속" -> "실버";
            case "방어" -> "골드";
            case "격류" -> "프리즘";
            case "황금" -> "신화";
            default -> "일반";
        };
    }

    private String getTierColor(String tier) {
        return switch (tier) {
            case "실버" -> "§7";
            case "골드" -> "§e";
            case "프리즘" -> "§b";
            case "신화" -> "§d";
            default -> "§f";
        };
    }

    private boolean isArmor(String part) {
        return part.equals("투구") || part.equals("갑옷") || part.equals("각반") || part.equals("장화");
    }

    /**
     * 방어구(갑옷) 전용 유틸리티 효과 로어
     */
    private void addArmorLore(String setName, List<String> lore) {
        switch (setName) {
            case "신속" -> lore.add("§f- 장비 내구도 보호 (소모 확률 -15%)");
            case "방어" -> lore.add("§f- 상시 야간 투시 효과");
            case "격류" -> lore.add("§f- 이동 속도 I 부여");
            case "황금" -> lore.add("§f- 상시 재생 I 효과");
        }
    }

    /**
     * 무기 및 도구 전용 고유 기능 로어
     */
    private void addToolWeaponLore(String setName, String part, List<String> lore) {
        switch (setName) {
            case "신속": // 실버 등급 기능
                if (part.equals("활")) lore.add("§f- 발사 속도 10% 증가");
                else lore.add("§f- 내구도 소모 15% 감소");
                break;
            case "방어": // 골드 등급 기능
                if (part.equals("검")) lore.add("§f- 경험치 보너스 (1.5배)");
                else if (part.equals("활")) lore.add("§f- 전리품 자동 수집");
                else lore.add("§f- 자동 정제 기능 (화로/벌목 등)");
                break;
            case "격류": // 프리즘 등급 기능
                if (part.equals("검")) lore.add("§f- 둔화의 칼날 (적 2초 둔화)");
                else if (part.equals("활")) lore.add("§f- 추적 화살");
                else lore.add("§f- 범위 채광 및 기능 확장");
                break;
            case "황금": // 신화 등급 기능
                if (part.equals("검")) lore.add("§f- 흡혈 (피해량의 4% 회복)");
                else if (part.equals("활")) lore.add("§f- 폭발 화살");
                else lore.add("§f- 3x3 절대 권능 범위 채광/벌목");
                break;
        }
    }

    /**
     * 부위명에 맞는 기본 마인크래프트 재질 반환
     */
    private Material getMaterial(String part) {
        return switch (part) {
            case "투구" -> Material.NETHERITE_HELMET;
            case "갑옷" -> Material.NETHERITE_CHESTPLATE;
            case "각반" -> Material.NETHERITE_LEGGINGS;
            case "장화" -> Material.NETHERITE_BOOTS;
            case "곡괭이" -> Material.NETHERITE_PICKAXE;
            case "도끼" -> Material.NETHERITE_AXE;
            case "삽" -> Material.NETHERITE_SHOVEL;
            case "검" -> Material.NETHERITE_SWORD;
            case "활" -> Material.BOW;
            default -> Material.PAPER; // 오타 입력 시 처리
        };
    }

    public NamespacedKey getNameKey() { return nameKey; }
}