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

public class DokipaSavedData extends SavedData {
    private static String DATA_NAME = Dokipa.MODID;
    private HashMap<UUID, DoorInfo> doors = new HashMap<>();
    private int generatedRooms = 0;

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        // save doors
        int doorIndex = 0;
        CompoundTag doorsTag = new CompoundTag();
        for(UUID uuid : doors.keySet()) {
            CompoundTag doorTag = new CompoundTag();
            doorTag.putUUID("UUID", uuid);
            doorsTag.put("Door" + doorIndex, doors.get(uuid).save(doorTag));
            doorIndex++;
        }
        compoundTag.put("SavedDoors", doorsTag);

        // save memories
        // todo

        // other data
        compoundTag.putInt("GeneratedRooms", generatedRooms);

        return compoundTag;
    }

    public static DokipaSavedData load(CompoundTag tag) {
        DokipaSavedData savedData = new DokipaSavedData();

        // load doors
        CompoundTag doorsTag = tag.getCompound("SavedDoors");
        int doorIndex = 0;
        while(doorsTag.contains("Door" + doorIndex)) {
            CompoundTag doorTag = doorsTag.getCompound("Door" + doorIndex);
            UUID doorUUID = doorTag.getUUID("UUID");
            savedData.doors.put(doorUUID, new DoorInfo(doorTag, doorUUID));
            doorIndex++;
        }

        // load memories
        // todo

        // other data
        savedData.generatedRooms = tag.getInt("GeneratedRooms");

        return savedData;
    }

    public static DokipaSavedData init(MinecraftServer server) {
        DokipaSavedData savedData = Objects.requireNonNull(server.overworld()).getDataStorage().computeIfAbsent(DokipaSavedData::load, DokipaSavedData::new, DATA_NAME);
        return savedData;
    }

    public UUID newDoor(BlockPos placedPos, ServerLevel placedLevel) {
        UUID newDoorUUID = UUID.randomUUID();
        this.doors.put(newDoorUUID, new DoorInfo(new LocalizedBlockPos(placedPos, placedLevel), null, newDoorUUID));
        setDirty();
        return newDoorUUID;
    }

    public DoorInfo doorInfo(UUID doorUUID) {
        return doors.get(doorUUID);
    }

    public DoorInfo doorForEntity(Entity entity) {
        Dokipa.LOG.info("Trying to find a door for entity " + entity.getUUID());
        DoorInfo foundDoor = null;
        Iterator<DoorInfo> it = doors.values().iterator();
        while(foundDoor == null && it.hasNext()) {
            DoorInfo door = it.next();
            if(door.isOwner(entity)) foundDoor = door;
        }
        return foundDoor;
    }

    public boolean hasDoor(Entity entity) {
        return doorForEntity(entity) != null;
    }

    public int nextRoomNumber() {
        int ret = this.generatedRooms;
        this.generatedRooms++;
        setDirty();
        return ret;
    }
}
