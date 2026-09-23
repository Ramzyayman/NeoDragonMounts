package net.dragonmounts.neo.common.client.renderer.block;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/// 1.21.9 split block-entity rendering into extract and submit phases, so everything the
/// renderer needs has to be copied off the block entity first. Mirrors vanilla
/// ShulkerBoxRenderState.
public class DragonCoreRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.SOUTH;
    public float progress;
}
