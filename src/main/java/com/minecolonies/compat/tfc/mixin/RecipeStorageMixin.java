package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.api.crafting.RecipeStorage;
import com.minecolonies.compat.tfc.TFCFoodHelper;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Stops MineColonies' stored recipe outputs from being permanently rotten.
 *
 * <p>TFC stamps a creation date on a food stack when it is constructed, from a calendar that still
 * reads 0 during datapack parsing — so every output stack MineColonies builds at load time is dated
 * "6 hours after world start" and is long expired in any established world. The state then latches:
 * the first read rewrites the date to {@code ROTTEN_FLAG}, and the persistent codec repeats that on
 * the encode side, so it is written into the colony save.</p>
 *
 * <p>That matters more here than it would elsewhere, because these stacks are not display-only.
 * {@code RecipeStorage.insertCraftedItems} hands workers {@code outputStack.copy()} of the stored
 * output, and {@code ItemStack.copy()} deep-copies the component patch — so a stale date propagates
 * into every item a worker ever crafts.</p>
 *
 * <p>TFC solves this for vanilla recipes in {@code FoodCapability.markRecipeOutputsAsNonDecaying},
 * which walks the vanilla {@code RecipeManager} after each reload. MineColonies keeps its recipes in
 * its own storage, so that loop never sees them; this constructor is the equivalent choke point.
 * Custom {@code crafterrecipes}, recipes taught to a worker, {@code FurnaceRecipes.loadRecipes}, and
 * recipes deserialized from colony NBT all build through it, so marking here also repairs colonies
 * that already have the rotten flag baked into their save.</p>
 */
@Mixin(value = RecipeStorage.class, remap = false)
public class RecipeStorageMixin
{
    @Shadow
    @Final
    private ItemStack primaryOutput;

    @Shadow
    @Final
    private List<ItemStack> secondaryOutputs;

    @Shadow
    @Final
    private List<ItemStack> alternateOutputs;

    /**
     * {@code secondaryOutputs} is filled by {@code processInputsAndTools} on the last line of the
     * constructor, so TAIL is the earliest point at which all three lists are populated.
     */
    @Inject(method = "<init>(Lcom/minecolonies/api/crafting/RecipeStorage$Builder;)V", at = @At("TAIL"))
    private void markFoodOutputsNonDecaying(final RecipeStorage.Builder builder, final CallbackInfo ci)
    {
        TFCFoodHelper.markNonDecaying(primaryOutput);
        secondaryOutputs.forEach(TFCFoodHelper::markNonDecaying);
        alternateOutputs.forEach(TFCFoodHelper::markNonDecaying);
    }
}
