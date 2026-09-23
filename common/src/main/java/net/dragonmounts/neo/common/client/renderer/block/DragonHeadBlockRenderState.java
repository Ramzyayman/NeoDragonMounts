package net.dragonmounts.neo.common.client.renderer.block;

import net.dragonmounts.neo.compat.registry.DragonVariant;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/// 1.21.9 split block-entity rendering into extract and submit phases, so the submit phase no
/// longer sees the BlockEntity or its BlockState - everything it needs is copied here first.
///
/// Named to avoid colliding with the existing `DragonHeadRenderState` interface in this package,
/// which is an accessor mixed into entity render states and unrelated to this.
public class DragonHeadBlockRenderState extends BlockEntityRenderState {
    public @Nullable DragonVariant variant;
    public float animation;
    public float yRot;
    public boolean isOnWall;
    public Direction facing = Direction.NORTH;
}
