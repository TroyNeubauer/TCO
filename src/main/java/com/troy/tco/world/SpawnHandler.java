package com.troy.tco.world;

import com.troy.tco.DimensionHandler;
import com.troy.tco.TCO;
import com.troy.tco.commands.CommandTCO;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Vector3f;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TCO.MODID)
public class SpawnHandler {

	private static final NBTTagCompound SPAWNER_NBT;
	private static final ItemStack SPAWNER_ITEM_STACK;
	
	private static final HashMap<UUID, Integer> THROW_COUNTS = new HashMap<>();

	static {
		SPAWNER_NBT = new NBTTagCompound();
		SPAWNER_NBT.setByteArray("TCO_SPAWNER", new byte[] { 69, 4, 20 });

		//this.getOrCreateSubCompound("display").setString("Name", displayName);
		NBTTagCompound name = new NBTTagCompound();
		String nameStr = "Villager Spawner";
		name.setString("Name", nameStr);
		SPAWNER_NBT.setTag("display", name);

		SPAWNER_ITEM_STACK = new ItemStack(Items.SNOWBALL, 64, 0, null);
		SPAWNER_ITEM_STACK.setTagCompound(SPAWNER_NBT);
	}

	private static boolean enabled = false;

	@SideOnly(Side.SERVER)
	public static void enable(EntityPlayerMP player, ICommandSender sender) {
		enabled = true;
		player.getServer().getPlayerList().sendMessage(new TextComponentString("[TCO]: Adding villager spawns has been enabled! Use /tco spawn start to get your spawn egg"));
		start(player, sender);
	}

	@SideOnly(Side.SERVER)
	public static void disable(EntityPlayerMP player, ICommandSender sender) {
		enabled = false;
		for (EntityPlayerMP p : player.getServer().getPlayerList().getPlayers()) {
			stop(p, p);
		}
		player.getServer().getPlayerList().sendMessage(new TextComponentString("[TCO]: Adding new villager spawns has been disabled"));
	}

	@SideOnly(Side.SERVER)
	public static void start(EntityPlayerMP player, ICommandSender sender) {
		if (!enabled) {
			CommandTCO.sendMessage(sender, "Global spawning is not enabled! Ask an op to run /tco /spawn enable first");
		} else {
			player.addItemStackToInventory(SPAWNER_ITEM_STACK.copy());
			player.inventoryContainer.detectAndSendChanges();
			CommandTCO.sendMessage(sender, "Added villager spawner");
		}
	}

	@SideOnly(Side.SERVER)
	public static void stop(EntityPlayerMP player, ICommandSender sender) {
		int count = player.inventory.clearMatchingItems(Items.SNOWBALL, 0, Integer.MAX_VALUE, SPAWNER_NBT);
		player.inventoryContainer.detectAndSendChanges();

		CommandTCO.sendMessage(sender, "Cleared " + count + " spawner items");
	}

	@SideOnly(Side.SERVER)
	public static void policeSpawn(EntityPlayerMP player, ICommandSender sender) {
		Vector3f pos = new Vector3f((float) player.posX, (float) player.posY, (float) player.posZ);
		TCOWorldData.getOrCreate(player.getEntityWorld()).setPoliceSpawn(pos);
		CommandTCO.sendMessage(sender, "Set police spawn to " + pos);
	}

	@SideOnly(Side.SERVER)
	public static void tcoSpawn(EntityPlayerMP player, ICommandSender sender) {
		Vector3f pos = new Vector3f((float) player.posX, (float) player.posY, (float) player.posZ);
		TCOWorldData.getOrCreate(player.getEntityWorld()).setTcoSpawn(pos);
		CommandTCO.sendMessage(sender, "Set tco spawn to " + pos);
	}

	@SideOnly(Side.SERVER)
	public static void lobbySpawn(EntityPlayerMP player, ICommandSender sender) {
		Vector3f pos = new Vector3f((float) player.posX, (float) player.posY, (float) player.posZ);
		TCOWorldData.getOrCreate(player.getEntityWorld()).setLobbySpawn(pos);
		CommandTCO.sendMessage(sender, "Set lobby spawn to " + pos);
	}
	
	@SubscribeEvent
	@SideOnly(Side.SERVER)
	public static void snowballImpact(ProjectileImpactEvent.Throwable event) {
		if (event.getThrowable().getThrower() != null && event.getThrowable().getThrower() instanceof EntityPlayerMP) {
			EntityPlayerMP player = (EntityPlayerMP) event.getThrowable().getThrower();

			synchronized (THROW_COUNTS) {
				Integer countObj = THROW_COUNTS.get(player.getUniqueID());
				Objects.requireNonNull(countObj, "throwSnowball not called before useItem impact!");
				int count = countObj;
				count -= 1;
				if (count >= 0) {
					Vector3f pos = new Vector3f((float) event.getThrowable().posX, (float) event.getThrowable().posY, (float) event.getThrowable().posZ);
					TCOWorldData data = TCOWorldData.getOrCreate(player.getEntityWorld());
					data.addSpawn(pos);
					TCO.logger.info("Adding spawn " + pos + " for player " + player.getName() + ". World now has " + data.getSpawns().size());
					THROW_COUNTS.put(player.getUniqueID(), count);
				}
			}
		}
	}

	@SubscribeEvent
	@SideOnly(Side.SERVER)
	public static void throwSnowball(PlayerInteractEvent.RightClickItem event) {
		NBTTagCompound nbt = event.getItemStack().getTagCompound();
		if (nbt != null && NBTUtil.areNBTEquals(nbt, SPAWNER_NBT, true)) {
			TCO.logger.info("NBT matches ");
			synchronized (THROW_COUNTS) {
				UUID uuid = event.getEntityPlayer().getUniqueID();
				THROW_COUNTS.merge(uuid, 1, Integer::sum);
			}
			EntityPlayerMP player = ((EntityPlayerMP) event.getEntityPlayer());
		}
	}

	@SubscribeEvent
	public static void worldUnload(WorldEvent.Unload event) {
		int dimensionID = event.getWorld().provider.getDimension();
		TCO.logger.info("Unloading world " + dimensionID);

		if (!event.getWorld().isRemote) {
			DimensionHandler.getInstance().unload(event.getWorld(), dimensionID);
		}
	}
}
