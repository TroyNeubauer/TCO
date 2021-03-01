package com.troy.speed;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class SpeedrunAssistant extends JavaPlugin {
	@Override
	public void onEnable() {
		super.onEnable();
		System.out.println("Server is: " + Bukkit.getServer().getClass());
	}
	
	@Override
	public void onDisable() {
		super.onDisable();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("regen") && sender.isOp()) { // If the player typed /basic then do the following, note: If you only registered this executor for one command, you don't need this
			sender.sendMessage("Worlds are:");
			for (World world : sender.getServer().getWorlds()) {
				sender.sendMessage("  " + world.getName());
			}
			WorldCreator c = new WorldCreator("world2");
			Random random = new Random();
			c.seed(random.nextLong());
			//sender.getServer().createWorld(c);
			return true;
		} //If this has happened the function will return true. 
	        // If this hasn't happened the value of false will be returned.
		return false; 
	}
	
}
