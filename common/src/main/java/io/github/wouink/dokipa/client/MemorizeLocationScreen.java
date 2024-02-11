package io.github.wouink.dokipa.client;

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
    private LocalizedBlockPos pos;
    private Direction facing;

    private EditBox nameField;

    protected MemorizeLocationScreen(Component component) {
        super(component);
    }

    public MemorizeLocationScreen(Level level, BlockPos pos, Direction facing) {
        super(Component.translatable("dokipa.name_location"));
        this.pos = new LocalizedBlockPos(pos, level);
        this.facing = facing;
    }

    @Override
    protected void init() {
        super.init();
        nameField = new EditBox(Minecraft.getInstance().font, 10, 10, 100, 16, Component.empty());
        nameField.setEditable(true);
        nameField.setMaxLength(32);
        this.addRenderableWidget(nameField);
        this.setInitialFocus(nameField);

        // todo create a default name related to the biome, the height, the coordinates...
        nameField.setValue("Northern Plains");

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            String description = nameField.getValue();
            if(!description.isEmpty()) {
                MemorizedLocation loc = new MemorizedLocation(description, pos, facing);
                new C2S_MemorizeLocationMessage(C2S_MemorizeLocationMessage.Type.MEMORIZE, loc).sendToServer();
                Minecraft.getInstance().setScreen(null);
            }
        }).pos(10, 20).build());
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
