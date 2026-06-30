package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.api.util.Pond;
import com.minecolonies.compat.tfc.TFCFishingHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Lets the fisherman's pond scan recognise TFC water. {@code Pond.checkWaterForFishing} tests
 * {@code fluidstate.is(FluidTags.WATER)}, which excludes TFC salt/spring/river water. We swap the
 * tested tag to {@code tfc:any_infinite_water} (a superset that still contains vanilla water), so
 * TFC oceans qualify as ponds. The following {@code isSource()} branch is untouched, so a salt
 * source block is {@code VALID} and flowing water is {@code SUBOPTIMAL}, exactly like vanilla.
 */
@Mixin(value = Pond.class, remap = false)
public abstract class PondMixin
{
    @ModifyArg(
        method = "checkWaterForFishing",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private static TagKey<Fluid> tfc$acceptTFCWater(final TagKey<Fluid> tag)
    {
        return TFCFishingHelper.INFINITE_WATER;
    }
}
