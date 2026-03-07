package com.jackhuey;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.stream.Collectors;

public class ListCommand implements CommandExecutor {
    private final BedHopper plugin;

    public ListCommand(BedHopper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this!");
            return true;
        }

        Player player = (Player) sender;

        player.sendMessage(player.getName()+"'s spawns: "+String.join(", ", plugin.getPlayerSpawns(player.getUniqueId()).stream().map(s -> s.replace(",", " ")).collect(Collectors.toList())));
        return true;
    }
}