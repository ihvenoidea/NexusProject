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
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Bukkit;
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
    
    // [신규 통합] 숙련도 및 랭킹 시스템
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

        // 1. DB 연결
        this.databaseManager = new DatabaseManager(this);
        databaseManager.connect(
            getConfig().getString("database.host", "127.0.0.1"),
            getConfig().getInt("database.port", 3306),
            getConfig().getString("database.name", "minecraft_server"),
            getConfig().getString("database.user", "root"),
            getConfig().getString("database.password", "rladudwo7@")
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
        }
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NexusExpansion(this).register(); // 통합된 하나의 PAPI만 등록
        }

        registerCommands();
        registerEvents();

        for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
            databaseManager.loadUserData(p.getUniqueId());
        }

        getLogger().info("✔ NexusCore(Life Mastery 포함) 시스템이 완벽하게 로드되었습니다.");
    }

    @Override
    public void onDisable() {
        if (discordManager != null) discordManager.stopLocalServer();
        if (databaseManager != null && databaseManager.getConnection() != null) {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                getUserManager().getUser(player.getUniqueId()).ifPresent(user -> getUserManager().saveUserData(user));
            }
            databaseManager.close();
        }
        if (townManager != null) townManager.saveTowns();
    }

    private void setupEconomy() {
        NexusEconomy nexusEconomy = new NexusEconomy(this);
        getServer().getServicesManager().register(Economy.class, nexusEconomy, this, ServicePriority.Highest);
        this.econ = nexusEconomy;
    }

    private void registerAugments() {
        // 기존 증강체 등록 로직
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

        // [신규 등록] 생활 숙련도 명령어
        MasteryCommand masteryCmd = new MasteryCommand(this);
        getCommand("숙련도").setExecutor(masteryCmd);
        getCommand("숙련도").setTabCompleter(masteryCmd);
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

        // [신규 등록] 생활 숙련도 이벤트
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
    
    // [신규 Getter]
    public MasteryManager getMasteryManager() { return masteryManager; }
    public MasteryRankingManager getMasteryRankingManager() { return masteryRankingManager; }
    public MasteryRankingGUI getMasteryRankingGUI() { return masteryRankingGUI; }

    public AbyssalTrialListener getAbyssalTrialListener() { return abyssalTrialListener; }
    public OminousGachaListener getOminousGachaListener() { return ominousGachaListener; }

    private void createMarketFolder() {
        File marketDir = new File(getDataFolder(), "market");
        if (!marketDir.exists()) marketDir.mkdirs();
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