package com.minecolonies.compat.tfc;

import com.minecolonies.api.compatibility.Compatibility;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import net.dries007.tfc.common.TFCTags;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Equipment types contributed by this compat mod.
 *
 * <p>MineColonies ships no {@code hammer} equipment type — its own equipment list stops at
 * pickaxe/shovel/axe/hoe/sword/bow/rod/shears/shield/armor/flintandsteel/lead. TFC's smithing
 * chain needs one, because every anvil step requires a hammer in hand.</p>
 *
 * <p>Registering it here lets a {@code crafterrecipes} entry declare {@code "tool":
 * "minecolonies_tfc_compat:hammer"}. At craft time the blacksmith then:</p>
 * <ul>
 *   <li>raises a {@code Tool} request for a hammer if it holds none
 *       ({@code AbstractEntityAIBasic.checkForToolOrWeapon}),</li>
 *   <li>holds it in the main hand while hammering, and</li>
 *   <li>takes 1 durability per completed craft
 *       ({@code AbstractEntityAICrafting.executeCraftingAction}).</li>
 * </ul>
 *
 * <p>The registry is created by MineColonies during {@code NewRegistryEvent}, which fires before
 * any {@code RegisterEvent}, so a foreign {@link DeferredRegister} targeting the same key is safe.
 * {@code EquipmentTypeEntry.parseResourceLocation} preserves non-{@code minecraft} namespaces, so
 * the fully-qualified id survives the JSON round trip.</p>
 */
public final class TFCEquipmentTypes
{
    public static final DeferredRegister<EquipmentTypeEntry> DEFERRED_REGISTER =
      DeferredRegister.create(CommonMinecoloniesAPIImpl.EQUIPMENT_TYPES, TFCMineColoniesCompat.MOD_ID);

    /**
     * Any item in {@code #c:tools/hammer} — that is every TFC metal hammer plus the rock hammers.
     * The level comes from {@link Compatibility#getItemLevel}, which {@link TFCMineColoniesCompat}
     * populates from TFC's {@code LevelTier}, so a steel hammer outranks a copper one.
     */
    public static final DeferredHolder<EquipmentTypeEntry, EquipmentTypeEntry> hammer =
      DEFERRED_REGISTER.register("hammer", () -> new EquipmentTypeEntry.Builder()
        .setRegistryName(ResourceLocation.fromNamespaceAndPath(TFCMineColoniesCompat.MOD_ID, "hammer"))
        .setDisplayName(Component.translatable("com." + TFCMineColoniesCompat.MOD_ID + ".equipment.hammer"))
        .setIsEquipment((stack, equipmentType) -> stack.is(TFCTags.Items.TOOLS_HAMMER))
        .setEquipmentLevel((stack, equipmentType) -> Compatibility.getItemLevel(stack))
        .build());

    private TFCEquipmentTypes()
    {
        throw new IllegalStateException("Utility class");
    }
}
