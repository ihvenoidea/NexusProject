package main_plugin;

import main_plugin.api.NexusExpansion;
import main_plugin.augments.AugmentManager;
import main_plugin.augments.synergies.*;
import main_plugin.collection.CollectionManager;
import main_plugin.collection.CollectionListener;
import main_plugin.commands.*;
import main_plugin.database.DatabaseManager;
import main_plugin.discord.DiscordManager;
import main_plugin.economy.NexusEconomy;
import main_plugin.economy.ExchangeManager;
import main_plugin.economy.ExchangeCommand;
import main_plugin.farming.FarmingListener;
import main_plugin.fishing.FishingListener;
import main_plugin.gui.*;
import main_plugin.mail.MailCommand;
import main_plugin.mail.MailManager;
import main_plugin.items.*;
import main_plugin.mastery.MasteryCommand;
import main_plugin.mastery.MasteryListener;
import main_plugin.mastery.MasteryManager;
import main_plugin.mastery.MasteryRankingGUI;
import main_plugin.mastery.MasteryRankingManager;
import main_plugin.politics.SiegeManager;
import main_plugin.politics.SiegeListener;
import main_plugin.politics.TributeManager;
import main_plugin.storage.StorageManager;
import main_plugin.storage.StorageListener;
import main_plugin.town.TownManager;
import main_plugin.town.TownListener;
import main_plugin.town.TownTotemListener;
import main_plugin.user.PlayerListener;
import main_plugin.user.UserManager;
import main_plugin.traits.VanillaShopTrait;
import main_plugin.traits.ShardExchangeTrait;
import main_plugin.traits.SetItemExchangeTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class NexusCore extends JavaPlugin {

    private static NexusCore instance;
    private Economy econ = null;

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
    private ExchangeManager exchangeManager;
    private StorageManager storageManager; 
    
    // 숙련도 및 랭킹 시스템
    private MasteryManager masteryManager;
    private MasteryRankingManager masteryRankingManager;
    private MasteryRankingGUI masteryRankingGUI;

    private AbyssalTrialListener abyssalTrialListener;
    private OminousGachaListener ominousGachaListener;

    @Override
    public void onEnable() {
        instance = this;

        if (!new File(getDataFolder(), "config.yml").exists()) saveDefaultConfig();
        reloadConfig();
        createMarketFolder();

        // 1. DB 연결 (HikariCP 적용 완료)
        this.databaseManager = new DatabaseManager(this);
        databaseManager.connect(
            getConfig().getString("database.host", "127.0.0.1"),
            getConfig().getInt("database.port", 3306),
            getConfig().getString("database.name", "minecraft_server"),
            getConfig().getString("database.user", "root"),
            getConfig().getString("database.password", "") // config.yml에 실제 비밀번호 기입
        );

        setupEconomy();

        // 2. 매니저 초기화
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
        this.exchangeManager = new ExchangeManager(this);
        this.storageManager = new StorageManager(this); 
        
        // 숙련도 매니저 초기화
        this.masteryManager = new MasteryManager(this);
        this.masteryRankingManager = new MasteryRankingManager(this);
        this.masteryRankingGUI = new MasteryRankingGUI(this);

        this.abyssalTrialListener = new AbyssalTrialListener(this);
        this.ominousGachaListener = new OminousGachaListener(this);

        registerAugments();

        // 3. Citizens NPC 및 PAPI 등록
        if (getServer().getPluginManager().getPlugin("Citizens") != null) {
            CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(VanillaShopTrait.class).withName("vanilla_shop"));
            CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(ShardExchangeTrait.class).withName("shard_exchange"));
            CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(SetItemExchangeTrait.class).withName("set_exchange"));
        }
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NexusExpansion(this).register(); // 단일 PAPI 클래스 통합 등록 완료
        }

        // ====================================================================
        // [신규] 프록시(Velocity/BungeeCord) 서버 간 이동을 위한 통신 채널 등록
        // ====================================================================
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        registerCommands();
        registerEvents();

        // 4. 접속 중인 유저 데이터 로드 및 자동 저장 태스크 실행
        for (Player p : getServer().getOnlinePlayers()) {
            databaseManager.loadUserData(p.getUniqueId());
        }
        startAutoSaveTask();

        getLogger().info("✔ NexusCore(Life Mastery & 최적화 & 프록시 지원) 시스템이 완벽하게 로드되었습니다.");
    }

    @Override
    public void onDisable() {
        if (discordManager != null) discordManager.stopLocalServer();
        
        if (databaseManager != null) {
            getLogger().info("[NexusCore] 서버 종료 중... 유저 데이터를 DB에 안전하게 저장합니다.");
            for (Player player : getServer().getOnlinePlayers()) {
                getUserManager().getUser(player.getUniqueId()).ifPresent(user -> databaseManager.saveUserData(user.getUuid()));
            }
            databaseManager.close();
        }
        if (townManager != null) townManager.saveTowns();
    }

    // ==========================================
    // [핵심] 5분 주기 자동 저장 시스템 (렉 방지용)
    // ==========================================
    private void startAutoSaveTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) return;
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                getUserManager().getUser(p.getUniqueId()).ifPresent(user -> {
                    databaseManager.saveUserData(user.getUuid());
                });
            }
            getLogger().info("[자동저장] 모든 접속 유저의 데이터가 안전하게 DB에 저장되었습니다.");
        }, 6000L, 6000L); // 6000틱 = 5분
    }

    private void setupEconomy() {
        NexusEconomy nexusEconomy = new NexusEconomy(this);
        getServer().getServicesManager().register(Economy.class, nexusEconomy, this, ServicePriority.Highest);
        this.econ = nexusEconomy;
    }

    private void registerAugments() {
        augmentManager.registerAugment(new IroncladSynergy.IroncladKnight());
        augmentManager.registerAugment(new IroncladSynergy.BloodPig());
        augmentManager.registerAugment(new IroncladSynergy.ThornArmor());
        augmentManager.registerAugment(new WarlordSynergy.GreatPower());
        augmentManager.registerAugment(new WarlordSynergy.SwiftKiller());
        augmentManager.registerAugment(new WarlordSynergy.Bloodthirst());
        augmentManager.registerAugment(new ToxicSynergy.ToxicThorn());
        augmentManager.registerAugment(new ToxicSynergy.ToxicWeapon());
        augmentManager.registerAugment(new ToxicSynergy.Stimpack());
        augmentManager.registerAugment(new BombSynergy.BombLauncher());
        augmentManager.registerAugment(new BombSynergy.ExplosionResistance());
        augmentManager.registerAugment(new BombSynergy.SuicideBomber());
        augmentManager.registerAugment(new MythicAugments.ImmortalShield());
        augmentManager.registerAugment(new MythicAugments.GodOfWar());
        augmentManager.registerAugment(new MythicAugments.PlagueLord());
        augmentManager.registerAugment(new MythicAugments.Armageddon());
    }

    private void registerCommands() {
        getCommand("money").setExecutor(new MoneyCommand(this));
        getCommand("포인트").setExecutor(new PointCommand(this));
        getCommand("dp").setExecutor(new DPCommand(this));
        getCommand("도감").setExecutor(new CollectionCommand(this));
        getCommand("nexus").setExecutor(new NexusCommand(this));
        getCommand("상점").setExecutor(new ShopCommand(this));
        getCommand("타운").setExecutor(new TownCommand(this));
        getCommand("공성전").setExecutor(new SiegeCommand(this));
        getCommand("setitem").setExecutor(new AdminItemCommand(this));

        TributeCommand tributeCmd = new TributeCommand(this);
        getCommand("조공").setExecutor(tributeCmd);
        getCommand("조공순위").setExecutor(tributeCmd);
        getCommand("조공종료").setExecutor(tributeCmd);

        MailCommand mailSystem = new MailCommand(this);
        getCommand("우편함").setExecutor(mailSystem);
        getCommand("우편보내기").setExecutor(mailSystem);

        ExchangeCommand exchangeSystem = new ExchangeCommand(this, exchangeManager);
        getCommand("거래소").setExecutor(exchangeSystem);
        getCommand("판매").setExecutor(exchangeSystem);

        getCommand("강화").setExecutor(new EnhanceSystem(this));

        // 생활 숙련도 명령어
        MasteryCommand masteryCmd = new MasteryCommand(this);
        getCommand("숙련도").setExecutor(masteryCmd);
        getCommand("숙련도").setTabCompleter(masteryCmd);
        
        // [신규] 잠수 서버 이동 명령어 연결
        if (getCommand("잠수") != null) {
            getCommand("잠수").setExecutor(new AfkCommand(this));
        }
    }

    private void registerEvents() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new TownListener(this), this);
        pm.registerEvents(new SiegeListener(this), this);
        pm.registerEvents(new SetItemListener(this), this);
        pm.registerEvents(new WeaponToolListener(this), this);
        pm.registerEvents(new WeaponBoxListener(this), this);
        pm.registerEvents(new CollectionListener(this), this);
        pm.registerEvents(augmentManager, this);
        pm.registerEvents(new TicketListener(this), this);
        pm.registerEvents(new AugmentGUIListener(this), this);
        pm.registerEvents(new MainMenuListener(this), this); 
        pm.registerEvents(new StorageListener(this), this); 
        pm.registerEvents(new MailCommand(this), this);
        pm.registerEvents(new ExchangeCommand(this, exchangeManager), this);
        pm.registerEvents(new EnhanceSystem(this), this);
        pm.registerEvents(this.abyssalTrialListener, this);
        pm.registerEvents(this.ominousGachaListener, this);

        // 생활 숙련도 및 특수 기믹 이벤트
        pm.registerEvents(new FarmingListener(this), this);
        pm.registerEvents(new FishingListener(this), this);
        pm.registerEvents(new MasteryListener(this), this);
        pm.registerEvents(new TownTotemListener(this), this);
        pm.registerEvents(this.masteryRankingGUI, this);
    }

    public static NexusCore getInstance() { return instance; }
    public static Economy getEconomy() { return instance.econ; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public UserManager getUserManager() { return userManager; }
    public VanillaShopManager getVanillaShopManager() { return vanillaShopManager; }
    public PointShopManager getPointShopManager() { return pointShopManager; }
    public AugmentManager getAugmentManager() { return augmentManager; }
    public SiegeManager getSiegeManager() { return siegeManager; }
    public TributeManager getTributeManager() { return tributeManager; }
    public SetItemManager getSetItemManager() { return setItemManager; }
    public CollectionManager getCollectionManager() { return collectionManager; }
    public TownManager getTownManager() { return townManager; }
    public ExchangeManager getExchangeManager() { return exchangeManager; }
    public StorageManager getStorageManager() { return storageManager; }
    
    public MasteryManager getMasteryManager() { return masteryManager; }
    public MasteryRankingManager getMasteryRankingManager() { return masteryRankingManager; }
    public MasteryRankingGUI getMasteryRankingGUI() { return masteryRankingGUI; }

    public AbyssalTrialListener getAbyssalTrialListener() { return abyssalTrialListener; }
    public OminousGachaListener getOminousGachaListener() { return ominousGachaListener; }

    // ==========================================
    // [수정됨] 상점 파일 자동 추출 및 폴더 생성
    // ==========================================
    private void createMarketFolder() {
        File marketDir = new File(getDataFolder(), "market");
        if (!marketDir.exists()) {
            marketDir.mkdirs();
            
            // JAR 파일 내부에 있는 기본 상점 파일들을 서버 폴더로 자동 추출합니다.
            String[] defaultMarkets = {
                "minerals.yml", "farming.yml", "wood.yml", "stone.yml", 
                "ocean.yml", "nether_end.yml", "mob_loot.yml", 
                "decoration.yml", "color_blocks.yml"
            };
            
            for (String file : defaultMarkets) {
                try {
                    // false: 이미 파일이 서버에 존재하면 덮어쓰지 않음
                    saveResource("market/" + file, false); 
                } catch (Exception e) {
                    getLogger().warning("[NexusCore] 기본 상점 파일 생성 실패: " + file + " (파일이 resources/market 에 있는지 확인하세요)");
                }
            }
            getLogger().info("[NexusCore] 기본 상점 설정 파일들이 market 폴더에 성공적으로 생성되었습니다.");
        }
    }

    public void reloadMarketConfig() {
        reloadConfig();
        if (vanillaShopManager != null) vanillaShopManager.reloadConfigs();
    }

    public Location getSpawnLocation() {
        String worldName = getConfig().getString("spawn.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) world = Bukkit.getWorlds().get(0);
        return new Location(world, getConfig().getDouble("spawn.x"), getConfig().getDouble("spawn.y"), getConfig().getDouble("spawn.z"),
                (float)getConfig().getDouble("spawn.yaw"), (float)getConfig().getDouble("spawn.pitch"));
    }
}