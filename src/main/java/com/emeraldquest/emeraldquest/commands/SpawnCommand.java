package com.emeraldquest.emeraldquest.commands;

import com.emeraldquest.emeraldquest.EmeraldQuest;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class SpawnCommand extends CommandAction {
    private EmeraldQuest emeraldQuest;

    public SpawnCommand(EmeraldQuest plugin) {
        this.emeraldQuest = plugin;
    }
    public boolean run(CommandSender sender, Command cmd, String label, String[] args, Player player) {
	player.sendMessage(ChatColor.GREEN + "Teleporting to " + emeraldQuest.SERVER_NAME + "!!!");
        player.setMetadata("teleporting", new FixedMetadataValue(emeraldQuest, true));
        emeraldQuest.getServer().getScheduler().scheduleSyncDelayedTask(emeraldQuest, new Runnable() {
	        public void run() {
			Location location;
			location = emeraldQuest.getServer().getWorld("world").getSpawnLocation();
			player.teleport(location); //Teleport to the location
       		}
            }, 60L);
     return true;
    }
}
