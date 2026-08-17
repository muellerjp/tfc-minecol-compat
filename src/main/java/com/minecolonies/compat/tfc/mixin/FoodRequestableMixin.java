package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.api.colony.requestsystem.requestable.Food;
import com.minecolonies.compat.tfc.TFCFoodHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes the {@code Food} requestable accept TFC-managed food.
 *
 * <p>Its final clause is {@code ISCOOKABLE.test(stack) || nutrition() >= minNutrition}. With the
 * stubbed nutrition of 0 that comparison fails for every building level, so the only stacks that can
 * ever fulfil a food request are raw cookables — already-edible food is rejected outright and the
 * cook's requests go unanswered.</p>
 */
@Mixin(value = Food.class, remap = false)
public class FoodRequestableMixin
{
    @Redirect(
        method = "matches",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getFoodProperties(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/food/FoodProperties;"),
        remap = false
    )
    private @Nullable FoodProperties repairRequestProperties(
        final Item item,
        final ItemStack stack,
        final @Nullable LivingEntity entity)
    {
        return TFCFoodHelper.getEffectiveProperties(stack);
    }
}
