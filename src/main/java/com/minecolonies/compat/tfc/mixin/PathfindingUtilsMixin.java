package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.compat.tfc.TFCFishingHelper;
import com.minecolonies.core.entity.pathfinding.PathfindingUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Teaches the citizen pathfinder that TFC infinite water (salt/spring/river) counts as water.
 * {@code PathfindingUtils.isWater} only accepts {@code Fluids.WATER}/{@code FLOWING_WATER} by
 * identity, so without this the pathfinder never traverses into or marks TFC salt water as a
 * "swimming" node — and {@code PathJobFindWater} (which requires a swimming node) can never discover
 * an ocean pond for the fisherman.
 *
 * <p>This is the broadest-impact change in the fishing rework: it also lets citizens generally treat
 * TFC water as swimmable, which is correct for TFC but is the change to watch for navigation
 * regressions. Solid (e.g. waterlogged) blocks are still excluded, matching vanilla {@code isWater}.
 */
@Mixin(value = PathfindingUtils.class, remap = false)
public abstract class PathfindingUtilsMixin
{
    @Inject(
        method = "isWater(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)Z",
        at = @At("RETURN"),
        cancellable = true,
        remap = false)
    private static void tfc$acceptTFCWater(
        final BlockGetter world,
        final BlockPos pos,
        final BlockState pState,
        final FluidState pFluidState,
        final CallbackInfoReturnable<Boolean> cir)
    {
        if (cir.getReturnValueZ())
        {
            return;
        }

        final BlockState state = pState != null ? pState : world.getBlockState(pos);
        if (state.isSolid())
        {
            return;
        }

        final FluidState fluidState = pFluidState != null ? pFluidState : state.getFluidState();
        if (TFCFishingHelper.isFishableWater(fluidState))
        {
            cir.setReturnValue(true);
        }
    }
}
