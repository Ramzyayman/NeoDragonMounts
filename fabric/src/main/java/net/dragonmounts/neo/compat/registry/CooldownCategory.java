package net.dragonmounts.neo.compat.registry;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import static net.dragonmounts.neo.common.DragonMountsShared.COOLDOWN_CATEGORY;
import static net.dragonmounts.neo.compat.registry.RegistryHandler.makeSimpleRegistry;

public class CooldownCategory {
    public static final MappedRegistry<CooldownCategory> REGISTRY = makeSimpleRegistry(COOLDOWN_CATEGORY);

    public final Identifier identifier;
    private final int id;

    public CooldownCategory(Identifier identifier) {
        this.id = REGISTRY.getId(Registry.register(REGISTRY, this.identifier = identifier, this));
    }

    public final int getId() {
        return this.id;
    }
}
