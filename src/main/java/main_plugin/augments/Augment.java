package main_plugin.augments;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.List;

/**
 * 모든 증강체의 기본 규격을 정의하는 인터페이스입니다.
 */
public interface Augment {

    /**
     * @return 증강체의 고유 ID (예: fire_aura_1)
     */
    String getId();

    /**
     * @return 증강체의 표시 이름
     */
    String getName();

    /**
     * @return 증강체의 등급 (NORMAL, RARE, LEGEND, MYTHIC)
     */
    AugmentTier getTier();

    /**
     * @return 증강체의 설명 문구 리스트
     */
    List<String> getDescription();

    /**
     * 증강체 고유의 태그를 반환합니다. (시너지 계산용)
     * @return 예: ["FIRE", "ATTACK"]
     */
    List<String> getTags();

    /**
     * ItemsAdder 아이템 ID와 연결하기 위한 메서드
     * @return ItemsAdder 내의 namespace:id
     */
    String getItemsAdderId();

    /**
     * 증강체가 실제로 발동될 때 실행될 로직입니다.
     * @param player 증강체를 보유한 플레이어
     * @param event 해당 로직을 트리거한 이벤트 (Damage, Interact 등)
     */
    void execute(Player player, Event event);

    /**
     * 증강체의 쿨타임을 반환합니다 (단위: 밀리초)
     * @return 0이면 쿨타임 없음
     */
    default long getCooldown() {
        return 0;
    }
}