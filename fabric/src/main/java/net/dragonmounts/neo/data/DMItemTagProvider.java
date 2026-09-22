package net.dragonmounts.neo.data;

import net.dragonmounts.neo.common.init.DMBlocks;
import net.dragonmounts.neo.common.init.DMItems;
import net.dragonmounts.neo.common.init.DragonVariants;
import net.dragonmounts.neo.common.item.*;
import net.dragonmounts.neo.common.tag.DMBlockTags;
import net.dragonmounts.neo.common.tag.DMItemTags;
import net.dragonmounts.neo.compat.registry.DragonScaleArmorSuit;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ItemLike;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DMItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public DMItemTagProvider(
            FabricDataOutput output,
            CompletableFuture<HolderLookup.Provider> provider,
            FabricTagProvider.BlockTagProvider block
    ) {
        super(output, provider, block);
    }

    protected TagAppender<ResourceKey<Item>, Item> addToParent(TagAppender<ResourceKey<Item>, Item> parent, TagKey<Item> child) {
        parent.addTag(child);
        return this.builder(child);
    }

    /// Fabric 23.x replaced getOrCreateTagBuilder with builder() (ResourceKey-typed) and
    /// valueLookupBuilder() (object-typed). This mod registers via ResourceKey, so builder() is the
    /// default; the Consumer<Item> chains keep the object-typed one.
    static ResourceKey<Item> key(ItemLike item) {
        return item.asItem().builtInRegistryHolder().key();
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        var scales = this.builder(DMItemTags.DRAGON_SCALES);
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
        this.builder(DMItemTags.HARD_SHEARS)
                .add(DMItems.DIAMOND_SHEARS.key)
                .add(DMItems.NETHERITE_SHEARS.key);
        this.builder(ItemTags.PIGLIN_LOVED)
                .add(DMItems.GOLDEN_DRAGON_ARMOR.key);
        this.builder(ItemTags.PIGLIN_REPELLENTS)
                .add(key(DMBlocks.DRAGON_CORE));
        this.builder(ItemTags.MEAT)
                .add(DMItems.DRAGON_MEAT.key)
                .add(DMItems.COOKED_DRAGON_MEAT.key);
        this.builder(DMItemTags.BATONS)
                .forceAddTag(ConventionalItemTags.RODS)
                .add(key(Items.DEBUG_STICK))
                .add(key(Items.BONE))
                .add(key(Items.BAMBOO));
        this.builder(DMItemTags.DRAGON_SADDLES)
                .add(key(Items.SADDLE));
        var head = this.builder(ItemTags.HEAD_ARMOR);
        var chest = this.builder(ItemTags.CHEST_ARMOR);
        var leg = this.builder(ItemTags.LEG_ARMOR);
        var foot = this.builder(ItemTags.FOOT_ARMOR);
        Consumer<DragonScaleArmorSuit> addScaleSuit = suit -> {
            head.add(suit.helmet);
            chest.add(suit.chestplate);
            leg.add(suit.leggings);
            foot.add(suit.boots);
        };
        Consumer<Item> addToSwords = this.valueLookupBuilder(ItemTags.SWORDS)::add;
        Consumer<Item> addToBows = this.valueLookupBuilder(DMItemTags.DRAGON_SCALE_BOWS)::add;
        Consumer<Item> addToAxes = this.valueLookupBuilder(ItemTags.AXES)::add;
        Consumer<Item> addToHoes = this.valueLookupBuilder(ItemTags.HOES)::add;
        Consumer<Item> addToPickaxes = this.valueLookupBuilder(ItemTags.PICKAXES)::add;
        Consumer<Item> addToShovels = this.valueLookupBuilder(ItemTags.SHOVELS)::add;
        Consumer<Item> addToShields = this.valueLookupBuilder(DMItemTags.DRAGON_SCALE_SHIELDS)::add;
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
        this.builder(DMItemTags.DRAGON_INEDIBLE)
                .add(key(Items.PUFFERFISH)) // it is considered as food in conventional tags...
                .add(key(Items.PUFFERFISH_BUCKET))
                .add(key(Items.AXOLOTL_BUCKET))
                .add(key(Items.TADPOLE_BUCKET))
                .add(DMItems.DRAGON_MEAT.key)
                .add(DMItems.COOKED_DRAGON_MEAT.key);
        this.builder(DMItemTags.COOKED_DRAGON_FOODS)
                .forceAddTag(ConventionalItemTags.COOKED_MEAT_FOODS)
                .forceAddTag(ConventionalItemTags.COOKED_FISH_FOODS);
        this.builder(DMItemTags.RAW_DRAGON_FOODS)
                .forceAddTag(ConventionalItemTags.RAW_MEAT_FOODS)
                .forceAddTag(ConventionalItemTags.RAW_FISH_FOODS)
                .add(key(Items.COD_BUCKET))
                .add(key(Items.SALMON_BUCKET))
                .add(key(Items.TROPICAL_FISH_BUCKET));
        this.builder(ItemTags.BOW_ENCHANTABLE).addTag(DMItemTags.DRAGON_SCALE_BOWS);
        this.builder(ConventionalItemTags.BOW_TOOLS).addTag(DMItemTags.DRAGON_SCALE_BOWS);
        this.builder(ConventionalItemTags.SHEAR_TOOLS).addTag(DMItemTags.HARD_SHEARS);
        this.builder(ItemTags.MINING_ENCHANTABLE).addTag(DMItemTags.HARD_SHEARS);
        this.builder(ConventionalItemTags.SHIELD_TOOLS).addTag(DMItemTags.DRAGON_SCALE_SHIELDS);
        this.builder(ItemTags.DURABILITY_ENCHANTABLE)
                .addTag(DMItemTags.DRAGON_SCALE_BOWS)
                .addTag(DMItemTags.HARD_SHEARS)
                .addTag(DMItemTags.DRAGON_SCALE_SHIELDS);
        this.copy(DMBlockTags.DRAGON_EGGS, DMItemTags.DRAGON_EGGS);
        this.copy(DMBlockTags.DRAGON_SCALE_BLOCKS, DMItemTags.DRAGON_SCALE_BLOCKS);
        this.builder(ItemTags.PIGLIN_REPELLENTS).add(key(DMBlocks.DRAGON_CORE));
        this.builder(ItemTags.PIGLIN_LOVED).add(DMItems.GOLDEN_DRAGON_ARMOR.key);
        var skulls = this.builder(DMItemTags.DRAGON_HEADS).add(key(Items.DRAGON_HEAD));
        for (var variant : DragonVariants.BUILTIN_VALUES) {
            skulls.add(key(variant.head));
        }
        this.builder(ItemTags.SKULLS).addTag(DMItemTags.DRAGON_HEADS);
        this.builder(ItemTags.NOTE_BLOCK_TOP_INSTRUMENTS).addTag(DMItemTags.DRAGON_HEADS);
    }
}
