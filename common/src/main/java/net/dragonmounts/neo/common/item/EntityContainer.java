package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.util.EntityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.TypedEntityData;
import org.jetbrains.annotations.Nullable;

import static net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity.SERIALIZATION_KEY_FLYING;

public interface EntityContainer<T extends Entity> {
    /// 1.21.10 gave ENTITY_DATA its own component type: TypedEntityData carries the entity type
    /// beside the tag instead of leaving an "id" key inside it for readers to re-parse, so the type
    /// has to be handed in here. TypedEntityData#of strips any leftover "id" itself.
    static TypedEntityData<EntityType<?>> simplifyData(EntityType<?> type, CompoundTag tag) {
        tag.remove("Air");
        tag.remove("DeathTime");
        tag.remove("FallDistance");
        tag.remove("FallFlying");
        tag.remove("Fire");
        tag.remove("HurtByTimestamp");
        tag.remove("HurtTime");
        tag.remove("InLove");
        tag.remove("Leash");
        tag.remove("Motion");
        tag.remove("OnGround");
        tag.remove("Passengers");
        tag.remove("PortalCooldown");
        tag.remove("Pos");
        tag.remove("Rotation");
        tag.remove("Sitting");
        tag.remove("SleepingX");
        tag.remove("SleepingY");
        tag.remove("SleepingZ");
        tag.remove("TicksFrozen");
        return TypedEntityData.of(type, tag);
    }

    static ItemStack saveEntityData(Item item, Entity entity, DataComponentPatch patch) {
        var stack = new ItemStack(item);
        var tag = EntityUtil.saveWithId(entity, new CompoundTag());
        tag.remove(SERIALIZATION_KEY_FLYING);
        tag.remove("UUID");
        stack.set(DataComponents.ENTITY_DATA, EntityContainer.simplifyData(entity.getType(), tag));
        stack.applyComponents(patch);
        return stack;
    }

    ItemStack saveEntity(T entity, DataComponentPatch patch);

    /**
     * @see net.minecraft.world.entity.EntityType#spawn(ServerLevel, ItemStack, Player, BlockPos, EntitySpawnReason, boolean, boolean)
     * @see EntityUtil#finalizeSpawn(ServerLevel, Entity, BlockPos, EntitySpawnReason, boolean, boolean)
     */
    @Nullable
    Entity loadEntity(
            ServerLevel level,
            ItemStack stack,
            @Nullable Player player,
            BlockPos pos,
            EntitySpawnReason reason,
            boolean yOffset,
            boolean extraOffset
    );

    Class<T> getContentType();

    default boolean isEmpty(ItemStack stack) {
        return !stack.has(DataComponents.ENTITY_DATA);
    }
}
