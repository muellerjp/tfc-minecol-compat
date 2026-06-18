package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.api.items.ModTags;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.compat.tfc.TFCFoodHelper;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes two {@link FoodUtils} methods that read vanilla {@code FoodProperties.nutrition()}
 * and return zero for TFC food (which always has {@code nutrition=0} in the vanilla stub).
 *
 * <h3>{@code getFoodValue}</h3>
 * Intercepts when the passed {@code FoodProperties} has {@code nutrition=0} (the TFC stub)
 * and substitutes the effective TFC nutrition so citizens actually become satiated.
 * TFC food receives the same 0.25× non-MineColonies-food saturation nerf as vanilla food,
 * keeping it balanced relative to MineColonies-native crops.
 *
 * <h3>{@code canEatLevel}</h3>
 * Intercepts when vanilla returns false for TFC food and substitutes a check based on
 * effective TFC nutrition, so higher-quality TFC food (cooked meat, soups) is available
 * at level-4/5 buildings while simple fruit is limited to level 1–3.
 * Raw TFC meats are blocked unconditionally via {@link ModTags#rawMeat}.
 */
@Mixin(value = FoodUtils.class, remap = false)
public class FoodUtilsMixin
{
    /**
     * Descriptor: {@code (Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;D)D}
     */
    @Inject(
        method = "getFoodValue(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;D)D",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void injectTFCFoodValue(
        final ItemStack foodStack,
        final @Nullable FoodProperties itemFood,
        final double researchBonus,
        final CallbackInfoReturnable<Double> cir)
    {
        // Only intercept the TFC stub (non-null props but nutrition == 0).
        // Null props and real nutrition are handled by the vanilla path.
        if (itemFood == null || itemFood.nutrition() > 0) return;

        final int nutrition = TFCFoodHelper.getTFCEffectiveNutrition(foodStack);
        if (nutrition == 0)
        {
            cir.setReturnValue(0.0);
            return;
        }

        // TFC food is not IMinecoloniesFoodItem → apply the same 0.25× saturation nerf
        // that vanilla food receives compared to MineColonies-native food.
        cir.setReturnValue(nutrition * 0.25 / 1.2 * (1.0 + researchBonus));
    }

    @Inject(method = "canEatLevel", at = @At("HEAD"), cancellable = true, remap = false)
    private static void injectTFCCanEatLevel(
        final ItemStack stack,
        final int buildingLevel,
        final CallbackInfoReturnable<Boolean> cir)
    {
        // Raw TFC meats are never edible by citizens (belt-and-suspenders alongside ISCOOKABLE).
        if (stack.is(ModTags.rawMeat))
        {
            cir.setReturnValue(false);
            return;
        }

        final @Nullable FoodProperties fp = stack.getItem().getFoodProperties(stack, null);

        // If the item has real vanilla nutrition, let the vanilla path handle it.
        if (fp != null && fp.nutrition() > 0) return;

        // Handle the TFC stub case (fp non-null but nutrition=0) and non-food items (fp=null).
        final int nutrition = TFCFoodHelper.getTFCEffectiveNutrition(stack);
        if (nutrition == 0)
        {
            cir.setReturnValue(false);
            return;
        }

        // Levels 1–2: accept any TFC food with hunger > 0.
        if (buildingLevel < 3)
        {
            cir.setReturnValue(true);
            return;
        }

        // Levels 3–5: require effectiveNutrition >= buildingLevel + 1.
        // fruits/veg(4) → max level 3 | breads(5) → max level 4 | meats(6) → all levels.
        cir.setReturnValue(nutrition >= buildingLevel + 1);
    }
}
