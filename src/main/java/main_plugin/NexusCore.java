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
import main_plugin.traits.VanillaShopTrait; // [수정] 올바른 경로로 통일

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
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

        // 4. 매니저 초기화 (순서 주의: DB와 유저 매니저가 우선)
        this.userManager = new UserManager(this);
        this.augmentManager = new AugmentManager(this);
        this.siegeManager = new SiegeManager(this);
        this.vanillaShopManager = new VanillaShopManager(this);
        this.pointShopManager = new PointShopManager(this);

        // 5. Citizens Trait 등록
        if (getServer().getPluginManager().getPlugin("Citizens") != null) {
            // [수정] traits 패키지의 클래스를 사용하여 등록
            CitizensAPI.getTraitFactory().registerTrait(
                TraitInfo.create(VanillaShopTrait.class).withName("vanilla_shop")
            );
            getLogger().info("✔ Citizens Trait 'vanilla_shop' 등록 완료!");
        }

        // 6. 명령어 및 리스너 등록
        registerCommands();
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        // AugmentManager도 이벤트를 처리하므로 등록 필요
        getServer().getPluginManager().registerEvents(augmentManager, this);

        getLogger().info("✔ NexusProject 시스템이 정상적으로 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.close();
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

    private void createMarketConfig() {
        marketFile = new File(getDataFolder(), "market.yml");
        if (!marketFile.exists()) saveResource("market.yml", false);
        marketConfig = YamlConfiguration.loadConfiguration(marketFile);
    }

    public FileConfiguration getMarketConfig() { return marketConfig; }

    public void reloadMarketConfig() {
        marketFile = new File(getDataFolder(), "market.yml");
        marketConfig = YamlConfiguration.loadConfiguration(marketFile);
        reloadConfig();
        if (augmentManager != null) augmentManager.loadConfigs();
        getLogger().info("⚙ 설정 파일 리로드 완료.");
    }

    // --- Getter 메서드 ---
    public static NexusCore getInstance() { return instance; }
    public static Economy getEconomy() { return econ; }
    
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public DatabaseManager getDbManager() { return databaseManager; } // UserManager 호환용 별칭

    public UserManager getUserManager() { return userManager; }
    public VanillaShopManager getVanillaShopManager() { return vanillaShopManager; }
    public PointShopManager getPointShopManager() { return pointShopManager; }
    public AugmentManager getAugmentManager() { return augmentManager; }
    public SiegeManager getSiegeManager() { return siegeManager; }
}