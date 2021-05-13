package com.troy.tco.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TCOWorldInfo extends WorldInfo {
	WorldInfo superInfo;

	public TCOWorldInfo(NBTTagCompound nbt)
	{
		super(nbt);
		superInfo = FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld().getWorldInfo();
	}

	public TCOWorldInfo(WorldSettings settings, String name)
	{
		super(settings, name);
		superInfo = FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld().getWorldInfo();
	}

	@Override
	public NBTTagCompound getPlayerNBTTagCompound()
	{
		return superInfo.getPlayerNBTTagCompound();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public long getLastTimePlayed()
	{
		return superInfo.getLastTimePlayed();
	}

	@Override
	public GameType getGameType()
	{
		return superInfo.getGameType();
	}

	@Override
	public boolean isHardcoreModeEnabled()
	{
		return superInfo.isHardcoreModeEnabled();
	}

	@Override
	public boolean areCommandsAllowed()
	{
		return superInfo.areCommandsAllowed();
	}

	@Override
	public GameRules getGameRulesInstance()
	{
		return superInfo.getGameRulesInstance();
	}

	@Override
	public EnumDifficulty getDifficulty()
	{
		return superInfo.getDifficulty();
	}

	@Override
	public boolean isDifficultyLocked()
	{
		return superInfo.isDifficultyLocked();
	}
}
