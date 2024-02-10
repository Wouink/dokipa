package io.github.wouink.dokipa.server;

import io.github.wouink.dokipa.Dokipa;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public class DoorInfo {
    private LocalizedBlockPos placedPos;
    private BlockPos posInRoom;
    private UUID ownerUUID;
    private UUID doorUUID;

    public CompoundTag save(CompoundTag tag) {
        if(placedPos != null) {
            tag.put("PlacedPos", placedPos.save(new CompoundTag()));
        }
        if(posInRoom != null) {
            tag.put("PosInRoom", Util.saveBlockPos(posInRoom, new CompoundTag()));
        }
        if(ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        return tag;
    }

    public DoorInfo(CompoundTag tag, UUID doorUUID) {
        this.doorUUID = doorUUID;
        if(tag.contains("PlacedPos")) {
            this.placedPos = new LocalizedBlockPos(tag.getCompound("PlacedPos"));
        }
        if(tag.contains("PosInRoom")) {
            this.posInRoom = Util.loadBlockPos(tag.getCompound("PosInRoom"));
        }
        if(tag.contains("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
    }

    public DoorInfo(UUID doorUUID) {
        this.doorUUID = doorUUID;
    }

    public DoorInfo(LocalizedBlockPos placedPos, BlockPos posInRoom, UUID doorUUID) {
        this.doorUUID = doorUUID;
        this.placedPos = placedPos;
        this.posInRoom = posInRoom;
    }

    public UUID uuid() {
        return this.doorUUID;
    }

    public void setPlacedPos(LocalizedBlockPos lpos) {
        this.placedPos = lpos;
    }

    public void setPosInRoom(BlockPos pos) {
        this.posInRoom = pos;
    }

    public LocalizedBlockPos placedPos() {
        return this.placedPos;
    }

    public BlockPos posInRoom() {
        return this.posInRoom;
    }

    public LocalizedBlockPos localizedPosInRoom(MinecraftServer server) {
        return new LocalizedBlockPos(posInRoom(), server.getLevel(Dokipa.Dimension));
    }

    public UUID getOwner() {
        return ownerUUID;
    }

    public void setOwner(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public void setOwner(Entity entity) {
        this.ownerUUID = entity.getUUID();
    }

    public boolean hasOwner() {
        return ownerUUID != null;
    }

    public boolean isOwner(Entity entity) {
        return hasOwner() && ownerUUID.equals(entity.getUUID());
    }

    public boolean isRoomGenerated() {
        return posInRoom() != null;
    }
}
