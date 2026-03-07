package com.jackhuey;

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class SpawnListener implements Listener {
    private final BedHopper plugin;

    public SpawnListener(BedHopper plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        event.setNotifyPlayer(false);

        Player player = event.getPlayer();
        Location previousSpawn = player.getBedSpawnLocation();
        Location newSpawn = event.getLocation();

        //Player removed bed spawn (respawn falls back to world spawn)
        if (newSpawn == null) {
            if (previousSpawn != null && plugin.removePlayerSpawn(player.getUniqueId(), previousSpawn)) {
                player.sendMessage("[BedHopper] Spawn removed.");
            }
            return;
        }

        plugin.addPlayerSpawn(player.getUniqueId(), newSpawn);
        player.sendMessage("[BedHopper] Spawn added.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBedBreak(BlockBreakEvent event) {
        if (!Tag.BEDS.isTagged(event.getBlock().getType())) {
            return;
        }

        int removed = plugin.removeSpawnForAllPlayers(event.getBlock().getLocation());
        if (removed > 0) {
            //event.getPlayer().sendMessage("[BedHopper] Removed " + removed + " spawn point(s) for destroyed bed.");
        }
    }
}