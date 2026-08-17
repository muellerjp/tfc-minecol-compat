package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.api.util.InventoryUtils;
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
 * Fixes the cook handing food to a player.
 *
 * <p>{@code transferFoodUpToSaturation} sizes each transfer with
 * {@code ceil((required - found) / (float) nutrition())}. A stubbed nutrition of 0 turns that into
 * {@code ceil(x / 0f)} = infinity, which {@code Math.round} saturates to {@code Long.MAX_VALUE} and
 * the enclosing {@code (int)} cast truncates to <b>-1</b>. {@code extractItem(i, -1, false)} then
 * moves nothing, the running total never advances, and the cook concludes it has nothing to serve —
 * silently, with no error anywhere.</p>
 *
 * <p>One redirect covers both {@code nutrition()} reads in the method, since they share the single
 * {@code getFoodProperties} call.</p>
 */
@Mixin(value = InventoryUtils.class, remap = false)
public class InventoryUtilsMixin
{
    @Redirect(
        method = "transferFoodUpToSaturation",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getFoodProperties(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/food/FoodProperties;"),
        remap = false
    )
    private static @Nullable FoodProperties repairServedProperties(
        final Item item,
        final ItemStack stack,
        final @Nullable LivingEntity entity)
    {
        return TFCFoodHelper.getEffectiveProperties(stack);
    }
}
