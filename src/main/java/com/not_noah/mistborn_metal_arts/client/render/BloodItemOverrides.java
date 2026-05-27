package com.not_noah.mistborn_metal_arts.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Custom item overrides wrapper that intercepts model resolution right before rendering.
 * If the item is a weapon or tool, and it is held by an entity with a non-zero blood level,
 * it returns a wrapped {@link BloodBakedModel} that applies a red blood tint.
 */
public class BloodItemOverrides extends ItemOverrides {
    private final ItemOverrides originalOverrides;

    public BloodItemOverrides(ItemOverrides originalOverrides) {
        this.originalOverrides = originalOverrides;
    }

    @Nullable
    @Override
    public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        BakedModel resolvedModel = this.originalOverrides.resolve(model, stack, level, entity, seed);
        if (resolvedModel == null) {
            resolvedModel = model;
        }

        if (isBloodableWeapon(stack)) {
            float weaponBlood = 0.0F;
            if (stack.hasTag() && stack.getTag().contains("BloodLevel")) {
                weaponBlood = stack.getTag().getFloat("BloodLevel");
            }
            
            if (weaponBlood > 0.01F) {
                BloodBakedModel.WeaponType type = getWeaponType(stack);
                // If the resolved model is already a BloodBakedModel, return a new one with the updated blood level
                if (resolvedModel instanceof BloodBakedModel bloodModel) {
                    return new BloodBakedModel(bloodModel.getOriginalModel(), weaponBlood, type);
                }
                return new BloodBakedModel(resolvedModel, weaponBlood, type);
            }
        }
        return resolvedModel;
    }

    /**
     * Classifies the weapon class based on the ItemStack's class and registry name.
     */
    public static BloodBakedModel.WeaponType getWeaponType(ItemStack stack) {
        if (stack.isEmpty()) return BloodBakedModel.WeaponType.GENERIC_TOOL;
        net.minecraft.world.item.Item item = stack.getItem();
        if (item instanceof net.minecraft.world.item.SwordItem) return BloodBakedModel.WeaponType.SWORD;
        if (item instanceof net.minecraft.world.item.AxeItem) return BloodBakedModel.WeaponType.AXE;
        if (item instanceof net.minecraft.world.item.PickaxeItem) return BloodBakedModel.WeaponType.PICKAXE;
        if (item instanceof net.minecraft.world.item.ShieldItem) return BloodBakedModel.WeaponType.SHIELD;
        if (item instanceof net.minecraft.world.item.BowItem || item instanceof net.minecraft.world.item.CrossbowItem) return BloodBakedModel.WeaponType.BOW;
        
        String className = item.getClass().getSimpleName().toLowerCase();
        if (className.contains("sword") || className.contains("scythe") || className.contains("dagger") 
                || className.contains("spear") || className.contains("halberd") || className.contains("cleaver") 
                || className.contains("weapon")) {
            return BloodBakedModel.WeaponType.SWORD;
        }
        if (className.contains("axe")) {
            return BloodBakedModel.WeaponType.AXE;
        }
        if (className.contains("pickaxe") || className.contains("hammer")) {
            return BloodBakedModel.WeaponType.PICKAXE;
        }
        if (className.contains("shield")) {
            return BloodBakedModel.WeaponType.SHIELD;
        }
        if (className.contains("bow")) {
            return BloodBakedModel.WeaponType.BOW;
        }
        return BloodBakedModel.WeaponType.GENERIC_TOOL;
    }

    /**
     * Determines whether the given item stack is a weapon or tool capable of accumulating blood.
     */
    public static boolean isBloodableWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.world.item.Item item = stack.getItem();
        
        // Base standard weapon types
        if (item instanceof net.minecraft.world.item.SwordItem) return true;
        if (item instanceof net.minecraft.world.item.AxeItem) return true;
        if (item instanceof net.minecraft.world.item.TridentItem) return true;
        if (item instanceof net.minecraft.world.item.BowItem || item instanceof net.minecraft.world.item.CrossbowItem) return true;
        if (item instanceof net.minecraft.world.item.ShieldItem) return true;
        
        // Check by class name patterns to catch modded weapons / custom tools
        String className = item.getClass().getSimpleName().toLowerCase();
        return className.contains("sword") || className.contains("axe") || className.contains("scythe") 
                || className.contains("weapon") || className.contains("spear") || className.contains("dagger") 
                || className.contains("halberd") || className.contains("cleaver") || className.contains("hammer")
                || className.contains("pickaxe") || className.contains("shovel") || className.contains("hoe");
    }
}
