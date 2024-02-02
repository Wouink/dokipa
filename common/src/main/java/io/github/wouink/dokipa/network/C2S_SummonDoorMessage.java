package io.github.wouink.dokipa.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import io.github.wouink.dokipa.Dokipa;
import io.github.wouink.dokipa.DokipaDoorBlock;
import io.github.wouink.dokipa.server.DokipaDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class C2S_SummonDoorMessage extends BaseC2SMessage {
    public static final BlockPos Unsummon_Pos = new BlockPos(0, -127, 0);
    private UUID entityUUID;
    private BlockPos lookingAt;
    private Direction facing;

    public C2S_SummonDoorMessage(UUID _entityUUID, BlockPos _lookingAt, Direction _facing) {
        this.entityUUID = _entityUUID;
        this.lookingAt = _lookingAt;
        this.facing = _facing;
    }

    public C2S_SummonDoorMessage(FriendlyByteBuf buf) {
        this.entityUUID = buf.readUUID();
        this.lookingAt = buf.readBlockPos();
        this.facing = Direction.values()[buf.readByte()];
    }

    @Override
    public MessageType getType() {
        return Dokipa.C2S_DOOR_SUMMON_MSG;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
        buf.writeBlockPos(lookingAt);
        buf.writeByte(facing.ordinal());
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            Dokipa.LOG.info("Received Door Summon message for Entity " + entityUUID + " at " + lookingAt);
            MinecraftServer server = context.getPlayer().getServer();
            DokipaDataManager dataManager = DokipaDataManager.getInstance(server);
            UUID doorUUID = dataManager.getDoorForEntity(server.overworld().getEntity(entityUUID));
            if(doorUUID != null) {
                Level level = server.overworld().getLevel();
                BlockPos currentDoorPos = dataManager.getDoorPos(doorUUID);
                if(currentDoorPos != null) DokipaDoorBlock.unsummon(level, currentDoorPos);
                if(!lookingAt.equals(Unsummon_Pos)) DokipaDoorBlock.summon(level, lookingAt.above(), doorUUID, facing);
            }
        });
    }
}
