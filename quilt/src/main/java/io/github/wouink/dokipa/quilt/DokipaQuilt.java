package io.github.wouink.dokipa.quilt;

import io.github.wouink.dokipa.fabriclike.DokipaFabricLike;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

public class DokipaQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer mod) {
        DokipaFabricLike.init();
    }
}
