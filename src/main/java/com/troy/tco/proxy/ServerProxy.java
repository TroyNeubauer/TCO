package com.troy.tco.proxy;


import com.troy.tco.DimensionHandler;
import com.troy.tco.TCO;
import com.troy.tco.commands.CommandTCO;
import com.troy.tco.world.SpawnHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.SERVER)
public class ServerProxy extends CommonProxy {

	@Override
	public void serverStarting(FMLServerStartingEvent event) {
		TCO.logger.info("Registering commands");
		event.registerServerCommand(new CommandTCO());
		DimensionHandler.getInstance().loadDimensions();
	}

	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
		MinecraftForge.EVENT_BUS.register(SpawnHandler.class);
	}
}
