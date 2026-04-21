package main_plugin.user;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserData {
    private final UUID uuid;
    private final String name;
    private double money;
    private int points;
    
    // 추가된 필드: 강화 시스템 및 정치 시스템 호환용
    private List<String> augments;
    private double totalTribute;

    // 창고 시스템: 해금된 창고 박스 개수 (기본 1개)
    private int unlockedBoxes;

    // ==========================================
    // [신규] 생활 숙련도 (Life Mastery) 데이터
    // ==========================================
    private String job;  // 직업 코드: NONE, MINER, FARMER, FISHER, LOGGER
    private long jobExp; // 100레벨 요구량이 매우 높으므로 안전하게 long 타입 사용

    public UserData(UUID uuid, String name, double money, int points) {
        this.uuid = uuid;
        this.name = name;
        this.money = money;
        this.points = points;
        this.augments = new ArrayList<>(); // 리스트 초기화 필수
        this.totalTribute = 0.0;
        this.unlockedBoxes = 1; // 기본 창고 1개 지급
        
        // 직업 초기값 설정
        this.job = "NONE";
        this.jobExp = 0L;
    }

    // --- 기본 정보 Getter & Setter ---
    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    
    public double getMoney() { return money; }
    public void setMoney(double money) { this.money = money; }
    
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    // --- 강화 시스템(Augment) 호환 메서드 ---
    public List<String> getAugments() { 
        if (this.augments == null) {
            this.augments = new ArrayList<>();
        }
        return augments; 
    }

    public void setAugments(List<String> augments) {
        this.augments = augments;
    }

    // --- 정치/조공 시스템(Tribute) 호환 메서드 ---
    public double getTotalTribute() { return totalTribute; }
    public void setTotalTribute(double totalTribute) { this.totalTribute = totalTribute; }

    // --- 창고 시스템 메서드 ---
    public int getUnlockedBoxes() { return unlockedBoxes; }
    public void setUnlockedBoxes(int unlockedBoxes) { this.unlockedBoxes = unlockedBoxes; }

    // --- [신규] 생활 숙련도 시스템 메서드 ---
    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }

    public long getJobExp() { return jobExp; }
    public void setJobExp(long jobExp) { this.jobExp = jobExp; }

    // --- 편의 기능을 위한 유틸리티 메서드 ---
    public void addMoney(double amount) { 
        this.money += amount; 
    }

    public void subtractMoney(double amount) { 
        this.money -= amount; 
    }
}