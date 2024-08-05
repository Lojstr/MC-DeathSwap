package me.bram2323.DeathSwap.Commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import me.bram2323.DeathSwap.Main;
import net.md_5.bungee.api.ChatColor;

public class DSStart implements TabExecutor {

	@SuppressWarnings("unused")
	private Main plugin;
	
	public DSStart(Main plugin) {
		this.plugin = plugin;
		plugin.getCommand("dsstart").setExecutor(this);
		plugin.getCommand("dsstart").setTabCompleter(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if (!sender.hasPermission("ds.start")) {
			sender.sendMessage(ChatColor.RED + "Nemáte povolení použít tento příkaz!");
			return true;
		}
		
		if (Main.game.State != 0) {
			sender.sendMessage(ChatColor.RED + "Hra je ještě aktivní!");
			return true;
		}
		
		Boolean dev =  (args.length == 1 && args[0].equals("true"));
		
		Main.game.Start(dev);
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		List<String> list = new ArrayList<>();
		return list;
	}
}
