package com.troy.tco.commands;

import com.troy.tco.DimensionHandler;
import com.troy.tco.GameManager;
import com.troy.tco.TCO;
import com.troy.tco.world.SpawnHandler;
import com.troy.tco.world.TCOTeleport;
import com.troy.tco.world.TCOWorldData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
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
		return "/tco <load:save:new:start:delete:warp:list:spawn>";
	}

	@Override
	public int getRequiredPermissionLevel()
	{
		return 0;
	}

	private void sendUsage(ICommandSender sender) {
		sendUsage(sender, getUsage(sender));
	}

	private static void sendUsage(ICommandSender sender, String usage) {
		sendMessage(sender, "Usage: " + usage);
	}

	public static void sendMessage(ICommandSender sender, String message) {
		sender.sendMessage(new TextComponentString(message));
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender rawSender, String[] args) throws CommandException {
		if (!(rawSender instanceof EntityPlayerMP)) {
			sendMessage(rawSender, "This command can only be run ingame!");
			return;
		}
		EntityPlayerMP sender = (EntityPlayerMP) rawSender;
		if (args.length >= 1) {
			switch (args[0]) {
				case "load":
					if (args.length != 2)
						sendUsage(sender, "/tco load {Map Name} | Loads a tco map (a template map that can be cloned to play a game). Map name must be the name of a world on the server");
					else if (!server.getPlayerList().canSendCommands(sender.getGameProfile()))
						sendMessage(sender, "You must be op to use this command");
					else {
						String mapName = args[1];
						if (DimensionHandler.getInstance().getDimensionID(mapName) != null) {
							sendMessage(sender, "Error: Map " + mapName + " already exists! Use /tco delete ... first");
						} else {
							TCO.logger.info("About to load tco world: " + mapName);
							DimensionHandler.getInstance().createTemplate(sender, mapName);
						}
					}

					break;
				case "save":
					if (args.length != 2)
						sendUsage(sender, "/tco save {Map Name} | Saves chunk data back to the overworld in the maps directory");
					else if (!server.getPlayerList().canSendCommands(sender.getGameProfile()))
						sendMessage(sender, "You must be op to use this command");
					else {
						String mapName = args[1];
						Integer id = DimensionHandler.getInstance().getDimensionID(mapName);
						if (id == null) {
							sendMessage(sender, "Error: Map " + mapName + " doesn't exist");
						} else {
							TCO.logger.info("Saving map " + mapName + " (" + id + ")");
							DimensionHandler.getInstance().save(mapName, id);
						}
					}

					break;
				case "new":
					if (args.length != 2)
						sendUsage(sender, "/tco new {Map Name} | Creates a new clone of the given map and teleports all players there");
					else if (!server.getPlayerList().canSendCommands(sender.getGameProfile()))
						sendMessage(sender, "You must be op to use this command");
					else {
						String mapName = args[1];
						TCO.logger.info("About create game world from world template: " + mapName);
						Integer dimensionID = DimensionHandler.getInstance().getDimensionID(mapName);
						if (dimensionID == null) {
							sendMessage(sender, "Error: Unable to find map " + mapName);
						} else {
							Integer newDimID = DimensionHandler.getInstance().createFromTemplate(sender, dimensionID);
							if (newDimID == null) {
								sendMessage(sender, "Failed to create new world for template: " + mapName);
							} else {
								TCOWorldData data = TCOWorldData.get(server.getWorld(newDimID));
								if (data == null || data.getLobbySpawn() == null) {
									sendMessage(sender, "WARNING: Map " + mapName + " has no spawn set! Go to the spawn location and run /tco spawn lobby");
								}
								for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
									teleportPlayer(server, player, dimensionID);
								}

							}
						}
					}

					break;
				case "start":
					if (args.length != 1)
						sendUsage(sender, "/tco start | Teleports players to either the TCO or police spawn for the current map and starts the game");
					else if (!server.getPlayerList().canSendCommands(sender.getGameProfile()))
						sendMessage(sender, "You must be op to use this command");
					else {
						TCO.logger.info("Starting TCO match");
						GameManager.start();
					}

					break;
				case "stop":
					if (args.length != 1) sendUsage(sender, "/tco stop | Ends the current TCO match");
					else if (!server.getPlayerList().canSendCommands(sender.getGameProfile()))
						sendMessage(sender, "You must be op to use this command");
					else {
						TCO.logger.info("Stopping TCO match");
						GameManager.stop();
					}

					break;
				case "delete":
					if (args.length != 2) sendUsage(sender, "/tco delete {Map Name} | deletes a TCO world");
					else if (!server.getPlayerList().canSendCommands(sender.getGameProfile()))
						sendMessage(sender, "You must be op to use this command");
					else {
						String mapName = args[1];

						Integer dimensionID = DimensionHandler.getInstance().getDimensionID(mapName);
						if (dimensionID == null) {
							sendMessage(sender, "Error: Unable to find map " + mapName);
						} else {
							sendMessage(sender, "Starting delete of " + mapName + " (" + dimensionID + ")");
							DimensionHandler.getInstance().deleteDimension(sender, dimensionID);
							sendMessage(sender, "Delete finished");
						}
					}

					break;
				case "warp":
					if (args.length != 2)
						sendUsage(sender, "/tco warp {Map Name} | Teleports the current player to the desired TCO world. Use /tco warp overworld to return");
					else if (!server.getPlayerList().canSendCommands(sender.getGameProfile()))
						sendMessage(sender, "You must be op to use this command");
					else {
						String mapName = args[1];
						TCO.logger.info("About create game world from world template: " + mapName);
						Integer dimensionID = DimensionHandler.getInstance().getDimensionID(mapName);
						if (dimensionID == null) {
							sendMessage(sender, "Error: Unable to find map " + mapName);
						} else {
							sendMessage(sender, "Teleporting players to " + mapName + " (" + dimensionID + ")");
							teleportPlayer(sender.getServer(), sender, dimensionID);
						}
					}

					break;
				case "list":
					if (args.length == 1) {
						sender.sendMessage(DimensionHandler.getInstance().generateList());
					} else {
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

					break;
				case "spawn":
					if (args.length != 2)
						sendUsage(sender, "/tco spawn <enable:disable:start:end:lobby:police:tco> | Manages if villager spawn locations can be added globally and by player. Or sets spawn location");
					else {
						String subCommand = args[1];
						if ((subCommand.equals("enable") || subCommand.equals("disable") || subCommand.equals("lobby") || subCommand.equals("police") || subCommand.equals("tco")) && !server.getPlayerList().canSendCommands(sender.getGameProfile())) {
							sendMessage(sender, "You must be op to use this command");
						} else {
							switch (subCommand) {
								case "enable":
									SpawnHandler.enable(sender, sender);

									break;
								case "disable":
									SpawnHandler.disable(sender, sender);

									break;
								case "start":
									SpawnHandler.start(sender, sender);

									break;
								case "stop":
									SpawnHandler.stop(sender, sender);

									break;
								case "police":
									SpawnHandler.policeSpawn(sender, sender);

									break;
								case "tco":
									SpawnHandler.tcoSpawn(sender, sender);

									break;
								case "lobby":
									SpawnHandler.lobbySpawn(sender, sender);

									break;
								default:
									sendUsage(sender, "Unknown sub command: " + subCommand);
									break;
							}
						}
					}
					break;
				default:
					sendUsage(sender);
					break;
			}
		} else {
			sendUsage(sender);
		}
	}


	private void teleportPlayer(MinecraftServer server, EntityPlayerMP player, int dimension) throws CommandException {
		World world = server.getWorld(dimension);
		TCOWorldData data = TCOWorldData.get(world);
		server.getPlayerList().transferPlayerToDimension(player, dimension, new TCOTeleport((WorldServer) server.getEntityWorld()));
		if (data != null && data.getLobbySpawn() != null) {
			TCO.logger.info("Moving player " + player.getName() + " to " + data.getLobbySpawn());
			player.setPosition(data.getLobbySpawn().x, data.getLobbySpawn().y, data.getLobbySpawn().z);
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, "load", "new", "start", "stop", "warp", "list", "spawn");
		} else if (args.length == 2 && args[1].equals("spawn")) {
			return getListOfStringsMatchingLastWord(args, "enable", "disable", "start", "end", "police", "tco", "lobby");
		} else {
			return new ArrayList<String>();
		}
	}
}
