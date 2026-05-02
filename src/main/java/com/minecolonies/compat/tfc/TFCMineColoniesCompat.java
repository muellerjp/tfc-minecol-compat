package com.minecolonies.compat.tfc;

import com.minecolonies.api.compatibility.Compatibility;
import net.dries007.tfc.common.LevelTier;
import net.dries007.tfc.common.items.JavelinItem;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.items.TFCMaceItem;
import net.dries007.tfc.util.Metal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TieredItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod(TFCMineColoniesCompat.MOD_ID)
public class TFCMineColoniesCompat
{
    public static final String MOD_ID = "minecolonies_tfc_compat";

    private static volatile Map<Item, Integer> maceToLevel = null;

    public TFCMineColoniesCompat(final IEventBus modEventBus)
    {
        Compatibility.registerEquipmentLevelProvider((stack, equipmentType) -> {
            if (stack.getItem() instanceof final TieredItem tiered
                && tiered.getTier() instanceof final LevelTier lt)
            {
                return lt.level();
            }
            // TFCMaceItem extends MaceItem (not TieredItem) — level via reverse lookup built
            // from TFC's metal registry. Initialised lazily so item registration is complete.
            final Integer maceLevel = getMaceToLevel().get(stack.getItem());
            return maceLevel != null ? maceLevel : -1;
        });

        // JavelinItem extends SwordItem but excludes SWORD_SWEEP, so it fails
        // MineColonies' canPerformDefaultActions(DEFAULT_SWORD_ACTIONS) check.
        // TFCMaceItem extends MaceItem (not TieredItem) and also excludes SWORD_SWEEP.
        Compatibility.registerWeaponRecognizer(stack -> stack.getItem() instanceof JavelinItem);
        Compatibility.registerWeaponRecognizer(stack -> stack.getItem() instanceof TFCMaceItem);
    }

    private static Map<Item, Integer> getMaceToLevel()
    {
        if (maceToLevel == null)
        {
            final Map<Item, Integer> map = new HashMap<>();
            for (final Metal metal : Metal.values())
            {
                final Map<Metal.ItemType, TFCItems.ItemId> metalItems = TFCItems.METAL_ITEMS.get(metal);
                if (metalItems == null) continue;
                final TFCItems.ItemId maceId = metalItems.get(Metal.ItemType.MACE);
                if (maceId == null) continue;
                // Metal has a MACE item only when its PartType is ALL or ALL_WEATHERING,
                // both of which guarantee a non-null toolTier.
                map.put(maceId.asItem(), metal.toolTier().level());
            }
            maceToLevel = map;
        }
        return maceToLevel;
    }
}
