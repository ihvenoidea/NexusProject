package main_plugin.config;

import main_plugin.NexusCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ConfigHandler {

    private final NexusCore plugin;
    
    // 개별 설정 파일 객체들
    private FileConfiguration augmentConfig;
    private File augmentFile;

    public ConfigHandler(NexusCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 모든 설정 파일을 로드합니다. (onEnable 에서 호출)
     */
    public void loadConfigs() {
        // 기본 config.yml 생성 및 로드
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        // augments.yml 로드
        createAugmentConfig();
    }

    /**
     * augments.yml 파일을 생성하고 초기화합니다.
     */
    private void createAugmentConfig() {
        augmentFile = new File(plugin.getDataFolder(), "augments.yml");
        
        if (!augmentFile.exists()) {
            // resources 폴더 내의 augments.yml 을 복사하거나 새로 생성
            plugin.saveResource("augments.yml", false);
        }

        augmentConfig = YamlConfiguration.loadConfiguration(augmentFile);
    }

    /**
     * @return 증강체 설정 객체
     */
    public FileConfiguration getAugmentConfig() {
        if (augmentConfig == null) {
            createAugmentConfig();
        }
        return augmentConfig;
    }

    /**
     * 설정 파일의 변경사항을 디스크에 저장합니다.
     */
    public void saveAugmentConfig() {
        try {
            getAugmentConfig().save(augmentFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "augments.yml 을 저장하는 중 오류 발생!", e);
        }
    }

    /**
     * 모든 파일을 리로드합니다. (관리자 명령용)
     */
    public void reloadConfigs() {
        plugin.reloadConfig();
        augmentConfig = YamlConfiguration.loadConfiguration(augmentFile);
        
        // 리로드 후 AugmentManager 등 다른 시스템에도 반영하는 로직 추가 필요
        plugin.getAugmentManager().loadConfigs(); 
    }
}