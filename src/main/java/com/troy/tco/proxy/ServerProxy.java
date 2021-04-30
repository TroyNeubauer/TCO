package com.troy.tco.proxy;


import com.troy.tco.TCO;
import com.troy.tco.commands.CommandTCO;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public class ServerProxy extends CommonProxy {

	@Override
	public void serverStarting(FMLServerStartingEvent event) {
		TCO.logger.info("Registering commands");
		event.registerServerCommand(new CommandTCO());
	}
}
