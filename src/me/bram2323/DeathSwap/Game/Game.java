package me.bram2323.DeathSwap.Game;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.advancement.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.Vector;

import io.netty.util.internal.ThreadLocalRandom;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import me.bram2323.DeathSwap.Main;
import me.bram2323.DeathSwap.Database.YmlFile;
import me.bram2323.DeathSwap.Settings.SettingsManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Game {

	Objective objective;
	
	public World world;
	
	public static HashMap<UUID, Location> Locations = new HashMap<UUID, Location>();
	
	public List<UUID> InGame = new ArrayList<>();
	public List<UUID> RandomTP = new ArrayList<>();
	public List<Location> RandomLoc = new ArrayList<>();
	
	public List<UUID[]> Pairs = new ArrayList<>();
	
	public int State = 0;

	int Try = 0;
	int Trys = 0;
	Boolean TrySucces;
	public Boolean dev;
	
	int TotalTimer = 0;
	int MainTimer = 0;
	int Timer = 0;
	int Seconds = 0;
	int Minutes = 0;
	int UntilSwap = 0;
	int TotalSwap = 0;
	
	public void Start(Boolean devMode) {
		
		dev = devMode;
		
		if (State != 0) {
			return;
		}
		
		if (!(Bukkit.getOnlinePlayers().size() > 1 || (dev && Bukkit.getOnlinePlayers().size() > 0))) {
			Bukkit.broadcastMessage(ChatColor.RED + "Potřebujete alespoň 2 hráče pro spuštění hry!");
			return;
		}
		
		world = Bukkit.getServer().getWorld((String)SettingsManager.instance.GetSetting("World"));
		if (world == null) world = Bukkit.getServer().getWorlds().get(0);
		
		Bukkit.broadcastMessage("DeathSwap za chvíli začne!");
		world.setTime(0);
		
		InGame.clear();
		Locations.clear();
		
		ScoreboardManager manager = Bukkit.getScoreboardManager();
		Scoreboard board = manager.getMainScoreboard();
		if (board.getObjective("showhealth") != null) board.getObjective("showhealth").unregister();
		objective = board.registerNewObjective("showhealth", "health", "Health");
		objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
		objective.setRenderType(RenderType.HEARTS);
		
		for (Player p : Bukkit.getOnlinePlayers())
		{
			InGame.add(p.getUniqueId());
			p.spigot().respawn();
			p.setGameMode(GameMode.SURVIVAL);
			if ((Boolean)SettingsManager.instance.GetSetting("ClearInv"))p.getInventory().clear();
			for (PotionEffect effect : p.getActivePotionEffects()) p.removePotionEffect(effect.getType());
			p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int)SettingsManager.instance.GetSetting("Safe"), 255));
			if ((Boolean)SettingsManager.instance.GetSetting("RandomSpawn")) p.teleport(world.getSpawnLocation());
			p.setHealth(19);
			p.setSaturation(20);
			p.setFoodLevel(20);
			p.setFallDistance(0);
			p.setExp(0);
			p.setLevel(0);
			p.setFireTicks(0);
			YmlFile ymlfile = new YmlFile();
			ymlfile.WriteData(p, "Stats.Games", 1 + ymlfile.ReadData(p.getUniqueId(), "Stats.Games"));

			if ((Boolean)SettingsManager.instance.GetSetting("RevokeAdvancements")){
				Iterator<Advancement> advancements = Bukkit.getServer().advancementIterator();
				while (advancements.hasNext()) {
					AdvancementProgress progress = p.getAdvancementProgress(advancements.next());
					for (String s : progress.getAwardedCriteria())
						progress.revokeCriteria(s);
				}
			}
		}
		
		if (SettingsManager.instance.GetTeleportMode() == 4) MakePairs();
		
		for(World w : Bukkit.getWorlds()){
			for(Entity e : w.getEntities()) {
				if(e.getType() == EntityType.ENDER_PEARL) {
					e.remove();
				}
			}
		}
		
		if (SettingsManager.instance.GetTeleportMode() != 3) Collections.shuffle(InGame);
		
		Random rand = new Random();
		
		UntilSwap = rand.nextInt((int)SettingsManager.instance.GetSetting("MaxTimer") + 1 - (int)SettingsManager.instance.GetSetting("MinTimer")) + (int)SettingsManager.instance.GetSetting("MinTimer");
		TotalTimer = 0;
		Timer = 0;
		Seconds = 0;
		Minutes = 0;
		MainTimer = 0;
		Trys = 0;
		TotalSwap = 0;
		
		if ((Boolean)SettingsManager.instance.GetSetting("RandomSpawn")) for (Player p : Bukkit.getOnlinePlayers()) p.teleport(GetRandomLocation(world));
		
		int Loaded = 0;
		for(Player p : Bukkit.getOnlinePlayers()) {
			Locations.put(p.getUniqueId(), p.getLocation());
			Chunk ch = p.getLocation().getChunk();
			p.setGameMode(GameMode.SPECTATOR);
			if ((int)SettingsManager.instance.GetSetting("LoadChunkRadius") > 0)
			for	(int i = (int)SettingsManager.instance.GetSetting("LoadChunkRadius") * -1 + 1; i < (int)SettingsManager.instance.GetSetting("LoadChunkRadius"); i++) {
				for	(int j = (int)SettingsManager.instance.GetSetting("LoadChunkRadius") * -1 + 1; j < (int)SettingsManager.instance.GetSetting("LoadChunkRadius"); j++) {
					Chunk chunk = Bukkit.getWorld(ch.getWorld().getUID()).getChunkAt(ch.getX() + i, ch.getX() + j);
					chunk.load(true);
					if (Bukkit.getWorld(ch.getWorld().getUID()).getChunkAt(ch.getX() + i, ch.getX() + j).isLoaded()) Loaded++;
					for(Player t : Bukkit.getOnlinePlayers()) {
						t.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("Generating chunks: " + ChatColor.GREEN + Loaded + ChatColor.WHITE + "/" + ChatColor.GOLD + Bukkit.getOnlinePlayers().size() * ((int)SettingsManager.instance.GetSetting("LoadChunkRadius") * 2 - 1) * ((int)SettingsManager.instance.GetSetting("LoadChunkRadius") * 2 - 1)));
					}
				}
			}
		}
		Loaded = 0;
		if ((int)SettingsManager.instance.GetSetting("LoadChunkRadius") > 0)
		for(Player p : Bukkit.getOnlinePlayers()) {
			Chunk ch = p.getLocation().getChunk();
			for	(int i = (int)SettingsManager.instance.GetSetting("LoadChunkRadius") * -1 + 1; i < (int)SettingsManager.instance.GetSetting("LoadChunkRadius"); i++) {
				for	(int j = (int)SettingsManager.instance.GetSetting("LoadChunkRadius") * -1 + 1; j < (int)SettingsManager.instance.GetSetting("LoadChunkRadius"); j++) {
					Chunk chunk = Bukkit.getWorld(ch.getWorld().getUID()).getChunkAt(ch.getX() + i, ch.getX() + j);
					SendChunk(chunk, p);
					Loaded++;
					for(Player t : Bukkit.getOnlinePlayers()) {
						t.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("Sending chunks: " + ChatColor.GREEN + Loaded + ChatColor.WHITE + "/" + ChatColor.GOLD + Bukkit.getOnlinePlayers().size() * ((int)SettingsManager.instance.GetSetting("LoadChunkRadius") * 2 - 1) * ((int)SettingsManager.instance.GetSetting("LoadChunkRadius") * 2 - 1)));
					}
				}
			}
		}
		
		Bukkit.broadcastMessage(ChatColor.GREEN + "Death Swap, coded by " + ChatColor.GOLD + "bram2323" + ChatColor.GREEN + ", přeložil" + ChatColor.GOLD + "Lojstr" + ChatColor.GREEN + ":\n" + ChatColor.WHITE + SettingsManager.instance.GetSettingsGameString() + ChatColor.GOLD + "\nGood luck!");
		
		String devm = "";
		Boolean first = true;
		
		for (UUID uuid : InGame) {
			if (!first) devm += ", ";
			devm += Bukkit.getPlayer(uuid).getName();
			first = false;
		}
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (p.isOp() && dev) {
				p.sendMessage(ChatColor.DARK_GREEN + "Další kruh teleportace je za: " + devm);
			}
		}
		
		State = 1;
		Ticker();
	}
	
	public void Stop() {
		
		if (State < 1) {
			return;
		}
		
		if (InGame.size() == 1) {
			Player tt = Bukkit.getPlayer(InGame.get(0));
			Bukkit.broadcastMessage(ChatColor.GOLD + tt.getName() + ChatColor.RESET + ChatColor.GREEN + " vyhrál hru!");
			
			for (Player t : Bukkit.getOnlinePlayers())
			{
				if ((Boolean)SettingsManager.instance.GetSetting("RandomSpawn")) t.teleport(world.getSpawnLocation());
				if (!t.getUniqueId().equals(tt.getUniqueId())) {
					t.teleport(world.getSpawnLocation());
					t.sendTitle(ChatColor.GOLD + tt.getName() + ChatColor.RESET + ChatColor.GREEN + " vyhrál!", ChatColor.DARK_RED + "Prohrál jsi...", 10, 80 , 10);
					t.playSound(t.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F);
				} else {
					t.sendTitle(ChatColor.GOLD + "Vyhrál" + ChatColor.RESET + ChatColor.GREEN + " jsi!", ChatColor.DARK_GREEN + "Dobrá práce!", 10, 80 , 10);
					t.playSound(t.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
					YmlFile ymlfile = new YmlFile();
					ymlfile.WriteData(t, "Stats.Wins", 1 + ymlfile.ReadData(t.getUniqueId(), "Stats.Wins"));
					ymlfile.WriteData(t, "Stats.Time", ymlfile.ReadData(t.getUniqueId(), "Stats.Time") + TotalTimer);
				}
			}
		} else {
			for (Player p : Bukkit.getOnlinePlayers())
			{
				p.teleport(world.getSpawnLocation());
				p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F);
				YmlFile ymlfile = new YmlFile();
				ymlfile.WriteData(p, "Stats.Disconnected", 1 + ymlfile.ReadData(p.getUniqueId(), "Stats.Disconnected"));
			}
			Bukkit.broadcastMessage("DeathSwap byl zastaven!");
		}
		
		ScoreboardManager manager = Bukkit.getScoreboardManager();
		objective.unregister();
		
		for (Player p : Bukkit.getOnlinePlayers())
		{
			p.setGameMode(GameMode.SURVIVAL);
			p.setFallDistance(0);
			p.setHealth(19);
			p.setSaturation(20);
			p.setFoodLevel(20);
			p.setFireTicks(0);
			p.setScoreboard(manager.getMainScoreboard());
		}
		
		for(World w : Bukkit.getWorlds()){
			for(Entity e : w.getEntities()) {
				if(e.getType() == EntityType.ENDER_PEARL) {
					e.remove();
				}
			}
		}
		
		InGame.clear();
		State = 0;
	}
	
	public void RemovePlayer(Player p) {
		
		if (State != 1 || !InGame.contains(p.getUniqueId())) {
			return;
		}
		
		p.setGameMode(GameMode.SPECTATOR);
		p.setFireTicks(0);
		
		InGame.remove(p.getUniqueId());
		Bukkit.broadcastMessage(ChatColor.DARK_RED + p.getName() + ChatColor.RED + " byl vyřazen! " + ChatColor.DARK_GREEN + InGame.size() + ChatColor.GREEN + " Hráčů zbývá!");
		
		p.getWorld().strikeLightningEffect(p.getLocation());
		
		YmlFile ymlfile = new YmlFile();
		ymlfile.WriteData(p, "Stats.Time", ymlfile.ReadData(p.getUniqueId(), "Stats.Time") + TotalTimer);
		
		if (SettingsManager.instance.GetTeleportMode() == 4) UpdatePairs(p.getUniqueId());
		
		if (InGame.size() <= 1) {
			State = 2;
		}
	}

	public void MakePairs() {
		
		if (InGame.size() < 2) {
			return;
		}
		
		Pairs.clear();
		RandomTP.clear();
		RandomTP.addAll(InGame);
		Collections.shuffle(RandomTP);
		
		if (RandomTP.size() % 2 != 0) {
			Pairs.add(new UUID[] {RandomTP.get(RandomTP.size() - 1), RandomTP.get(RandomTP.size() - 2), RandomTP.get(RandomTP.size() - 3)});
			Player p1 = Bukkit.getPlayer(RandomTP.get(RandomTP.size() - 1));
			Player p2 = Bukkit.getPlayer(RandomTP.get(RandomTP.size() - 2));
			Player p3 = Bukkit.getPlayer(RandomTP.get(RandomTP.size() - 3));
			p1.sendMessage(ChatColor.GREEN + "Tvůj pár: " + ChatColor.GOLD + p3.getName() + ChatColor.GREEN + " --> " + ChatColor.GOLD + p1.getName() + ChatColor.GREEN + " --> " + ChatColor.GOLD + p2.getName());
			p2.sendMessage(ChatColor.GREEN + "Tvůj pár: " + ChatColor.GOLD + p1.getName() + ChatColor.GREEN + " --> " + ChatColor.GOLD + p2.getName() + ChatColor.GREEN + " --> " + ChatColor.GOLD + p3.getName());
			p3.sendMessage(ChatColor.GREEN + "Tvůj pár: " + ChatColor.GOLD + p2.getName() + ChatColor.GREEN + " --> " + ChatColor.GOLD + p3.getName() + ChatColor.GREEN + " --> " + ChatColor.GOLD + p1.getName());
			int Size = RandomTP.size();
			RandomTP.remove(Size - 1);
			RandomTP.remove(Size - 2);
			RandomTP.remove(Size - 3);
		}
		
		while (RandomTP.size() >= 2) {
			Pairs.add(new UUID[] {RandomTP.get(RandomTP.size() - 1), RandomTP.get(RandomTP.size() - 2)});
			Player p1 = Bukkit.getPlayer(RandomTP.get(RandomTP.size() - 1));
			Player p2 = Bukkit.getPlayer(RandomTP.get(RandomTP.size() - 2));
			p1.sendMessage(ChatColor.GREEN + "Tvůj pár: " + ChatColor.GOLD + p1.getName() + ChatColor.GREEN + " <--> " + ChatColor.GOLD + p2.getName());
			p2.sendMessage(ChatColor.GREEN + "Tvůj pár: " + ChatColor.GOLD + p2.getName() + ChatColor.GREEN + " <--> " + ChatColor.GOLD + p1.getName());
			int Size = RandomTP.size();
			RandomTP.remove(Size - 1);
			RandomTP.remove(Size - 2);
		}
		
		for (Player p : Bukkit.getOnlinePlayers()) {
			if ((p.isOp() && dev) || !InGame.contains(p.getUniqueId())) ShowPairs(p);
		}
	}
	
	public void ShowPairs(Player p) {
		if (InGame.size() < 2) {
			p.sendMessage(ChatColor.RED + "Nejsou žádné páry!");
			return;
		}
		
		p.sendMessage(ChatColor.DARK_GREEN + "Páry:");
		
		for (UUID[] uuids : Pairs) {
			Player p1 = Bukkit.getPlayer(uuids[0]);
			Player p2 = Bukkit.getPlayer(uuids[1]);
			if (uuids.length == 2) {
				p.sendMessage(ChatColor.GOLD + p1.getName() + ChatColor.GREEN + " <--> " + ChatColor.GOLD + p2.getName());
			} else {
				Player p3 = Bukkit.getPlayer(uuids[2]);
				p.sendMessage(ChatColor.GOLD + p1.getName() + ChatColor.GREEN + " --> " + ChatColor.GOLD + p2.getName() + ChatColor.GREEN + " --> " + ChatColor.GOLD + p3.getName());
			}
		}
	}
	
	
	public void Teleport0() {
		
		RandomTP.clear();
		RandomTP.addAll(InGame);
		
		Try = 0;
		TrySucces = true;
		
		Teleport00();
		
		if (!TrySucces) {
			Collections.shuffle(InGame);
			Teleport1();
			return;
		}
		
		for (UUID uuid : InGame) {
			Player p = Bukkit.getPlayer(uuid);
			
			Player t = Bukkit.getPlayer(RandomTP.get(InGame.indexOf(uuid)));
			
			p.teleport(RandomLoc.get(InGame.indexOf(uuid)));
			p.sendMessage(ChatColor.GREEN + "Byl jsi teleportován k " + ChatColor.GOLD + t.getName());
			p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
			p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int)SettingsManager.instance.GetSetting("Safe"), 255));
		}
	}
	
	public void Teleport00() {
		Try++;
		
		if (Try == 500) {
			Trys++;
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (p.isOp() && dev) {
					p.sendMessage(ChatColor.DARK_RED + "Couldn't find random assortment in " + Try + " try's! Used random circle method instead!");
					if (Trys == 10) {
						p.sendMessage(ChatColor.DARK_RED + "This has happend 10 times now, you can change the teleport mode to random circle to get rid of these messages!");
					}
				}
			}
			TrySucces = false;
			return;
		}
		
		RandomLoc.clear();
		Collections.shuffle(RandomTP);
		for (UUID uuid : InGame) {
			
			if (uuid.equals(RandomTP.get(InGame.indexOf(uuid)))) {
				Teleport00();
			}
			
			Player t = Bukkit.getPlayer(RandomTP.get(InGame.indexOf(uuid)));
			RandomLoc.add(t.getLocation());
		}
	}
	
	
	public void Teleport1() {
		
		Location location = null;
		String name = "";
		
		for (UUID uuid : InGame)
		{
			Player p = Bukkit.getPlayer(uuid);
			
			if (InGame.indexOf(uuid) == 0) {
				location = p.getLocation();
				name = p.getName();
			}
			
			if (InGame.indexOf(uuid) + 1 == InGame.size()) {
				p.teleport(location);
				p.sendMessage(ChatColor.GREEN + "Byl jsi teleportován k " + ChatColor.GOLD + name);
				p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
				p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int)SettingsManager.instance.GetSetting("Safe"), 255));
				continue;
			}
			
			Player t = Bukkit.getPlayer((UUID)InGame.get(InGame.indexOf(uuid) + 1));
			
			p.teleport(t.getLocation());
			p.sendMessage(ChatColor.GREEN + "Byl jsi teleportován k " + ChatColor.GOLD + t.getName());
			p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
			p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int)SettingsManager.instance.GetSetting("Safe"), 255));
		}
	}
	
	
	public void Teleport2 () {
		
		if (InGame.size() <= 1) {
			Teleport1();
			return;
		}
		
		for (UUID[] uuids : Pairs) {
			
			Player p1 = Bukkit.getPlayer(uuids[0]);
			Player p2 = Bukkit.getPlayer(uuids[1]);
			Location location = p1.getLocation();
			
			p1.teleport(p2.getLocation());
			p1.sendMessage(ChatColor.GREEN + "Byl jsi teleportován k " + ChatColor.GOLD + p2.getName());
			p1.playSound(p1.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
			p1.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int)SettingsManager.instance.GetSetting("Safe"), 255));
			
			if (uuids.length > 2) {
				Player p3 = Bukkit.getPlayer(uuids[2]);
				
				p2.teleport(p3.getLocation());
				p2.sendMessage(ChatColor.GREEN + "Byl jsi teleportován k " + ChatColor.GOLD + p3.getName());
				p2.playSound(p2.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
				p2.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int)SettingsManager.instance.GetSetting("Safe"), 255));
				
				p3.teleport(location);
				p3.sendMessage(ChatColor.GREEN + "Byl jsi teleportován k " + ChatColor.GOLD + p1.getName());
				p3.playSound(p3.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
				p3.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int)SettingsManager.instance.GetSetting("Safe"), 255));
				continue;
			} else {
				p2.teleport(location);
				p2.sendMessage(ChatColor.GREEN + "Byl jsi teleportován k " + ChatColor.GOLD + p1.getName());
				p2.playSound(p2.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
				p2.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int)SettingsManager.instance.GetSetting("Safe"), 255));
			}
		}
	}
	
	public void UpdatePairs (UUID uuid) {
		
		if (InGame.size() <= 1) {
			return;
		}
		
		for (UUID[] uuids : Pairs) {
			int Index = Pairs.indexOf(uuids);
			if (uuids.length == 3) {
				if (uuids[0].equals(uuid)) {
					uuids = new UUID[] {uuids[1], uuids[2]};
				} else if (uuids[1].equals(uuid)) {
					uuids = new UUID[] {uuids[0], uuids[2]};
				} else if (uuids[2].equals(uuid)) {
					uuids = new UUID[] {uuids[0], uuids[1]};
				}
				Pairs.set(Index, uuids);
			} else {
				if (uuids[0].equals(uuid)) {
					uuids = new UUID[] {uuids[1]};
				} else if (uuids[1].equals(uuid)) {
					uuids = new UUID[] {uuids[0]};
				}
				Pairs.set(Index, uuids);
			}
		}
		
		UUID Change = null;
		UUID[] Change2 = null;
		
		if (InGame.size() % 2 == 0) {
			for (UUID[] uuids : Pairs) {
				if (uuids.length == 1) {
					Change = uuids[0];
					Pairs.remove(uuids);
				} else if (uuids.length == 3) {
					Change2 = uuids.clone();
					Pairs.remove(uuids);
				}
			}
			if (Change != null) {
				Pairs.add(new UUID[] {Change, Change2[2]});
				Pairs.add(new UUID[] {Change2[0], Change2[1]});
			}
		} else {
			for (UUID[] uuids : Pairs) {
				if (uuids.length == 1) {
					Change = uuids[0];
					Pairs.remove(uuids);
				} else if (Change2 == null) {
					Change2 = uuids.clone();
					Pairs.remove(uuids);
				}
			}
			Pairs.add(new UUID[] {Change2[0], Change2[1], Change});
		}
		
		for (UUID[] uuids : Pairs) {
			if (uuids.length == 2) {
				Player p1 = Bukkit.getPlayer(uuids[0]);
				Player p2 = Bukkit.getPlayer(uuids[1]);
				p1.sendMessage(ChatColor.GREEN + "Tvůj pár: " + ChatColor.GOLD + p1.getName() + ChatColor.GREEN + " <--> " + ChatColor.GOLD + p2.getName());
				p2.sendMessage(ChatColor.GREEN + "Tvůj pár: " + ChatColor.GOLD + p2.getName() + ChatColor.GREEN + " <--> " + ChatColor.GOLD + p1.getName());
			} else {
				Player p1 = Bukkit.getPlayer(uuids[0]);
				Player p2 = Bukkit.getPlayer(uuids[1]);
				Player p3 = Bukkit.getPlayer(uuids[2]);
				p1.sendMessage(ChatColor.GREEN + "Tvůj pár: " + ChatColor.GOLD + p3.getName() + ChatColor.GREEN + " --> " + ChatColor.GOLD + p1.getName() + ChatColor.GREEN + " --> " + ChatColor.GOLD + p2.getName());
				p2.sendMessage(ChatColor.GREEN + "Tvůj pár: " + ChatColor.GOLD + p1.getName() + ChatColor.GREEN + " --> " + ChatColor.GOLD + p2.getName() + ChatColor.GREEN + " --> " + ChatColor.GOLD + p3.getName());
				p3.sendMessage(ChatColor.GREEN + "Tvůj pár: " + ChatColor.GOLD + p2.getName() + ChatColor.GREEN + " --> " + ChatColor.GOLD + p3.getName() + ChatColor.GREEN + " --> " + ChatColor.GOLD + p1.getName());
			}
		}
		
		for (Player p : Bukkit.getOnlinePlayers()) {
			if ((p.isOp() && dev) || !InGame.contains(p.getUniqueId())) ShowPairs(p);
		}
	}
	

	public void Timer() {
		
		if (!Locations.isEmpty()) {
			int Total = 0;
			int Loaded = 0;
			for(UUID uuid : Locations.keySet()) {
				Player p = Bukkit.getServer().getPlayer(uuid);
				Location loc = Locations.get(uuid);
				Chunk ch = loc.getChunk();
				
				if((float) p.getLocation().getX() != (float) loc.getX() || (float) p.getLocation().getY() != (float) loc.getY() || (float) p.getLocation().getZ() != (float) loc.getZ()) {
					loc.setYaw(p.getLocation().getYaw());
					loc.setPitch(p.getLocation().getPitch());
					p.teleport(loc);
				}
				
				p.setVelocity(new Vector (0, 0, 0));
				
				if ((int)SettingsManager.instance.GetSetting("LoadChunkRadius") > 0)
				for	(int i = (int)SettingsManager.instance.GetSetting("LoadChunkRadius") * -1 + 1; i < (int)SettingsManager.instance.GetSetting("LoadChunkRadius"); i++) {
					for	(int j = (int)SettingsManager.instance.GetSetting("LoadChunkRadius") * -1 + 1; j < (int)SettingsManager.instance.GetSetting("LoadChunkRadius"); j++) {
						Total++;
						Chunk chunk = Bukkit.getWorld(ch.getWorld().getUID()).getChunkAt(ch.getX() + i, ch.getX() + j);
						if (chunk.isLoaded()) Loaded++;
					}
				}
			}
			Timer++;
			for (Player t : Bukkit.getOnlinePlayers()) {
				t.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("Načítání chunků: " + ChatColor.GREEN + Loaded + ChatColor.WHITE + "/" + ChatColor.GOLD + Total + ChatColor.GREEN + " Start za: " + (10 - Timer / 20)));
				
			}
			if (Total == Loaded && Timer > 200) {
				Locations.clear();
				for (Player t : Bukkit.getOnlinePlayers()) {
					t.setGameMode(GameMode.SURVIVAL);
				}
				Timer = 0;
			}
			return;
		}
		
		if (State != 1) {
			return;
		}
		
		UntilSwap--;
		MainTimer++;
		TotalTimer++;
		Timer++;
		if (Timer == 20) {
			Seconds++;
			Timer = 0;
		}
		if (Seconds == 60) {
			Minutes++;
			Seconds = 0;
		}
		
		String TimerMessage = "";
		if ((Boolean)SettingsManager.instance.GetSetting("ShowTime"))
		if (Seconds <= 9) {
			TimerMessage += Minutes + ":0" + Seconds;
		} else {
			TimerMessage += Minutes + ":" + Seconds;
		}
		
		TimerMessage += ChatColor.GRAY + " Swaps [" + ChatColor.DARK_GREEN + TotalSwap + ChatColor.GRAY + "]";
		
		if ((int)SettingsManager.instance.GetSetting("Warning") >= UntilSwap) {
			BigDecimal US = new BigDecimal((float)UntilSwap / 20);
			TimerMessage += ChatColor.DARK_RED + " Výměna za: " + US.setScale(1, RoundingMode.HALF_UP) + " Sekund!";
		}
		
		for(Player p : Bukkit.getOnlinePlayers()) {
			
			if (MainTimer < (int)SettingsManager.instance.GetSetting("MinTimer") && (Boolean)SettingsManager.instance.GetSetting("ShowTime")) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.GREEN + "[bezpečné] Čas od výměny: " + TimerMessage));
			} else if ((Boolean)SettingsManager.instance.GetSetting("ShowTime")) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.RED + "[nebezpečné] Čas od výměny: " + TimerMessage));
			} else {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.GREEN + TimerMessage));
			}
		}
		
		if (UntilSwap == 0) {
			Bukkit.broadcastMessage(ChatColor.BOLD + "SWAP!");
			Timer = 0;
			Seconds = 0;
			Minutes = 0;
			MainTimer = 0;
			TotalSwap++;
			int EPearl = 0;
			if ((Boolean)SettingsManager.instance.GetSetting("KillPearls")) {
				for(World w : Bukkit.getWorlds()){
					for(Entity e : w.getEntities()) {
						if(e.getType() == EntityType.ENDER_PEARL) {
							e.remove();
							EPearl++;
						}
					}
				}
			}
			if(EPearl > 0) {
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (p.isOp() && dev) {
						p.sendMessage("Odstraněny " + EPearl + " enderpearly!");
					}
				}
			}
			for (UUID uuid : InGame) {
				Player p = Bukkit.getPlayer(uuid);
				YmlFile ymlfile = new YmlFile();
				ymlfile.WriteData(p, "Stats.Swaps", 1 + ymlfile.ReadData(p.getUniqueId(), "Stats.Swaps"));
			}
		}
		
		if (UntilSwap == 0 && SettingsManager.instance.GetTeleportMode() == 0) {
			
			Teleport0();
			Random rand = new Random();
			UntilSwap = rand.nextInt((int)SettingsManager.instance.GetSetting("MaxTimer") + 1 - (int)SettingsManager.instance.GetSetting("MinTimer")) + (int)SettingsManager.instance.GetSetting("MinTimer");
			for (Player p : Bukkit.getOnlinePlayers()) {
				Chunk chunk = p.getLocation().getChunk();
				SendChunk(chunk, p);
			}
			
		} else if (UntilSwap == 0 && SettingsManager.instance.GetTeleportMode() != 0 && SettingsManager.instance.GetTeleportMode() != 4) {
			
			Teleport1();
			Random rand = new Random();
			UntilSwap = rand.nextInt((int)SettingsManager.instance.GetSetting("MaxTimer") + 1 - (int)SettingsManager.instance.GetSetting("MinTimer")) + (int)SettingsManager.instance.GetSetting("MinTimer");
			if (SettingsManager.instance.GetTeleportMode() == 2) Collections.shuffle(InGame);
			Boolean first = true;
			String devm = "";
			for (UUID uuid : InGame) {
				if (!first) devm += ", ";
				devm += Bukkit.getPlayer(uuid).getName();
				first = false;
			}
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (p.isOp() && dev) p.sendMessage(ChatColor.DARK_GREEN + "Další kruh teleportace je za: " + devm);
				Chunk chunk = p.getLocation().getChunk();
				SendChunk(chunk, p);
			}
		} else if (UntilSwap == 0 && SettingsManager.instance.GetTeleportMode() == 4) {
			
			Teleport2();
			Random rand = new Random();
			UntilSwap = rand.nextInt((int)SettingsManager.instance.GetSetting("MaxTimer") + 1 - (int)SettingsManager.instance.GetSetting("MinTimer")) + (int)SettingsManager.instance.GetSetting("MinTimer");
			
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (p.isOp() && dev) ShowPairs(p);
				Chunk chunk = p.getLocation().getChunk();
				SendChunk(chunk, p);
			}
		}
	}
	
	public void Ticker() {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
	    scheduler.scheduleSyncDelayedTask(Main.main, new Runnable() {
	    	@Override
	        public void run() {
	            
	    		if (State == 0) return;
	    		
	    		Timer();
		        if (State == 2) {
		            State = 3;
		            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		    	    scheduler.scheduleSyncDelayedTask(Main.main, new Runnable() {
		    	    	@Override
		    	        public void run() {
		    	    		Stop();
		    	        }
		    	    }, 69);
		        }
		        
		        Ticker();
	        }
	    }, 1);
	}

	public Location GetRandomLocation(World world) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		
		WorldBorder border = world.getWorldBorder();
		Location borderCenter = border.getCenter();
		double borderSize = border.getSize() / 2;
		
		int centerX = (int) borderCenter.getX();
		int centerZ = (int )borderCenter.getZ();

		int MaxX = (int) (centerX + borderSize);
		int MinX = (int) (centerX - borderSize);
		int MaxZ = (int) (centerZ + borderSize);
		int MinZ = (int) (centerZ - borderSize);

		int maxRadius = (int)SettingsManager.instance.GetSetting("TeleportRadius");
		if (MaxX > centerX + maxRadius) MaxX = centerX + maxRadius;
		if (MinX < centerX - maxRadius) MinX = centerX - maxRadius;
		if (MaxZ > centerZ + maxRadius) MaxZ = centerZ + maxRadius;
		if (MinZ < centerZ - maxRadius) MinZ = centerZ - maxRadius;
		
		Location loc = new Location(world, random.nextInt(MinX, MaxX), 62, random.nextInt(MinZ, MaxZ));
		if (world.getHighestBlockAt(loc).isLiquid()) return GetRandomLocation(world);
		loc = world.getHighestBlockAt(loc).getLocation().add(0.5, 1, 0.5);
		return loc;
	}
	
	public void SendChunk(Chunk ch, Player p) {
		/*
		String mcVersion = Bukkit.getServer().getClass().getPackage().getName();
		if ((Boolean)SettingsManager.instance.GetSetting("SendChunks")) {
			if (mcVersion.contains("1_15")) PacketMapChunk.refreshChunk(ch, p);
			else if (mcVersion.contains("1_16_R1")) PacketMapChunk2.refreshChunk(ch, p);
			else if (mcVersion.contains("1_16_R2")) PacketMapChunk3.refreshChunk(ch, p);
		}
		*/
	}
}
