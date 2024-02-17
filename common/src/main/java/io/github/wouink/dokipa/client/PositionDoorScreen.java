package io.github.wouink.dokipa.client;

import io.github.wouink.dokipa.Dokipa;
import io.github.wouink.dokipa.DokipaClient;
import io.github.wouink.dokipa.MemorizedLocation;
import io.github.wouink.dokipa.network.C2S_MemorizeLocationMessage;
import io.github.wouink.dokipa.network.C2S_SummonDoorMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/*
    Displays a list of all memorized locations.
    When the player clicks one, a C2S_SummonDoorMessage is sent to server.
    The list of MemorizedLocations is DokipaClient.memorizedLocations.
    It is populated when the player connects to a server with S2C_SendMemorizedLocationMessages.
 */
public class PositionDoorScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.dokipa.position_door.title");
    private static final Component SHIFT_HINT = Component.translatable("screen.dokipa.position_door.shift_hint");

    private int halfWidth, titleY;
    private int columns, pages, currentPage = 1, buttonsPerPage;
    private GridLayout gridLayout;
    private GridLayout.RowHelper rowHelper;
    private Font font = Minecraft.getInstance().font;
    private int betweenLines = font.lineHeight + 4;

    private List<MemorizedLocation> cachedLocations = DokipaClient.getCachedLocations();

    protected PositionDoorScreen(Component component) {
        super(component);
    }

    public PositionDoorScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        super.init();

        Dokipa.LOG.info("width = " + this.width + ", height = " + this.height);

        halfWidth = this.width / 2;
        titleY = (int) (this.height * 0.05f);

        columns = 2;
        if(width >= 640) columns = 4;
        else if(width >= 500) columns = 3;

        // titleY + 2 * font.lineHeight reserved at top
        // for symmetry, the same space is reserved at bottom
        // 28px oh height (20px + 4px padding on top and bottom) per button
        int remain = this.height - 2 * (titleY + 3 * font.lineHeight);
        int rows = (int) Math.floor((double) remain / 28);
        Dokipa.LOG.info("remains " + remain + "px, " + rows + " rows");

        int totalLocations = DokipaClient.getCachedLocations().size();
        buttonsPerPage = rows * columns;
        pages = (int) Math.ceil((double) totalLocations / buttonsPerPage);

        initPage(currentPage);

        // arrows for switching pages
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            nextPage();
        }).pos(this.width - 30, this.height - 30).size(20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            prevPage();
        }).pos(10, this.height - 30).size(20, 20).build());
    }

    private void nextPage() {
        if(currentPage < pages) {
            currentPage++;
            initPage(currentPage);
        }
    }

    private void prevPage() {
        if(currentPage > 1) {
            currentPage--;
            initPage(currentPage);
        }
    }

    private void initPage(int pageNumber) {
        // remove display of previously created buttons
        if(gridLayout != null) gridLayout.visitWidgets(this::removeWidget);

        // recreate gridLayout because we can't remove it's content,
        // we keep adding widgets over previously existing widgets,
        // and they end up being all added by this::addRenderableWidget at the end
        gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().padding(4);
        rowHelper = gridLayout.createRowHelper(columns);

        // create the menu
        // with help from net.minecraft.client.gui.screens.PauseScreen
        int start = (pageNumber - 1) * buttonsPerPage;
        int end = Math.min(start + buttonsPerPage, cachedLocations.size());

        for(int locIndex = start; locIndex < end; locIndex++) {
            MemorizedLocation memorizedLocation = cachedLocations.get(locIndex);
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
        }

        gridLayout.arrangeElements();

        // alignInRectangle(element, x, y, width, height, interpolateX, interpolateY)
        // resultX = x + (width - x) * interpolateX : x + the proportion of the remaining space to occupy
        // same for Y, but here Y should be fixed
        FrameLayout.alignInRectangle(gridLayout, 0, titleY + 3 * font.lineHeight, this.width, this.height, 0.5f, 0);

        gridLayout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderBackground(guiGraphics);
        // drawCenteredString(font, component, x = center of centered text, y, color)
        guiGraphics.drawCenteredString(font, TITLE, halfWidth, titleY, 0xffffff);
        guiGraphics.drawCenteredString(font, SHIFT_HINT, halfWidth, titleY + betweenLines, 0xffffff);
        guiGraphics.drawCenteredString(font, currentPage + " / " + pages, halfWidth, this.height + font.lineHeight - 30, 0xffffff);
        super.render(guiGraphics, i, j, f);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
