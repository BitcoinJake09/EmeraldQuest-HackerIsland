package com.emeraldquest.emeraldquest.commands;

import com.emeraldquest.emeraldquest.EmeraldQuest;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class TpCommand extends CommandAction {
    private EmeraldQuest emeraldQuest;

    public TpCommand(EmeraldQuest plugin) {
        this.emeraldQuest = plugin;
    }
	
	public boolean run(CommandSender sender, Command cmd, String label, String[] args, Player player) {
		if(cmd.getName().equalsIgnoreCase("tp")&& sender instanceof Player)
			{
			 if(args.length == 2)
				{
				 Player victim = Bukkit.getPlayer(args[0]);
				 Player target = Bukkit.getPlayer(args[1]);
				 if (victim == null)
					{
						sender.sendMessage("No user " + args[0] + ".");
					}
					else if (target == null)
					{
					sender.sendMessage("No user " + args[1] + ".");
					}
					else
					{
					  Command.broadcastCommandMessage(sender, "Teleporting " + victim.getName() + " to " + target.getName());
					  victim.teleport(target);
					}
					return true;
				}
			 if(args.length == 1)
			 {
				 Player player2 = (Player) sender;		
				 Player slap = Bukkit.getPlayerExact(args[0]);
				 	if (slap == null)
					{
					sender.sendMessage("No user " + args[0] + ".");
					}
					else
					{
					  Command.broadcastCommandMessage(sender, "Teleporting to " + slap.getName());
					  player2.teleport(slap);
					}
					return true;
			 }
			}
		return false;

}
}

