package io.github.wouink.dokipa.server;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public class DokipaDoorData {
    private BlockPos pos;
    private UUID owner;

    public DokipaDoorData(BlockPos pos, UUID owner) {
        this.pos = pos;
        this.owner = owner;
    }

    public DokipaDoorData(CompoundTag tag) {
        if(tag.contains("X")) {
            int x = tag.getInt("X");
            int y = tag.getInt("Y");
            int z = tag.getInt("Z");
            this.pos = new BlockPos(x, y, z);
        } else this.pos = null;
        if(tag.contains("OwnerUUID")) {
            this.owner = tag.getUUID("OwnerUUID");
        } else this.owner = null;
    }

    public CompoundTag save(CompoundTag tag) {
        if(pos != null) {
            tag.putInt("X", pos.getX());
            tag.putInt("Y", pos.getY());
            tag.putInt("Z", pos.getZ());
        }
        if(owner != null) tag.putUUID("OwnerUUID", owner);
        return tag;
    }

    public boolean setOwner(Entity entity) {
        this.owner = entity.getUUID();
        return true;
    }

    public boolean setPos(BlockPos pos) {
        this.pos = pos;
        return true;
    }

    public UUID getOwner() {
        return this.owner;
    }

    public BlockPos getPos() {
        return this.pos;
    }
}
