package net.dragonmounts.neo.data;

import net.dragonmounts.neo.common.DragonMountsShared;
import net.dragonmounts.neo.common.init.DMEntities;
import net.dragonmounts.neo.common.tag.DMEntityTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;

import java.util.concurrent.CompletableFuture;

public class DMEntityTagProvider extends EntityTypeTagsProvider {
    public DMEntityTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
        super(output, provider, DragonMountsShared.NAMESPACE);
    }

    /// 1.21.6 replaced IntrinsicTagAppender with TagAppender<E, T>, where the intrinsic providers
    /// hand out an object-typed appender. This mod registers everything as ResourceKey (see
    /// DeferredHolder#key), so build the key-typed appender directly - the same thing KeyTagProvider does.
    protected TagAppender<ResourceKey<EntityType<?>>, EntityType<?>> keyTag(TagKey<EntityType<?>> key) {
        return TagAppender.forBuilder(this.getOrCreateRawBuilder(key));
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.keyTag(DMEntityTags.DRAGONS)
                .add(DMEntities.TAMEABLE_DRAGON.key)
                .add(DMEntities.HATCHABLE_DRAGON_EGG.key);
        this.keyTag(EntityTypeTags.CAN_BREATHE_UNDER_WATER)
                .addTag(DMEntityTags.DRAGONS);
        this.keyTag(EntityTypeTags.FALL_DAMAGE_IMMUNE)
                .addTag(DMEntityTags.DRAGONS);
    }
}
