package io.github.wouink.dokipa.network;

import com.google.common.base.Charsets;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import io.github.wouink.dokipa.Dokipa;
import io.github.wouink.dokipa.DokipaDoorBlock;
import io.github.wouink.dokipa.DokipaDoorBlockEntity;
import io.github.wouink.dokipa.MemorizedLocation;
import io.github.wouink.dokipa.server.DokipaSavedData;
import io.github.wouink.dokipa.server.DoorInfo;
import io.github.wouink.dokipa.server.LocalizedBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/*
    The client sends a message containing the UUID of a door it wants
    to save the location for later use with an associated description.
    The server saves this location in the DokipaSavedData and sends back
    a S2C_SendMemoryMessage to the client.
 */
public class C2S_MemorizeLocationMessage extends BaseC2SMessage {
    private String description;
    private BlockPos targetPos;
    private Type control;

    public enum Type {
        MEMORIZE,
        FORGET
    }

    public C2S_MemorizeLocationMessage(Type control, String description, BlockPos targetPos) {
        this.control = control;
        this.description = description;
        this.targetPos = targetPos;
    }

    public C2S_MemorizeLocationMessage(FriendlyByteBuf buf) {
        this.control = Type.values()[buf.readByte()];
        int descLength = buf.readInt();
        this.description = buf.readCharSequence(descLength, Charsets.UTF_8).toString();
        this.targetPos = buf.readBlockPos();
    }

    @Override
    public MessageType getType() {
        return Dokipa.C2S_MEMORIZE_MSG;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(this.control.ordinal());
        buf.writeInt(this.description.length());
        buf.writeCharSequence(this.description, Charsets.UTF_8);
        buf.writeBlockPos(this.targetPos);
    }

    // C2S Message -> handle on server
    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            Player player = context.getPlayer();
            Level playerLevel = player.level();
            UUID doorUUID = null;

            BlockState state = playerLevel.getBlockState(targetPos);
            if(state.is(Dokipa.Dokipa_Door.get())) {
                if(playerLevel.getBlockEntity(DokipaDoorBlock.getBlockEntityPos(state, targetPos)) instanceof DokipaDoorBlockEntity dokipaDoor) {
                    doorUUID = dokipaDoor.getDoorUUID();
                }
            }

            if(doorUUID != null) {
                MinecraftServer server = player.getServer();
                DokipaSavedData savedData = Dokipa.savedData(server);
                DoorInfo doorInfo = savedData.doorInfo(doorUUID);

                if(doorInfo != null && doorInfo.isOwner(player)) {
                    if (control == Type.MEMORIZE) {
                        LocalizedBlockPos doorPos = doorInfo.placedPos();
                        if(doorPos != null) {
                            MemorizedLocation loc = new MemorizedLocation(description, doorPos, state.getValue(DokipaDoorBlock.FACING));
                            savedData.memorizedLocations(player).add(loc);
                            savedData.setDirty();
                            new S2C_SendMemorizedLocationMessage(loc).sendTo((ServerPlayer) player);
                        }
                    }
                }

                // todo inform the player it cannot memorize the location when it does not own the door
            }

            // todo implement FORGET control type
        });
    }
}
