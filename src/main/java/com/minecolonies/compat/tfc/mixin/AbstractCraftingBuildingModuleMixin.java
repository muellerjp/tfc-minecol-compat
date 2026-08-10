package com.minecolonies.compat.tfc.mixin;

import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.compat.tfc.TFCMetalTiers;
import com.minecolonies.core.colony.buildings.modules.AbstractCraftingBuildingModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Orders datapack-granted TFC recipes so the highest metal tier is tried first.
 *
 * <p>Both {@code getFirstRecipe} and {@code getFirstFulfillableRecipe} walk the module's
 * {@code recipes} list and take the first match, so list position <em>is</em> priority. That order
 * is not controllable from a datapack for two reasons:</p>
 * <ol>
 *   <li>{@code CustomRecipeManager.getRecipes(String)} returns {@code new HashSet<>(map.values())},
 *       so the order the recipes arrive in is undefined and unstable across runs.</li>
 *   <li>{@code checkForWorkerSpecificRecipes} appends with {@code addRecipeToList(token, false)},
 *       and re-appends recipes at the tail whenever a {@code min-building-level} gate opens. A
 *       black steel recipe unlocked at hut level 5 would therefore land <em>behind</em> the copper
 *       one it is supposed to outrank.</li>
 * </ol>
 *
 * <p>So instead of trying to control insertion, this re-sorts after the fact, once per colony tick,
 * at the tail of the same method that does the inserting. Because it runs every tick it self-heals
 * after any upgrade/downgrade churn.</p>
 *
 * <p><b>Scope.</b> Only recipes that are datapack-granted ({@code getRecipeSource() != null})
 * <em>and</em> whose primary output is a TFC item of a tool-capable metal are touched, and they are
 * written back into the exact index slots they already occupied. Player-taught recipes keep their
 * hand-picked priority, and other crafters (baker, cook, ...) are unaffected because none of their
 * outputs carry a metal tier.</p>
 *
 * <p><b>Consequence worth knowing.</b> {@code getFirstRecipe} is a capability check that ignores
 * stock, so the blacksmith will now answer "yes, a black steel pickaxe" and raise child requests
 * for black steel ingots even when the colony only has copper. Setting the hut's recipe mode to
 * {@code MAX_STOCK} restores stock-driven selection, which overrides this ordering.</p>
 */
@Mixin(value = AbstractCraftingBuildingModule.class, remap = false)
public abstract class AbstractCraftingBuildingModuleMixin
{
    @Shadow
    protected List<IToken<?>> recipes;

    @Inject(method = "checkForWorkerSpecificRecipes", at = @At("TAIL"))
    private void reorderTfcRecipesByMetalTier(final CallbackInfo ci)
    {
        final Map<IToken<?>, IRecipeStorage> known = IColonyManager.getInstance().getRecipeManager().getRecipes();

        final List<Integer> slots = new ArrayList<>();
        final List<IRecipeStorage> movable = new ArrayList<>();

        for (int i = 0; i < recipes.size(); i++)
        {
            final IRecipeStorage storage = known.get(recipes.get(i));
            if (storage == null || storage.getRecipeSource() == null)
            {
                // Missing, or taught by the player — leave it exactly where it is.
                continue;
            }
            if (TFCMetalTiers.tierOf(storage.getPrimaryOutput()) == TFCMetalTiers.NO_TIER)
            {
                continue;
            }
            slots.add(i);
            movable.add(storage);
        }

        if (movable.size() < 2)
        {
            return;
        }

        final List<IRecipeStorage> sorted = new ArrayList<>(movable);
        // Highest tier first; ties broken by recipe id so the result is fully deterministic
        // rather than inheriting the HashSet iteration order.
        sorted.sort(Comparator
                      .comparingInt((final IRecipeStorage storage) -> TFCMetalTiers.tierOf(storage.getPrimaryOutput()))
                      .reversed()
                      .thenComparing(storage -> storage.getRecipeSource().toString()));

        boolean changed = false;
        for (int k = 0; k < sorted.size(); k++)
        {
            // Compare tokens, not storages: IRecipeStorage.equals is deep content equality.
            if (!sorted.get(k).getToken().equals(movable.get(k).getToken()))
            {
                changed = true;
                break;
            }
        }

        if (!changed)
        {
            return;
        }

        for (int k = 0; k < sorted.size(); k++)
        {
            recipes.set(slots.get(k), sorted.get(k).getToken());
        }

        ((AbstractBuildingModule) (Object) this).markDirty();
    }
}
