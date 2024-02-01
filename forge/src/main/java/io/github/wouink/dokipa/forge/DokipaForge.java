package io.github.wouink.dokipa.forge;

import dev.architectury.platform.forge.EventBuses;
import io.github.wouink.dokipa.Dokipa;
import io.github.wouink.dokipa.DokipaClient;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Dokipa.MODID)
public class DokipaForge {

    public DokipaForge() {
        EventBuses.registerModEventBus(Dokipa.MODID, FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initClient);
        Dokipa.init();
    }

    private void initClient(final FMLClientSetupEvent event) {
        DokipaClient.init();
    }
}
