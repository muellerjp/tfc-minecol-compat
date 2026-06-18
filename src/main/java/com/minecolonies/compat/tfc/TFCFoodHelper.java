package com.minecolonies.compat.tfc;

import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.FoodData;
import net.dries007.tfc.common.component.food.FoodDefinition;
import net.dries007.tfc.common.component.food.IFood;
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

    private TFCFoodHelper() {}
}
