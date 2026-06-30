package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.api.util.Tuple;
import com.minecolonies.compat.tfc.TFCFishingHelper;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingFisherman;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Makes the fisherman keep exactly the TFC metal rod matching its building level (copper at L1 …
 * black steel at L5). MineColonies' own keepX entry gates rods through the equipment-level system,
 * which rejects TFC rods entirely (their {@code Compatibility.getItemLevel} is -1) and could only
 * ever express a level *range*, not the exact per-level rule. The original entry is left in place
 * (harmless: it never matches a TFC rod, and no vanilla rods exist in TFC).
 *
 * The predicate reads {@code getBuildingLevel()} dynamically, so the kept rod follows hut upgrades.
 */
@Mixin(value = BuildingFisherman.class, remap = false)
public abstract class BuildingFishermanMixin
{
    @Shadow
    protected Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> keepX;

    @Shadow
    public abstract int getBuildingLevel();

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void tfc$keepLevelRod(final CallbackInfo ci)
    {
        keepX.put(stack -> TFCFishingHelper.isRodForLevel(stack, getBuildingLevel()), new Tuple<>(1, true));
    }
}
