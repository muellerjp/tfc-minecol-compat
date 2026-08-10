package com.minecolonies.compat.tfc;

import net.dries007.tfc.common.LevelTier;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.util.Metal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps every TFC metal item — ingots, tool heads and finished tools alike — to the tool tier of
 * its metal.
 *
 * <p>{@link com.minecolonies.api.compatibility.Compatibility#getItemLevel} only knows about items
 * that are actually equipment, so it returns {@code -1} for an ingot or a pickaxe head. The
 * blacksmith recipe ordering in {@link com.minecolonies.compat.tfc.mixin.AbstractCraftingBuildingModuleMixin}
 * needs a tier for both halves of the two-stage chain, so this map is keyed off TFC's metal
 * registry instead of off equipment behaviour.</p>
 *
 * <p>Only metals that produce tools ({@code PartType.ALL}, i.e. those with a non-null
 * {@code toolTier()}) are recorded. Everything else — and every non-TFC item — reports
 * {@link #NO_TIER}.</p>
 */
public final class TFCMetalTiers
{
    /**
     * Reported for any item that is not a TFC item of a tool-capable metal.
     */
    public static final int NO_TIER = -1;

    private static final Map<Item, Integer> TIER_BY_ITEM = new HashMap<>();

    /**
     * Populate the lookup. Must run after item registration; {@code FMLCommonSetupEvent} is fine.
     */
    static void init()
    {
        TIER_BY_ITEM.clear();

        for (final Metal metal : Metal.values())
        {
            // Must be checked BEFORE toolTier(): despite the backing field being @Nullable,
            // Metal.toolTier() is Objects.requireNonNull and throws for any metal without one
            // (bismuth, gold, tin, the intermediate steels, ...). allParts() is the predicate TFC
            // itself uses to decide which metals get tools, and holds exactly when a tier exists.
            if (!metal.allParts())
            {
                continue;
            }
            final LevelTier tier = metal.toolTier();

            final Map<Metal.ItemType, TFCItems.ItemId> items = TFCItems.METAL_ITEMS.get(metal);
            if (items == null)
            {
                continue;
            }

            for (final TFCItems.ItemId itemId : items.values())
            {
                TIER_BY_ITEM.put(itemId.asItem(), tier.level());
            }
        }
    }

    /**
     * @param item the item to look up.
     * @return the tool tier level of the item's metal, or {@link #NO_TIER} if it has none.
     */
    public static int tierOf(final Item item)
    {
        return TIER_BY_ITEM.getOrDefault(item, NO_TIER);
    }

    /**
     * @param stack the stack to look up.
     * @return the tool tier level of the stack's metal, or {@link #NO_TIER} if it has none.
     */
    public static int tierOf(final ItemStack stack)
    {
        return stack.isEmpty() ? NO_TIER : tierOf(stack.getItem());
    }

    private TFCMetalTiers()
    {
        throw new IllegalStateException("Utility class");
    }
}
