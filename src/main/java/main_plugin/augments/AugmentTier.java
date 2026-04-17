package main_plugin.augments;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * 증강체의 등급을 정의하는 Enum 클래스입니다.
 */
public enum AugmentTier {
    NORMAL("일반", NamedTextColor.WHITE),
    RARE("희귀", NamedTextColor.BLUE),
    LEGEND("전설", NamedTextColor.GOLD),
    MYTHIC("신화", NamedTextColor.RED);

    private final String displayName;
    private final TextColor color;

    AugmentTier(String displayName, TextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    /**
     * @return 등급의 한글 표시 이름
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return 등급에 배정된 색상 (Adventure API)
     */
    public TextColor getColor() {
        return color;
    }

    /**
     * 등급 이름을 포함한 서식화된 문자열을 반환합니다.
     * @return 예: "[전설]" (금색)
     */
    public String getFormattedName() {
        return "[" + displayName + "]";
    }
}