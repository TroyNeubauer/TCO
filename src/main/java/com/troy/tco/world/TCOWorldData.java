package com.troy.tco.world;

import com.troy.tco.TCO;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;

public class TCOWorldData extends WorldSavedData {

	private Vector3f lobbySpawn, policeSpawn, tcoSpawn;
	private ArrayList<Vector3f> spawns;
	private boolean isTemplate;

	private static final String DATA_NAME = TCO.MODID + "_GameData";

	public TCOWorldData() {
		super(DATA_NAME);
		this.isTemplate = true;
		this.spawns = new ArrayList<>();
	}

	public TCOWorldData(TCOWorldData data) {
		super(DATA_NAME);
		this.lobbySpawn = data.lobbySpawn;
		this.policeSpawn = data.policeSpawn;
		this.tcoSpawn = data.tcoSpawn;
		this.isTemplate = false;
		this.spawns = new ArrayList<>(data.spawns);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		this.lobbySpawn = readVec(nbt, "lobbySpawn");
		this.policeSpawn = readVec(nbt, "policeSpawn");
		this.tcoSpawn = readVec(nbt, "tcoSpawn");
		this.isTemplate = nbt.getBoolean("isTemplate");

		NBTTagCompound nbtSpawns = nbt.getCompoundTag("spawns");
		for (int i = 0; i < nbtSpawns.getSize(); i++) {
			NBTTagCompound pos = nbtSpawns.getCompoundTag(Integer.toHexString(i));
			Vector3f vec = readVec(pos, "");
			this.spawns.add(vec);
		}
	}

	private void writeVec(NBTTagCompound nbt, Vector3f vec, String name) {
		nbt.setFloat(name + "x", vec.x);
		nbt.setFloat(name + "y", vec.y);
		nbt.setFloat(name + "z", vec.z);
	}

	private Vector3f readVec(NBTTagCompound nbt, String name) {
		return new Vector3f(nbt.getFloat(name + "x"), nbt.getFloat(name + "y"), nbt.getFloat(name + "z"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTTagCompound result = new NBTTagCompound();
		writeVec(result, this.lobbySpawn, "lobbySpawn");
		writeVec(result, this.policeSpawn, "policeSpawn");
		writeVec(result, this.tcoSpawn, "tcoSpawn");
		result.setBoolean("isTemplate", this.isTemplate);

		NBTTagCompound nbtSpawns = new NBTTagCompound();
		int i = 0;
		for (Vector3f spawn : this.spawns) {
			NBTTagCompound pos = new NBTTagCompound();
			writeVec(pos, spawn, "");
			nbtSpawns.setTag(Integer.toHexString(i++), pos);
		}
		result.setTag("spawns", nbtSpawns);
		return result;
	}

	public static TCOWorldData get(World world) {
		// The IS_GLOBAL constant is there for clarity, and should be simplified into the right branch.
		return (TCOWorldData) world.getPerWorldStorage().getOrLoadData(TCOWorldData.class, DATA_NAME);
	}

	public static TCOWorldData getOrCreate(World world) {
		// The IS_GLOBAL constant is there for clarity, and should be simplified into the right branch.
		TCOWorldData instance = get(world);

		if (instance == null) {
			instance = new TCOWorldData();
			world.getPerWorldStorage().setData(DATA_NAME, instance);
		}
		return instance;
	}

	public Vector3f getLobbySpawn() {
		return lobbySpawn;
	}

	public Vector3f getPoliceSpawn() {
		return policeSpawn;
	}

	public Vector3f getTcoSpawn() {
		return tcoSpawn;
	}

	public ArrayList<Vector3f> getSpawns() {
		return spawns;
	}

	public boolean isTemplate() {
		return isTemplate;
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
					", isTemplate=" + isTemplate +
					'}';
		}
		return "TCOWorldData{" +
				"lobbySpawn=" + lobbySpawn +
				", policeSpawn=" + policeSpawn +
				", tcoSpawn=" + tcoSpawn +
				", spawns=(count:" + spawns.size() + ")" +
				", isTemplate=" + isTemplate +
				'}';
	}
}
