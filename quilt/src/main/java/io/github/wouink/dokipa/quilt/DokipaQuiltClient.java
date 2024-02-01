package io.github.wouink.dokipa.quilt;

import io.github.wouink.dokipa.DokipaClient;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

public class DokipaQuiltClient implements ClientModInitializer {
    @Override
    public void onInitializeClient(ModContainer mod) {
        DokipaClient.init();
    }
}
