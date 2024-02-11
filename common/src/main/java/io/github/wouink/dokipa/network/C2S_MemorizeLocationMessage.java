package io.github.wouink.dokipa.network;

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
import net.minecraft.nbt.CompoundTag;
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
    private MemorizedLocation loc;
    private Type control;

    public enum Type {
        MEMORIZE,
        FORGET
    }

    public C2S_MemorizeLocationMessage(Type control, MemorizedLocation loc) {
        this.control = control;
        this.loc = loc;
    }

    public C2S_MemorizeLocationMessage(FriendlyByteBuf buf) {
        this.control = Type.values()[buf.readByte()];
        this.loc = new MemorizedLocation(buf.readAnySizeNbt());
    }

    @Override
    public MessageType getType() {
        return Dokipa.C2S_MEMORIZE_MSG;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(this.control.ordinal());
        buf.writeNbt(this.loc.save(new CompoundTag()));
    }

    // C2S Message -> handle on server
    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            Player player = context.getPlayer();
            MinecraftServer server = player.getServer();
            DokipaSavedData savedData = Dokipa.savedData(server);

            if(control == Type.MEMORIZE) {
                Level playerLevel = player.level();
                UUID doorUUID = null;

                // we are always in the player's level. no need to find the level on the server
                BlockPos pos = loc.getLoc().getPos();

                BlockState state = playerLevel.getBlockState(pos);
                if(state.is(Dokipa.Dokipa_Door.get())) {
                    if(playerLevel.getBlockEntity(DokipaDoorBlock.getBlockEntityPos(state, pos)) instanceof DokipaDoorBlockEntity dokipaDoor) {
                        doorUUID = dokipaDoor.getDoorUUID();
                    }
                }

                if(doorUUID != null) {
                    DoorInfo doorInfo = savedData.doorInfo(doorUUID);
                    if(doorInfo != null && doorInfo.isOwner(player)) {
                        LocalizedBlockPos doorPos = doorInfo.placedPos();
                        if(doorPos != null) {
                            savedData.memorizedLocations(player).add(loc);
                            savedData.setDirty();
                            new S2C_SendMemorizedLocationMessage(loc).sendTo((ServerPlayer) player);
                        }
                    }

                    // todo inform the player it cannot memorize the location when it does not own the door
                }
            } else if(control == Type.FORGET) {
                if(savedData.memorizedLocations(player).remove(loc)) {
                    Dokipa.LOG.info("Removed MemorizedLocation \"" + loc.getDescription() + "\" of player " + player.getStringUUID());
                    // the client cleared its cache when it sent the message
                    savedData.sendMemorizedLocations((ServerPlayer) player);
                } else {
                    Dokipa.LOG.error("Did not remove MemorizedLocation \"" + loc.getDescription() + "\"");
                }
            }
        });
    }
}
