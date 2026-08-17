package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.api.compatibility.CompatibilityManager;
import com.minecolonies.compat.tfc.TFCFoodHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes {@code getEdibles} judge food by its effective nutrition instead of the stubbed vanilla one.
 *
 * <p>{@code getEdibles(minNutrition)} filters the discovered edibles on
 * {@code getFoodProperties(null).nutrition() >= minNutrition}. TFC-managed food reports 0 there, so
 * every one of its items is dropped as soon as {@code minNutrition >= 1}.</p>
 *
 * <p>Both callers pass {@code buildingLevel - 1}, which makes the failure look like a UI bug: the
 * restaurant menu picker ({@code RestaurantMenuModuleWindow}) is populated only while the hut is
 * still level 1 and silently empties on the first upgrade, so the player cannot put TFC or
 * MineColonies food on the menu at all. The nether miner's travel rations
 * ({@code EntityAIWorkNether}) empty the same way.</p>
 */
@Mixin(value = CompatibilityManager.class, remap = false)
public class CompatibilityManagerMixin
{
    /**
     * Called twice per candidate — once for the null check, once for the comparison — and both are
     * redirected, so the null check keeps guarding genuinely non-food items.
     */
    @Redirect(
        method = "getEdibles",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getFoodProperties(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/food/FoodProperties;"),
        remap = false
    )
    private @Nullable FoodProperties repairEdibleProperties(final ItemStack stack, final @Nullable LivingEntity entity)
    {
        return TFCFoodHelper.getEffectiveProperties(stack);
    }
}
