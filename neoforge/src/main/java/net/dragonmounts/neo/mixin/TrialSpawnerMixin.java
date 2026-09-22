package net.dragonmounts.neo.mixin;

import net.dragonmounts.neo.common.api.TrialSpawnerExtension;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static net.dragonmounts.neo.common.util.BlockUtil.overrideEntityToSpawn;

@Mixin(TrialSpawner.class)
public abstract class TrialSpawnerMixin implements TrialSpawnerExtension {
    @Shadow
    @Final
    private TrialSpawnerStateData data;

    /// 1.21.8 folded the separate normal/ominous config fields into a single
    /// TrialSpawner.FullConfig record, and renamed TrialSpawnerData to TrialSpawnerStateData.
    @Shadow
    private TrialSpawner.FullConfig config;

    @Shadow
    public abstract void setState(Level level, TrialSpawnerState state);

    @Override
    public void neodragonmounts$overrideEntityToSpawn(Level level, CompoundTag entity) {
        this.data.reset();
        var old = this.config;
        this.config = new TrialSpawner.FullConfig(
                Holder.direct(overrideEntityToSpawn(old.normal().value(), entity)),
                Holder.direct(overrideEntityToSpawn(old.ominous().value(), entity)),
                old.targetCooldownLength(),
                old.requiredPlayerRange()
        );
        this.setState(level, TrialSpawnerState.INACTIVE);
    }

    private TrialSpawnerMixin() {}
}
