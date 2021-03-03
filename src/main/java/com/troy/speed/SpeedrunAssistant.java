package com.troy.speed;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SpeedrunAssistant extends JavaPlugin {

	private Deque<String> preppedWorlds = new ArrayDeque<String>();

	@Override
	public void onEnable() {
		super.onEnable();

		getCommand("regen").setExecutor(regenCommand);
	}

	@Override
	public void onDisable() {
		super.onDisable();
	}

	private World renameWorld(String srcName, String destName) {
		System.out.println("renaming: " + srcName + " -> " + destName);

		//If we are replacing an existing world then kick all the players off first
		World old = getServer().getWorld(destName);
		long seed = old.getSeed();
		World.Environment env = old.getEnvironment();

		old.setAutoSave(false);
		File destWorldDir = null;
		if (old != null) {
			destWorldDir = old.getWorldFolder();
			for (Player player : old.getPlayers()) {
				player.kickPlayer("Swapping worlds! Please relog!");
			}
			System.out.println("dest file is: " + destWorldDir);
			if (!deleteWorld(old))
				throw new Error("Failed to delete world: " + destWorldDir);
		}

		File worldsContainer = getServer().getWorldContainer();
		if (destWorldDir == null) {
			System.out.println("having to guess the path of the dest world: " + destWorldDir);
			destWorldDir = new File(worldsContainer, destName);
		}
		File srcWorldDir = new File(worldsContainer, srcName);
		System.out.println("src file is: " + srcWorldDir);

		try {
			Files.move(srcWorldDir.toPath(), destWorldDir.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error(e);
		}
		for (World world : getServer().getWorlds()) {
			if (world.getName().equals(destName)) {
				throw new Error("Old world: " + destName + " still exists");
			}
		}
		WorldCreator c = new WorldCreator(destName);
		c.environment(env);
		c.seed(seed);
		World dest = getServer().createWorld(c);
		if (dest == null) {
			throw new Error("Failed to load renamed world " + srcName + " -> " + destName);
		}
		return dest;
	}

	private static final String NETHER_SUFFIX = "_nether";
	private static final String THE_END_SUFFIX = "_the_end";

	private void applyNewWorld(CommandSender sender) {
		if (preppedWorlds.isEmpty()) {
			if (sender != null)
				sender.sendMessage("Cannot use a new world before one was created. Try /regen prep 1");
			return;
		}
		String worldName = preppedWorlds.pop();
		sender.sendMessage("Applying world: " + worldName + ". Please wait. This will take several seconds");

		String destName = "world";
		renameWorld(worldName, destName);
		renameWorld(worldName + NETHER_SUFFIX, destName + NETHER_SUFFIX);
		renameWorld(worldName + THE_END_SUFFIX, destName + THE_END_SUFFIX);
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
		File worldFile = world.getWorldFolder();
		getServer().unloadWorld(world, false);

		if (deleteDirectory(worldFile)) {
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

	private void prepWorld(CommandSender sender) {
		Random random = new Random();
		long id = Math.abs(random.nextLong());
		String worldName = "world" + id;
		if (sender != null)
			sender.sendMessage("Generating world... Please wait. This will take several seconds");

		World overworld = nextWorld(worldName, World.Environment.NORMAL, random);
		sender.sendMessage("Overworld generation finished");
		getServer().unloadWorld(overworld, true);
		sender.sendMessage("Saving overworld chunks finished");

		World nether = nextWorld(worldName + NETHER_SUFFIX, World.Environment.NETHER, random);
		sender.sendMessage("Nether generation finished");
		getServer().unloadWorld(nether, true);
		sender.sendMessage("Saving nether chunks finished");

		World the_end = nextWorld(worldName + THE_END_SUFFIX, World.Environment.THE_END, random);
		sender.sendMessage("The end generation finished");
		getServer().unloadWorld(the_end, true);
		sender.sendMessage("Saving the end chunks finished");

		sender.sendMessage("Current worlds are:");
		for (World world : getServer().getWorlds()) {
			sender.sendMessage("  " + world.getName());
		}

		
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
						return true;
					}
					if (args.length >= 1 && args[0].equals("prep")) {
						if (args.length == 1) {
							sender.sendMessage("You must specify the number of worlds to prep");
							return false;
						}
						try {
							int prepCount = Integer.parseInt(args[1]);
							sender.sendMessage("Starting prepare of " + prepCount + " worlds");
							for (int i = 0; i < prepCount; i++) {
								prepWorld(sender);
							}
							return true;
						} catch (Exception e) {
							return false;
						}
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
