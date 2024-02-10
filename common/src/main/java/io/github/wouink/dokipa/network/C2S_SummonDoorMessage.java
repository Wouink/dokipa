package io.github.wouink.dokipa.network;

import com.google.common.base.Charsets;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import io.github.wouink.dokipa.Dokipa;
import io.github.wouink.dokipa.DokipaDoorBlock;
import io.github.wouink.dokipa.server.DokipaSavedData;
import io.github.wouink.dokipa.server.DoorInfo;
import io.github.wouink.dokipa.server.LocalizedBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class C2S_SummonDoorMessage extends BaseC2SMessage {
    public static final BlockPos Unsummon_Pos = new BlockPos(0, -127, 0);
    private BlockPos summonPos;
    private String summonLevelId;
    private Direction facing;

    public C2S_SummonDoorMessage(Level _summonLevel, BlockPos _summonPos, Direction _facing) {
        this.summonLevelId = _summonLevel.dimension().location().toString();
        this.summonPos = _summonPos;
        this.facing = _facing;
    }

    public C2S_SummonDoorMessage(FriendlyByteBuf buf) {
        //Dokipa.LOG.info("Reading buffer...");
        int levelIdStringLength = buf.readInt();
        //Dokipa.LOG.info("levelIdStringLength = " + levelIdStringLength);
        this.summonLevelId = buf.readCharSequence(levelIdStringLength, Charsets.UTF_8).toString();
        //Dokipa.LOG.info("summonLevelId = " + summonLevelId);
        this.summonPos = buf.readBlockPos();
        //Dokipa.LOG.info("summonPos = " + summonPos);
        this.facing = Direction.values()[buf.readByte()];
        //Dokipa.LOG.info("facing = " + facing);
    }

    @Override
    public MessageType getType() {
        return Dokipa.C2S_DOOR_SUMMON_MSG;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(summonLevelId.length());
        buf.writeCharSequence(summonLevelId, Charsets.UTF_8);
        buf.writeBlockPos(summonPos);
        buf.writeByte(facing.ordinal());
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            Player player = context.getPlayer();
            Level playerLevel = player.level();
            MinecraftServer server = player.getServer();
            Level summonLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(summonLevelId)));

            if(summonLevel == null) {
                Dokipa.logWithLevel(playerLevel, "SummonDoorMessage (handle)", "Could not find level " + summonLevelId + " on server");
            } else if(!summonLevel.dimension().equals(Dokipa.Dimension)) {
                DokipaSavedData savedData = Dokipa.savedData(server);
                DoorInfo doorInfo = savedData.doorForEntity(player);

                if (doorInfo != null) {
                    LocalizedBlockPos currentDoorPos = doorInfo.placedPos();
                    if (currentDoorPos != null) {
                        currentDoorPos.executeAt(server, DokipaDoorBlock::unsummon);
                    }
                    if (!summonPos.equals(Unsummon_Pos)) {
                        DokipaDoorBlock.summon(summonLevel, summonPos, doorInfo.uuid(), facing);
                    }
                }
            }
        });
    }
}
