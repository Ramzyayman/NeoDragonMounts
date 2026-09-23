package net.dragonmounts.neo.common.client;

import net.minecraft.resources.Identifier;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

public interface DMParticleSprites {
    Identifier FLAME_BREATH = makeId("breath_fire");
    Identifier BLUE_FLAME_BREATH = makeId("breath_blue_fire");
    Identifier AIRFLOW_BREATH = makeId("breath_air");
    Identifier DARK_BREATH = makeId("breath_dark");
    Identifier ENDER_BREATH = makeId("breath_acid");
    Identifier WATER_BREATH = makeId("breath_hydro");
    Identifier ICE_BREATH = makeId("breath_ice");
    Identifier NETHER_BREATH = makeId("breath_nether");
    Identifier SOUL_BREATH = makeId("breath_soul");
    Identifier POISON_BREATH = makeId("breath_poison");
    Identifier WITHER_BREATH = makeId("breath_wither");
}
