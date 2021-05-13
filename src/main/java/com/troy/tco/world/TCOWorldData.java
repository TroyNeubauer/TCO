package com.troy.tco.world;

import com.troy.tco.TCO;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import org.lwjgl.util.vector.Vector3f;
import scala.Int;

import java.util.ArrayList;
import java.util.Objects;

public class TCOWorldData extends WorldSavedData {

	private Vector3f lobbySpawn, policeSpawn, tcoSpawn;
	private Vector3f minPolice, maxPolice;
	private ArrayList<Vector3f> spawns;
	//The dimension ID of the parent map or null if template map
	private Integer parentID;

	private static final String DATA_NAME = TCO.MODID + "_GameData";

	public TCOWorldData(String name) {
		super(name);
		this.parentID = null;
		this.spawns = new ArrayList<>();
	}

	public TCOWorldData() {
		this(DATA_NAME);
		markDirty();
	}

	public TCOWorldData(TCOWorldData data, int parentID) {
		super(DATA_NAME);
		this.lobbySpawn = data.lobbySpawn;
		this.policeSpawn = data.policeSpawn;
		this.tcoSpawn = data.tcoSpawn;
		this.minPolice = data.minPolice;
		this.maxPolice = data.maxPolice;
		this.parentID = parentID;
		this.spawns = new ArrayList<>(data.spawns);
		this.markDirty();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		TCO.logger.trace("TCOWorldData::readFromNBT");
		this.lobbySpawn = readVec(nbt, "lobbySpawn");
		this.policeSpawn = readVec(nbt, "policeSpawn");
		this.tcoSpawn = readVec(nbt, "tcoSpawn");
		this.minPolice = readVec(nbt, "minPolice");
		this.maxPolice = readVec(nbt, "maxPolice");
		if (nbt.getBoolean("isTemplate")) {
			this.parentID = null;
		} else {
			this.parentID = nbt.getInteger("parentID");
		}

		NBTTagList nbtSpawns = nbt.getTagList("spawns", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < nbtSpawns.tagCount(); i++) {
			NBTTagCompound pos = (NBTTagCompound) nbtSpawns.get(i);
			Vector3f vec = readVec(pos, "");
			this.spawns.add(vec);
		}
	}

	private void writeVec(NBTTagCompound nbt, Vector3f vec, String name) {
		if (vec == null) {
			nbt.setBoolean(name, true);
		} else {
			nbt.setFloat(name + "x", vec.x);
			nbt.setFloat(name + "y", vec.y);
			nbt.setFloat(name + "z", vec.z);
		}
	}

	private Vector3f readVec(NBTTagCompound nbt, String name) {
		if (nbt.hasKey(name) && nbt.getBoolean(name)) {
			return null;
		} else {
			return new Vector3f(nbt.getFloat(name + "x"), nbt.getFloat(name + "y"), nbt.getFloat(name + "z"));
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		TCO.logger.trace("TCOWorldData::writeToNBT");
		NBTTagCompound result = new NBTTagCompound();
		writeVec(result, this.lobbySpawn, "lobbySpawn");
		writeVec(result, this.policeSpawn, "policeSpawn");
		writeVec(result, this.tcoSpawn, "tcoSpawn");
		writeVec(result, this.minPolice, "minPolice");
		writeVec(result, this.maxPolice, "maxPolice");
		result.setBoolean("isTemplate", this.isTemplate());
		if (!this.isTemplate()) {
			result.setInteger("parentID", parentID);
		}

		NBTTagList nbtSpawns = new NBTTagList();
		int i = 0;
		for (Vector3f spawn : this.spawns) {
			NBTTagCompound pos = new NBTTagCompound();
			writeVec(pos, spawn, "");
			nbtSpawns.appendTag(pos);
		}
		result.setTag("spawns", nbtSpawns);
		return result;
	}

	public static TCOWorldData create(World world) {
		TCOWorldData data = new TCOWorldData();
		world.getPerWorldStorage().setData(DATA_NAME, data);
		return data;
	}

	public static void set(World world, TCOWorldData data) {
		world.getPerWorldStorage().setData(DATA_NAME, data);
	}

	public static TCOWorldData get(World world) {
		if (world == null) {
			return null;
		}
		return (TCOWorldData) world.getPerWorldStorage().getOrLoadData(TCOWorldData.class, DATA_NAME);
	}

	public static TCOWorldData getOrCreate(World world) {
		Objects.requireNonNull(world);
		TCOWorldData instance = get(world);

		if (instance == null) {
			return create(world);
		} else {
			return instance;
		}
	}

	public Vector3f getLobbySpawn() {
		return lobbySpawn;
	}

	public void setLobbySpawn(Vector3f lobbySpawn) {
		this.lobbySpawn = lobbySpawn;
		this.markDirty();
	}

	public Vector3f getPoliceSpawn() {
		return policeSpawn;
	}

	public void setPoliceSpawn(Vector3f policeSpawn) {
		this.policeSpawn = policeSpawn;
		this.markDirty();
	}

	public Vector3f getTcoSpawn() {
		return tcoSpawn;
	}

	public void setTcoSpawn(Vector3f tcoSpawn) {
		this.tcoSpawn = tcoSpawn;
		this.markDirty();
	}

	public ArrayList<Vector3f> getSpawns() {
		return spawns;
	}
	
	public void addSpawn(Vector3f pos) {
		this.spawns.add(pos);
		this.markDirty();
	}

	public boolean isTemplate() {
		return this.parentID == null;
	}

	public int getParentID() {
		return parentID;
	}

	public boolean isReady() {
		return lobbySpawn != null && policeSpawn != null && tcoSpawn != null && spawns.size() > 0;
	}

	@Override
	public String toString() {
		if (spawns.size() <= 5) {
			return "TCOWorldData{" +
					"lobbySpawn=" + lobbySpawn +
					", policeSpawn=" + policeSpawn +
					", tcoSpawn=" + tcoSpawn +
					", spawns=" + spawns +
					", parentID=" + String.valueOf(parentID) +
					'}';
		}
		return "TCOWorldData{" +
				"lobbySpawn=" + lobbySpawn +
				", policeSpawn=" + policeSpawn +
				", tcoSpawn=" + tcoSpawn +
				", spawns=(count:" + spawns.size() + ")" +
				", parentID=" + String.valueOf(parentID) +
				'}';
	}
}
