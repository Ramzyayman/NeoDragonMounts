package net.dragonmounts.neo.client;

import net.dragonmounts.neo.common.entity.breath.BreathParticleOption;
import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

public record BreathParticleProvider(FabricSpriteProvider sprite) implements ParticleProvider<BreathParticleOption> {
    @Override
    public @Nullable Particle createParticle(BreathParticleOption option, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
        return option.variant().appearance.createBreathParticle(option, this.sprite.getAtlas(), level, x, y, z, xSpeed, ySpeed, zSpeed);
    }
}
