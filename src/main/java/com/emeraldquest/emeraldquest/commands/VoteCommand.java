package com.emeraldquest.emeraldquest.commands;

import com.emeraldquest.emeraldquest.EmeraldQuest;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;


public class VoteCommand extends CommandAction {
  private EmeraldQuest emeraldQuest;

  public VoteCommand(EmeraldQuest plugin) {
    emeraldQuest = plugin;
  }
    public boolean run(CommandSender sender, Command cmd, String label, String[] args, Player player) {
	String playerName = player.getName(); //(player.getName()
	//playerName = args[0]; // for testing
	if (emeraldQuest.didVote(playerName) == 0) {
		player.sendMessage(ChatColor.GREEN + "Please Vote here for Rewards! https://minecraft-mp.com/server/189942/vote/");
		player.sendMessage(ChatColor.AQUA + "Run command again after you vote for reward!");
	} else if (emeraldQuest.didVote(playerName) == 1) {
		if (emeraldQuest.giveVoteReward(player) == true){
		//player.sendMessage(ChatColor.GREEN + "You just got a reward for voting!");
		int claimed = emeraldQuest.claimVote(playerName);
		}

	} else if (emeraldQuest.didVote(playerName) == 2) {
		player.sendMessage(ChatColor.GREEN + "You've already voted, try again later please!");
	} 
        return true;
    }
}
