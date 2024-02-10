package io.github.wouink.dokipa.server;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class Util {
    public static CompoundTag saveBlockPos(BlockPos pos, CompoundTag tag) {
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        return tag;
    }

    public static BlockPos loadBlockPos(CompoundTag tag) {
        int x = tag.getInt("X");
        int y = tag.getInt("Y");
        int z = tag.getInt("Z");
        return new BlockPos(x, y, z);
    }
}
