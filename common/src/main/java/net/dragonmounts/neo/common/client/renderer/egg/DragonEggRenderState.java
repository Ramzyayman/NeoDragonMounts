package net.dragonmounts.neo.common.client.renderer.egg;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.UnknownNullability;

public class DragonEggRenderState extends EntityRenderState {
    public float amplitude;
    public float axis;
    public float progress;
    public @UnknownNullability BlockState block;
    /// 26.1 deleted BlockRenderDispatcher; a block is drawn by resolving it into one of these
    /// during extract and submitting it later, so the state has to carry it.
    public final BlockModelRenderState blockModel = new BlockModelRenderState();
}
