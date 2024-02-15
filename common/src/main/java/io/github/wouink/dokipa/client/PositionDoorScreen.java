package io.github.wouink.dokipa.client;

import io.github.wouink.dokipa.Dokipa;
import io.github.wouink.dokipa.DokipaClient;
import io.github.wouink.dokipa.network.C2S_MemorizeLocationMessage;
import io.github.wouink.dokipa.network.C2S_SummonDoorMessage;
import net.minecraft.client.Minecraft;
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
public class PositionDoorScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.dokipa.position_door.title");
    private static final Component SHIFT_HINT = Component.translatable("screen.dokipa.position_door.shift_hint");

    private int midWidth;

    protected PositionDoorScreen(Component component) {
        super(component);
    }

    public PositionDoorScreen() {
        super(Component.translatable("dokipa.position_door"));
    }

    @Override
    protected void init() {
        super.init();

        Dokipa.LOG.info("Width = " + width + ", Height = " + height);

        midWidth = this.width / 2;

        int columns = 2;
        if(width >= 640) columns = 4;
        else if(width >= 500) columns = 3;

        // create the menu
        // with help from net.minecraft.client.gui.screens.PauseScreen

        GridLayout gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().padding(4);
        GridLayout.RowHelper rowHelper = gridLayout.createRowHelper(columns);

        DokipaClient.getCachedLocations().forEach(memorizedLocation -> {
            rowHelper.addChild(Button.builder(Component.literal(memorizedLocation.getDescription()), button -> {
                if(hasShiftDown()) {
                    new C2S_MemorizeLocationMessage(C2S_MemorizeLocationMessage.Type.FORGET, memorizedLocation).sendToServer();
                    // the server will send back all known locations to the client
                    DokipaClient.clearCachedLocations();
                } else {
                    new C2S_SummonDoorMessage(memorizedLocation.getLoc(), memorizedLocation.getFacing()).sendToServer();
                }
                Minecraft.getInstance().setScreen(null);
            }).build());
        });

        gridLayout.arrangeElements();
        FrameLayout.alignInRectangle(gridLayout, 0, 0, this.width, this.height, 0.5f, 0.25f);
        gridLayout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderBackground(guiGraphics);
        // drawCenteredString(font, component, x = center of centered text, y, color)
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, TITLE, midWidth, 10, 0xffffff);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, SHIFT_HINT, midWidth, 30, 0xffffff);
        super.render(guiGraphics, i, j, f);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
