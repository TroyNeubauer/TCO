package com.troy.tco.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.ArrayList;

public class MessageSyncTCOWorlds implements IMessage
{
	private ArrayList<Integer> dimensions;

	public MessageSyncTCOWorlds()
	{
		dimensions = new ArrayList<>();
	}

	@Override
	public void fromBytes(ByteBuf buf)
	{
		int length = buf.readInt();
		for (int i = 0; i < length; i++) {
			dimensions.add(buf.readInt());
		}
	}

	@Override
	public void toBytes(ByteBuf buf)
	{
		buf.writeInt(this.dimensions.size());
		for (Integer dimension : this.dimensions) {
			buf.writeInt(dimension);
		}
	}

	public void addDimension(int id) {
		dimensions.add(id);
	}

	public ArrayList<Integer> getDimensions() {
		return dimensions;
	}
}