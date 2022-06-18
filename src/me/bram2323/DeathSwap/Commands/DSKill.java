package me.bram2323.DeathSwap.Commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import me.bram2323.DeathSwap.Main;
import me.bram2323.DeathSwap.Game.Game;
import net.md_5.bungee.api.ChatColor;

public class DSKill implements TabExecutor {
	
	@SuppressWarnings("unused")
	private Main plugin;
	
	public DSKill(Main plugin) {
		this.plugin = plugin;
		plugin.getCommand("dskill").setExecutor(this);
		plugin.getCommand("dskill").setTabCompleter(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Only player may execute this command");
			return true;
		}
		
		Player p = (Player) sender;
		Game game = Main.game;
		
		if (game == null || game.State == 0) {
			p.sendMessage(ChatColor.RED + "Your not in a game!");
			return true;
		}
		
		p.setHealth(0);
		
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		List<String> list = new ArrayList<>();		
		return list;
	}
}
