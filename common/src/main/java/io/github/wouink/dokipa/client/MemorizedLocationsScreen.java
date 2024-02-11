package io.github.wouink.dokipa.client;

import io.github.wouink.dokipa.DokipaClient;
import io.github.wouink.dokipa.network.C2S_SummonDoorMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/*
    Displays a list of all memorized locations.
    When the player clicks one, a C2S_SummonDoorMessage is sent to server.
    The list of MemorizedLocations is DokipaClient.memorizedLocations.
    It is populated when the player connects to a server with S2C_SendMemorizedLocationMessages.
 */
public class MemorizedLocationsScreen extends Screen {
    protected MemorizedLocationsScreen(Component component) {
        super(component);
    }

    public MemorizedLocationsScreen() {
        super(Component.translatable("dokipa.position_door"));
    }

    @Override
    protected void init() {
        super.init();

        // create the menu
        // with help from net.minecraft.client.gui.screens.PauseScreen

        GridLayout gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().padding(4);
        // 4 buttons (columns) per row
        GridLayout.RowHelper rowHelper = gridLayout.createRowHelper(4);

        DokipaClient.getCachedLocations().forEach(memorizedLocation -> {
            rowHelper.addChild(Button.builder(Component.literal(memorizedLocation.getDescription()), button -> {
                new C2S_SummonDoorMessage(memorizedLocation.getLoc(), memorizedLocation.getFacing()).sendToServer();
            }).build());
        });

        gridLayout.arrangeElements();
        FrameLayout.alignInRectangle(gridLayout, 0, 0, this.width, this.height, 0.5f, 0.25f);
        gridLayout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, i, j, f);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
