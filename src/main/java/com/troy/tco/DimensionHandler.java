package com.troy.tco;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.troy.tco.network.MessageSyncTCOWorlds;
import com.troy.tco.network.PacketHandler;
import com.troy.tco.world.TCOWorld;
import com.troy.tco.world.TCOWorldData;
import com.troy.tco.world.TCOWorldInfo;
import net.minecraft.world.*;
import org.apache.commons.io.FileUtils;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class DimensionHandler extends WorldSavedData
{
	static String NAME = "SimpleDimensionsHandler";

	HashMap<Integer, TCOWorldInfo> dimensionInfo;
	HashMap<Integer, UUID> toBeDeleted;

	public DimensionHandler(String name) {
		super(name);

		dimensionInfo = new HashMap<Integer, TCOWorldInfo>();
		toBeDeleted = new HashMap<Integer, UUID>();
	}

	public DimensionHandler() {
		super(NAME);

		dimensionInfo = new HashMap<Integer, TCOWorldInfo>();
		toBeDeleted = new HashMap<Integer, UUID>();
	}

	@Override
	public boolean isDirty() {
		return true;
	}

	public void createTemplate(EntityPlayerMP playerEntity, String name) {
		File src = getMapFile(name);
		if (!src.exists()) {
			TCO.logger.warn("Failed to find map: " + src.getAbsolutePath());
			playerEntity.sendMessage(new TextComponentString("Could not find map " + name).setStyle(new Style().setColor(TextFormatting.RED)));
			return;
		}
		int dimensionID = DimensionManager.getNextFreeDimId();
		File dest = new File(DimensionManager.getCurrentSaveRootDirectory(), "DIM" + dimensionID);
		//copy region data
		copyRegionData(src, dest);

		TCOWorldInfo info = new TCOWorldInfo(new WorldSettings(0, GameType.SURVIVAL, true, false, WorldType.DEFAULT), name);
		createDimension(playerEntity, info, dimensionID);

		World world = playerEntity.getEntityWorld().getMinecraftServer().getWorld(dimensionID);
		TCOWorldData data = TCOWorldData.create(world);
		TCO.logger.info("Created initial data:" + data);
	}

	private File getMapFile(String name) {
		return new File(new File(DimensionManager.getCurrentSaveRootDirectory().getParentFile(), "maps"), name);
	}

	private void copyFiles(File src, File dest) {
		if (!dest.exists()) {
			dest.mkdirs();
		}
		TCO.logger.trace("Copying " + src + "/* to " + dest + "/*");
		int count = 0;
		for (File file : src.listFiles()) {
			if (file.isFile()) {
				try {
					FileUtils.copyFile(file, new File(dest, file.getName()));
					count++;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		TCO.logger.info("Copied " + count + " files successfully");
	}

	/**
	 * Tries to create a copy of the given dimension. 
	 * @param playerEntity The player who initiated the creation. Used only for sending status update messages through chas
	 * @param parentDimID The dimension to clone
	 * @return The ID of the new dimension or null if the duplication failed
	 */
	public Integer createFromTemplate(EntityPlayerMP playerEntity, int parentDimID) {
		World parentWorld = playerEntity.getEntityWorld().getMinecraftServer().getWorld(parentDimID);
		TCOWorldData data = TCOWorldData.get(parentWorld);
		if (data == null) {
			TCO.logger.warn("World id " + parentDimID + " does not have TCO data");
			return null;
		}
		int newDimID = DimensionManager.getNextFreeDimId();
		cloneWorld(parentDimID, newDimID);
		int subID = 0;
		for (Map.Entry<Integer, TCOWorldInfo> entry : dimensionInfo.entrySet()) {
			TCOWorldData worldData = TCOWorldData.get(playerEntity.getServer().getWorld(entry.getKey()));
			if (!worldData.isTemplate()) {
				if (worldData.getParentID() == parentDimID) {
					subID++;
				}
			}
		}
		String worldName = parentWorld.getWorldInfo().getWorldName() + "-" + subID;
		TCOWorldInfo info = new TCOWorldInfo(new WorldSettings(0, GameType.SURVIVAL, true, false, WorldType.DEFAULT), worldName);
		createDimension(playerEntity, info, newDimID);

		World world = playerEntity.getEntityWorld().getMinecraftServer().getWorld(newDimID);
		TCO.logger.info("Got world " + world);
		TCOWorldData.set(world, data);

		return newDimID;
	}

	private void cloneWorld(int srcID, int destID) {
		File src = new File(DimensionManager.getCurrentSaveRootDirectory(), "DIM" + srcID);
		File dest = new File(DimensionManager.getCurrentSaveRootDirectory(), "DIM" + destID);
		TCO.logger.info("Copying " + src + " to " + dest);
		try {
			FileUtils.copyDirectory(src, dest);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the id of the dimension with the given name or null if none exists
	 * @param name The world name to search for
	 */
	public Integer getDimensionID(String name) {
		for (Map.Entry<Integer, TCOWorldInfo> info : dimensionInfo.entrySet()) {
			if (info.getValue().getWorldName().equals(name)) {
				return info.getKey();
			}
		}
		return null;
	}

	public int createDimension(EntityPlayerMP playerEntity, TCOWorldInfo worldInfo, int dimensionID) {
		if (dimensionID == -1) {
			dimensionID = DimensionManager.getNextFreeDimId();
		}

		dimensionInfo.put(dimensionID, worldInfo);
		DimensionManager.registerDimension(dimensionID, DimensionType.OVERWORLD);

		loadDimension(dimensionID, worldInfo);
		playerEntity.sendMessage(new TextComponentString(String.format("Created %s using id %s", worldInfo.getWorldName(), dimensionID)).setStyle(new Style().setColor(TextFormatting.GREEN)));
		syncWithClients();

		return dimensionID;
	}

	public ITextComponent generateList()
	{
		StringBuilder stringBuilder = new StringBuilder();

		if (dimensionInfo.isEmpty())
		{
			return new TextComponentString("No TCO worlds");
		}
		else
		{
			int counter = 0;
			for (Map.Entry<Integer, TCOWorldInfo> entry : dimensionInfo.entrySet())
			{
				stringBuilder.append(String.format("%s %s", "DIM " + entry.getKey(), "(" + entry.getValue().getWorldName() + ")"));
				counter++;
				if (counter < dimensionInfo.size())
				{
					stringBuilder.append("\n");
				}
			}

			return new TextComponentString(stringBuilder.toString());
		}
	}

	public static DimensionHandler getInstance()
	{
		DimensionHandler INSTANCE;
		INSTANCE = (DimensionHandler) FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld().getMapStorage().getOrLoadData(DimensionHandler.class, NAME);

		if (INSTANCE == null)
		{
			INSTANCE = new DimensionHandler();
			FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld().getMapStorage().setData(NAME, INSTANCE);
		}

		return INSTANCE;
	}

	public String getDimensionName(int dimensionId)
	{
		return dimensionInfo.get(dimensionId).getWorldName();
	}

	public HashMap<Integer, TCOWorldInfo> getDimensionInfo()
	{
		return dimensionInfo;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		NBTTagList nbtList = nbt.getTagList("dimensionInfo", 10);

		for (int i = 0; i < nbtList.tagCount(); i++)
		{
			NBTTagCompound compound = nbtList.getCompoundTagAt(i);

			dimensionInfo.put(compound.getInteger("dimensionID"), new TCOWorldInfo(compound.getCompoundTag("worldInfo")));
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		NBTTagList nbtList = new NBTTagList();

		for (Map.Entry<Integer, TCOWorldInfo> entry : dimensionInfo.entrySet())
		{
			NBTTagCompound compound = new NBTTagCompound();

			compound.setInteger("dimensionID", entry.getKey());
			compound.setTag("worldInfo", entry.getValue().cloneNBTCompound(null));

			nbtList.appendTag(compound);
		}

		nbt.setTag("dimensionInfo", nbtList);

		return nbt;
	}

	public void loadDimensions()
	{
		for (Map.Entry<Integer, TCOWorldInfo> entry : dimensionInfo.entrySet())
		{
			int dimensionID = entry.getKey();
			WorldInfo worldInfo = entry.getValue();

			DimensionManager.registerDimension(dimensionID, DimensionType.OVERWORLD);

			loadDimension(dimensionID, worldInfo);
			TCO.logger.info("Loaded world: " + dimensionID);
		}
	}

	private void loadDimension(int dimensionID, WorldInfo worldInfo)
	{
		WorldServer overworld = (WorldServer) FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld();

		MinecraftServer mcServer = overworld.getMinecraftServer();
		ISaveHandler savehandler = overworld.getSaveHandler();
		EnumDifficulty difficulty = mcServer.getEntityWorld().getDifficulty();

		WorldServer world = (WorldServer) (new TCOWorld(worldInfo, mcServer, savehandler, dimensionID, overworld, mcServer.profiler).init());
		world.addEventListener(new ServerWorldEventHandler(mcServer, world));
		MinecraftForge.EVENT_BUS.post(new WorldEvent.Load(world));
		TCO.logger.info("Loaded world: " + dimensionID);

		if (!mcServer.isSinglePlayer())
		{
			world.getWorldInfo().setGameType(mcServer.getGameType());
		}

		mcServer.setDifficultyForAllWorlds(difficulty);
	}

	public void deleteDimension(ICommandSender sender, int dimensionID)	{
		if (!dimensionInfo.containsKey(dimensionID)) {
			sender.sendMessage(new TextComponentString("The dimension associated with that id is not from TCO").setStyle(new Style().setColor(TextFormatting.RED)));
			return;
		}

		World worldObj = DimensionManager.getWorld(dimensionID);

		if (worldObj.playerEntities.size() > 0)	{
			sender.sendMessage(new TextComponentString("Can't delete a dimension with players inside it").setStyle(new Style().setColor(TextFormatting.RED)));
			return;
		}

		Entity entitySender = sender.getCommandSenderEntity();
		toBeDeleted.put(dimensionID, entitySender != null ? entitySender.getUniqueID() : null);

		DimensionManager.unloadWorld(dimensionID);

		if (dimensionInfo.containsKey(dimensionID)) {
			WorldInfo worldInfo = dimensionInfo.get(dimensionID);

			DimensionManager.unregisterDimension(dimensionID);
		}
		dimensionInfo.remove(dimensionID);
		syncWithClients();
	}

	public void unload(World world, int dimensionID) {

		if (toBeDeleted.containsKey(dimensionID)) {
			TCO.logger.info("Deleting dimension: " + dimensionID);
			UUID uniqueID = toBeDeleted.get(dimensionID);

			toBeDeleted.remove(dimensionID);

			((WorldServer) world).flush();
			File dimensionFolder = getDimensionFile(dimensionID);

			EntityPlayerMP player = null;
			if (uniqueID != null) {
				player = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(uniqueID);
			}

			try {
				FileUtils.deleteDirectory(dimensionFolder);
				TCO.logger.info("Deleted DIM" + dimensionID + " on disk fully");
			} catch (IOException e) {
				e.printStackTrace();
				if (player != null) {
					player.sendMessage(new TextComponentString("Error deleting dimension folder of " + dimensionID + ". Has to be removed manually.").setStyle(new Style().setColor(TextFormatting.RED)));
				}
			} finally {
				if (player != null) {
					player.sendMessage(new TextComponentString("Completely deleted dimension " + dimensionID).setStyle(new Style().setColor(TextFormatting.GREEN)));
				}
			}
		}
	}

	private File getDimensionFile(int dimensionID) {
		return new File(DimensionManager.getCurrentSaveRootDirectory(), "DIM" + dimensionID);
	}

	private void copyRegionData(File src, File dest) {
		copyFiles(new File(src, "region"), new File(dest, "region"));
	}

	private void syncWithClients() {
		PacketHandler.INSTANCE.sendToAll(this.constructSyncMessage());
	}

	public IMessage constructSyncMessage() {
		MessageSyncTCOWorlds message = new MessageSyncTCOWorlds();

		for (Integer i : dimensionInfo.keySet()) {
			message.addDimension(i);
		}

		return message;
	}

	public void save(String mapName, int id) {
		File dest = getMapFile(mapName);
		File src = getDimensionFile(id);
		copyRegionData(src, dest);
	}

}