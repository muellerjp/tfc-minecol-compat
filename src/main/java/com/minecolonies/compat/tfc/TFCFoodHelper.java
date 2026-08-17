package com.minecolonies.compat.tfc;

import com.minecolonies.api.items.IMinecoloniesFoodItem;
import com.minecolonies.core.items.ItemCrop;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.FoodData;
import net.dries007.tfc.common.component.food.FoodDefinition;
import net.dries007.tfc.common.component.food.IFood;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Isolates all TFC food API calls so that the Mixin classes never import TFC directly.
 * Both Mixins call only this class; if TFC is absent the Mixin code paths are never reached
 * because the mod requires both mods to be present.
 */
public final class TFCFoodHelper
{
    /**
     * True when this stack is a non-rotten, edible TFC food item with hunger > 0.
     * Returns false for vanilla items, rotten food, non-edible TFC items (edible=false),
     * and TFC items whose hunger value is 0 (drinks, dried seaweed, etc.).
     */
    public static boolean isTFCEdibleFood(final ItemStack stack)
    {
        if (stack.isEmpty()) return false;
        final @Nullable IFood food = FoodCapability.get(stack);
        if (food == null || food.isRotten()) return false;
        final @Nullable FoodDefinition def = FoodCapability.getDefinition(stack);
        if (def != null && !def.edible()) return false;
        return food.getData().hunger() > 0;
    }

    /**
     * Raw TFC hunger, or 0 for items with no TFC food data.
     *
     * <p>For MineColonies food this is its <em>original vanilla nutrition</em>: the bundled
     * {@code data/minecolonies_tfc_compat/tfc/food/} definitions deliberately set {@code hunger} to
     * the value the item was registered with, so the number survives TFC stripping the vanilla
     * component. See {@code tools/generate_minecolonies_food.py}.</p>
     */
    public static int getTFCHunger(final ItemStack stack)
    {
        final @Nullable IFood food = FoodCapability.get(stack);
        return food == null ? 0 : food.getData().hunger();
    }

    /**
     * Effective nutrition proxy for MineColonies building-level gates.
     * Formula: {@code hunger + max(0, round(saturation))}, giving:
     * <ul>
     *   <li>fruits / simple veg  (sat≈0): 4 → accepted at building levels 1–3</li>
     *   <li>breads / boiled egg  (sat=1): 5 → accepted at levels 1–4</li>
     *   <li>cooked meats / fish  (sat=2): 6 → accepted at all levels (1–5)</li>
     *   <li>soups / sandwiches   (sat≥3): 7+ → accepted at all levels</li>
     * </ul>
     * Returns 0 for non-TFC items or items with no food data.
     */
    public static int getTFCEffectiveNutrition(final ItemStack stack)
    {
        final @Nullable IFood food = FoodCapability.get(stack);
        if (food == null) return 0;
        final FoodData data = food.getData();
        return data.hunger() + Math.max(0, Math.round(data.saturation()));
    }

    /**
     * The nutrition value MineColonies should judge this stack by, whatever mod it came from.
     *
     * <p>TFC replaces the vanilla food component of every food it manages with a stub whose
     * {@code nutrition} is 0, so every MineColonies check that reads {@code nutrition()} directly
     * sees a zero. This resolves the real value for all three cases:</p>
     * <ol>
     *   <li><b>Untouched food</b> (no TFC definition matched, real nutrition) — returned as is.</li>
     *   <li><b>MineColonies food</b> — its authored TFC hunger, which <em>is</em> its original
     *       vanilla nutrition, so every level gate behaves exactly as it does without TFC.</li>
     *   <li><b>TFC food</b> — the {@code hunger + saturation} proxy above.</li>
     * </ol>
     */
    public static int getEffectiveNutrition(final ItemStack stack)
    {
        final @Nullable FoodProperties fp = stack.getItem().getFoodProperties(stack, null);
        if (fp != null && fp.nutrition() > 0)
        {
            return fp.nutrition();
        }

        // ItemCrop does not implement IMinecoloniesFoodItem but is still MineColonies food.
        if (stack.getItem() instanceof IMinecoloniesFoodItem || stack.getItem() instanceof ItemCrop)
        {
            return getTFCHunger(stack);
        }

        return getTFCEffectiveNutrition(stack);
    }

    /**
     * The stack's food properties with {@link #getEffectiveNutrition} substituted for the stubbed
     * nutrition, for the {@code @Redirect}s that need a {@code FoodProperties} rather than an int.
     *
     * <p>Only the nutrition is repaired. The result is meant for MineColonies' own arithmetic and
     * must not be used to actually feed anyone — eating goes through TFC, which reads its own
     * component. Returns {@code null} when the item genuinely has no food properties, so the
     * null guards at the call sites keep working.</p>
     */
    public static @Nullable FoodProperties getEffectiveProperties(final ItemStack stack)
    {
        final @Nullable FoodProperties fp = stack.getItem().getFoodProperties(stack, null);
        if (fp == null || fp.nutrition() > 0)
        {
            return fp;
        }

        final int nutrition = getEffectiveNutrition(stack);
        if (nutrition == 0)
        {
            return fp;
        }

        final FoodProperties.Builder builder = new FoodProperties.Builder()
                                                 .nutrition(nutrition)
                                                 .saturationModifier(fp.saturation());
        if (fp.canAlwaysEat())
        {
            builder.alwaysEdible();
        }
        return builder.build();
    }

    /**
     * The saturation multiplier MineColonies applies when a citizen eats this stack, mirroring
     * {@code FoodUtils#getFoodValue}: its own food is worth full value, everything else a quarter.
     * TFC food is not {@code IMinecoloniesFoodItem}, so it keeps the nerf.
     */
    public static double getSaturationMultiplier(final ItemStack stack)
    {
        return stack.getItem() instanceof IMinecoloniesFoodItem ? 1.0 : 0.25;
    }

    private TFCFoodHelper() {}
}
