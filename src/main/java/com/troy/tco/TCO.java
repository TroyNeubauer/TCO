package com.troy.tco;

import java.io.File;
import java.io.IOException;
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

public class TCO extends JavaPlugin implements Listener {

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
		
		getCommand("tco").setExecutor(tcoCommand);

		getServer().getPluginManager().registerEvents(this, this);
		for (Player player : getServer().getOnlinePlayers()) {
			//Update
		}
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
	void onRespawn(PlayerRespawnEvent event) {
		if (!event.isBedSpawn()) {
			//If this is a non bed spawn (bed destroyed or uset) then we need to bring them to the current universe
			event.setRespawnLocation(null);
			System.out.println("changing respawn pos for player: " + event.getPlayer().getDisplayName());
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	void onQuitEvent(PlayerQuitEvent event) {
	}

	@EventHandler(priority = EventPriority.HIGH)
	void onQuitEvent(PlayerKickEvent event) {
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


	private class TCOCommandExecutor implements CommandExecutor {

		@Override
		public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
			if (command.getName().equals("tco")) {
				if (!sender.isOp()) {
					sender.sendMessage("Only server ops can use this command");

				} else {
					return false;
				}
			}
			return false;
		}
	};

	private TCOCommandExecutor tcoCommand = new TCOCommandExecutor();

}
