package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Utils;
import com.minecolonies.compat.tfc.MinecoloniesAccess;
import com.minecolonies.compat.tfc.TFCFishingHelper;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkFisherman;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reworks the fisherman's rod handling and water detection for TerraFirmaCraft.
 *
 * <h3>Rod handling</h3>
 * MineColonies gates the rod through its equipment-level system, which rejects TFC metal rods
 * outright ({@code Compatibility.getItemLevel} returns -1 for them, so {@code verifyEquipmentLevel}
 * fails) and can only express a level <em>range</em>. This replaces that with an exact
 * rod-per-building-level rule (copper→L1 … black steel→L5) via {@link TFCFishingHelper}:
 * <ul>
 *   <li>{@code prepareForFishing} – have the right rod? proceed; otherwise raise a concrete
 *       {@code Stack} request for it and wait, mirroring the original "missing rod" branch.</li>
 *   <li>{@code getRodSlot} – locate the level-appropriate rod (also fixes {@code isReadyToFish}
 *       and {@code equipRod}, which call it).</li>
 *   <li>{@code hasRodButNotEquipped} – render flag based on the same exact rod.</li>
 * </ul>
 *
 * <h3>Water detection</h3>
 * {@code isReadyToFish} only accepts proximity to vanilla {@code Blocks.WATER}; the redirect also
 * accepts TFC infinite water so the citizen casts next to TFC salt-water oceans.
 *
 * <h3>Accessing MineColonies internals</h3>
 * Only {@code playNeedRodSound()} is declared on {@code EntityAIWorkFisherman} itself, so it is the
 * one member we can {@code @Shadow}. Everything else is inherited from the AI superclasses, and
 * Mixin cannot attach inherited {@code @Shadow} members in this environment (it crashes mod load).
 * We therefore reach them without shadowing: the public {@code building} field and the
 * {@code checkIfRequestForItemExistOrCreate}/{@code getState} methods via a runtime
 * {@link AbstractEntityAIBasic} cast (resolved by the JVM, not Mixin), and the {@code protected}
 * {@code worker} field via {@link MinecoloniesAccess}.
 */
@Mixin(value = EntityAIWorkFisherman.class, remap = false)
public abstract class EntityAIWorkFishermanMixin
{
    @Shadow
    private void playNeedRodSound() { throw new AssertionError(); }

    /**
     * Replaces the vanilla equipment-system rod check with the exact-rod-per-level rule.
     */
    @Inject(method = "prepareForFishing", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfc$prepareForFishing(final CallbackInfoReturnable<IAIState> cir)
    {
        final AbstractEntityAIBasic<?, ?> ai = (AbstractEntityAIBasic<?, ?>) (Object) this;
        final AbstractEntityCitizen worker = MinecoloniesAccess.worker(this);
        final int level = ((IBuilding) ai.building).getBuildingLevel();
        // Item-only match (matchNBT = false) so a damaged or bait-bearing rod still counts; this
        // also pulls the rod from the hut into the citizen, or raises a single Stack request for it.
        if (ai.checkIfRequestForItemExistOrCreate(TFCFishingHelper.rodStackForLevel(level), 1, 1, false, false))
        {
            cir.setReturnValue(AIWorkerState.FISHERMAN_WALKING_TO_WATER);
            return;
        }

        worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStackUtils.EMPTY);
        playNeedRodSound();
        cir.setReturnValue(ai.getState());
    }

    /**
     * Locate the level-appropriate TFC rod in the citizen's inventory.
     */
    @Inject(method = "getRodSlot", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfc$getRodSlot(final CallbackInfoReturnable<Integer> cir)
    {
        final AbstractEntityAIBasic<?, ?> ai = (AbstractEntityAIBasic<?, ?>) (Object) this;
        final int level = ((IBuilding) ai.building).getBuildingLevel();
        cir.setReturnValue(InventoryUtils.findFirstSlotInItemHandlerWith(
            MinecoloniesAccess.worker(this).getInventoryCitizen(),
            stack -> TFCFishingHelper.isRodForLevel(stack, level)));
    }

    /**
     * Render flag: the citizen has its rod in inventory but is not currently holding it.
     */
    @Inject(method = "hasRodButNotEquipped", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfc$hasRodButNotEquipped(final CallbackInfoReturnable<Boolean> cir)
    {
        final AbstractEntityAIBasic<?, ?> ai = (AbstractEntityAIBasic<?, ?>) (Object) this;
        final AbstractEntityCitizen worker = MinecoloniesAccess.worker(this);
        final int level = ((IBuilding) ai.building).getBuildingLevel();
        final boolean inInventory = InventoryUtils.findFirstSlotInItemHandlerWith(
            worker.getInventoryCitizen(), stack -> TFCFishingHelper.isRodForLevel(stack, level)) != -1;
        final ItemStack main = worker.getMainHandItem();
        cir.setReturnValue(inInventory && (main == null || !TFCFishingHelper.isRodForLevel(main, level)));
    }

    /**
     * Cast-readiness water check: also accept proximity to TFC infinite water (salt/spring/river).
     */
    @Redirect(
        method = "isReadyToFish",
        at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/api/util/Utils;isBlockInRange(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/Block;IIII)Z"),
        remap = false)
    private boolean tfc$waterInRange(final Level world, final Block block, final int x, final int y, final int z, final int range)
    {
        return Utils.isBlockInRange(world, block, x, y, z, range) || TFCFishingHelper.isTFCWaterInRange(world, x, y, z, range);
    }
}
