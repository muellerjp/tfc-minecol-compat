package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.compat.tfc.MinecoloniesAccess;
import com.minecolonies.compat.tfc.TFCFishingHelper;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingFisherman;
import net.minecraft.util.Tuple;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the fisherman keep exactly the TFC metal rod matching its building level (copper at L1 …
 * black steel at L5). MineColonies' own keepX entry gates rods through the equipment-level system,
 * which rejects TFC rods entirely (their {@code Compatibility.getItemLevel} is -1) and could only
 * ever express a level *range*, not the exact per-level rule. The original entry is left in place
 * (harmless: it never matches a TFC rod, and no vanilla rods exist in TFC).
 *
 * <p>Both members used here are inherited: {@code keepX} from {@code AbstractBuildingContainer} and
 * {@code getBuildingLevel()} from the {@link IBuilding} interface. Mixin's {@code @Shadow} cannot
 * attach inherited members in this environment (it crashes mod load), so neither is shadowed:
 * {@code getBuildingLevel()} is reached with a runtime {@link IBuilding} cast, and {@code keepX} via
 * {@link MinecoloniesAccess}. The predicate reads the level dynamically, so the kept rod follows
 * hut upgrades.
 */
@Mixin(value = BuildingFisherman.class, remap = false)
public abstract class BuildingFishermanMixin
{
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void tfc$keepLevelRod(final CallbackInfo ci)
    {
        MinecoloniesAccess.keepX(this).put(
            stack -> TFCFishingHelper.isRodForLevel(stack, ((IBuilding) (Object) this).getBuildingLevel()),
            new Tuple<>(1, true));
    }
}
