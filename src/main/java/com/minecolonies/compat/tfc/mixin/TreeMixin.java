package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.core.entity.ai.workers.util.Tree;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Teaches the forester trunk-walk that TFC log and wood (bark) variants of the same
 * species belong to the same tree.
 *
 * MineColonies' isBlockPartOfSameTree() strips "_log"/"_wood" from the END of a block
 * path (e.g. "oak_log" → "oak"). TFC block paths are structured differently: the type
 * token is in the middle ("wood/log/oak", "wood/wood/oak"), so the suffix-stripping
 * produces different prefixes and the species check fails.
 *
 * This injection fires when the vanilla return value is false and both candidates are
 * TFC blocks. It compares only the final path segment (the species name, e.g. "oak"),
 * returning true when they match so branch/trunk wood blocks are included in the fell.
 */
@Mixin(value = Tree.class, remap = false)
public abstract class TreeMixin
{
    @Inject(
        method = "isBlockPartOfSameTree",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void injectTFCSameSpecies(
        final BlockState checkBlock,
        final BlockState stumpBlock,
        final CallbackInfoReturnable<Boolean> cir)
    {
        if (cir.getReturnValue()) return;

        final ResourceLocation checkKey = BuiltInRegistries.BLOCK.getKey(checkBlock.getBlock());
        final ResourceLocation stumpKey = BuiltInRegistries.BLOCK.getKey(stumpBlock.getBlock());

        if (!"tfc".equals(checkKey.getNamespace()) || !"tfc".equals(stumpKey.getNamespace())) return;

        final String checkPath = checkKey.getPath();
        final String stumpPath = stumpKey.getPath();

        final int ci = checkPath.lastIndexOf('/');
        final int si = stumpPath.lastIndexOf('/');

        if (ci >= 0 && si >= 0 && checkPath.substring(ci).equals(stumpPath.substring(si)))
        {
            cir.setReturnValue(true);
        }
    }
}
