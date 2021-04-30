package com.troy.tco.client;

import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.HashSet;

public class ClientHandler {
	static ClientHandler INSTANCE;

	HashSet<Integer> simpleDimensions;

	public ClientHandler()
	{
		simpleDimensions = new HashSet<Integer>();
	}

	public void cleanUp() {
		for (Integer i : simpleDimensions) {
			if (DimensionManager.isDimensionRegistered(i)) {
				DimensionManager.unregisterDimension(i);
			}
		}
	}

	public static ClientHandler getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ClientHandler();
		}

		return INSTANCE;
	}

	public void sync(ArrayList<Integer> dimensions) {
		this.cleanUp();

		this.simpleDimensions = new HashSet<Integer>();
		this.simpleDimensions.addAll(dimensions);

		for (Integer i : simpleDimensions) {
			if (!DimensionManager.isDimensionRegistered(i)) {
				DimensionManager.registerDimension(i, DimensionType.OVERWORLD);
			}
		}
	}
}
