package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.compat.tfc.TFCFoodHelper;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

/**
 * Wraps {@link ItemStackUtils#IS_ANY_FOOD} so that TFC food items — which carry a dummy
 * vanilla {@code FoodProperties(nutrition=0)} — are still recognized as food by MineColonies.
 *
 * <p>{@code IS_ANY_FOOD} is a {@code public static final} field set in the static initializer.
 * {@code @Mutable} removes the {@code final} qualifier in bytecode so the {@code <clinit>}
 * injection can overwrite it after vanilla sets it.</p>
 *
 * <p>Because {@code ISFOOD}, {@code ISCOOKABLE}, and {@code EDIBLE} are all derived from
 * {@code IS_ANY_FOOD}, this single patch propagates to the entire predicate chain:</p>
 * <pre>
 *   IS_ANY_FOOD  ← patched here
 *     ↓
 *   ISFOOD = IS_ANY_FOOD && !excludedFood
 *     ↓
 *   ISCOOKABLE = ISFOOD(smelting_result(item))   // true for raw TFC meat (compat smelting recipes)
 *     ↓
 *   EDIBLE = ISFOOD && !ISCOOKABLE               // true for TFC cooked food ✓
 * </pre>
 */
@Mixin(value = ItemStackUtils.class, remap = false)
public class ItemStackUtilsMixin
{
    @Shadow
    @Mutable
    public static Predicate<ItemStack> IS_ANY_FOOD;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void wrapISANYFOOD(final CallbackInfo ci)
    {
        final Predicate<ItemStack> vanilla = IS_ANY_FOOD;
        IS_ANY_FOOD = stack -> vanilla.test(stack) || TFCFoodHelper.isTFCEdibleFood(stack);
    }
}
