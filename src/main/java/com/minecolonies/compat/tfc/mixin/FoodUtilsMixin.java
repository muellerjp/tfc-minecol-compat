package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.api.items.ModTags;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.compat.tfc.TFCFoodHelper;
import com.minecolonies.core.items.ItemCrop;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes the {@link FoodUtils} methods that read vanilla {@code FoodProperties.nutrition()} and see a
 * zero, because TFC replaces the food component of every food it manages — its own <em>and</em>
 * MineColonies', which the bundled {@code tfc/food} definitions hand back to it — with a stub whose
 * nutrition is 0.
 *
 * <h3>{@code getFoodValue}</h3>
 * Substitutes the effective nutrition so citizens actually become satiated, and keeps MineColonies'
 * own rule that its food is worth full value while everything else takes the 0.25× nerf. A
 * MineColonies dish is therefore worth exactly what it is worth without TFC installed.
 *
 * <h3>{@code canEatLevel}</h3>
 * Substitutes the effective nutrition in the building-level gate, so higher-quality food is
 * available at level-4/5 buildings while simple fruit is limited to level 1–3. Raw TFC meats are
 * blocked unconditionally via {@link ModTags#rawMeat}, and MineColonies crops stay uneatable.
 *
 * <h3>{@code getBuildingLevelForFood}</h3>
 * Repairs the food-quality tooltip, which would otherwise claim "level 2" for every food.
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
        // Only intercept the stub (non-null props but nutrition == 0).
        // Null props and real nutrition are handled by the vanilla path.
        if (itemFood == null || itemFood.nutrition() > 0) return;

        final int nutrition = TFCFoodHelper.getEffectiveNutrition(foodStack);
        if (nutrition == 0)
        {
            cir.setReturnValue(0.0);
            return;
        }

        // Same shape as the vanilla method: MineColonies food is worth full value, everything
        // else — TFC food included — keeps the 0.25× saturation nerf.
        cir.setReturnValue(nutrition * TFCFoodHelper.getSaturationMultiplier(foodStack) / 1.2 * (1.0 + researchBonus));
    }

    @Inject(method = "canEatLevel", at = @At("HEAD"), cancellable = true, remap = false)
    private static void injectTFCCanEatLevel(
        final ItemStack stack,
        final int buildingLevel,
        final CallbackInfoReturnable<Boolean> cir)
    {
        // MineColonies crops are never eaten by citizens. The vanilla method rejects them first;
        // now that they carry a stub this mixin owns the decision, so it has to reject them too.
        if (stack.getItem() instanceof ItemCrop)
        {
            cir.setReturnValue(false);
            return;
        }

        // Raw TFC meats are never edible by citizens (belt-and-suspenders alongside ISCOOKABLE).
        if (stack.is(ModTags.rawMeat))
        {
            cir.setReturnValue(false);
            return;
        }

        final @Nullable FoodProperties fp = stack.getItem().getFoodProperties(stack, null);

        // If the item has real vanilla nutrition, let the vanilla path handle it.
        if (fp != null && fp.nutrition() > 0) return;

        // Handle the stub case (fp non-null but nutrition=0) and non-food items (fp=null).
        final int nutrition = TFCFoodHelper.getEffectiveNutrition(stack);
        if (nutrition == 0)
        {
            cir.setReturnValue(false);
            return;
        }

        // Levels 1–2: accept any food with nutrition > 0.
        if (buildingLevel < 3)
        {
            cir.setReturnValue(true);
            return;
        }

        // Levels 3–5: require effectiveNutrition >= buildingLevel + 1, matching the vanilla gate.
        // TFC fruits/veg(4) → max level 3 | breads(5) → max level 4 | meats(6) → all levels.
        cir.setReturnValue(nutrition >= buildingLevel + 1);
    }

    /**
     * {@code getBuildingLevelForFood} reads {@code nutrition() - 1} with no null guard, so with a
     * stub every food reports the minimum of 2. Feed it repaired properties instead.
     */
    @Redirect(
        method = "getBuildingLevelForFood",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getFoodProperties(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/food/FoodProperties;"),
        remap = false
    )
    private static FoodProperties repairTooltipProperties(final ItemStack stack, final @Nullable LivingEntity entity)
    {
        return TFCFoodHelper.getEffectiveProperties(stack);
    }
}
