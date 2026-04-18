package main_plugin.augments;

import org.bukkit.ChatColor;

/**
 * 증강체의 등급(티어)을 정의하는 Enum 클래스입니다.
 * 상점 상자 등급 및 도감 시스템과 동일한 체계를 사용합니다.
 */
public enum AugmentTier {
    SILVER("실버", ChatColor.GRAY),       // §7
    GOLD("골드", ChatColor.YELLOW),       // §e
    PRISM("프리즘", ChatColor.AQUA),      // §b
    MYTHIC("신화", ChatColor.LIGHT_PURPLE); // §d

    private final String displayName;
    private final ChatColor color;

    AugmentTier(String displayName, ChatColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    /**
     * @return 등급의 한글 표시 이름 (예: "실버")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return 등급에 배정된 색상 코드
     */
    public ChatColor getColor() {
        return color;
    }

    /**
     * 등급 이름을 포함한 서식화된 문자열을 반환합니다.
     * @return 예: "§7[실버]"
     */
    public String getFormattedName() {
        return color + "[" + displayName + "]";
    }
}