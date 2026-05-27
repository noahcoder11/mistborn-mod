package com.not_noah.mistborn_metal_arts.registry;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import com.not_noah.mistborn_metal_arts.entity.MetalbornRole;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MistbornMetalArts.MOD_ID);
    public static final EnumMap<MetalbornRole, RegistryObject<EntityType<MetalbornEnemy>>> METALBORN = new EnumMap<>(MetalbornRole.class);

    public static final RegistryObject<EntityType<MetalbornEnemy>> COINSHOT_BANDIT = metalborn(MetalbornRole.COINSHOT_BANDIT, 0.6F, 1.95F);
    public static final RegistryObject<EntityType<MetalbornEnemy>> LURCHER_GUARD = metalborn(MetalbornRole.LURCHER_GUARD, 0.6F, 1.95F);
    public static final RegistryObject<EntityType<MetalbornEnemy>> PEWTER_THUG = metalborn(MetalbornRole.PEWTER_THUG, 0.7F, 2.05F);
    public static final RegistryObject<EntityType<MetalbornEnemy>> TINEYE_SCOUT = metalborn(MetalbornRole.TINEYE_SCOUT, 0.55F, 1.9F);
    public static final RegistryObject<EntityType<MetalbornEnemy>> RIOTER = metalborn(MetalbornRole.RIOTER, 0.6F, 1.95F);
    public static final RegistryObject<EntityType<MetalbornEnemy>> SOOTHER = metalborn(MetalbornRole.SOOTHER, 0.6F, 1.95F);
    public static final RegistryObject<EntityType<MetalbornEnemy>> SEEKER = metalborn(MetalbornRole.SEEKER, 0.6F, 1.95F);
    public static final RegistryObject<EntityType<MetalbornEnemy>> SMOKER = metalborn(MetalbornRole.SMOKER, 0.6F, 1.95F);
    public static final RegistryObject<EntityType<MetalbornEnemy>> ATIUM_SEER = metalborn(MetalbornRole.ATIUM_SEER, 0.6F, 1.95F);
    public static final RegistryObject<EntityType<MetalbornEnemy>> MISTBORN_ASSASSIN = metalborn(MetalbornRole.MISTBORN_ASSASSIN, 0.6F, 1.95F);
    public static final RegistryObject<EntityType<MetalbornEnemy>> KOLOSS = metalborn(MetalbornRole.KOLOSS, 1.25F, 2.95F);
    public static final RegistryObject<EntityType<MetalbornEnemy>> KANDRA = metalborn(MetalbornRole.KANDRA, 0.8F, 1.5F);
    public static final RegistryObject<EntityType<MetalbornEnemy>> STEEL_INQUISITOR = metalborn(MetalbornRole.STEEL_INQUISITOR, 0.85F, 2.35F);

    private ModEntityTypes() {
    }

    private static RegistryObject<EntityType<MetalbornEnemy>> metalborn(MetalbornRole role, float width, float height) {
        RegistryObject<EntityType<MetalbornEnemy>> object = ENTITY_TYPES.register(role.id(), () -> EntityType.Builder
                .of((EntityType<MetalbornEnemy> type, net.minecraft.world.level.Level level) -> new MetalbornEnemy(type, level, role), MobCategory.MONSTER)
                .sized(width, height)
                .clientTrackingRange(role.isBoss() ? 12 : 8)
                .updateInterval(2)
                .build(MistbornMetalArts.MOD_ID + ":" + role.id()));
        METALBORN.put(role, object);
        return object;
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        for (Map.Entry<MetalbornRole, RegistryObject<EntityType<MetalbornEnemy>>> entry : METALBORN.entrySet()) {
            event.put(entry.getValue().get(), MetalbornEnemy.createAttributes(entry.getKey()).build());
        }
    }

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
