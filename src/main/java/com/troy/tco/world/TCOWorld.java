package com.troy.tco.world;

import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.village.VillageCollection;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.border.IBorderListener;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

public class TCOWorld extends WorldServer {
	private final WorldServer delegate;
	private final IBorderListener borderListener;

	public TCOWorld(WorldInfo worldInfo, MinecraftServer server, ISaveHandler saveHandlerIn, int dimensionId, WorldServer delegate, Profiler profilerIn)
	{
		super(server, saveHandlerIn, worldInfo, dimensionId, profilerIn);
		this.delegate = delegate;
		this.borderListener = new IBorderListener()
		{
			public void onSizeChanged(WorldBorder border, double newSize)
			{
				TCOWorld.this.getWorldBorder().setTransition(newSize);
			}
			public void onTransitionStarted(WorldBorder border, double oldSize, double newSize, long time)
			{
				TCOWorld.this.getWorldBorder().setTransition(oldSize, newSize, time);
			}
			public void onCenterChanged(WorldBorder border, double x, double z)
			{
				TCOWorld.this.getWorldBorder().setCenter(x, z);
			}
			public void onWarningTimeChanged(WorldBorder border, int newTime)
			{
				TCOWorld.this.getWorldBorder().setWarningTime(newTime);
			}
			public void onWarningDistanceChanged(WorldBorder border, int newDistance)
			{
				TCOWorld.this.getWorldBorder().setWarningDistance(newDistance);
			}
			public void onDamageAmountChanged(WorldBorder border, double newAmount)
			{
				TCOWorld.this.getWorldBorder().setDamageAmount(newAmount);
			}
			public void onDamageBufferChanged(WorldBorder border, double newSize)
			{
				TCOWorld.this.getWorldBorder().setDamageBuffer(newSize);
			}
		};
		this.delegate.getWorldBorder().addListener(this.borderListener);
	}

	/**
	 * Saves the chunks to disk.
	 */
	protected void saveLevel() throws MinecraftException
	{
		this.perWorldStorage.saveAllData();
	}

	public World init()
	{
		this.mapStorage = this.delegate.getMapStorage();
		this.worldScoreboard = this.delegate.getScoreboard();
		this.lootTable = this.delegate.getLootTableManager();
		String s = VillageCollection.fileNameForProvider(this.provider);
		VillageCollection villagecollection = (VillageCollection)this.perWorldStorage.getOrLoadData(VillageCollection.class, s);

		if (villagecollection == null)
		{
			this.villageCollection = new VillageCollection(this);
			this.perWorldStorage.setData(s, this.villageCollection);
		}
		else
		{
			this.villageCollection = villagecollection;
			this.villageCollection.setWorldsForAll(this);
		}

		return this;
	}


	/**
	 * Syncs all changes to disk and wait for completion.
	 */
	@Override
	public void flush()
	{
		super.flush();
		this.delegate.getWorldBorder().removeListener(this.borderListener); // Unlink ourselves, to prevent world leak.
	}

	/**
	 * Called during saving of a world to give children worlds a chance to save additional data. Only used to save
	 * WorldProviderEnd's data in Vanilla.
	 */
	public void saveAdditionalData()
	{
		this.provider.onWorldSave();
	}
}
