package me.bram2323.DeathSwap.Game;


import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.ScoreboardManager;

import me.bram2323.DeathSwap.Main;
import me.bram2323.DeathSwap.Commands.DSReady;
import me.bram2323.DeathSwap.Database.YmlFile;
import me.bram2323.DeathSwap.Settings.SettingsManager;
import net.md_5.bungee.api.ChatColor;

public class Events implements Listener{

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		
		Player p = event.getPlayer();
		
		if (Main.game.State != 0) {
			p.setGameMode(GameMode.SPECTATOR);
		}
	}
	
	@EventHandler
    public void onPlayerDisconect(PlayerQuitEvent event)
    {
		Player p = event.getPlayer();
		Game game = Main.game;
		
		DSReady.Ready.remove(p.getUniqueId());
		if (game.State != 0) {
			if (game.InGame.contains(p.getUniqueId())) {
				YmlFile ymlfile = new YmlFile();
				ymlfile.WriteData(p, "Stats.Disconnected", 1 + ymlfile.ReadData(p.getUniqueId(), "Stats.Disconnected"));
			}
			game.RemovePlayer(p);
			if ((Boolean)SettingsManager.instance.GetSetting("RandomSpawn")) p.teleport(Main.world.getSpawnLocation());
			ScoreboardManager manager = Bukkit.getScoreboardManager();
			p.setScoreboard(manager.getNewScoreboard());
			p.setGameMode(GameMode.SURVIVAL);
		}
    }
    
	private HashMap<UUID, Location> PlayerNextSpawnLocation = new HashMap<>();

	@EventHandler
    public void onPlayerDeath(PlayerDeathEvent event)
    {
		Player p = event.getEntity();
		Game game = Main.game;
		Location deathLocation = p.getLocation().clone();
		if (game.State != 0 && game.InGame.contains(p.getUniqueId())) {
			YmlFile ymlfile = new YmlFile();
			ymlfile.WriteData(p, "Stats.Deaths", 1 + ymlfile.ReadData(p.getUniqueId(), "Stats.Deaths"));
		}
		if (game.State != 0) {
			game.RemovePlayer(p);
			p.sendMessage(ChatColor.AQUA + "You can use /dstp <player> to teleport to players!");
			PlayerNextSpawnLocation.put(p.getUniqueId(), deathLocation);
			BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
    	    scheduler.scheduleSyncDelayedTask(Main.main, new Runnable() {
    	    	@Override
    	        public void run() {
					p.spigot().respawn();
    	        }
    	    }, 1);
		}
    }

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player p = event.getPlayer();
		if (PlayerNextSpawnLocation.containsKey(p.getUniqueId())){
			event.setRespawnLocation(PlayerNextSpawnLocation.get(p.getUniqueId()));
			PlayerNextSpawnLocation.remove(p.getUniqueId());
		}
	}
}
