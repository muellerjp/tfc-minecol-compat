package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.compat.tfc.TFCFishingHelper;
import com.minecolonies.core.entity.other.NewBobberEntity;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Makes the MineColonies bobber float and run its bite countdown on TFC water. The bobber only
 * treats a block as water where {@code fluidstate.is(FluidTags.WATER)} is true (in {@code tick},
 * which drives the BOBBING state, gravity, and {@code catchingFish}; and in
 * {@code getOpenWaterTypeForBlock}, which scores open water). We swap the tested tag to
 * {@code tfc:any_infinite_water} so the bobber works on TFC salt/spring water as well as vanilla
 * water. The purely cosmetic {@code Blocks.WATER} particle checks in {@code catchingFish} are left
 * alone.
 */
@Mixin(value = NewBobberEntity.class, remap = false)
public abstract class NewBobberEntityMixin
{
    @ModifyArg(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private TagKey<Fluid> tfc$tickWater(final TagKey<Fluid> tag)
    {
        return TFCFishingHelper.INFINITE_WATER;
    }

    @ModifyArg(
        method = "getOpenWaterTypeForBlock",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private TagKey<Fluid> tfc$openWater(final TagKey<Fluid> tag)
    {
        return TFCFishingHelper.INFINITE_WATER;
    }
}
