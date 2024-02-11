package io.github.wouink.dokipa.network;

import com.google.common.base.Charsets;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.architectury.utils.Env;
import io.github.wouink.dokipa.Dokipa;
import io.github.wouink.dokipa.DokipaClient;
import io.github.wouink.dokipa.MemorizedLocation;
import io.github.wouink.dokipa.server.LocalizedBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class S2C_SendMemorizedLocationMessage extends BaseS2CMessage {
    private String description;
    private String levelId;
    private BlockPos pos;

    public S2C_SendMemorizedLocationMessage(MemorizedLocation memLoc) {
        this.description = memLoc.getDescription();
        this.levelId = memLoc.getLoc().getDimension().toString();
        this.pos = memLoc.getLoc().getPos();
    }

    public S2C_SendMemorizedLocationMessage(FriendlyByteBuf buf) {
        int descriptionLength = buf.readInt();
        this.description = buf.readCharSequence(descriptionLength, Charsets.UTF_8).toString();
        int levelIdLength = buf.readInt();
        this.levelId = buf.readCharSequence(levelIdLength, Charsets.UTF_8).toString();
        this.pos = buf.readBlockPos();
    }

    @Override
    public MessageType getType() {
        return Dokipa.S2C_SEND_MEMLOC_MSG;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(description.length());
        buf.writeCharSequence(description, Charsets.UTF_8);
        buf.writeInt(levelId.length());
        buf.writeCharSequence(levelId, Charsets.UTF_8);
        buf.writeBlockPos(pos);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        if(context.getEnvironment() == Env.CLIENT) context.queue(() -> {
            LocalizedBlockPos localizedBlockPos = new LocalizedBlockPos(pos, new ResourceLocation(levelId));
            MemorizedLocation memLoc = new MemorizedLocation(description, localizedBlockPos);
            DokipaClient.cacheLocation(memLoc);
        });
    }
}
