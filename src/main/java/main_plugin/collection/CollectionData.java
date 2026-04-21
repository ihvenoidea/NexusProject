package main_plugin.collection;

import java.util.HashSet;
import java.util.Set;

/**
 * 유저의 도감 수집 상태, 총 포인트 및 보상 수령 단계를 관리하는 데이터 클래스입니다.
 */
public class CollectionData {

    private final Set<String> unlockedIds; // 수집된 아이템/증강체 ID 목록
    private int totalPoints;               // 도감 총 점수
    private int rewardTier;                // 수령한 보상 단계 (0, 1, 2, 3)

    /**
     * 신규 유저를 위한 기본 생성자
     */
    public CollectionData() {
        this.unlockedIds = new HashSet<>();
        this.totalPoints = 0;
        this.rewardTier = 0;
    }

    /**
     * DB 로드 및 연동을 위한 생성자
     * @param dataString 쉼표(,)로 구분된 ID 목록
     * @param totalPoints 저장된 총 점수
     * @param rewardTier 저장된 보상 단계
     */
    public CollectionData(String dataString, int totalPoints, int rewardTier) {
        this.unlockedIds = new HashSet<>();
        this.totalPoints = totalPoints;
        this.rewardTier = rewardTier;

        if (dataString != null && !dataString.isEmpty()) {
            String[] split = dataString.split(",");
            for (String id : split) {
                if (!id.trim().isEmpty()) {
                    unlockedIds.add(id.trim());
                }
            }
        }
    }

    /**
     * 새로운 항목을 해금하고 등급에 따른 포인트를 가산합니다.
     * @param id 해금할 고유 ID
     * @param grade 등급 (common, rare, legendary, synergy)
     * @return 성공적으로 새로 해금되었다면 true
     */
    public boolean addUnlock(String id, String grade) {
        if (unlockedIds.contains(id)) return false;

        unlockedIds.add(id);
        
        // 기획안 기반 등급별 포인트 가산
        switch (grade.toLowerCase()) {
            case "common":    this.totalPoints += 1;  break;
            case "rare":      this.totalPoints += 5;  break;
            case "legendary": this.totalPoints += 20; break;
            case "synergy":   this.totalPoints += 50; break;
            default:          break;
        }
        return true;
    }

    /**
     * 특정 항목이 해금되었는지 확인합니다.
     */
    public boolean isUnlocked(String id) {
        return unlockedIds.contains(id);
    }

    /**
     * [컴파일 에러 해결] CollectionGUI와의 호환성을 위한 메서드
     */
    public boolean hasCollected(String id) {
        return isUnlocked(id);
    }

    /**
     * DB 저장을 위해 해금 목록을 문자열 형식으로 변환합니다.
     */
    public String toDataString() {
        if (unlockedIds.isEmpty()) return "";
        return String.join(",", unlockedIds);
    }

    // --- Getter & Setter ---

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public int getRewardTier() {
        return rewardTier;
    }

    public void setRewardTier(int rewardTier) {
        this.rewardTier = rewardTier;
    }

    public Set<String> getUnlockedIds() {
        return new HashSet<>(unlockedIds);
    }
}