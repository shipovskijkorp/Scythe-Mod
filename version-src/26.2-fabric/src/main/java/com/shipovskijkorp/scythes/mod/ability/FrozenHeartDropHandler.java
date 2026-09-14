package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
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
    private static final float HEART_CHANCE = 0.05F;

    private FrozenHeartDropHandler() {
    }

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) return;
            if (!STRAY_LOOT_TABLE.equals(key) && !IGLOO_CHEST_LOOT_TABLE.equals(key)) return;

            LootPool.Builder pool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(ScytheMod.FROZEN_HEART)
                            .when(LootItemRandomChanceCondition.randomChance(HEART_CHANCE)));
            tableBuilder.withPool(pool);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayer player)) return;
            if (!damageSource.is(DamageTypes.FREEZE)) return;
            if (!player.isInPowderSnow && !player.wasInPowderSnow) return;

            ServerLevel level = (ServerLevel) player.level();
            ItemEntity drop = new ItemEntity(
                    level,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    new ItemStack(ScytheMod.FROZEN_HEART)
            );
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        });
    }
}
