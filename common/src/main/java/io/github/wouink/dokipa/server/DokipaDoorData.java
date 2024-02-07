package io.github.wouink.dokipa.server;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public class DokipaDoorData {
    private BlockPos pos;
    private BlockPos roomPos;
    private UUID owner;
    private ResourceLocation dimension;

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
        if(tag.contains("RoomX")) {
            int x = tag.getInt("RoomX");
            int y = tag.getInt("RoomY");
            int z = tag.getInt("RoomZ");
            this.roomPos = new BlockPos(x, y, z);
        } else this.roomPos = null;
        if(tag.contains("Dimension")) this.dimension = new ResourceLocation(tag.getString("Dimension"));
        else this.dimension = new ResourceLocation("minecraft", "overworld");
    }

    public CompoundTag save(CompoundTag tag) {
        if(pos != null) {
            tag.putInt("X", pos.getX());
            tag.putInt("Y", pos.getY());
            tag.putInt("Z", pos.getZ());
        }
        if(owner != null) tag.putUUID("OwnerUUID", owner);
        if(roomPos != null) {
            tag.putInt("RoomX", roomPos.getX());
            tag.putInt("RoomY", roomPos.getY());
            tag.putInt("RoomZ", roomPos.getZ());
        }
        if(dimension != null) tag.putString("Dimension", dimension.toString());
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

    public boolean setRoomPos(BlockPos pos) {
        this.roomPos = pos;
        return true;
    }

    public UUID getOwner() {
        return this.owner;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public BlockPos getRoomPos() {
        return this.roomPos;
    }

    public ResourceLocation getDimension() {
        return this.dimension;
    }

    public void setDimension(ResourceLocation dim) {
        this.dimension = dim;
    }
}
