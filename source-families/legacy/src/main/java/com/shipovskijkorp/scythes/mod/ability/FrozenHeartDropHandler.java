package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class FrozenHeartDropHandler {

    private static final Identifier STRAY_LOOT_TABLE = new Identifier("minecraft", "entities/stray");
    private static final Identifier IGLOO_CHEST_LOOT_TABLE = new Identifier("minecraft", "chests/igloo_chest");

    private FrozenHeartDropHandler() {
    }

    /** Add loot only to builtin tables; called by a loader loot hook. */
    public static void modifyLoot(Identifier id, LootTable.Builder tableBuilder, boolean builtin) {
        if (!builtin) {
            return;
        }
        if (!STRAY_LOOT_TABLE.equals(id) && !IGLOO_CHEST_LOOT_TABLE.equals(id)) {
            return;
        }

        LootPool.Builder pool = LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(ScytheBalance.Drops.FROZEN_HEART_LOOT_ROLLS))
                .with(ItemEntry.builder(ScytheMod.FROZEN_HEART)
                        .conditionally(RandomChanceLootCondition.builder(ScytheBalance.Drops.HEART_CHANCE)));
        tableBuilder.pool(pool);
    }

    public static void onDeath(LivingEntity entity, DamageSource damageSource) {
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
                new ItemStack(ScytheMod.FROZEN_HEART, ScytheBalance.Drops.FROZEN_HEART_COUNT)
        );
        drop.setToDefaultPickupDelay();
        player.getWorld().spawnEntity(drop);
    }
}
