package net.dragonmounts.neo.common.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.function.Function;

public abstract class ToggleButton extends Button {
    /// 1.21.11 made Button abstract and final-ised renderWidget; the default sprite+label
    /// draw that Button used to provide now has to be supplied by the subclass.
    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractDefaultSprite(guiGraphics);
        this.extractDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }

    protected final Function<ToggleButton, MutableComponent> narration;
    private boolean state;

    public ToggleButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            OnPress onPress,
            Function<ToggleButton, MutableComponent> narration
    ) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.narration = narration;
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return this.narration.apply(this);
    }

    public final void setState(boolean state) {
        this.state = state;
    }

    public final boolean getState() {
        return state;
    }
}
