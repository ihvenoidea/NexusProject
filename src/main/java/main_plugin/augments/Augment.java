package main_plugin.augments;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

/**
 * 모든 증강체의 기본 규격을 정의하는 인터페이스입니다.
 * 새로운 증강체를 추가할 때는 반드시 이 인터페이스를 구현(implements)해야 합니다.
 */
public interface Augment {

    /**
     * 시스템에서 증강체를 식별하기 위한 고유 ID를 반환합니다.
     * (예: "ironclad_knight", "toxic_thorn")
     */
    String getId();

    /**
     * 인게임 GUI 및 메시지에 표시될 증강체의 이름을 반환합니다.
     * (예: "철갑 기사", "맹독 가시")
     */
    String getName();

    /**
     * 증강체의 등급(티어)을 반환합니다.
     * (SILVER, GOLD, PRISM, MYTHIC)
     */
    AugmentTier getTier();

    /**
     * 시너지 조합을 판별하기 위한 태그 리스트를 반환합니다.
     * (예: ["IRONCLAD"], ["WARLORD"], ["TOXIC"], ["BOMB"])
     */
    List<String> getTags();

    /**
     * 도감 및 선택 GUI에 표시될 상세 설명 로어(Lore) 리스트를 반환합니다.
     */
    List<String> getDescription();

    /**
     * GUI(선택창, 도감)에서 보여줄 바닐라 아이콘 재질을 반환합니다.
     * (예: Material.SPIDER_EYE, Material.IRON_INGOT)
     */
    Material getIcon();

    /**
     * 특정 이벤트가 발생했을 때 패시브 효과를 발동시키는 핵심 로직입니다.
     * @param player 증강체를 발동시키는 플레이어
     * @param event  트리거가 된 이벤트 (EntityDamageEvent, EntityDeathEvent 등)
     */
    void execute(Player player, Event event);

    /**
     * 증강체의 쿨타임을 설정합니다. (단위: 밀리초)
     * 기본값은 0(쿨타임 없음)이며, 스킬형 패시브일 경우 오버라이드하여 사용합니다.
     * @return 쿨타임 (ms)
     */
    default long getCooldown() {
        return 0;
    }
}