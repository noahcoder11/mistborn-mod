package com.not_noah.mistborn_metal_arts.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class GlassDaggerItem extends SwordItem {

    // Custom Glass Tier
    public static final Tier GLASS_TIER = new Tier() {
        @Override
        public int getUses() {
            return 32; // Extremely low durability, breaks very quickly
        }

        @Override
        public float getSpeed() {
            return 8.0f;
        }

        @Override
        public float getAttackDamageBonus() {
            return 6.0f; // 4 + base 3 = 7 damage (Diamond Sword equivalent)
        }

        @Override
        public int getLevel() {
            return 1;
        }

        @Override
        public int getEnchantmentValue() {
            return 1;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(net.minecraft.world.item.Items.GLASS);
        }
    };

    public GlassDaggerItem(Properties pProperties) {
        // Fast attack speed (baseline is 4, minus 1.6 = 2.4 speed, compared to vanilla
        // sword 1.6)
        super(GLASS_TIER, 3, -1.6f, pProperties);
    }

    @Override
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        // Shatter mechanic: 25% chance to instantly break on hit
        if (!pAttacker.level().isClientSide && pAttacker.level().random.nextFloat() < 0.25f) {
            pAttacker.level().playSound(null, pAttacker.getX(), pAttacker.getY(), pAttacker.getZ(),
                    net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

            pStack.setDamageValue(pStack.getMaxDamage()); // Break the item
        }
        return super.hurtEnemy(pStack, pTarget, pAttacker);
    }
}
