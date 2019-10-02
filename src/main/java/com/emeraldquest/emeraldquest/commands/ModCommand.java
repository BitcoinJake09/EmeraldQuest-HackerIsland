package com.emeraldquest.emeraldquest.commands;

import com.emeraldquest.emeraldquest.EmeraldQuest;
import org.bukkit.Bukkit;
import org.bukkit.*;
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
import org.bukkit.WorldBorder;

public class ModCommand extends CommandAction {
    private EmeraldQuest emeraldQuest;

    public ModCommand(EmeraldQuest plugin) {
        this.emeraldQuest = plugin;
    }
    public boolean run(CommandSender sender, Command cmd, String label, String[] args, Player player) {
        if(args[0].equalsIgnoreCase("add")) {
            // Sub-command: /mod add

            if(EmeraldQuest.REDIS.exists("uuid:"+args[1])) {
                UUID uuid=UUID.fromString(EmeraldQuest.REDIS.get("uuid:"+args[1]));
                EmeraldQuest.REDIS.sadd("moderators",uuid.toString());
                sender.sendMessage(ChatColor.GREEN+EmeraldQuest.REDIS.get("name:"+uuid)+" added to moderators group");
		EmeraldQuest.REDIS.set("ModFlag "+uuid.toString(),"false");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED+"Cannot find player "+args[1]);
                return true;
            }
        } else if(args[0].equalsIgnoreCase("remove")) {
            // Sub-command: /mod del
            if(EmeraldQuest.REDIS.exists("uuid:"+args[1])) {
                UUID uuid=UUID.fromString(EmeraldQuest.REDIS.get("uuid:"+args[1]));
                EmeraldQuest.REDIS.srem("moderators",uuid.toString());
                return true;
            }
            return false;
        } else if(args[0].equalsIgnoreCase("list")) {
            // Sub-command: /mod list
            Set<String> moderators=EmeraldQuest.REDIS.smembers("moderators");
            for(String uuid:moderators) {
                sender.sendMessage(ChatColor.YELLOW+EmeraldQuest.REDIS.get("name:"+uuid));
            }
            return true;
        } else if(args[0].equalsIgnoreCase("flag")) {
	try{
	if (!(EmeraldQuest.REDIS.exists("ModFlag "+player.getUniqueId().toString()))){
		EmeraldQuest.REDIS.set("ModFlag "+player.getUniqueId().toString(),"true");
		player.sendMessage(ChatColor.RED + "ModFlag is ON");
	 }         
	else if (EmeraldQuest.REDIS.get("ModFlag "+player.getUniqueId().toString()).equals("false")){
		EmeraldQuest.REDIS.set("ModFlag "+player.getUniqueId().toString(),"true");
		player.sendMessage(ChatColor.RED + "ModFlag is ON");
           }
	 else {
		EmeraldQuest.REDIS.set("ModFlag "+player.getUniqueId().toString(),"false");
		player.sendMessage(ChatColor.RED + "ModFlag is OFF");
           }
		} catch (NullPointerException nullPointer)
		{
                	System.out.println("modflag: "+nullPointer);
		}

	return true;	
	} else if (args[0].equalsIgnoreCase("hq")){
            player.sendMessage(ChatColor.GREEN + "Teleporting to MOD-HQ...");
            player.setMetadata("teleporting", new FixedMetadataValue(emeraldQuest, true));
	    Location location=player.getLocation();
		if(emeraldQuest.getServer().getWorld("MOD-HQ") == null){
		location = new WorldCreator("MOD-HQ").createWorld().getSpawnLocation();
		WorldBorder wb = Bukkit.getWorld("MOD-HQ").getWorldBorder();
		wb.setCenter(0, 0);
		wb.setSize(512);
		System.out.println("MOD-HQ Size: " + wb.getSize() + " CenterX: " + wb.getCenter());
		} else {
			WorldBorder wb = Bukkit.getWorld("MOD-HQ").getWorldBorder();
		wb.setCenter(0, 0);
		wb.setSize(512);
		System.out.println("MOD-HQ Size: " + wb.getSize() + " CenterX: " + wb.getCenter());
		}
            World world = Bukkit.getWorld("MOD-HQ");

            location.setX(0);
            location.setY(3);
            location.setZ(0);

            final Location spawn=location;

            Chunk c = spawn.getChunk();
            if (!c.isLoaded()) {
                c.load();
            } emeraldQuest.getServer().getScheduler().scheduleSyncDelayedTask(emeraldQuest, new Runnable() {
	        public void run() {
			player.teleport(spawn); //Teleport to the location
       		}
            }, 60L);
	return true;		
      } else {
            return false;
        }
    }
}
