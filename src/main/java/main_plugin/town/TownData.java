package main_plugin.town;

import org.bukkit.Location;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TownData {
    private final UUID owner;
    private Location spawnLoc;
    private final Set<String> claimedChunks; // 형식: "world,x,z"

    public TownData(UUID owner, Location spawnLoc) {
        this.owner = owner;
        this.spawnLoc = spawnLoc;
        this.claimedChunks = new HashSet<>();
    }

    public UUID getOwner() { return owner; }
    public Location getSpawnLoc() { return spawnLoc; }
    public void setSpawnLoc(Location spawnLoc) { this.spawnLoc = spawnLoc; }
    
    public Set<String> getClaimedChunks() { return claimedChunks; }
    public void addChunk(String chunkKey) { claimedChunks.add(chunkKey); }
    public boolean hasChunk(String chunkKey) { return claimedChunks.contains(chunkKey); }
}