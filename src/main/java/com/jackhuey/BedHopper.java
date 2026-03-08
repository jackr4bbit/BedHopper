package com.jackhuey;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BedHopper extends JavaPlugin {

    @Override
    public void onEnable() {
        this.getCommand("spawns").setExecutor(new ListCommand(this));
        getServer().getPluginManager().registerEvents(new SpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new OnRespawn(), this);
        getLogger().info("BedHopper enabled!");
    }

    public void setPlayerSpawns(UUID playerId, List<String> spawns) {
        String path = "players." + playerId + ".spawns";
        getConfig().set(path, spawns.isEmpty() ? null : spawns);
        saveConfig();
    }

    public void setCurrentPlayerSpawn(UUID playerId, Location spawnLocation) {
        addPlayerSpawn(playerId, spawnLocation);
    }

    public boolean addPlayerSpawn(UUID playerId, Location spawnLocation) {
        List<String> spawns = new ArrayList<>(getPlayerSpawns(playerId));
        boolean exists = spawns.stream().anyMatch(s -> isSameBedLocation(s, spawnLocation));
        if (exists) {
            return false;
        }

        spawns.add(serializeLocation(spawnLocation));
        setPlayerSpawns(playerId, spawns);
        return true;
    }

    public boolean removePlayerSpawn(UUID playerId, Location spawnLocation) {
        List<String> spawns = new ArrayList<>(getPlayerSpawns(playerId));
        int before = spawns.size();
        spawns.removeIf(s -> isSameBedLocation(s, spawnLocation));
        int removed = before - spawns.size();

        if (removed > 0) {
            setPlayerSpawns(playerId, spawns);
            return true;
        }
        return false;
    }

    public int removeSpawnForAllPlayers(Location bedLocation) {
        ConfigurationSection players = getConfig().getConfigurationSection("players");
        if (players == null) {
            return 0;
        }

        int removedCount = 0;
        for (String playerKey : players.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            List<String> spawns = new ArrayList<>(getPlayerSpawns(playerId));
            int before = spawns.size();
            spawns.removeIf(s -> isSameBedLocation(s, bedLocation));
            int removedForPlayer = before - spawns.size();

            if (removedForPlayer > 0) {
                setPlayerSpawns(playerId, spawns);
                removedCount += removedForPlayer;
            }
        }

        return removedCount;
    }

    public List<String> getPlayerSpawns(UUID playerId) {
        return getConfig().getStringList("players." + playerId + ".spawns");
    }

    private String serializeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location world cannot be null");
        }
        return world.getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private boolean isSameBedLocation(String serialized, Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        String[] parts = serialized.split(",");
        if (parts.length != 4) {
            return false;
        }

        if (!parts[0].equals(world.getName())) {
            return false;
        }

        int x, y, z;
        try {
            x = Integer.parseInt(parts[1]);
            y = Integer.parseInt(parts[2]);
            z = Integer.parseInt(parts[3]);
        } catch (NumberFormatException ex) {
            return false;
        }

        int dx = Math.abs(x - location.getBlockX());
        int dy = Math.abs(y - location.getBlockY());
        int dz = Math.abs(z - location.getBlockZ());

        //Same bed block or the other half of the same bed
        return dy == 0 && ((dx == 0 && dz == 0) || (dx + dz == 1));
    }

    @Override
    public void onDisable() {
        saveConfig();
        getLogger().info("Goodbye from BedHopper!");
    }
}
