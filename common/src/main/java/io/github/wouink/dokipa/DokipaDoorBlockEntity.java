package io.github.wouink.dokipa;

import io.github.wouink.dokipa.server.DokipaSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class DokipaDoorBlockEntity extends BlockEntity {
    private UUID doorUUID;

    public DokipaDoorBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(Dokipa.Dokipa_Door_BlockEntity.get(), blockPos, blockState);
        // level is null here
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        // level is null here
        if(compoundTag.contains("DoorUUID")) this.doorUUID = compoundTag.getUUID("DoorUUID");
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        initialize();
        super.saveAdditional(compoundTag);
        if(!getLevel().isClientSide()) compoundTag.putUUID("DoorUUID", this.doorUUID);
    }

    // this is called often because we cannot initialize the door
    // in the constructor or in the load method (where level is null)
    public void initialize() {
        if(!getLevel().isClientSide() && this.doorUUID == null) {
            Dokipa.logWithLevel(getLevel(), "Door Block Entity", "Initializing door at " + getBlockPos());
            DokipaSavedData savedData = Dokipa.savedData(level.getServer());
            this.doorUUID = savedData.newDoor(getBlockPos(), (ServerLevel) getLevel());
        }
    }

    public boolean hasOwner() {
        initialize();
        if(!getLevel().isClientSide()) {
            DokipaSavedData savedData = Dokipa.savedData(getLevel().getServer());
            return savedData.doorInfo(getDoorUUID()).hasOwner();
        } else {
            Dokipa.LOG.error("DokipaDoorBlockEntity.hasOwner called on client");
            return true;
        }
    }

    public boolean trySetOwner(Entity entity) {
        initialize();
        boolean ownerSet = false;
        if(!getLevel().isClientSide()) {
            DokipaSavedData savedData = Dokipa.savedData(getLevel().getServer());
            if(savedData.doorForEntity(entity) == null) {
                savedData.doorInfo(getDoorUUID()).setOwner(entity);
                savedData.setDirty();
                ownerSet = true;
            }
        } else {
            Dokipa.LOG.error("DokipaDoorBlockEntity.trySetOwner called on client");
        }
        return ownerSet;
    }

    public boolean isOwner(Entity entity) {
        initialize();
        if(!getLevel().isClientSide()) {
            DokipaSavedData savedData = Dokipa.savedData(getLevel().getServer());
            return savedData.doorInfo(getDoorUUID()).isOwner(entity);
        } else {
            Dokipa.LOG.error("DokipaDoorBlockEntity.isOwner called on client");
        }
        return false;
    }

    public UUID getDoorUUID() {
        return this.doorUUID;
    }

    public void setDoorUUID(UUID _doorUUID) {
        this.doorUUID = _doorUUID;
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }
}