package com.troy.tco.network;

import com.troy.tco.TCO;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
	public static SimpleNetworkWrapper INSTANCE = new SimpleNetworkWrapper(TCO.MODID);

	public static void init() {
		int id = 0;
		INSTANCE.registerMessage(HandlerSyncTCOWorlds.class, MessageSyncTCOWorlds.class, id++, Side.CLIENT);
	}
}
