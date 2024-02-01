package io.github.wouink.dokipa.fabric;

import io.github.wouink.dokipa.DokipaClient;
import net.fabricmc.api.ClientModInitializer;

public class DokipaFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DokipaClient.init();
    }
}
