package com.minecolonies.compat.tfc.loot;

import com.minecolonies.api.compatibility.Compatibility;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.jetbrains.annotations.NotNull;

/**
 * Loot condition that checks the held tool's registered MineColonies equipment level.
 * Use {@code min_level} (required) and {@code max_level} (optional, default MAX_INT) in JSON.
 * Replaces the vanilla item-ID hoe checks so TFC hoes receive the correct tier bonuses.
 */
public class HoeTierCondition implements LootItemCondition
{
    public static final MapCodec<HoeTierCondition> CODEC = RecordCodecBuilder.mapCodec(b -> b
        .group(
            Codec.INT.fieldOf("min_level").forGetter(c -> c.minLevel),
            Codec.INT.optionalFieldOf("max_level", Integer.MAX_VALUE).forGetter(c -> c.maxLevel)
        )
        .apply(b, HoeTierCondition::new));

    private final int minLevel;
    private final int maxLevel;

    HoeTierCondition(final int minLevel, final int maxLevel)
    {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    @NotNull
    @Override
    public LootItemConditionType getType()
    {
        return TFCCompatLootConditions.HOE_TIER.get();
    }

    @Override
    public boolean test(@NotNull final LootContext ctx)
    {
        final ItemStack tool = ctx.getParamOrNull(LootContextParams.TOOL);
        if (tool == null || tool.isEmpty() || !tool.is(ItemTags.HOES))
        {
            return false;
        }
        int level = Compatibility.getItemLevel(tool);
        if (level < 0)
        {
            // Item is in #minecraft:hoes but not registered with MineColonies — treat as level 0.
            level = 0;
        }
        return level >= minLevel && level <= maxLevel;
    }
}
