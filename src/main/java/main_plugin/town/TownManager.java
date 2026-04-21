package main_plugin.town;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TownManager {

    private final NexusCore plugin;
    private final Map<UUID, TownData> towns = new HashMap<>();
    private final Map<String, UUID> chunkMap = new HashMap<>(); // 빠른 보호 조회를 위한 맵
    
    // 파티클 시각화를 켠 유저들을 추적하는 Set
    private final Set<UUID> visualizingPlayers = new HashSet<>();

    private File file;
    private FileConfiguration config;

    public TownManager(NexusCore plugin) {
        this.plugin = plugin;
        loadTowns();
        startVisualizationTask();
    }

    // ==========================================
    // [ 1. 타운 생성 ]
    // ==========================================
    public void createTown(Player player) {
        UUID uuid = player.getUniqueId();
        if (towns.containsKey(uuid)) {
            player.sendMessage("§c[!] 이미 타운을 소유하고 있습니다.");
            return;
        }

        Location loc = player.getLocation();
        double minDist = plugin.getConfig().getDouble("town.min-distance-from-spawn", 1000);
        
        if (loc.getWorld().getSpawnLocation().distance(loc) < minDist) {
            player.sendMessage("§c[!] 스폰에서 너무 가깝습니다. (최소 " + minDist + "블록 밖에서 생성 가능)");
            return;
        }

        Chunk chunk = loc.getChunk();
        String chunkKey = getChunkKey(chunk);
        
        if (chunkMap.containsKey(chunkKey)) {
            player.sendMessage("§c[!] 이미 다른 유저가 점령한 청크입니다.");
            return;
        }
        if (!canClaim(player, chunk)) return;

        TownData town = new TownData(uuid, loc);
        town.addChunk(chunkKey);
        
        towns.put(uuid, town);
        chunkMap.put(chunkKey, uuid);
        saveTowns();

        player.sendMessage("§a[!] 성공적으로 타운을 설립했습니다! (현재 위치 1청크가 무료로 지급되었습니다.)");
    }

    // ==========================================
    // [ 2. 타운 확장 ]
    // ==========================================
    public void claimChunk(Player player) {
        TownData town = towns.get(player.getUniqueId());
        if (town == null) {
            player.sendMessage("§c[!] 먼저 타운을 생성해야 합니다. (/타운 생성)");
            return;
        }

        int maxChunks = plugin.getConfig().getInt("town.max-chunks", 25);
        if (town.getClaimedChunks().size() >= maxChunks) {
            player.sendMessage("§c[!] 최대 확장 한도(" + maxChunks + "청크)에 도달했습니다.");
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = getChunkKey(chunk);
        
        if (chunkMap.containsKey(chunkKey)) {
            player.sendMessage("§c[!] 이미 점령된 청크입니다.");
            return;
        }
        if (!canClaim(player, chunk)) return;

        double basePrice = plugin.getConfig().getDouble("town.price.base", 10000);
        double increase = plugin.getConfig().getDouble("town.price.increase-per-chunk", 5000);
        double cost = basePrice + ((town.getClaimedChunks().size() - 1) * increase);

        if (!NexusCore.getEconomy().has(player, cost)) {
            player.sendMessage("§c[!] 돈이 부족합니다. (필요 금액: " + String.format("%,.0f원", cost) + ")");
            return;
        }

        NexusCore.getEconomy().withdrawPlayer(player, cost);
        town.addChunk(chunkKey);
        chunkMap.put(chunkKey, player.getUniqueId());
        saveTowns();

        player.sendMessage("§a[!] §e" + String.format("%,.0f원", cost) + "§a을 지불하고 영토를 확장했습니다! (현재: " + town.getClaimedChunks().size() + "/" + maxChunks + "청크)");
    }

    // ==========================================
    // [ 3. 타운 이동 (5초 대기 & 가장 높은 위치 보정) ]
    // ==========================================
    public void teleportToTown(Player player) {
        TownData town = towns.get(player.getUniqueId());
        if (town == null) {
            player.sendMessage("§c[!] 소유한 타운이 없습니다.");
            return;
        }

        player.sendMessage("§e[!] 5초 후 타운으로 이동합니다. 움직이지 마세요!");
        Location startLoc = player.getLocation();

        new BukkitRunnable() {
            int time = 5; // 5초 대기
            @Override
            public void run() {
                if (player.getLocation().distance(startLoc) > 0.5) {
                    player.sendMessage("§c[!] 움직임이 감지되어 텔레포트가 취소되었습니다.");
                    cancel();
                    return;
                }
                
                if (time <= 0) {
                    Location target = town.getSpawnLoc();
                    // 해당 X, Z 좌표에서 가장 높은 블록의 Y 좌표를 가져옴 (안전한 텔레포트)
                    int highestY = target.getWorld().getHighestBlockYAt(target.getBlockX(), target.getBlockZ());
                    Location safeLoc = new Location(target.getWorld(), target.getX(), highestY + 1.0, target.getZ(), target.getYaw(), target.getPitch());
                    
                    player.teleport(safeLoc);
                    player.sendMessage("§a[!] 성공적으로 타운으로 이동했습니다.");
                    cancel();
                    return;
                }
                time--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    // ==========================================
    // [ 4. 타운 스폰 설정 (좌표 변경) ]
    // ==========================================
    public void setSpawn(Player player) {
        TownData town = towns.get(player.getUniqueId());
        if (town == null) {
            player.sendMessage("§c[!] 먼저 타운을 생성해주세요.");
            return;
        }
        
        Chunk chunk = player.getLocation().getChunk();
        if (!town.hasChunk(getChunkKey(chunk))) {
            player.sendMessage("§c[!] 자신의 타운 영토 내에서만 스폰 좌표를 설정할 수 있습니다.");
            return;
        }
        
        town.setSpawnLoc(player.getLocation());
        saveTowns();
        player.sendMessage("§a[!] 타운 스폰 위치가 현재 밟고 있는 좌표로 변경되었습니다.");
    }

    // ==========================================
    // [ 5. 파티클 시각화 기능 ]
    // ==========================================
    public void toggleVisualization(Player player) {
        UUID uuid = player.getUniqueId();
        if (visualizingPlayers.contains(uuid)) {
            visualizingPlayers.remove(uuid);
            player.sendMessage("§c[!] 타운 영토 시각화가 꺼졌습니다.");
        } else {
            if (!towns.containsKey(uuid)) {
                player.sendMessage("§c[!] 소유한 타운이 없어 시각화할 수 없습니다.");
                return;
            }
            visualizingPlayers.add(uuid);
            player.sendMessage("§a[!] 타운 영토 시각화가 켜졌습니다. (내 영토의 경계선이 불꽃으로 표시됩니다)");
        }
    }

    private void startVisualizationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // 오프라인이거나 타운이 없어진 유저는 목록에서 자동 제거
                visualizingPlayers.removeIf(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    return p == null || !p.isOnline() || !towns.containsKey(uuid);
                });

                for (UUID uuid : visualizingPlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    TownData town = towns.get(uuid);
                    if (player != null && town != null) {
                        showChunkBorders(player, town);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 1초(20틱)마다 파티클 갱신
    }

    private void showChunkBorders(Player player, TownData town) {
        World world = player.getWorld();
        int y = player.getLocation().getBlockY() + 1; // 플레이어 눈높이 근처에 표시

        for (String chunkKey : town.getClaimedChunks()) {
            String[] split = chunkKey.split(",");
            if (!split[0].equals(world.getName())) continue;
            
            int cx = Integer.parseInt(split[1]);
            int cz = Integer.parseInt(split[2]);
            
            double minX = cx * 16;
            double minZ = cz * 16;
            double maxX = minX + 16;
            double maxZ = minZ + 16;
            
            // 모서리 4면에 파티클을 뿌립니다. (서버 렉 방지를 위해 2블록 간격으로 표시)
            for (double x = minX; x <= maxX; x += 2) {
                player.spawnParticle(Particle.FLAME, x, y, minZ, 1, 0, 0, 0, 0);
                player.spawnParticle(Particle.FLAME, x, y, maxZ, 1, 0, 0, 0, 0);
            }
            for (double z = minZ; z <= maxZ; z += 2) {
                player.spawnParticle(Particle.FLAME, minX, y, z, 1, 0, 0, 0, 0);
                player.spawnParticle(Particle.FLAME, maxX, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    // ==========================================
    // [ 관리자 전용 기능 ]
    // ==========================================
    public TownData getTown(UUID uuid) {
        return towns.get(uuid);
    }

    public boolean deleteTown(UUID uuid) {
        TownData town = towns.remove(uuid);
        if (town != null) {
            chunkMap.entrySet().removeIf(entry -> entry.getValue().equals(uuid));
            saveTowns();
            return true;
        }
        return false;
    }

    // ==========================================
    // [ 유틸리티 및 권한 판별 ]
    // ==========================================
    private boolean canClaim(Player player, Chunk chunk) {
        int buffer = plugin.getConfig().getInt("town.chunk-buffer", 2);
        for (int x = -buffer; x <= buffer; x++) {
            for (int z = -buffer; z <= buffer; z++) {
                String key = chunk.getWorld().getName() + "," + (chunk.getX() + x) + "," + (chunk.getZ() + z);
                if (chunkMap.containsKey(key) && !chunkMap.get(key).equals(player.getUniqueId())) {
                    player.sendMessage("§c[!] 근처에 다른 유저의 타운이 있어 점령할 수 없습니다.");
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canBuild(Player player, Location loc) {
        String key = getChunkKey(loc.getChunk());
        if (!chunkMap.containsKey(key)) return true;
        return chunkMap.get(key).equals(player.getUniqueId());
    }

    public String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();
    }

    private void loadTowns() {
        file = new File(plugin.getDataFolder(), "towns.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);

        if (config.contains("towns")) {
            for (String uuidStr : config.getConfigurationSection("towns").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Location loc = config.getLocation("towns." + uuidStr + ".spawn");
                TownData town = new TownData(uuid, loc);
                
                List<String> chunks = config.getStringList("towns." + uuidStr + ".chunks");
                for (String c : chunks) {
                    town.addChunk(c);
                    chunkMap.put(c, uuid);
                }
                towns.put(uuid, town);
            }
        }
    }

    public void saveTowns() {
        config.set("towns", null);
        for (TownData town : towns.values()) {
            String path = "towns." + town.getOwner().toString();
            config.set(path + ".spawn", town.getSpawnLoc());
            config.set(path + ".chunks", new ArrayList<>(town.getClaimedChunks()));
        }
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}