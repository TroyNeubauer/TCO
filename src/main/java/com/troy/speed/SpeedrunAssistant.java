package com.troy.speed;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class SpeedrunAssistant extends JavaPlugin implements Listener {

	private Deque<String> preppedWorlds = new ArrayDeque<String>();

	private static final String CURRENT_UNIVERSE_FILE_NAME = "current_universe_name.txt";
	private static final String PREP_FILE_NAME = "prep";

	// A universe is a overworld-nether-end triple that are linked through their
	// portals and inaccessible from others
	static class UniverseData {
		ItemStack[] inventory;
		Location lastPos;
		float health, saturation;
		int hunger;
	}

	private String currentWorld = null;
	private long ticks = 0, nextPrint = 20 * 30;

	// Maps universe names to a map of players and their data
	private final HashMap<String, HashMap<UUID, UniverseData>> data = new HashMap<String, HashMap<UUID, UniverseData>>();

	@Override
	public void onEnable() {
		super.onEnable();
		
		getCommand("regen").setExecutor(regenCommand);

		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {

			@Override
			public void run() {
				ticks++;

				if (ticks >= nextPrint) {
					long hours = ticks / 20 / 60 / 60;
					long minutes = ticks / 20 / 60 - hours * 60;
					long seconds = ticks / 20 - hours * 60 * 60 - minutes * 60;
					getServer().broadcastMessage("Speedrun time at T+" + hours + "h:" + minutes + "m:" + seconds + "s");
					nextPrint += 20 * 30;
				}
			}
		}, 1, 1);

		//Try to load the name of the last universe if it exists
		try {
			currentWorld = Files.readAllLines(new File(CURRENT_UNIVERSE_FILE_NAME).toPath()).get(0);
		} catch (Exception _ignore) {
		}
		boolean hasCurrentWorld = false;
		//If we have an old universe then try to load it from disk
		if (currentWorld != null) {
			World testWorld = nextWorld(currentWorld, World.Environment.NORMAL, new Random());
			if (testWorld != null) {
				nextWorld(currentWorld + NETHER_SUFFIX, World.Environment.NETHER, new Random());
				nextWorld(currentWorld + THE_END_SUFFIX, World.Environment.THE_END, new Random());
				hasCurrentWorld = true;
			}
		}
		//If we couldn't find a past universe than just pick the first overworld world to be it. Usually just "world"
		if (!hasCurrentWorld) {
			for (World world : getServer().getWorlds()) {
				if (world.getEnvironment() == World.Environment.NORMAL) {
					currentWorld = world.getName();
				}
			}
		}
		System.out.println("Picked world: " + currentWorld);
		//Discover past worlds that we prepped and can use
		for (File dir : getServer().getWorldContainer().listFiles()) {
			if (new File(dir, PREP_FILE_NAME).exists()) {
				System.out.println("Found prep: " + dir);
				preppedWorlds.addLast(dir.getName());
			}
		}

		getServer().getPluginManager().registerEvents(this, this);
		for (Player player : getServer().getOnlinePlayers()) {
			setPlayerUniverse(currentWorld, player);
		}
	}

	private World getOverworldForPlayer(Player player) {
		World current = player.getWorld();
		if (current.getEnvironment() == World.Environment.NORMAL) {
			return current;
		} else {
			String name = current.getName();
			name = name.replaceAll(NETHER_SUFFIX, "");
			name = name.replaceAll(THE_END_SUFFIX, "");
			World result = getServer().getWorld(name);
			if (result == null) {
				throw new RuntimeException("Unable to get matching overworld name: " + name + ". From player "
						+ player.getDisplayName() + " current non-overworld name: " + current.getName());
			}
			return result;
		}
	}

	private void updateUniverseData(Player player, UniverseData data) {
		data.inventory = player.getInventory().getContents();
		data.lastPos = player.getLocation();
		data.health = (float) player.getHealth();
		data.saturation = player.getSaturation();
		data.hunger = player.getFoodLevel();
	}

	private boolean hasData(String universeName, Player player) {
		HashMap<UUID, UniverseData> players = data.get(universeName);
		if (players == null) {
			return false;
		}
		UniverseData data = players.get(player.getUniqueId());
		if (data == null) {
			return false;
		}
		return true;
	}

	// Gets or creates the universe data for a player. If the player has no data in
	// this universe then fields in the returned object will be uninitialized
	private UniverseData getData(String universeName, Player player) {
		HashMap<UUID, UniverseData> players = data.get(universeName);
		if (players == null) {
			players = new HashMap<UUID, UniverseData>();
			data.put(universeName, players);
		}
		UniverseData data = players.get(player.getUniqueId());
		if (data == null) {
			data = new UniverseData();
			players.put(player.getUniqueId(), data);
		}
		return data;
	}

	// Gets the data associated a player for the universe they are in
	private UniverseData getData(Player player) {
		return getData(getOverworldForPlayer(player).getName(), player);
	}

	@Override
	public void onDisable() {
		super.onDisable();
	}

	@EventHandler(priority = EventPriority.HIGH)
	void onPlayerJoin(PlayerJoinEvent event) {
		System.out.println("Player join");
	}

	@EventHandler(priority = EventPriority.HIGH)
	void onPortal(PlayerPortalEvent event) {
		System.out.println("portal event " + event.getPlayer().getDisplayName());
		Location location = event.getTo();
		if (location.getWorld().getEnvironment() == World.Environment.NETHER) {
			// Use the same coords - just change the world
			location.setWorld(getServer().getWorld(currentWorld + NETHER_SUFFIX));

		} else if (location.getWorld().getEnvironment() == World.Environment.NORMAL) {
			location.setWorld(getServer().getWorld(currentWorld));

		} else if (location.getWorld().getEnvironment() == World.Environment.THE_END) {
			// use the end spawn pos
			location = getServer().getWorld(currentWorld + THE_END_SUFFIX).getSpawnLocation();

		} else {
			throw new RuntimeException("Unknown evniroment: " + location.getWorld().getEnvironment().toString());
		}

		event.setTo(location);
	}

	@EventHandler(priority = EventPriority.HIGH)
	void onRespawn(PlayerRespawnEvent event) {
		if (!event.isBedSpawn()) {
			//If this is a non bed spawn (bed destroyed or uset) then we need to bring them to the current universe
			event.setRespawnLocation(getOverworldForPlayer(event.getPlayer()).getSpawnLocation());
			System.out.println("changing respawn pos for player: " + event.getPlayer().getDisplayName());
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	void onQuitEvent(PlayerQuitEvent event) {
		UniverseData oldData = getData(event.getPlayer());
		updateUniverseData(event.getPlayer(), oldData);
		//Update data on quit so we have it for next time
	}

	@EventHandler(priority = EventPriority.HIGH)
	void onQuitEvent(PlayerKickEvent event) {
		UniverseData oldData = getData(event.getPlayer());
		updateUniverseData(event.getPlayer(), oldData);
	}

	private static final String NETHER_SUFFIX = "_nether";
	private static final String THE_END_SUFFIX = "_the_end";

	private void applyNewWorld(CommandSender sender) {
		if (preppedWorlds.isEmpty()) {
			if (sender != null)
				sender.sendMessage("Cannot use a new world before one was created. Try /regen prep");
			return;
		}
		String oldWorld = currentWorld;
		String worldName = preppedWorlds.pop();
		sender.sendMessage("Applying world: " + worldName + ". Teleporting players...");

		// Load worlds from disk
		Random random = new Random();
		World newOverworld = nextWorld(worldName, World.Environment.NORMAL, random);
		World nether = nextWorld(worldName + NETHER_SUFFIX, World.Environment.NETHER, random);
		World end = nextWorld(worldName, World.Environment.THE_END, random);

		if (newOverworld == null) {
			throw new Error("Cannot get newOverworld: " + worldName);
		}
		File prepFile = new File(newOverworld.getWorldFolder(), PREP_FILE_NAME);
		//This world is no longer prepped since its in use so delete the prepped flag
		if (prepFile.exists())
			prepFile.delete();

		//teleport players
		for (Player player : getServer().getOnlinePlayers()) {
			setPlayerUniverse(worldName, player);
		}
		currentWorld = worldName;
		try {
			Files.writeString(new File(CURRENT_UNIVERSE_FILE_NAME).toPath(), currentWorld);
		} catch (IOException _ignore) {
		}

		boolean deleteOld = !currentWorld.equals("world");
		if (deleteOld) {
			deleteWorld(getServer().getWorld(oldWorld));
			deleteWorld(getServer().getWorld(oldWorld + NETHER_SUFFIX));
			deleteWorld(getServer().getWorld(oldWorld + THE_END_SUFFIX));
		}
		ticks = 0;
		nextPrint = 20 * 30;
	}

	private void setPlayerUniverse(String worldName, Player player) {
		// Update the data before we reset and tp them
		UniverseData oldData = getData(player);
		updateUniverseData(player, oldData);

		World newOverworld = getServer().getWorld(worldName);

		boolean hasData = hasData(worldName, player);
		if (hasData) {
			UniverseData data = getData(worldName, player);
			player.getInventory().setContents(data.inventory);
			player.teleport(data.lastPos);
			player.setHealth(data.health);
			player.setSaturation(data.saturation);
			player.setFoodLevel(data.hunger);

		} else {
			UniverseData data = getData(worldName, player);
			Location spawn = newOverworld.getSpawnLocation();
			player.teleport(spawn);
			player.setBedSpawnLocation(null);
			player.getInventory().clear();
			player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
			player.setSaturation(5);
			player.setFoodLevel(20);
			System.out.println("Setting player: " + player.getDisplayName() + " to default values for the new world");
			updateUniverseData(player, data);
		}

	}

	private boolean deleteDirectory(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		return directoryToBeDeleted.delete();
	}

	private boolean deleteWorld(World world) {
		if (world == null)
			return false;
		File worldFile = world.getWorldFolder();
		getServer().unloadWorld(world, false);

		if (deleteDirectory(worldFile)) {
			System.out.println("Successfully deleted world: " + worldFile);
			return true;
		} else {
			System.out.println("World '" + world.getName() + "' was NOT deleted.");
			System.out.println("Are you sure the folder exists?");
			System.out.println("Please check your file permissions!");
			return false;
		}
	}

	private World nextWorld(String name, World.Environment env, Random random) {
		WorldCreator c = new WorldCreator(name);
		c.environment(env);
		c.seed(random.nextLong());
		return getServer().createWorld(c);
	}

	private void prepWorld(CommandSender sender, boolean autoUnload) {
		Random random = new Random();
		long id = Math.abs(random.nextLong());
		String worldName = "world" + id;
		if (sender != null)
			sender.sendMessage("Generating world... Please wait. This will take several seconds");

		World overworld = nextWorld(worldName, World.Environment.NORMAL, random);
		sender.sendMessage("Overworld generation finished");
		try {
			Files.writeString(new File(overworld.getWorldFolder(), PREP_FILE_NAME).toPath(), "");
		} catch (IOException ignore) {
		}

		if (autoUnload)
			getServer().unloadWorld(overworld, true);
		else
			overworld.save();

		sender.sendMessage("Saving overworld chunks finished");

		World nether = nextWorld(worldName + NETHER_SUFFIX, World.Environment.NETHER, random);
		sender.sendMessage("Nether generation finished");

		if (autoUnload)
			getServer().unloadWorld(nether, true);
		else
			nether.save();

		sender.sendMessage("Saving nether chunks finished");

		World the_end = nextWorld(worldName + THE_END_SUFFIX, World.Environment.THE_END, random);
		sender.sendMessage("The end generation finished");

		if (autoUnload)
			getServer().unloadWorld(the_end, true);
		else
			the_end.save();

		sender.sendMessage("Saving the end chunks finished");

		if (overworld == null || nether == null || the_end == null) {
			throw new Error("World gen failed!");
		}

		preppedWorlds.addLast(worldName);
		if (sender != null)
			sender.sendMessage("Finished creating world: " + worldName);
	}

	private class RegenCommandExecutor implements CommandExecutor {

		@Override
		public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
			if (command.getName().equals("regen")) {
				if (!sender.isOp()) {
					sender.sendMessage("Only server ops can use this command");

				} else {
					if (args.length == 1 && args[0].equals("list")) {
						sender.sendMessage("Worlds are:");
						for (World world : sender.getServer().getWorlds()) {
							sender.sendMessage("  " + world.getName());
						}
						sender.sendMessage("Prepped unloaded worlds include: ");
						for (File dir : getServer().getWorldContainer().listFiles()) {
							if (new File(dir, PREP_FILE_NAME).exists()) {
								sender.sendMessage(dir.getName());
							}
						}
						return true;
					}
					if (args.length >= 1 && args[0].equals("prep")) {
						prepWorld(sender, false);
					}

					if (args.length == 1 && args[0].equals("apply")) {
						applyNewWorld(sender);
						return true;
					}
				}
			}
			return false;
		}
	};

	private RegenCommandExecutor regenCommand = new RegenCommandExecutor();

}
