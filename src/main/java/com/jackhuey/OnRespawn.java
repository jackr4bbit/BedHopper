package com.jackhuey;

import com.jackhuey.BedHopper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.event.player.PlayerRespawnEvent;

public class OnRespawn implements Listener {
    private static final String BED_MENU_TITLE = "§8Select a Bed";
    private static final float DEFAULT_WALK_SPEED = 0.2f;
    private final Set<UUID> selectingPlayers = new HashSet<>();
    private final Set<UUID> selectedPlayers = new HashSet<>(); // Add this


    void bedInventory(Player player) {

        BedHopper plugin = JavaPlugin.getPlugin(BedHopper.class);
        var spawns = plugin.getPlayerSpawns(player.getUniqueId());

        int size = Math.max(9, (((spawns.size() + 1) + 8) / 9) * 9); // +1 for World Spawn item
        Inventory bedInventory = Bukkit.createInventory(player, size, BED_MENU_TITLE);

        for (int i = 0; i < spawns.size(); i++) {
            String spawnString = String.valueOf(spawns.get(i));

            ItemStack bedItem = new ItemStack(Material.RED_BED);
            ItemMeta meta = bedItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§f" + spawnString.replace(",", ", "));
                bedItem.setItemMeta(meta);
            }
            bedInventory.setItem(i, bedItem);
        }

        ItemStack worldSpawnItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta worldSpawnMeta = worldSpawnItem.getItemMeta();
        if (worldSpawnMeta != null) {
            worldSpawnMeta.setDisplayName("§aWorld Spawn");
            worldSpawnItem.setItemMeta(worldSpawnMeta);
        }
        bedInventory.setItem(spawns.size(), worldSpawnItem);

        selectingPlayers.add(player.getUniqueId());
        player.openInventory(bedInventory);
    }

    @EventHandler
    public void onPostRespawn(PlayerPostRespawnEvent event) {
        Player player = event.getPlayer();
        player.setWalkSpeed(0f);
        player.setCollidable(false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION, 1, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, false));

        bedInventory(player);
    }

    @EventHandler
    public void onBedSelected(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!"Select a Bed".equalsIgnoreCase(ChatColor.stripColor(event.getView().getTitle()))) return;

        selectedPlayers.add(player.getUniqueId());
        event.setCancelled(true);
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory()))
            return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta() == null || !clicked.getItemMeta().hasDisplayName())
            return;

        String raw = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).trim();

        if ("World Spawn".equalsIgnoreCase(raw)) {
            player.teleport(player.getWorld().getSpawnLocation());
            player.closeInventory();
            clearSelectionState(player);
            return;
        }

        String[] parts = raw.split(",\\s*");
        if (parts.length != 4) return;

        try {
            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);

            World world = Bukkit.getWorld(worldName);
            if (world == null) return;

            player.teleport(new Location(world, x + 0.5, y, z + 0.5));
            player.closeInventory();
            clearSelectionState(player);
        } catch (NumberFormatException ignored) {
            //Invalid title payload
        }
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!"Select a Bed".equalsIgnoreCase(ChatColor.stripColor(event.getView().getTitle()))) return;

        boolean madeSelection = selectedPlayers.remove(player.getUniqueId());

        if (!madeSelection) {
            // Re-open on next tick to avoid recursive close events
            Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(BedHopper.class), () -> {
                if (player.isOnline() && selectingPlayers.contains(player.getUniqueId())) {
                    bedInventory(player);
                }
            });
        } else {
            clearSelectionState(player);
        }
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearSelectionState(event.getPlayer());
    }

    private void clearSelectionState(Player player) {
        if (!selectingPlayers.remove(player.getUniqueId())) return;
        player.setWalkSpeed(DEFAULT_WALK_SPEED);
        player.setCollidable(true);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(event.getPlayer().getServer().getWorlds().get(0).getSpawnLocation());
    }
}
