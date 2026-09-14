package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Registers all non-recipe sources of Frozen Hearts. */
public final class FrozenHeartDropHandler {

    private static final Identifier STRAY_LOOT_TABLE = Identifier.ofVanilla("entities/stray");
    private static final Identifier IGLOO_CHEST_LOOT_TABLE = Identifier.ofVanilla("chests/igloo_chest");
    private static final float HEART_CHANCE = 0.05F;

    private FrozenHeartDropHandler() {
    }

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) {
                return;
            }

            Identifier id = key.getValue();
            if (!STRAY_LOOT_TABLE.equals(id) && !IGLOO_CHEST_LOOT_TABLE.equals(id)) {
                return;
            }

            LootPool.Builder pool = LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1.0F))
                    .with(ItemEntry.builder(ScytheMod.FROZEN_HEART)
                            .conditionally(RandomChanceLootCondition.builder(HEART_CHANCE)));
            tableBuilder.pool(pool);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return;
            }
            if (!damageSource.isOf(DamageTypes.FREEZE)) {
                return;
            }
            if (!player.inPowderSnow && !player.wasInPowderSnow) {
                return;
            }

            ItemEntity drop = new ItemEntity(
                    player.getWorld(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    new ItemStack(ScytheMod.FROZEN_HEART)
            );
            drop.setToDefaultPickupDelay();
            player.getWorld().spawnEntity(drop);
        });
    }
}
