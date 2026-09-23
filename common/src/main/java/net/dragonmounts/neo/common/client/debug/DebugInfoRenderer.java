package net.dragonmounts.neo.common.client.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
import org.jetbrains.annotations.NotNull;

/// 1.21.11 replaced immediate debug drawing with gizmos: SimpleDebugRenderer#render became
/// #emitGizmos, which gets no PoseStack and no MultiBufferSource - shapes are submitted in world
/// space and the gizmo system draws them. That removes the whole VoxelShape conversion and the
/// cached shape lists this class used to keep, since Gizmos takes an AABB and a Vec3 directly.
public enum DebugInfoRenderer implements DebugRenderer.SimpleDebugRenderer {
    INSTANCE;

    private static final GizmoStyle BOX_STYLE = GizmoStyle.stroke(-1);
    private static final int POINT_COLOR = -1;
    private static final float POINT_SIZE = 0.2F;

    @Override
    public void emitGizmos(
            double camX,
            double camY,
            double camZ,
            DebugValueAccess access,
            @NotNull Frustum frustum,
            float partialTick
    ) {
        var boxes = DebugInfo.DEBUG_BOXES;
        if (boxes != null) {
            for (var box : boxes) {
                Gizmos.cuboid(box, BOX_STYLE);
            }
        }
        var points = DebugInfo.DEBUG_POINTS;
        if (points != null) {
            for (var point : points) {
                Gizmos.point(point, POINT_COLOR, POINT_SIZE);
            }
        }
    }
}
