package com.minecolonies.compat.tfc;

import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.compatibility.ICompatibilityManager;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.wood.Wood;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

@EventBusSubscriber(modid = TFCMineColoniesCompat.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class TFCCompatEvents
{
    @SubscribeEvent
    static void onTagsUpdated(final TagsUpdatedEvent event)
    {
        final ICompatibilityManager cm = IColonyManager.getInstance().getCompatibilityManager();
        for (final Wood wood : Wood.values())
        {
            final Block leaves = TFCBlocks.WOODS.get(wood).get(Wood.BlockType.LEAVES).get();
            final ItemStack sapling = new ItemStack(TFCBlocks.WOODS.get(wood).get(Wood.BlockType.SAPLING).get().asItem());
            cm.connectLeafToSapling(leaves, sapling);
        }
    }
}
