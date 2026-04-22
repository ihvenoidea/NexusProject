package main_plugin.items;

import main_plugin.NexusCore;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class SetItemManager {

    private final NexusCore plugin;
    public static final String SET_NAME_TAG = "nexus_set_name";
    private final NamespacedKey nameKey;

    public SetItemManager(NexusCore plugin) {
        this.plugin = plugin;
        this.nameKey = new NamespacedKey(plugin, SET_NAME_TAG);
    }

    public ItemStack createSetItem(String setName, String part) {
        Material mat = getMaterial(part);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String tier = getFixedTier(setName);
            String color = getTierColor(tier);

            meta.setDisplayName(color + "§l[" + tier + "] §f" + setName + " " + part);

            List<String> lore = new ArrayList<>();
            lore.add("§7아이템 등급: " + color + tier);
            lore.add("");
            
            if (isArmor(part)) {
                lore.add("§e[세트 고유 기능] §7(2부위 이상 착용 시)");
                addArmorLore(setName, lore);
            } else {
                lore.add("§e[무기/도구 고유 기능]");
                addToolWeaponLore(setName, part, lore);
            }
            
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(nameKey, PersistentDataType.STRING, setName);
            item.setItemMeta(meta);
        }
        return item;
    }

    // [추가됨] 총 12개의 세트를 등급별로 분류합니다.
    private String getFixedTier(String setName) {
        return switch (setName) {
            case "견고", "도약", "재생" -> "실버";
            case "풍요", "탐욕", "화염" -> "골드";
            case "신속", "혹한", "환영" -> "프리즘";
            case "권능", "재앙", "불멸" -> "신화";
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

    // [추가됨] 8종 신규 방어구 설명 추가
    private void addArmorLore(String setName, List<String> lore) {
        switch (setName) {
            // --- 실버 ---
            case "견고" -> lore.add("§f- 장비 내구도 보호 (소모 확률 -15%)");
            case "도약" -> lore.add("§f- 상시 점프 강화 II 효과");
            case "재생" -> lore.add("§f- 비전투 시 체력 지속 회복");
            // --- 골드 ---
            case "풍요" -> lore.add("§f- 상시 야간 투시 효과");
            case "탐욕" -> lore.add("§f- 몬스터 처치 시 10% 확률로 DP 획득");
            case "화염" -> lore.add("§f- 상시 화염 저항 (용암 면역)");
            // --- 프리즘 ---
            case "신속" -> lore.add("§f- 상시 이동 속도 I 효과");
            case "혹한" -> lore.add("§f- 피격 시 공격자에게 둔화 III 부여 (3초)");
            case "환영" -> lore.add("§f- 15% 확률로 모든 물리 피해 회피");
            // --- 신화 ---
            case "권능" -> lore.add("§f- 상시 재생 I 효과");
            case "재앙" -> lore.add("§f- 피격 시 5% 확률로 공격자 위치에 벼락 소환");
            case "불멸" -> lore.add("§f- 치명상 시 1회 부활 및 3초 무적 (쿨타임 10분)");
        }
    }

    // [추가됨] 8종 신규 무기/도구 설명 추가 (화염 세트 업그레이드 반영)
    private void addToolWeaponLore(String setName, String part, List<String> lore) {
        switch (setName) {
            // --- 실버 ---
            case "견고": 
                if (part.equals("활")) lore.add("§f- 발사 속도 10% 증가");
                else lore.add("§f- 내구도 소모 15% 감소");
                break;
            case "도약":
                if (part.equals("검")) lore.add("§f- 공중 크리티컬 공격 시 데미지 20% 추가");
                else if (part.equals("활")) lore.add("§f- 자신을 밀어내는 반동 화살 발사");
                else lore.add("§f- 블록 파괴 시 3초간 이동 속도 증가");
                break;
            case "재생":
                if (part.equals("검")) lore.add("§f- 적 처치 시 배고픔 1칸 회복");
                else if (part.equals("활")) lore.add("§f- 명중 시 배고픔 1칸 회복");
                else lore.add("§f- 블록 파괴 시 1% 확률로 체력 1 회복");
                break;

            // --- 골드 ---
            case "풍요": 
                if (part.equals("검")) lore.add("§f- 몬스터 처치 경험치 1.5배");
                else if (part.equals("활")) lore.add("§f- 적중 시 10초간 발광 효과 부여");
                else lore.add("§f- 전천후 자동 정제 (원목, 광물, 모래 등)");
                break;
            case "탐욕":
                if (part.equals("검")) lore.add("§f- 타격 시 2% 확률로 랜덤 광물 드롭");
                else if (part.equals("활")) lore.add("§f- 적 처치 시 경험치 구슬 대량 드롭");
                else lore.add("§f- 광물 파괴 시 획득하는 경험치 2배");
                break;
            case "화염": // 업그레이드 반영!
                if (part.equals("검")) {
                    lore.add("§f- 타격 시 3x3 광역 발화");
                    lore.add("§c- 불타는 적 타격 시 피해량 1.5배 폭딜");
                }
                else if (part.equals("활")) lore.add("§f- 폭발 화염구 발사 (지형 파괴 X)");
                else {
                    lore.add("§f- 네더(지옥)에서 상시 성급함 II 적용");
                    lore.add("§f- 네더 블록 채굴 시 보너스 광물 획득");
                }
                break;

            // --- 프리즘 ---
            case "신속": 
                if (part.equals("검")) lore.add("§f- 둔화의 칼날 (적 2초 둔화)");
                else if (part.equals("활")) lore.add("§f- 적을 쫓는 추적 화살");
                else lore.add("§f- 채광/벌목 시 성급함 II 부여");
                break;
            case "혹한":
                if (part.equals("검")) lore.add("§f- 타격 시 5% 확률로 2초간 완전 빙결");
                else if (part.equals("활")) lore.add("§f- 착탄 지점 주변 3x3 둔화 장판 생성");
                else lore.add("§f- 피격당한 적을 3초간 빙결 상태로 만듦");
                break;
            case "환영":
                if (part.equals("검")) lore.add("§f- 적의 뒤(배후) 공격 시 피해량 1.5배");
                else if (part.equals("활")) lore.add("§f- 화살이 맞은 곳으로 즉시 순간이동");
                else lore.add("§f- Shift + 우클릭 시 5칸 순간이동 (쿨타임 10초)");
                break;

            // --- 신화 ---
            case "권능": 
                if (part.equals("검")) lore.add("§f- 흡혈 (피해량의 4% 회복)");
                else if (part.equals("활")) lore.add("§f- 폭발 화살");
                else lore.add("§f- 3x3 절대 권능 범위 채광/벌목");
                break;
            case "재앙":
                if (part.equals("검")) lore.add("§f- 타격 시 전방 3x3 휩쓸기 광역 피해");
                else if (part.equals("활")) lore.add("§f- 화살 착탄 지점에 벼락 소환");
                else lore.add("§f- 파괴한 블록에 벼락 소환 및 주변 적 타격");
                break;
            case "불멸":
                if (part.equals("검")) lore.add("§f- 내 체력이 30% 이하일 때 피해량 2배 폭증");
                else if (part.equals("활")) lore.add("§f- 내 체력이 낮을수록 화살 데미지 비례 증가");
                else lore.add("§f- 절대 파괴되지 않는 무한 내구도");
                break;
        }
    }

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
            default -> Material.PAPER; 
        };
    }

    public NamespacedKey getNameKey() { return nameKey; }
}