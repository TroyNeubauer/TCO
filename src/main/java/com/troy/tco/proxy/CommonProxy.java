package com.troy.tco.proxy;

import com.troy.tco.network.PacketHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public abstract class CommonProxy implements IProxy {
	@Override
	public void preInit(FMLPreInitializationEvent event) {
	}

	@Override
	public void init(FMLInitializationEvent event) {
		PacketHandler.init();
	}

	@Override
	public void postInit(FMLPostInitializationEvent event) {

	}
}
