package io.github.wouink.dokipa;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.simple.MessageType;
import dev.architectury.networking.simple.SimpleNetworkManager;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.wouink.dokipa.network.C2S_MemorizeLocationMessage;
import io.github.wouink.dokipa.network.C2S_SummonDoorMessage;
import io.github.wouink.dokipa.network.S2C_SendMemorizedLocationMessage;
import io.github.wouink.dokipa.server.DokipaSavedData;
import io.github.wouink.dokipa.server.DoorInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class Dokipa {
    public static final String MODID = "dokipa";
    public static final Logger LOG = LogManager.getLogger("Dokipa");

    public static final ResourceLocation Dimension_Id = new ResourceLocation(MODID, "doors");
    public static final ResourceKey<Level> Dimension = ResourceKey.create(Registries.DIMENSION, Dimension_Id);
    private static DokipaSavedData savedData;

    public static final SimpleNetworkManager NET = SimpleNetworkManager.create(MODID);
    public static final MessageType C2S_DOOR_SUMMON_MSG = NET.registerC2S("c2s_door_summon_msg", C2S_SummonDoorMessage::new);
    public static final MessageType S2C_SEND_MEMLOC_MSG = NET.registerS2C("s2c_send_memloc_msg", S2C_SendMemorizedLocationMessage::new);
    public static final MessageType C2S_MEMORIZE_MSG = NET.registerC2S("c2s_memorize_msg", C2S_MemorizeLocationMessage::new);

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MODID, Registries.BLOCK);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MODID, Registries.ITEM);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<Block> Dokipa_Door = BLOCKS.register("dokipa_door", () -> new DokipaDoorBlock(BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F)));
    public static final RegistrySupplier<BlockEntityType<DokipaDoorBlockEntity>> Dokipa_Door_BlockEntity = BLOCK_ENTITIES.register(
            "dokipa_door",
            () -> BlockEntityType.Builder.of(DokipaDoorBlockEntity::new, new Block[]{Dokipa_Door.get()}).build(null)
    );

    public static final RegistrySupplier<Block> Room_Separator = BLOCKS.register("room_separator", () -> new Block(BlockBehaviour.Properties.copy(Blocks.BEDROCK).lightLevel(blockState -> 15)));
    public static final RegistrySupplier<Item> Room_Separator_Item = ITEMS.register("room_separator", () -> new BlockItem(Room_Separator.get(), new Item.Properties().arch$tab(CreativeModeTabs.OP_BLOCKS)));

    // gamerule maxDokipas = how many doors will be naturally generated to be taken by a Dokipa
    // cf https://github.com/TeamTwilight/twilightforest/blob/1.20.x/src/main/java/twilightforest/TwilightForestMod.java#L67
    public static final GameRules.Key<GameRules.IntegerValue> Max_Dokipas_Gamerule = GameRules.register(
            "maxDokipas",
            GameRules.Category.UPDATES, /* all the world-related gamerules are categorized as "updates" */
            GameRules.IntegerValue.create(10)
    );

    // gamerule keepDoorOnDeath
    public static final GameRules.Key<GameRules.BooleanValue> Keep_Door_On_Death = GameRules.register(
            "keepDoorOnDeath",
            GameRules.Category.UPDATES,
            GameRules.BooleanValue.create(true)
    );

    // adding the dimensions consist of a single json file in the data/dokipa/dimension folder
    // file was generated using https://worldgen.syldium.dev/
    // doc at https://minecraft.wiki/w/Custom_dimension
    // and at https://fabricmc.net/wiki/tutorial:dimensionconcepts

    public static void init() {
        BLOCKS.register();
        BLOCK_ENTITIES.register();
        ITEMS.register();
        InteractionEvent.RIGHT_CLICK_BLOCK.register(DokipaDoorBlock::interact);

        // Platform.getEnvironment() == Env.SERVER is only true on dedicated server.
        // Even if the client embeds an integrated server, it does not pass the check.
        // Therefore, the event below is never registered for the integrated server.
        /*
        if(Platform.getEnvironment() == Env.SERVER) {
            PlayerEvent.PLAYER_JOIN.register(player -> { sendMemories });
        }
         */

        // Instead, perform a check of the level side to determine if messages should be sent.
        PlayerEvent.PLAYER_JOIN.register(player -> {
            // level is actually never client-side here
            if(!player.level().isClientSide()) {
                Dokipa.LOG.info("Player " + player.getUUID() + " joined server -> sending its MemorizedLocations...");
                DokipaSavedData sd = Dokipa.savedData(player.getServer());
                sd.sendMemorizedLocations(player);
            }
        });

        // apply gamerule keepDoorOnDeath
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            Level level = entity.level();
            if(!level.isClientSide() && entity instanceof Player) {
                if(!level.getGameRules().getBoolean(Keep_Door_On_Death)) {
                    DokipaSavedData sd = Dokipa.savedData(level.getServer());
                    DoorInfo doorInfo = sd.doorForEntity(entity);
                    if(doorInfo != null) {
                        doorInfo.setOwner((UUID) null);
                        sd.setDirty();
                    }
                }
            }
            return EventResult.pass();
        });
    }

    public static void logWithLevel(Level level, String source, String message) {
        String side = level.isClientSide() ? "client" : "server";
        String dimension = level.dimension().location().toString();
        LOG.info("[" + source + " / " + dimension + " / " + side + "] " + message);
    }

    public static DokipaSavedData savedData(MinecraftServer server) {
        if(savedData == null) savedData = DokipaSavedData.init(server);
        return savedData;
    }
}
