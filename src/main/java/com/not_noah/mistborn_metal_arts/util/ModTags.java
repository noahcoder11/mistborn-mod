package com.not_noah.mistborn_metal_arts.util;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    private ModTags() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(MistbornMetalArts.MOD_ID, path);
    }

    public static final class Blocks {
        public static final TagKey<Block> METALLIC_BLOCKS = BlockTags.create(id("metallic_blocks"));
        public static final TagKey<Block> PUSH_PULL_ANCHORS = BlockTags.create(id("push_pull_anchors"));

        private Blocks() {
        }
    }

    public static final class Items {
        public static final TagKey<Item> METALLIC_ITEMS = ItemTags.create(id("metallic_items"));
        public static final TagKey<Item> METALMINDS = ItemTags.create(id("metalminds"));
        public static final TagKey<Item> HEMALURGIC_SPIKES = ItemTags.create(id("hemalurgic_spikes"));
        public static final TagKey<Item> GOD_METALS = ItemTags.create(id("god_metals"));
        public static final TagKey<Item> METAL_ARMOR = ItemTags.create(id("metal_armor"));
        public static final TagKey<Item> METALMIND_RINGS = ItemTags.create(id("metalmind_rings"));
        public static final TagKey<Item> METALMIND_BRACERS = ItemTags.create(id("metalmind_bracers"));
        public static final TagKey<Item> METALMIND_NECKLACES = ItemTags.create(id("metalmind_necklaces"));

        private Items() {
        }
    }

    public static final class EntityTypes {
        private static final ResourceKey<Registry<EntityType<?>>> ENTITY_TYPE_REGISTRY = ResourceKey.createRegistryKey(new ResourceLocation("minecraft", "entity_type"));

        private static TagKey<EntityType<?>> PUSHABLE_ENTITIES;
        private static TagKey<EntityType<?>> PULLABLE_ENTITIES;

        public static TagKey<EntityType<?>> pushable() {
            if (PUSHABLE_ENTITIES == null) {
                PUSHABLE_ENTITIES = TagKey.create(ENTITY_TYPE_REGISTRY, id("pushable_entities"));
            }
            return PUSHABLE_ENTITIES;
        }

        public static TagKey<EntityType<?>> pullable() {
            if (PULLABLE_ENTITIES == null) {
                PULLABLE_ENTITIES = TagKey.create(ENTITY_TYPE_REGISTRY, id("pullable_entities"));
            }
            return PULLABLE_ENTITIES;
        }

        private EntityTypes() {
        }
    }
}
