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
//? if >=1.21.11 {
import net.minecraft.server.world.ServerWorld;
//? } else {
//? }
import net.minecraft.util.Identifier;

/** Registers all non-recipe sources of Frozen Hearts. */
public final class FrozenHeartDropHandler {

    private static final Identifier STRAY_LOOT_TABLE = Identifier.ofVanilla("entities/stray");
    private static final Identifier IGLOO_CHEST_LOOT_TABLE = Identifier.ofVanilla("chests/igloo_chest");

    private FrozenHeartDropHandler() {
    }

    /** Build the loader-neutral Frozen Heart pool for builtin vanilla loot tables. */
    public static LootPool.Builder createLootPool(Identifier id, boolean builtin) {
        if (!builtin) {
            return null;
        }
        if (!STRAY_LOOT_TABLE.equals(id) && !IGLOO_CHEST_LOOT_TABLE.equals(id)) {
            return null;
        }

        return LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(ScytheBalance.Drops.FROZEN_HEART_LOOT_ROLLS))
                .with(ItemEntry.builder(ScytheMod.FROZEN_HEART)
                        .conditionally(RandomChanceLootCondition.builder(ScytheBalance.Drops.HEART_CHANCE)));
    }

    /** Add loot only to builtin tables; called by the Fabric loot hook. */
    public static void modifyLoot(Identifier id, LootTable.Builder tableBuilder, boolean builtin) {
        LootPool.Builder pool = createLootPool(id, builtin);
        if (pool != null) {
            tableBuilder.pool(pool);
        }
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

//? if >=1.21.11 {
        ServerWorld world = player.getEntityWorld();
//? } else {
//? }
        ItemEntity drop = new ItemEntity(
//? if >=1.21.11 {
                world,
//? } else {
                player.getWorld(),
//? }
                player.getX(),
                player.getY(),
                player.getZ(),
                new ItemStack(ScytheMod.FROZEN_HEART, ScytheBalance.Drops.FROZEN_HEART_COUNT)
        );
        drop.setToDefaultPickupDelay();
//? if >=1.21.11 {
        world.spawnEntity(drop);
//? } else {
        player.getWorld().spawnEntity(drop);
//? }
    }
}
