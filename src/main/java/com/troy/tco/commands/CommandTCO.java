package com.troy.tco.commands;

import com.troy.tco.DimensionHandler;
import com.troy.tco.TCO;
import com.troy.tco.world.TCOWorldData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.List;

public class CommandTCO extends CommandBase {
	@Override
	public String getName()
	{
		return "tco";
	}

	@Override
	public String getUsage(ICommandSender sender)
	{
		return "/tco <add:new:list:stat:spawn>";
	}

	@Override
	public int getRequiredPermissionLevel()
	{
		return 3;
	}

	private void sendUsage(ICommandSender sender) {
		sendUsage(sender, getUsage(sender));
	}

	private void sendUsage(ICommandSender sender, String usage) {
		sendMessage(sender, "Usage: " + usage);
	}

	private void sendMessage(ICommandSender sender, String message) {
		sender.sendMessage(new TextComponentString(message));
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender rawSender, String[] args) throws CommandException
	{
		if (!(rawSender instanceof EntityPlayerMP)) {
			sendMessage(rawSender, "This command can only be run ingame!");
			return;
		}
		EntityPlayerMP sender = (EntityPlayerMP) rawSender;
		if (args.length >= 1) {
			if (args[0].equals("add"))
			{
				if (args.length != 2) sendUsage(sender, "/tco add {Map Name} | Loads a tco map (a template map that can be cloned to play a game). Map name must be the name of a world on the server");
				else {
					String mapName = args[1];
					TCO.logger.info("About to add tco world: " + mapName);
					DimensionHandler.getInstance().createTemplate(sender, mapName);
				}

			} else if (args[0].equals("new")) {
				if (args.length != 2) sendUsage(sender, "/tco new {Map Name} | Creates a new clone of the given map and teleports all players there");
				else {
					String mapName = args[1];
					TCO.logger.info("About create game world from world template: " + mapName);
					Integer dimensionID = DimensionHandler.getInstance().getDimensionID(mapName);
					if (dimensionID == null) {
						sendMessage(sender, "Error: Unable to find map " + mapName);
					} else {
						DimensionHandler.getInstance().createFromTemplate(sender, dimensionID);
					}
				}

			} else if (args[0].equals("list")) {
				if (args.length == 1) {
					sendMessage(sender, "All TCO worlds");
				}
				else {
					String mapName = args[1];
					Integer dimensionID = DimensionHandler.getInstance().getDimensionID(mapName);
					if (dimensionID == null) {
						sendMessage(sender, "Error: Unable to find map " + mapName);
					} else {
						TCOWorldData data = TCOWorldData.get(DimensionManager.getWorld(dimensionID));
						if (data == null) {
							sendMessage(sender, "Error: No TCO metadata for world " + mapName);
						} else {
							sendMessage(sender, data.toString());
						}
					}
				}

			} else if (args[0].equals("spawn")) {
				if (args.length != 2) sendUsage(sender, "/tco spawn <enable:disable:start:end> | Manages if villager spawn locations can be added globally and by player");
				else {
					String mapName = args[1];

					sendMessage(sender, "TODO");
				}
			} else {
				sendUsage(sender);
			}
		} else {
			sendUsage(sender);
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos)
	{
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, "add", "new", "list", "spawn");
		} else if (args.length == 2 && args[1].equals("spawn")) {
			return getListOfStringsMatchingLastWord(args, "enable", "disable", "start", "end");
		} else {
			return new ArrayList<String>();
		}
	}
}
