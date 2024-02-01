package io.github.wouink.dokipa;

import io.github.wouink.dokipa.server.DokipaDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
            this.doorUUID = DokipaDataManager.getInstance(getLevel().getServer()).newDoor(getBlockPos());
        }
    }

    public boolean hasOwner() {
        initialize();
        return DokipaDataManager.getInstance(getLevel().getServer()).hasOwner(this.doorUUID);
    }

    public boolean trySetOwner(Entity entity) {
        initialize();
        if(!DokipaDataManager.getInstance(getLevel().getServer()).isDokipa(entity)) {
            DokipaDataManager.getInstance(getLevel().getServer()).setDoorOwner(this.doorUUID, entity);
            return true;
        } else {
            Dokipa.logWithLevel(getLevel(), "Door Block Entity", "This entity is already a dokipa");
            return false;
        }
    }

    public boolean isOwner(Entity entity) {
        initialize();
        return DokipaDataManager.getInstance(getLevel().getServer()).isOwner(this.doorUUID, entity);
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