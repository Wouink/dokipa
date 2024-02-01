package io.github.wouink.dokipa;

import dev.architectury.networking.simple.MessageType;
import dev.architectury.networking.simple.SimpleNetworkManager;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.wouink.dokipa.network.C2S_SummonDoorMessage;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Dokipa {
    public static final String MODID = "dokipa";
    public static final Logger LOG = LogManager.getLogger("Dokipa");

    public static final SimpleNetworkManager NET = SimpleNetworkManager.create(MODID);
    public static final MessageType C2S_DOOR_SUMMON_MSG = NET.registerC2S("c2s_door_summon_msg", C2S_SummonDoorMessage::new);

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MODID, Registries.BLOCK);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<Block> Dokipa_Door = BLOCKS.register("dokipa_door", () -> new DokipaDoorBlock(BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F)));
    public static final RegistrySupplier<BlockEntityType<DokipaDoorBlockEntity>> Dokipa_Door_BlockEntity = BLOCK_ENTITIES.register(
            "dokipa_door",
            () -> BlockEntityType.Builder.of(DokipaDoorBlockEntity::new, new Block[]{Dokipa_Door.get()}).build(null)
    );

    public static void init() {
        BLOCKS.register();
        BLOCK_ENTITIES.register();
    }

    public static void logWithLevel(Level level, String source, String message) {
        String side = level.isClientSide() ? "client" : "server";
        LOG.info("[" + source + " / " + side + "] " + message);
    }
}
