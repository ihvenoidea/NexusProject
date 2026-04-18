package main_plugin;

import main_plugin.augments.AugmentManager;
import main_plugin.augments.synergies.BombSynergy;
import main_plugin.augments.synergies.IroncladSynergy;
import main_plugin.augments.synergies.MythicAugments; // 신화 증강체 추가
import main_plugin.augments.synergies.ToxicSynergy;
import main_plugin.augments.synergies.WarlordSynergy;
import main_plugin.collection.CollectionManager;
import main_plugin.collection.CollectionListener;
import main_plugin.commands.*;
import main_plugin.database.DatabaseManager;
import main_plugin.discord.DiscordManager;
import main_plugin.economy.NexusEconomy;
import main_plugin.gui.PointShopManager;
import main_plugin.gui.VanillaShopManager;
import main_plugin.gui.AugmentGUIListener;
import main_plugin.gui.TicketListener;
import main_plugin.items.SetItemManager;
import main_plugin.items.SetItemListener;
import main_plugin.items.AbyssalTrialListener;
import main_plugin.items.WeaponToolListener;
import main_plugin.items.WeaponBoxListener; // 무기 상자 리스너 추가
import main_plugin.politics.SiegeManager;
import main_plugin.politics.SiegeListener;
import main_plugin.politics.TributeManager;
import main_plugin.town.TownManager;
import main_plugin.town.TownListener;
import main_plugin.user.PlayerListener;
import main_plugin.user.UserManager;
import main_plugin.traits.VanillaShopTrait;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * 넥서스 프로젝트의 메인 클래스입니다.
 * 모든 시스템의 초기화와 의존성 주입, 명령어/이벤트 등록을 총괄합니다.
 */
public class NexusCore extends JavaPlugin {

    private static NexusCore instance;
    private static Economy econ = null;

    private DatabaseManager databaseManager;
    private UserManager userManager;
    private VanillaShopManager vanillaShopManager;
    private PointShopManager pointShopManager;
    private AugmentManager augmentManager;
    private SiegeManager siegeManager;
    private TributeManager tributeManager;
    private SetItemManager setItemManager;
    private CollectionManager collectionManager;
    private DiscordManager discordManager;
    private TownManager townManager;

    private File marketFile;
    private FileConfiguration marketConfig;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 기본 설정 및 리소스 로드
        saveDefaultConfig();
        createMarketConfig();

        // 2. 데이터베이스 서버 연결
        this.databaseManager = new DatabaseManager(this);
        databaseManager.connect(
            getConfig().getString("database.host", "localhost"),
            getConfig().getInt("database.port", 3306),
            getConfig().getString("database.name", "minecraft_server"),
            getConfig().getString("database.user", "root"),
            getConfig().getString("database.password", "")
        );

        // 3. Vault 경제 API 연동
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            setupEconomy();
        }

        // 4. 모든 시스템 매니저 객체 초기화
        this.userManager = new UserManager(this);
        this.augmentManager = new AugmentManager(this);
        this.siegeManager = new SiegeManager(this);
        this.vanillaShopManager = new VanillaShopManager(this);
        this.pointShopManager = new PointShopManager(this);
        this.tributeManager = new TributeManager(this);
        this.setItemManager = new SetItemManager(this);
        this.collectionManager = new CollectionManager(this);
        this.discordManager = new DiscordManager(this);
        this.townManager = new TownManager(this);

        // 5. 증강체 시스템 로드
        registerAugments();

        // 6. Citizens NPC 전용 Trait 등록
        if (getServer().getPluginManager().getPlugin("Citizens") != null) {
            CitizensAPI.getTraitFactory().registerTrait(
                TraitInfo.create(VanillaShopTrait.class).withName("vanilla_shop")
            );
        }

        // 7. 커맨드 및 이벤트 리스너 통합 등록
        registerCommands();
        registerEvents();

        getLogger().info("✔ NexusCore 시스템이 완벽하게 로드되었습니다.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    /**
     * 제작된 모든 증강체들을 AugmentManager에 등록합니다.
     */
    private void registerAugments() {
        // [철갑] 시너지
        augmentManager.registerAugment(new IroncladSynergy.IroncladKnight());
        augmentManager.registerAugment(new IroncladSynergy.BloodPig());
        augmentManager.registerAugment(new IroncladSynergy.ThornArmor());

        // [패왕] 시너지
        augmentManager.registerAugment(new WarlordSynergy.GreatPower());
        augmentManager.registerAugment(new WarlordSynergy.SwiftKiller());
        augmentManager.registerAugment(new WarlordSynergy.Bloodthirst());

        // [맹독] 시너지
        augmentManager.registerAugment(new ToxicSynergy.ToxicThorn());
        augmentManager.registerAugment(new ToxicSynergy.ToxicWeapon());
        augmentManager.registerAugment(new ToxicSynergy.Stimpack());

        // [폭탄] 시너지
        augmentManager.registerAugment(new BombSynergy.BombLauncher());
        augmentManager.registerAugment(new BombSynergy.ExplosionResistance());
        augmentManager.registerAugment(new BombSynergy.SuicideBomber());

        // [신화] 증강체 추가
        augmentManager.registerAugment(new MythicAugments.ImmortalShield());
        augmentManager.registerAugment(new MythicAugments.GodOfWar());
        augmentManager.registerAugment(new MythicAugments.PlagueLord());
        augmentManager.registerAugment(new MythicAugments.Armageddon());
    }

    /**
     * 서버에 등록된 모든 명령어를 관리합니다.
     */
    private void registerCommands() {
        getCommand("money").setExecutor(new MoneyCommand(this));
        getCommand("돈").setExecutor(new MoneyCommand(this));
        getCommand("nexus").setExecutor(new NexusCommand(this));
        getCommand("포인트").setExecutor(new PointCommand(this));
        getCommand("dp").setExecutor(new DPCommand(this));

        TributeCommand tributeCmd = new TributeCommand(this);
        getCommand("조공").setExecutor(tributeCmd);
        getCommand("조공순위").setExecutor(tributeCmd);
        getCommand("조공종료").setExecutor(tributeCmd);

        getCommand("공성전").setExecutor(new SiegeCommand(this));
        getCommand("setitem").setExecutor(new AdminItemCommand(this));
        getCommand("도감").setExecutor(new CollectionCommand(this));
        getCommand("타운").setExecutor(new TownCommand(this));
    }

    /**
     * 모든 이벤트 리스너를 서버에 등록합니다.
     */
    private void registerEvents() {
        // 유저 데이터 및 메인 시스템
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // 타운 및 공성전
        getServer().getPluginManager().registerEvents(new TownListener(this), this);
        getServer().getPluginManager().registerEvents(new SiegeListener(this), this);
        
        // 아이템 및 장비 세트
        getServer().getPluginManager().registerEvents(new SetItemListener(this), this);
        getServer().getPluginManager().registerEvents(new AbyssalTrialListener(this), this);
        getServer().getPluginManager().registerEvents(new WeaponToolListener(this), this);
        getServer().getPluginManager().registerEvents(new WeaponBoxListener(this), this); // 무기 상자 리스너 추가

        // 도감 시스템
        getServer().getPluginManager().registerEvents(new CollectionListener(this), this);

        // 증강체 시스템 (패시브 트리거 및 GUI 연동)
        getServer().getPluginManager().registerEvents(augmentManager, this);
        getServer().getPluginManager().registerEvents(new TicketListener(this), this);
        getServer().getPluginManager().registerEvents(new AugmentGUIListener(this), this);
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
    }

    // --- 시스템 접근을 위한 Getter 메소드 ---
    public static NexusCore getInstance() { return instance; }
    public static Economy getEconomy() { return econ; }
    
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public UserManager getUserManager() { return userManager; }
    public VanillaShopManager getVanillaShopManager() { return vanillaShopManager; }
    public PointShopManager getPointShopManager() { return pointShopManager; }
    public AugmentManager getAugmentManager() { return augmentManager; }
    public SiegeManager getSiegeManager() { return siegeManager; }
    public TributeManager getTributeManager() { return tributeManager; }
    public SetItemManager getSetItemManager() { return setItemManager; }
    public CollectionManager getCollectionManager() { return collectionManager; }
    public DiscordManager getDiscordManager() { return discordManager; }
    public TownManager getTownManager() { return townManager; }
}