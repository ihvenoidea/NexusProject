package main_plugin.user;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 플레이어의 모든 데이터를 담는 모델 객체입니다.
 */
public class NexusUser {

    private final UUID uuid;
    private String nickname;
    private String discordId;
    
    // 경제 및 통계 데이터
    private int discordPoints;
    private long totalTribute;
    private int dailyStreak;
    private boolean isVerified;

    // 보유 중인 증강체 ID 리스트
    private final List<String> augments;

    /**
     * 새로운 유저 객체를 생성합니다. (DB에서 불러올 때 사용)
     */
    public NexusUser(UUID uuid, String nickname) {
        this.uuid = uuid;
        this.nickname = nickname;
        this.augments = new ArrayList<>();
        this.discordPoints = 0;
        this.totalTribute = 0;
        this.dailyStreak = 0;
        this.isVerified = false;
    }

    // --- Getter & Setter ---

    public UUID getUuid() {
        return uuid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public int getDiscordPoints() {
        return discordPoints;
    }

    public void setDiscordPoints(int discordPoints) {
        this.discordPoints = discordPoints;
    }

    public long getTotalTribute() {
        return totalTribute;
    }

    public void setTotalTribute(long totalTribute) {
        this.totalTribute = totalTribute;
    }

    public int getDailyStreak() {
        return dailyStreak;
    }

    public void setDailyStreak(int dailyStreak) {
        this.dailyStreak = dailyStreak;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public List<String> getAugments() {
        return augments;
    }

    /**
     * 특정 증강체를 보유하고 있는지 확인합니다.
     */
    public boolean hasAugment(String augmentId) {
        return augments.contains(augmentId);
    }

    /**
     * 증강체를 추가합니다.
     */
    public void addAugment(String augmentId) {
        if (!augments.contains(augmentId)) {
            augments.add(augmentId);
        }
    }

    /**
     * 증강체를 제거합니다.
     */
    public void removeAugment(String augmentId) {
        augments.remove(augmentId);
    }
}