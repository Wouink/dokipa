package io.github.wouink.dokipa;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.utils.Env;
import io.github.wouink.dokipa.network.C2S_SummonDoorMessage;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class DokipaClient {

    public static final KeyMapping Door_Summon = new KeyMapping(
            "key.dokipa.summon_door",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_P,
            "category.dokipa.all_bindings"
    );

    public static void init() {
        if(Platform.getEnvironment() != Env.CLIENT) {
            Dokipa.LOG.error("Attempt to init Dokipa client on server");
            return;
        }

        // todo render like an end gateway (but it crashes)
        //RenderTypeRegistry.register(RenderType.endGateway(), Dokipa.Room_Separator.get());

        KeyMappingRegistry.register(Door_Summon);
        // todo this checks if the key is down
        // but it it not called only once when the player presses the key
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if(Door_Summon.isDown()) {
                Player player = minecraft.player;
                BlockPos lookingAt = null;

                // copied from net.minecraft.client.gui.components.DebugScreenOverlay
                HitResult res = minecraft.getCameraEntity().pick(20.0f, 0.0f, false);
                if(res.getType() == HitResult.Type.BLOCK) {
                    lookingAt = ((BlockHitResult) res).getBlockPos();
                }

                if(lookingAt == C2S_SummonDoorMessage.Unsummon_Pos) {
                    Dokipa.LOG.error("Client is looking at special Unsummon_Pos " + C2S_SummonDoorMessage.Unsummon_Pos);
                } else if(lookingAt == null) {
                    // the player is not looking at any block -> remove the door
                    new C2S_SummonDoorMessage(C2S_SummonDoorMessage.Unsummon_Pos, Direction.NORTH).sendToServer();
                } else {
                    // the player is looking at a block -> summon the door
                    Direction facing = player.getDirection().getOpposite();
                    new C2S_SummonDoorMessage(lookingAt, facing).sendToServer();
                }
            }
        });
    }
}
