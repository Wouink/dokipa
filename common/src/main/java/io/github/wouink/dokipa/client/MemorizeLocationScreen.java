package io.github.wouink.dokipa.client;

import io.github.wouink.dokipa.DokipaClient;
import io.github.wouink.dokipa.MemorizedLocation;
import io.github.wouink.dokipa.network.C2S_MemorizeLocationMessage;
import io.github.wouink.dokipa.server.LocalizedBlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/*
    Called when the player wants to save one of its doors locations.
    Will result in a C2S_MemorizeLocationMessage.
 */
public class MemorizeLocationScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.dokipa.name_location.title");
    private LocalizedBlockPos pos;
    private Direction facing;

    private EditBox nameField;
    private Button validateButton;

    private int halfWidth, titleY;

    private static Component LOCATION_DUPLICATE = Component.translatable("btn.dokipa.duplicate_location");

    protected MemorizeLocationScreen(Component component) {
        super(component);
    }

    public MemorizeLocationScreen(Level level, BlockPos pos, Direction facing) {
        super(TITLE);
        this.pos = new LocalizedBlockPos(pos, level);
        this.facing = facing;
    }

    @Override
    protected void init() {
        super.init();

        halfWidth = this.width / 2;
        titleY = this.height / 2 - 40 - 2 * Minecraft.getInstance().font.lineHeight;

        nameField = new EditBox(Minecraft.getInstance().font, halfWidth - 80, this.height / 2 - 40, 160, 16, Component.empty());
        nameField.setEditable(true);
        nameField.setMaxLength(32);
        this.addRenderableWidget(nameField);
        this.setInitialFocus(nameField);

        // todo create a default name related to the biome, the height, the coordinates...
        nameField.setValue("Northern Plains");

        validateButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            save();
        }).pos(halfWidth - 50, this.height / 2 - 16).width(100).build());

        // enable/disable "Done" button to ensure the description is unique
        nameField.setResponder(value -> {
            boolean canSave = !value.isEmpty();
            if(canSave) {
                for (MemorizedLocation loc : DokipaClient.getCachedLocations()) {
                    if (value.equals(loc.getDescription())) {
                        canSave = false;
                        break;
                    }
                }
            }
            validateButton.active = canSave;
            validateButton.setMessage((canSave || value.isEmpty()) ? CommonComponents.GUI_DONE : LOCATION_DUPLICATE);
        });
    }

    private void save() {
        String description = nameField.getValue();
        if(validateButton.active && !description.isEmpty()) {
            MemorizedLocation loc = new MemorizedLocation(description, pos, facing);
            new C2S_MemorizeLocationMessage(C2S_MemorizeLocationMessage.Type.MEMORIZE, loc).sendToServer();
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int j, int k) {
        if(keyCode == 257 /* return key */) {
            save();
            return true;
        } else return super.keyPressed(keyCode, j, k);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderBackground(guiGraphics);
        // drawCenteredString(font, component, x = center of centered text, y, color)
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, TITLE, halfWidth, titleY, 0xffffff);
        super.render(guiGraphics, i, j, f);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
