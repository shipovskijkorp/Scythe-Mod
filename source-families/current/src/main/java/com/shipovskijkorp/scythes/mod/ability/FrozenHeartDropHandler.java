package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/** Registers all non-recipe sources of Frozen Hearts. */
public final class FrozenHeartDropHandler {

    private static final ResourceKey<LootTable> STRAY_LOOT_TABLE = ResourceKey.create(
            Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath("minecraft", "entities/stray")
    );
    private static final ResourceKey<LootTable> IGLOO_CHEST_LOOT_TABLE = ResourceKey.create(
            Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath("minecraft", "chests/igloo_chest")
    );

    private FrozenHeartDropHandler() {
    }

    /** Add loot only to builtin tables; called by a loader loot hook. */
    public static void modifyLoot(ResourceKey<LootTable> key, LootTable.Builder tableBuilder, boolean builtin) {
        LootPool.Builder pool = createLootPool(key, builtin);
        if (pool != null) tableBuilder.withPool(pool);
    }

    /** Shared pool factory for loaders whose loot event exposes an already-built table. */
    public static LootPool.Builder createLootPool(ResourceKey<LootTable> key, boolean builtin) {
        if (!builtin) return null;
        if (!STRAY_LOOT_TABLE.equals(key) && !IGLOO_CHEST_LOOT_TABLE.equals(key)) return null;

        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(ScytheBalance.Drops.FROZEN_HEART_LOOT_ROLLS))
                .add(LootItem.lootTableItem(ScytheMod.FROZEN_HEART)
                        .when(LootItemRandomChanceCondition.randomChance(ScytheBalance.Drops.HEART_CHANCE)));
    }

    public static void onDeath(LivingEntity entity, DamageSource damageSource) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!damageSource.is(DamageTypes.FREEZE)) return;
        if (!player.isInPowderSnow && !player.wasInPowderSnow) return;

        ServerLevel level = (ServerLevel) player.level();
        ItemEntity drop = new ItemEntity(
                level,
                player.getX(),
                player.getY(),
                player.getZ(),
                new ItemStack(ScytheMod.FROZEN_HEART, ScytheBalance.Drops.FROZEN_HEART_COUNT)
        );
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }
}
