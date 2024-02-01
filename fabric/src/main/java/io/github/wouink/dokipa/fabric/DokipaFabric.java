package io.github.wouink.dokipa.fabric;

import io.github.wouink.dokipa.fabriclike.DokipaFabricLike;
import net.fabricmc.api.ModInitializer;

public class DokipaFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DokipaFabricLike.init();
    }
}
