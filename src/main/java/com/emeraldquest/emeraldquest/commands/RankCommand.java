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


public class RankCommand extends CommandAction {
    private EmeraldQuest emeraldQuest;

    public RankCommand(EmeraldQuest plugin) {
        this.emeraldQuest = plugin;
    }
    public boolean run(CommandSender sender, Command cmd, String label, String[] args, Player player) {
        if(args[0].equalsIgnoreCase("add")) {
            // Sub-command: /mod add

            if(EmeraldQuest.REDIS.exists("uuid:"+args[1])) {
                UUID uuid=UUID.fromString(EmeraldQuest.REDIS.get("uuid:"+args[1]));
                EmeraldQuest.REDIS.sadd("youtubers",uuid.toString());
                sender.sendMessage(ChatColor.GREEN+EmeraldQuest.REDIS.get("name:"+uuid)+" added to youtubers group");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED+"Cannot find player "+args[1]);
                return true;
            }
        } else if(args[0].equalsIgnoreCase("remove")) {
            // Sub-command: /mod del
            if(EmeraldQuest.REDIS.exists("uuid:"+args[1])) {
                UUID uuid=UUID.fromString(EmeraldQuest.REDIS.get("uuid:"+args[1]));
                EmeraldQuest.REDIS.srem("youtubers",uuid.toString());
                return true;
            }
            return false;
        } else if(args[0].equalsIgnoreCase("list")) {
            // Sub-command: /mod list
            Set<String> moderators=EmeraldQuest.REDIS.smembers("youtubers");
            for(String uuid:moderators) {
                sender.sendMessage(ChatColor.YELLOW+EmeraldQuest.REDIS.get("name:"+uuid));
            }
            return true;
        } else {
            return false;
        }
    }
}
