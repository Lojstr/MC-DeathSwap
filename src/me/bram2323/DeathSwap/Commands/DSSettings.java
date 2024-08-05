package me.bram2323.DeathSwap.Commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import me.bram2323.DeathSwap.Main;
import me.bram2323.DeathSwap.Settings.SettingsManager;
import net.md_5.bungee.api.ChatColor;

public class DSSettings implements TabExecutor {
 
	@SuppressWarnings("unused")
	private Main plugin;
	
	public DSSettings(Main plugin) {
		this.plugin = plugin;
		plugin.getCommand("dssettings").setExecutor(this);
		plugin.getCommand("dssettings").setTabCompleter(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if (!sender.hasPermission("ds.settings")) {
			sender.sendMessage(ChatColor.RED + "Nemáte povolení pro použití tohoto příkazu!");
			return true;
		}
		
		if (args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Použití: /dssettings <set,get,help>");
			return true;
		} 
		else if(args.length == 1 && args[0].equals("help")) {
			sender.sendMessage(ChatColor.GOLD + "'dssettings' help:");
			sender.sendMessage(ChatColor.DARK_GREEN + "- help: " + ChatColor.GREEN + "ukáže tuto stránku");
			sender.sendMessage(ChatColor.DARK_GREEN + "- get: " + ChatColor.GREEN + "ukáže všechna nastavení hry");
			sender.sendMessage(ChatColor.DARK_GREEN + "- set: " + ChatColor.GREEN + "změní nastavení hry");
			sender.sendMessage(ChatColor.AQUA + "(použijte '/dssettings help set' pro list všech nastavení)");
		} 
		else if (args.length == 2 && args[0].equals("help")) {	
			sender.sendMessage(ChatColor.GOLD + "'dssettings set' help:");
			sender.sendMessage(SettingsManager.instance.GetHelpString());
			sender.sendMessage(ChatColor.RED + "Čas je měřen v tick! (20 Ticků je 1 sekunda)");
			sender.sendMessage(ChatColor.AQUA + "(použijte '/dssettings help set TeleportMode' pro list všech teleport módů)");
		} 
		else if (args.length >= 3 && args[0].equals("help")) {
			sender.sendMessage(ChatColor.GOLD + "'dssettings set TeleportMode' help:");
			sender.sendMessage(ChatColor.DARK_GREEN + "- random: " + ChatColor.GREEN + "Hráči budou teleportováni random (Pokud se nenajde způsob ve 500 pokusech použije random circle metodu!)");
			sender.sendMessage(ChatColor.DARK_GREEN + "- circle: " + ChatColor.GREEN + "Hráči se vymění v náhodném kruhu, který je generován na začátku hry");
			sender.sendMessage(ChatColor.DARK_GREEN + "- random_circle: " + ChatColor.GREEN + "Metoda kruhu pouze kruh se změní, když dojde k výměně");
			sender.sendMessage(ChatColor.DARK_GREEN + "- fixed_circle: " + ChatColor.GREEN + "Metoda kruhu pouze kruh se nezmění mezi hrami");
			sender.sendMessage(ChatColor.DARK_GREEN + "- pairs: " + ChatColor.GREEN + "Hráči budou teleportováni ve dvojicích (pokud je počet hráčů lichý, bude vytvořena trojice)");
		} 
		else if (args.length == 1 || args.length == 2){
			if (args[0].equals("get")) {
				sender.sendMessage(SettingsManager.instance.GetSettingsString());
			} 
			else if(args[0].equals("set")) {
				sender.sendMessage(ChatColor.RED + "Použití: /dssettings set <setting> <value> (Čas musí být tickech, 20 Ticků = 1 sekunda!)");
			} 
			else {
				sender.sendMessage(ChatColor.RED + "Použití: /dssettings <set,get,help>");
			}
		} 
		else if (args.length >= 3) {
			if (args[0].equals("get")) {
				sender.sendMessage(SettingsManager.instance.GetSettingsString());
			} 
			else if(args[0].equals("set")) {
				if (args[1].toLowerCase().equals("teleportmode")) sender.sendMessage(SettingsManager.instance.SetTeleportMode(args[2]));
				else sender.sendMessage(SettingsManager.instance.SetSetting(args[1], args[2]));
				return true;
			} 
			else {
				sender.sendMessage(ChatColor.RED + "Použití: /dssettings <set,get,help>");
			}
		}
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		
		List<String> list = new ArrayList<>();
		
		if(args.length == 1) {
			list.add("get");
			list.add("set");
			list.add("help");
		}
		else if(args.length == 2 && args[0].equals("set")) {
			return SettingsManager.instance.GetSettingNamesList(args[1]);
		}
		else if(args.length == 3 && args[0].equals("set") && args[1].equals("World")) {
			for (World world : Bukkit.getServer().getWorlds()) {
				list.add(world.getName());
			}
		}
		else if(args.length == 3 && args[0].equals("set") && args[1].equals("TeleportMode")) {
			list.add("circle");
			list.add("random");
			list.add("random_circle");
			list.add("fixed_circle");
			list.add("pairs");
		}
		else if(args.length == 3 && args[0].equals("set") && SettingsManager.instance.SettingExists(args[1])) {
			return SettingsManager.instance.GetSettingTabcomplete(args[1]);
		}
		else if(args.length == 2 && args[0].equals("help")) {
			list.add("set");
		}
		else if(args.length == 3 && args[0].equals("help")) {
			list.add("TeleportMode");
		}
		
		return list;
	}

}
