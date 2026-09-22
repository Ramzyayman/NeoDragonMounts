package net.dragonmounts.neo.data;

import net.dragonmounts.neo.common.DragonMountsShared;
import net.dragonmounts.neo.common.init.DMBlocks;
import net.dragonmounts.neo.common.init.DMItems;
import net.dragonmounts.neo.common.init.DragonVariants;
import net.dragonmounts.neo.common.item.*;
import net.dragonmounts.neo.common.tag.DMBlockTags;
import net.dragonmounts.neo.common.tag.DMItemTags;
import net.dragonmounts.neo.compat.registry.DragonScaleArmorSuit;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ItemLike;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DMItemTagProvider extends ItemTagsProvider {
    public DMItemTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> provider,
            CompletableFuture<TagsProvider.TagLookup<Block>> block
    ) {
        /// NeoForge's ItemTagsProvider has no block-lookup constructor and vanilla's copy() is gone,
        /// so the lookup is unused - the two former copy() calls are now explicit adds below.
        super(output, provider, DragonMountsShared.NAMESPACE);
    }

    protected TagAppender<ResourceKey<Item>, Item> addToParent(TagAppender<ResourceKey<Item>, Item> parent, TagKey<Item> child) {
        parent.addTag(child);
        return this.keyTag(child);
    }

    /// 1.21.6 replaced IntrinsicTagAppender with TagAppender<E, T>. The intrinsic providers hand out
    /// an object-typed appender, but this mod registers via ResourceKey (DeferredHolder#key) and
    /// DragonScaleArmorSuit exposes bare ResourceKeys, so most chains need the key-typed one.
    /// Chains that feed Consumer<Item> keep using the inherited object-typed tag().
    protected TagAppender<ResourceKey<Item>, Item> keyTag(TagKey<Item> key) {
        return TagAppender.forBuilder(this.getOrCreateRawBuilder(key));
    }

    static ResourceKey<Item> key(ItemLike item) {
        return item.asItem().builtInRegistryHolder().key();
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        var scales = this.keyTag(DMItemTags.DRAGON_SCALES);
        this.addToParent(scales, DMItemTags.AETHER_DRAGON_SCALES).add(DMItems.AETHER_DRAGON_SCALES.key);
        this.addToParent(scales, DMItemTags.ENCHANTED_DRAGON_SCALES).add(DMItems.ENCHANTED_DRAGON_SCALES.key);
        this.addToParent(scales, DMItemTags.ENDER_DRAGON_SCALES).add(DMItems.ENDER_DRAGON_SCALES.key);
        this.addToParent(scales, DMItemTags.FIRE_DRAGON_SCALES).add(DMItems.FIRE_DRAGON_SCALES.key);
        this.addToParent(scales, DMItemTags.FOREST_DRAGON_SCALES).add(DMItems.FOREST_DRAGON_SCALES.key);
        this.addToParent(scales, DMItemTags.ICE_DRAGON_SCALES).add(DMItems.ICE_DRAGON_SCALES.key);
        this.addToParent(scales, DMItemTags.MOONLIGHT_DRAGON_SCALES).add(DMItems.MOONLIGHT_DRAGON_SCALES.key);
        this.addToParent(scales, DMItemTags.NETHER_DRAGON_SCALES).add(DMItems.NETHER_DRAGON_SCALES.key);
        this.addToParent(scales, DMItemTags.SCULK_DRAGON_SCALES).add(DMItems.SCULK_DRAGON_SCALES.key);
        this.addToParent(scales, DMItemTags.STORM_DRAGON_SCALES).add(DMItems.STORM_DRAGON_SCALES.key);
        this.addToParent(scales, DMItemTags.SUNLIGHT_DRAGON_SCALES).add(DMItems.SUNLIGHT_DRAGON_SCALES.key);
        this.addToParent(scales, DMItemTags.TERRA_DRAGON_SCALES).add(DMItems.TERRA_DRAGON_SCALES.key);
        this.addToParent(scales, DMItemTags.WATER_DRAGON_SCALES).add(DMItems.WATER_DRAGON_SCALES.key);
        this.addToParent(scales, DMItemTags.ZOMBIE_DRAGON_SCALES).add(DMItems.ZOMBIE_DRAGON_SCALES.key);
        this.addToParent(scales, DMItemTags.DARK_DRAGON_SCALES).add(DMItems.DARK_DRAGON_SCALES.key);
        this.keyTag(DMItemTags.HARD_SHEARS)
                .add(DMItems.DIAMOND_SHEARS.key)
                .add(DMItems.NETHERITE_SHEARS.key);
        this.keyTag(ItemTags.PIGLIN_LOVED)
                .add(DMItems.GOLDEN_DRAGON_ARMOR.key);
        this.keyTag(ItemTags.PIGLIN_REPELLENTS)
                .add(key(DMBlocks.DRAGON_CORE));
        this.keyTag(ItemTags.MEAT)
                .add(DMItems.DRAGON_MEAT.key)
                .add(DMItems.COOKED_DRAGON_MEAT.key);
        this.keyTag(DMItemTags.BATONS)
                .addTag(Tags.Items.RODS)
                .add(key(Items.DEBUG_STICK))
                .add(key(Items.BONE))
                .add(key(Items.BAMBOO));
        this.keyTag(DMItemTags.DRAGON_SADDLES)
                .add(key(Items.SADDLE));
        var head = this.keyTag(ItemTags.HEAD_ARMOR);
        var chest = this.keyTag(ItemTags.CHEST_ARMOR);
        var leg = this.keyTag(ItemTags.LEG_ARMOR);
        var foot = this.keyTag(ItemTags.FOOT_ARMOR);
        Consumer<DragonScaleArmorSuit> addScaleSuit = suit -> {
            head.add(suit.helmet);
            chest.add(suit.chestplate);
            leg.add(suit.leggings);
            foot.add(suit.boots);
        };
        Consumer<Item> addToSwords = this.tag(ItemTags.SWORDS)::add;
        Consumer<Item> addToBows = this.tag(DMItemTags.DRAGON_SCALE_BOWS)::add;
        Consumer<Item> addToAxes = this.tag(ItemTags.AXES)::add;
        Consumer<Item> addToHoes = this.tag(ItemTags.HOES)::add;
        Consumer<Item> addToPickaxes = this.tag(ItemTags.PICKAXES)::add;
        Consumer<Item> addToShovels = this.tag(ItemTags.SHOVELS)::add;
        Consumer<Item> addToShields = this.tag(DMItemTags.DRAGON_SCALE_SHIELDS)::add;
        for (var type : DragonType.REGISTRY) {
            type.ifPresent(DragonScaleArmorSuit.class, addScaleSuit);
            type.ifPresent(DragonScaleSwordItem.class, addToSwords);
            type.ifPresent(DragonScaleBowItem.class, addToBows);
            type.ifPresent(DragonScaleAxeItem.class, addToAxes);
            type.ifPresent(DragonScaleHoeItem.class, addToHoes);
            type.ifPresent(DragonScalePickaxeItem.class, addToPickaxes);
            type.ifPresent(DragonScaleShovelItem.class, addToShovels);
            type.ifPresent(DragonScaleShieldItem.class, addToShields);
        }
        this.keyTag(DMItemTags.DRAGON_INEDIBLE)
                .add(key(Items.PUFFERFISH)) // it is considered as food in conventional tags...
                .add(key(Items.PUFFERFISH_BUCKET))
                .add(key(Items.AXOLOTL_BUCKET))
                .add(key(Items.TADPOLE_BUCKET))
                .add(DMItems.DRAGON_MEAT.key)
                .add(DMItems.COOKED_DRAGON_MEAT.key);
        this.keyTag(DMItemTags.COOKED_DRAGON_FOODS)
                .addTag(Tags.Items.FOODS_COOKED_MEAT)
                .addTag(Tags.Items.FOODS_COOKED_FISH);
        this.keyTag(DMItemTags.RAW_DRAGON_FOODS)
                .addTag(Tags.Items.FOODS_RAW_MEAT)
                .addTag(Tags.Items.FOODS_RAW_FISH)
                .add(key(Items.COD_BUCKET))
                .add(key(Items.SALMON_BUCKET))
                .add(key(Items.TROPICAL_FISH_BUCKET));
        this.keyTag(ItemTags.BOW_ENCHANTABLE).addTag(DMItemTags.DRAGON_SCALE_BOWS);
        this.keyTag(Tags.Items.TOOLS_BOW).addTag(DMItemTags.DRAGON_SCALE_BOWS);
        this.keyTag(Tags.Items.TOOLS_SHEAR).addTag(DMItemTags.HARD_SHEARS);
        this.keyTag(ItemTags.MINING_ENCHANTABLE).addTag(DMItemTags.HARD_SHEARS);
        this.keyTag(Tags.Items.TOOLS_SHIELD).addTag(DMItemTags.DRAGON_SCALE_SHIELDS);
        this.keyTag(ItemTags.DURABILITY_ENCHANTABLE)
                .addTag(DMItemTags.DRAGON_SCALE_BOWS)
                .addTag(DMItemTags.HARD_SHEARS)
                .addTag(DMItemTags.DRAGON_SCALE_SHIELDS);
        /// vanilla ItemTagsProvider#copy is gone in 1.21.6, so mirror the block tags by hand.
        /// Keep in sync with DMBlockTagProvider.
        var eggItems = this.keyTag(DMItemTags.DRAGON_EGGS).add(key(Items.DRAGON_EGG));
        for (var block : DMBlocks.BUILTIN_DRAGON_EGGS) {
            eggItems.add(key(block));
        }
        var scaleItems = this.keyTag(DMItemTags.DRAGON_SCALE_BLOCKS);
        for (var block : DMBlocks.BUILTIN_DRAGON_SCALE_BLOCKS) {
            scaleItems.add(key(block));
        }
        this.keyTag(ItemTags.PIGLIN_REPELLENTS).add(key(DMBlocks.DRAGON_CORE));
        this.keyTag(ItemTags.PIGLIN_LOVED).add(DMItems.GOLDEN_DRAGON_ARMOR.key);
        var skulls = this.keyTag(DMItemTags.DRAGON_HEADS).add(key(Items.DRAGON_HEAD));
        for (var variant : DragonVariants.BUILTIN_VALUES) {
            skulls.add(key(variant.head));
        }
        this.keyTag(ItemTags.SKULLS).addTag(DMItemTags.DRAGON_HEADS);
        this.keyTag(ItemTags.NOTE_BLOCK_TOP_INSTRUMENTS).addTag(DMItemTags.DRAGON_HEADS);

    }
}
