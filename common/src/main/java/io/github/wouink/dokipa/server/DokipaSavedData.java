package io.github.wouink.dokipa.server;

import io.github.wouink.dokipa.Dokipa;
import io.github.wouink.dokipa.MemorizedLocation;
import io.github.wouink.dokipa.network.S2C_SendMemorizedLocationMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class DokipaSavedData extends SavedData {
    private static String DATA_NAME = Dokipa.MODID;
    private HashMap<UUID, DoorInfo> doors = new HashMap<>();
    private HashMap<UUID, List<MemorizedLocation>> locations = new HashMap<>();
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
        int memIndex = 0;
        CompoundTag memoriesTag = new CompoundTag();
        for(UUID playerUUID : locations.keySet()) {
            for(MemorizedLocation loc : locations.get(playerUUID)) {
                CompoundTag memory = new CompoundTag();
                memory.putUUID("OwnerUUID", playerUUID);
                loc.save(memory);
                memoriesTag.put("Mem" + memIndex, memory);
                memIndex++;
            }
        }
        compoundTag.put("MemorizedLocations", memoriesTag);

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
        CompoundTag memoriesTag = tag.getCompound("MemorizedLocations");
        int memIndex = 0;
        while(memoriesTag.contains("Mem" + memIndex)) {
            CompoundTag memory = memoriesTag.getCompound("Mem" + memIndex);
            UUID ownerUUID = memory.getUUID("OwnerUUID");
            MemorizedLocation loc = new MemorizedLocation(memory);

            if(!savedData.locations.containsKey(ownerUUID)) {
                savedData.locations.put(ownerUUID, new ArrayList<>());
            }

            savedData.locations.get(ownerUUID).add(loc);

            memIndex++;
        }

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

    public List<MemorizedLocation> memorizedLocations(Entity entity) {
        if(locations.get(entity.getUUID()) == null) {
            locations.put(entity.getUUID(), new ArrayList<>());
        }
        return locations.get(entity.getUUID());
    }

    public void sendMemorizedLocations(ServerPlayer player) {
        memorizedLocations(player).forEach(memorizedLocation -> {
            Dokipa.LOG.info("Sending MemorizedLocation \"" + memorizedLocation.getDescription() + "\" to player");
            new S2C_SendMemorizedLocationMessage(memorizedLocation).sendTo(player);
        });
    }

    public int nextRoomNumber() {
        int ret = this.generatedRooms;
        this.generatedRooms++;
        setDirty();
        return ret;
    }
}