package com.minecolonies.compat.tfc.loot;

import com.minecolonies.compat.tfc.TFCMineColoniesCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TFCCompatLootConditions
{
    public static final DeferredRegister<LootItemConditionType> REGISTER =
        DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, TFCMineColoniesCompat.MOD_ID);

    /** Checks the held hoe's MineColonies equipment level against a min/max range. */
    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> HOE_TIER =
        REGISTER.register("hoe_tier", () -> new LootItemConditionType(HoeTierCondition.CODEC));
}
