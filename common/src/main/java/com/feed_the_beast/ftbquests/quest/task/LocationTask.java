package com.feed_the_beast.ftbquests.quest.task;

import com.feed_the_beast.ftbquests.quest.PlayerData;
import com.feed_the_beast.ftbquests.quest.Quest;
import com.feed_the_beast.mods.ftbguilibrary.config.ConfigGroup;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * @author LatvianModder
 */
public class LocationTask extends Task {
	public ResourceKey<Level> dimension;
	public boolean ignoreDimension;
	public int x, y, z;
	public int w, h, d;

	public LocationTask(Quest quest) {
		super(quest);
		dimension = Level.OVERWORLD;
		ignoreDimension = false;
		x = 0;
		y = 0;
		z = 0;
		w = 1;
		h = 1;
		d = 1;
	}

	@Override
	public TaskType getType() {
		return TaskTypes.LOCATION;
	}

	@Override
	public void writeData(CompoundTag nbt) {
		super.writeData(nbt);
		nbt.putString("dimension", dimension.location().toString());
		nbt.putBoolean("ignore_dimension", ignoreDimension);
		nbt.putIntArray("position", new int[]{x, y, z});
		nbt.putIntArray("size", new int[]{w, h, d});
	}

	@Override
	public void readData(CompoundTag nbt) {
		super.readData(nbt);
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(nbt.getString("dimension")));
		ignoreDimension = nbt.getBoolean("ignore_dimension");

		int[] pos = nbt.getIntArray("position");

		if (pos.length == 3) {
			x = pos[0];
			y = pos[1];
			z = pos[2];
		}

		int[] size = nbt.getIntArray("size");

		if (pos.length == 3) {
			w = size[0];
			h = size[1];
			d = size[2];
		}
	}

	@Override
	public void writeNetData(FriendlyByteBuf buffer) {
		super.writeNetData(buffer);
		buffer.writeResourceLocation(dimension.location());
		buffer.writeBoolean(ignoreDimension);
		buffer.writeVarInt(x);
		buffer.writeVarInt(y);
		buffer.writeVarInt(z);
		buffer.writeVarInt(w);
		buffer.writeVarInt(h);
		buffer.writeVarInt(d);
	}

	@Override
	public void readNetData(FriendlyByteBuf buffer) {
		super.readNetData(buffer);
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buffer.readResourceLocation());
		ignoreDimension = buffer.readBoolean();
		x = buffer.readVarInt();
		y = buffer.readVarInt();
		z = buffer.readVarInt();
		w = buffer.readVarInt();
		h = buffer.readVarInt();
		d = buffer.readVarInt();
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void getConfig(ConfigGroup config) {
		super.getConfig(config);
		config.addString("dim", dimension.location().toString(), v -> dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(v)), "minecraft:overworld");
		config.addBool("ignore_dim", ignoreDimension, v -> ignoreDimension = v, false);
		config.addInt("x", x, v -> x = v, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
		config.addInt("y", y, v -> y = v, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
		config.addInt("z", z, v -> z = v, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
		config.addInt("w", w, v -> w = v, 1, 1, Integer.MAX_VALUE);
		config.addInt("h", h, v -> h = v, 1, 1, Integer.MAX_VALUE);
		config.addInt("d", d, v -> d = v, 1, 1, Integer.MAX_VALUE);
	}

	@Override
	public int autoSubmitOnPlayerTick() {
		return 3;
	}

	@Override
	public TaskData createData(PlayerData data) {
		return new Data(this, data);
	}

	public static class Data extends BooleanTaskData<LocationTask> {
		private Data(LocationTask task, PlayerData data) {
			super(task, data);
		}

		@Override
		public String getProgressString() {
			return progress > 0 ? "1" : "0";
		}

		@Override
		public boolean canSubmit(ServerPlayer player) {
			if (task.ignoreDimension || task.dimension == player.level.dimension()) {
				int y = Mth.floor(player.getY());

				if (y >= task.y && y < task.y + task.h) {
					int x = Mth.floor(player.getX());

					if (x >= task.x && x < task.x + task.w) {
						int z = Mth.floor(player.getZ());
						return z >= task.z && z < task.z + task.d;
					}
				}
			}

			return false;
		}
	}
}
