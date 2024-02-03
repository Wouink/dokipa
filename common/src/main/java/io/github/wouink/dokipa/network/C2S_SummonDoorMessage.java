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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class C2S_SummonDoorMessage extends BaseC2SMessage {
    public static final BlockPos Unsummon_Pos = new BlockPos(0, -127, 0);
    private BlockPos lookingAt;
    private Direction facing;

    public C2S_SummonDoorMessage(BlockPos _lookingAt, Direction _facing) {
        this.lookingAt = _lookingAt;
        this.facing = _facing;
    }

    public C2S_SummonDoorMessage(FriendlyByteBuf buf) {
        this.lookingAt = buf.readBlockPos();
        this.facing = Direction.values()[buf.readByte()];
    }

    @Override
    public MessageType getType() {
        return Dokipa.C2S_DOOR_SUMMON_MSG;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(lookingAt);
        buf.writeByte(facing.ordinal());
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            Player player = context.getPlayer();
            Dokipa.LOG.info("Received Door Summon message for Entity " + player.getUUID() + " at " + lookingAt);

            // the dokipa cannot summon his door when inside his door
            if(!player.level().dimension().location().equals(Dokipa.Dimension_Id)) {
                MinecraftServer server = player.getServer();
                DokipaDataManager dataManager = DokipaDataManager.getInstance(server);
                UUID doorUUID = dataManager.getDoorForEntity(player);

                if (doorUUID != null) {
                    Level currentDoorLevel = dataManager.getDoorDimension(doorUUID, server);
                    Level playerLevel = player.level();
                    BlockPos currentDoorPos = dataManager.getDoorPos(doorUUID);
                    // todo what is the current door's chunk is not loaded
                    if (currentDoorPos != null) DokipaDoorBlock.unsummon(currentDoorLevel, currentDoorPos);
                    if (!lookingAt.equals(Unsummon_Pos)) DokipaDoorBlock.summon(playerLevel, lookingAt.above(), doorUUID, facing);
                }
            }
        });
    }
}
