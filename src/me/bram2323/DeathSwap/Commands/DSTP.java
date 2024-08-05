package me.bram2323.DeathSwap.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import me.bram2323.DeathSwap.Main;
import net.md_5.bungee.api.ChatColor;

public class DSTP implements TabExecutor {
	
	@SuppressWarnings("unused")
	private Main plugin;
	
	public DSTP(Main plugin) {
		this.plugin = plugin;
		plugin.getCommand("dstp").setExecutor(this);
		plugin.getCommand("dstp").setTabCompleter(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Jen hráči mohou použít tento příkaz");
			return true;
		}
		
		Player p = (Player) sender;
		
		
		if (Main.game.State == 0 || Main.game.InGame.contains(p.getUniqueId())) {
			p.sendMessage(ChatColor.RED + "Nejsi spectator!");
			return true;
		}
		
		if (args.length == 0) {
			p.sendMessage(ChatColor.RED + "Použití: /dstp <player>");
			return true;
		} else {
			Player t = Bukkit.getPlayer(args[0]);
			if (t == null) {
				p.sendMessage(ChatColor.RED + args[0] + " není online!");
			} else {
				p.teleport(t.getLocation());
				p.sendMessage(ChatColor.GREEN + "Teleportován k: " + args[0]);
			}
		}
		return true;
		
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command arg1, String arg2, String[] arg3) {
		List<String> list = new ArrayList<>();
		
		if (arg3.length == 1) {
			for (UUID t : Main.game.InGame) {
				list.add(Bukkit.getPlayer(t).getName());
			}
		}
		
		return list;
	}
}
