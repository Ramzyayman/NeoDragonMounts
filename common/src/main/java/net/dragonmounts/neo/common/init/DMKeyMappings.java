package net.dragonmounts.neo.common.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.dragonmounts.neo.config.ClientConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;

import java.util.function.Consumer;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

public class DMKeyMappings {
    /// 1.21.10 replaced the free-form category string with a registered KeyMapping.Category keyed by
    /// a Identifier; the label key is derived from it, so it moved from
    /// "key.categories.neodragonmounts" to "key.category.neodragonmounts.dragon_mounts".
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(makeId("dragon_mounts"));
    public static final ToggleKeyMapping DESCEND = new ToggleKeyMapping(
            "key.neodragonmounts.descend",
            InputConstants.KEY_Z,
            CATEGORY,
            ClientConfig.INSTANCE.toggleDescending::get,
            true
    );
    public static final ToggleKeyMapping BREATHE = new ToggleKeyMapping(
            "key.neodragonmounts.breathe",
            InputConstants.KEY_R,
            CATEGORY,
            ClientConfig.INSTANCE.toggleBreathing::get,
            true
    );

    public static void register(Consumer<KeyMapping> registry) {
        registry.accept(DESCEND);
        registry.accept(BREATHE);
    }
}
