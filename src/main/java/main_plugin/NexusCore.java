package main_plugin;

import main_plugin.augments.AugmentManager;
import main_plugin.commands.MoneyCommand;
import main_plugin.commands.NexusCommand;
import main_plugin.commands.PointCommand;
import main_plugin.database.DatabaseManager;
import main_plugin.economy.NexusEconomy;
import main_plugin.gui.PointShopManager;
import main_plugin.gui.VanillaShopManager;
import main_plugin.politics.SiegeManager;
import main_plugin.user.PlayerListener;
import main_plugin.user.UserManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class NexusCore extends JavaPlugin {

    private static NexusCore instance;
    private static Economy econ = null;

    private DatabaseManager databaseManager;
    private UserManager userManager;
    private VanillaShopManager vanillaShopManager;
    private PointShopManager pointShopManager;
    
    // 추가: 다른 시스템 매니저들
    private AugmentManager augmentManager;
    private SiegeManager siegeManager;

    private File marketFile;
    private FileConfiguration marketConfig;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 설정 파일 로드
        saveDefaultConfig();
        createMarketConfig();

        // 2. DB 연결
        this.databaseManager = new DatabaseManager(this);
        databaseManager.connect(
            getConfig().getString("database.host", "localhost"),
            getConfig().getInt("database.port", 3306),
            getConfig().getString("database.name", "minecraft_server"),
            getConfig().getString("database.user", "root"),
            getConfig().getString("database.password", "")
        );

        // 3. Vault 연동
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            setupEconomy();
        }

        // 4. 매니저들 초기화 (순서 주의)
        this.userManager = new UserManager(this);
        this.augmentManager = new AugmentManager(this);
        this.siegeManager = new SiegeManager(this);
        this.vanillaShopManager = new VanillaShopManager(this);
        this.pointShopManager = new PointShopManager(this);

        // 5. 명령어 및 리스너 등록
        registerCommands();
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("✔ NexusProject 시스템이 완전히 가동되었습니다.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.close();
        getLogger().info("❌ NexusProject가 종료되었습니다.");
    }

    private void registerCommands() {
        getCommand("money").setExecutor(new MoneyCommand(this));
        getCommand("포인트").setExecutor(new PointCommand(this));
        getCommand("nexus").setExecutor(new NexusCommand(this));
    }

    private void setupEconomy() {
        getServer().getServicesManager().register(Economy.class, new NexusEconomy(this), this, ServicePriority.Highest);
        econ = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
    }

    // --- 설정 관리 메서드 ---

    private void createMarketConfig() {
        marketFile = new File(getDataFolder(), "market.yml");
        if (!marketFile.exists()) {
            saveResource("market.yml", false);
        }
        marketConfig = YamlConfiguration.loadConfiguration(marketFile);
    }

    public FileConfiguration getMarketConfig() { return marketConfig; }

    public void reloadMarketConfig() {
        marketFile = new File(getDataFolder(), "market.yml");
        marketConfig = YamlConfiguration.loadConfiguration(marketFile);
        reloadConfig();
        // 리로드 시 매니저들에게도 알림이 필요하다면 여기서 호출
        if (augmentManager != null) augmentManager.loadConfigs();
        getLogger().info("⚙ 모든 설정 파일이 리로드되었습니다.");
    }

    // --- Getter 메서드 (빌드 에러 해결 포인트) ---

    public static NexusCore getInstance() { return instance; }
    public static Economy getEconomy() { return econ; }
    
    // 타 클래스에서 getDbManager()와 getDatabaseManager() 둘 다 대응
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public DatabaseManager getDbManager() { return databaseManager; } 

    public UserManager getUserManager() { return userManager; }
    public VanillaShopManager getVanillaShopManager() { return vanillaShopManager; }
    public PointShopManager getPointShopManager() { return pointShopManager; }
    
    // 리턴 타입을 Object에서 실제 클래스로 변경
    public AugmentManager getAugmentManager() { return augmentManager; }
    public SiegeManager getSiegeManager() { return siegeManager; }
}