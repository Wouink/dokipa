package io.github.wouink.dokipa.server;

import io.github.wouink.dokipa.Dokipa;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

public class DokipaDataManager extends SavedData {
    private static final String DATA_NAME = Dokipa.MODID;
    private static DokipaDataManager instance = null;

    private HashMap<UUID, DokipaDoorData> doors = new HashMap<>();

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag doorsTag = new CompoundTag();
        int n = 0;
        Dokipa.LOG.info(doors.size() + " Dokipa doors will be saved");
        for(UUID uuid : doors.keySet()) {
            DokipaDoorData data = doors.get(uuid);
            Dokipa.LOG.info("- Door" + n + " = " + uuid + " at " + data.getPos());
            CompoundTag doorData = new CompoundTag();
            data.save(doorData);
            doorData.putUUID("DoorUUID", uuid);
            doorsTag.put("Door" + n, doorData);
            n++;
        }
        tag.put("SavedDoors", doorsTag);
        return tag;
    }

    public static DokipaDataManager load(CompoundTag tag) {
        DokipaDataManager dataManager = new DokipaDataManager();
        CompoundTag savedDoors = tag.getCompound("SavedDoors");
        int i = 0;
        while(savedDoors.contains("Door" + i)) {
            CompoundTag doorTag = savedDoors.getCompound("Door" + i);
            UUID doorUUID = doorTag.getUUID("DoorUUID");
            DokipaDoorData doorData = new DokipaDoorData(doorTag);
            dataManager.doors.put(doorUUID, doorData);
            i++;
        }
        return dataManager;
    }

    public UUID newDoor(BlockPos pos) {
        UUID newDoorUUID = UUID.randomUUID();
        DokipaDoorData doorData = new DokipaDoorData(pos, null);
        doors.put(newDoorUUID, doorData);
        setDirty();
        return newDoorUUID;
    }

    public void deleteDoor(UUID doorUUID) {
        this.doors.remove(doorUUID);
        setDirty();
    }

    public boolean setDoorOwner(UUID doorUUID, Entity entity) {
        boolean ret = doors.get(doorUUID).setOwner(entity);
        setDirty();
        return ret;
    }

    public boolean setDoorPos(UUID doorUUID, BlockPos pos) {
        boolean ret = doors.get(doorUUID).setPos(pos);
        setDirty();
        return ret;
    }

    public UUID getDoorOwner(UUID doorUUID) {
        return doors.get(doorUUID).getOwner();
    }

    public BlockPos getDoorPos(UUID doorUUID) {
        return doors.get(doorUUID).getPos();
    }

    public boolean hasOwner(UUID doorUUID) {
        return getDoorOwner(doorUUID) != null;
    }

    public boolean isOwner(UUID doorUUID, Entity entity) {
        Dokipa.logWithLevel(entity.level(), "Data Manager (isOwner)", "Door UUID is " + doorUUID + ", door owner is " + getDoorOwner(doorUUID) + ", entity is " + entity.getUUID());
        return hasOwner(doorUUID) && getDoorOwner(doorUUID).equals(entity.getUUID());
    }

    public UUID getDoorForEntity(Entity entity) {
        // the entity may only own one door
        Iterator<UUID> it = doors.keySet().iterator();
        UUID foundDoor = null;
        while(foundDoor == null && it.hasNext()) {
            UUID doorUUID = it.next();
            if(isOwner(doorUUID, entity)) foundDoor = doorUUID;
        }
        return foundDoor;
    }

    public boolean isDokipa(Entity entity) {
        return getDoorForEntity(entity) != null;
    }

    public static DokipaDataManager getInstance(MinecraftServer server) {
        if(instance != null) return instance;
        else {
            ServerLevel overworld = server.overworld();
            return Objects.requireNonNull(overworld).getDataStorage().computeIfAbsent(DokipaDataManager::load, DokipaDataManager::new, DATA_NAME);
        }
    }
}
