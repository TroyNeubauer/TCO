package com.troy.tco.network;

import com.troy.tco.client.ClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class HandlerSyncTCOWorlds implements IMessageHandler<MessageSyncTCOWorlds, IMessage>
{
	@Override
	public IMessage onMessage(MessageSyncTCOWorlds message, MessageContext ctx)
	{
		Minecraft.getMinecraft().addScheduledTask(new Runnable()
		{
			@Override
			public void run()
			{
				ClientHandler.getInstance().sync(message.getDimensions());
			}
		});

		return null;
	}

}