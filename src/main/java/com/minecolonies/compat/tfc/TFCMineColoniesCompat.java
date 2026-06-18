package com.minecolonies.compat.tfc;

import com.minecolonies.api.compatibility.Compatibility;
import com.minecolonies.compat.tfc.loot.TFCCompatLootConditions;
import net.dries007.tfc.common.LevelTier;
import net.dries007.tfc.common.items.JavelinItem;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.items.TFCMaceItem;
import net.dries007.tfc.util.Metal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TieredItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(TFCMineColoniesCompat.MOD_ID)
public class TFCMineColoniesCompat
{
    public static final String MOD_ID = "minecolonies_tfc_compat";

    public TFCMineColoniesCompat(final IEventBus modEventBus)
    {
        TFCCompatLootConditions.REGISTER.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event)
    {
        // Register every TFC TieredItem with the correct level from LevelTier.level().
        // This overwrites the auto-populated entry (which would use getAttackDamageBonus(),
        // a combat multiplier in TFC rather than a tier indicator).
        for (final Item item : BuiltInRegistries.ITEM)
        {
            if (item instanceof final TieredItem tiered && tiered.getTier() instanceof final LevelTier lt)
            {
                Compatibility.registerItemTier(item, lt, lt.level());
            }
        }

        // TFCMaceItem extends MaceItem (not TieredItem), so it is not caught above.
        // Resolve its level via TFC's metal registry.
        for (final Metal metal : Metal.values())
        {
            final var metalItems = TFCItems.METAL_ITEMS.get(metal);
            if (metalItems == null) continue;
            final TFCItems.ItemId maceId = metalItems.get(Metal.ItemType.MACE);
            if (maceId == null) continue;
            if (metal.toolTier() instanceof final LevelTier lt)
            {
                Compatibility.registerItemTier(maceId.asItem(), lt, lt.level());
            }
        }

        // JavelinItem extends SwordItem but excludes SWORD_SWEEP, so it fails
        // MineColonies' canPerformDefaultActions(DEFAULT_SWORD_ACTIONS) check.
        // TFCMaceItem extends MaceItem and also excludes SWORD_SWEEP.
        Compatibility.registerWeaponRecognizer(stack -> stack.getItem() instanceof JavelinItem);
        Compatibility.registerWeaponRecognizer(stack -> stack.getItem() instanceof TFCMaceItem);
    }
}
