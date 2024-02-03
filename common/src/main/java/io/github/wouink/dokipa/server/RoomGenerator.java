package io.github.wouink.dokipa.server;

import io.github.wouink.dokipa.Dokipa;
import io.github.wouink.dokipa.DokipaDoorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;
import java.util.UUID;

public class RoomGenerator {
    // the door position in the structure relative to its placement corner
    public static final int Offset_X = 5;
    public static final int Offset_Y = 2;
    public static final int Offset_Z = 1;

    // the gap between each placement corner
    public static final int Gap = 32;

    public static final BlockPos Null_Door_Pos = new BlockPos(0, 0, 0);

    public static final ResourceLocation Room_Structure = new ResourceLocation(Dokipa.MODID, "room");

    // returns the door position for this room in the dimension
    public static BlockPos generateRoom(ServerLevel level, BlockPos start, UUID doorUUID) {
        BlockPos doorPos = Null_Door_Pos;
        Optional<StructureTemplate> roomStructure = level.getStructureManager().get(Room_Structure);
        if(roomStructure.isPresent()) {
            StructureTemplate room = roomStructure.get();

            // why?
            BlockPos centerPos = start.offset(room.getSize().getX() / 2, 0, room.getSize().getZ() / 2);
            StructurePlaceSettings settings = new StructurePlaceSettings();
            if(room.placeInWorld(level, start, centerPos, settings, level.getRandom(), Block.UPDATE_ALL)) {
                Dokipa.LOG.info("Room generated at " + start + " for door " + doorUUID);
                // set the door uuid
                BlockPos potentialDoorPos = start.offset(Offset_X, Offset_Y, Offset_Z);
                if(level.getBlockEntity(potentialDoorPos) instanceof DokipaDoorBlockEntity dokipaDoor) {
                    doorPos = potentialDoorPos;
                    dokipaDoor.setDoorUUID(doorUUID);
                } else {
                    Dokipa.LOG.info("Did not find a door at " + potentialDoorPos);
                }
            }
        } else {
            Dokipa.LOG.error("Room structure not found");
        }
        return doorPos;
    }

    public static BlockPos getPosForRoom(int roomNumber) {
        return new BlockPos(Gap * roomNumber, 0, 0);
    }
}
