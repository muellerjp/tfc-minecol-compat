package com.minecolonies.compat.tfc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Isolates all TFC fishing references (the metal fishing rods and the TFC water fluid tag) so the
 * fisherman Mixins never import TFC directly, mirroring {@link TFCFoodHelper}. Everything here is
 * resolved by {@link ResourceLocation}, so the class loads fine even without TFC; the Mixin code
 * paths are only ever reached when both mods are present.
 */
public final class TFCFishingHelper
{
    /**
     * TFC's "any infinite water" fluid tag (salt / spring / river / vanilla fresh water). It is a
     * superset of {@code minecraft:water}, so accepting it keeps vanilla-water detection working
     * while also covering TFC salt-water oceans.
     */
    public static final TagKey<Fluid> INFINITE_WATER =
        TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("tfc", "any_infinite_water"));

    /**
     * Fishing rod required per fisherman building level. Index {@code 0..4} maps to building level
     * {@code 1..5}: copper, bronze, wrought iron, steel, black steel.
     */
    private static final String[] ROD_PATHS = {
        "metal/fishing_rod/copper",
        "metal/fishing_rod/bronze",
        "metal/fishing_rod/wrought_iron",
        "metal/fishing_rod/steel",
        "metal/fishing_rod/black_steel"
    };

    /**
     * @return the exact TFC fishing rod item required at the given fisherman building level
     * (clamped to 1..5). Resolved from the registry on each call so it is robust to load order.
     */
    public static Item getRodForLevel(final int buildingLevel)
    {
        final int index = Mth.clamp(buildingLevel, 1, 5) - 1;
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("tfc", ROD_PATHS[index]));
    }

    /**
     * @return true if {@code stack} is exactly the TFC rod required for the given building level.
     */
    public static boolean isRodForLevel(final ItemStack stack, final int buildingLevel)
    {
        return !stack.isEmpty() && stack.is(getRodForLevel(buildingLevel));
    }

    /**
     * @return a fresh single-item stack of the rod required for the given building level
     * (used to raise a {@code Stack} request when the fisherman has no rod).
     */
    public static ItemStack rodStackForLevel(final int buildingLevel)
    {
        return new ItemStack(getRodForLevel(buildingLevel));
    }

    /**
     * @return true if the fluid counts as fishable water for the fisherman (TFC infinite water,
     * which includes vanilla water).
     */
    public static boolean isFishableWater(final FluidState fluidState)
    {
        return fluidState.is(INFINITE_WATER);
    }

    /**
     * Scans a cube of radius {@code range} around the given position for any fishable TFC water,
     * mirroring the semantics of {@code com.minecolonies.api.util.Utils.isBlockInRange} that the
     * fisherman uses to decide it is close enough to cast.
     *
     * @return true if any block within range contains fishable water.
     */
    public static boolean isTFCWaterInRange(final Level world, final int x, final int y, final int z, final int range)
    {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -range; dx <= range; dx++)
        {
            for (int dy = -range; dy <= range; dy++)
            {
                for (int dz = -range; dz <= range; dz++)
                {
                    pos.set(x + dx, y + dy, z + dz);
                    if (isFishableWater(world.getFluidState(pos)))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private TFCFishingHelper() {}
}
