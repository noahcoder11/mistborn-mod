package com.not_noah.mistborn_metal_arts.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Obsidian Axe — a brutal, top-heavy weapon forged from volcanic glass,
 * wielded by Steel Inquisitors. Deals devastating damage with innate armor-piercing
 * and inflicts a bleeding effect on hit.
 *
 * Stats:
 *  - Durability: 876 (between Iron 250 and Diamond 1561, brittle volcanic glass)
 *  - Attack Damage: 11 base (exceeds Netherite's 10)
 *  - Attack Speed: -3.3 (very slow — heavy, top-heavy weapon)
 *  - Special: 30% armor bypass + Wither "bleeding" on hit
 */
public class ObsidianAxeItem extends AxeItem {

    /** Custom tier for obsidian weaponry. */
    public static final Tier OBSIDIAN_TIER = new Tier() {
        @Override public int getUses() { return 876; }
        @Override public float getSpeed() { return 7.0F; }
        @Override public float getAttackDamageBonus() { return 4.0F; }
        @Override public int getLevel() { return 3; }
        @Override public int getEnchantmentValue() { return 18; }
        @Override public Ingredient getRepairIngredient() {
            return Ingredient.of(Blocks.OBSIDIAN);
        }
    };

    /** Fraction of damage that bypasses armor (0.30 = 30%). */
    private static final float ARMOR_PIERCE_FRACTION = 0.30F;

    /** Bleeding (Wither) duration in ticks and amplifier. */
    private static final int BLEED_DURATION = 80; // 4 seconds
    private static final int BLEED_AMPLIFIER = 1;  // Wither II

    public ObsidianAxeItem(Properties properties) {
        // attackDamageModifier=7 → total = 4 (tier bonus) + 7 + 1 (base) = 12 damage
        // attackSpeedModifier=-3.3 → very slow swing
        super(OBSIDIAN_TIER, 7.0F, -3.3F, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // --- Armor-Piercing ---
        // Deal bonus magic damage (bypasses armor) equal to 30% of weapon base damage.
        float weaponDamage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float pierceDamage = weaponDamage * ARMOR_PIERCE_FRACTION;
        if (pierceDamage > 0F) {
            target.invulnerableTime = 0; // bypass i-frames for the bonus hit
            target.hurt(target.damageSources().magic(), pierceDamage);
        }

        // --- Bleeding (Wither) ---
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, BLEED_DURATION, BLEED_AMPLIFIER));

        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.obsidian_axe.pierce")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.obsidian_axe.bleed")
                .withStyle(ChatFormatting.DARK_RED));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
