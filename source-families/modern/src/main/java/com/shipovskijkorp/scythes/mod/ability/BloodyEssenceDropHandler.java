package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
//? if >=1.21.11 {
import net.minecraft.world.rule.GameRules;
//? } else {
import net.minecraft.world.GameRules;
//? }

public class BloodyEssenceDropHandler {

    /** Called once by the loader after a credited kill, on the server thread. */
    public static void onKill(ServerWorld world, Entity entity, LivingEntity killedEntity) {
        if (!(entity instanceof ServerPlayerEntity killer)) return;

//? if >=1.21.11 {
        if (!Boolean.TRUE.equals(world.getGameRules().getValue(GameRules.DO_MOB_LOOT))) return;
//? } else {
        if (!world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) return;
//? }

        double chance;
        if (killedEntity instanceof VillagerEntity) {
            chance = ScytheBalance.Drops.VILLAGER_DROP_CHANCE;
        } else if (killedEntity instanceof ServerPlayerEntity victim) {
            if (killer.isTeammate(victim)) return;
            chance = ScytheBalance.Drops.PLAYER_DROP_CHANCE;
        } else {
            return;
        }

        if (world.getRandom().nextDouble() >= chance) return;

        ItemStack stack = new ItemStack(ScytheMod.BLOODY_ESSENCE, ScytheBalance.Drops.BLOODY_ESSENCE_COUNT);

        ItemEntity drop = new ItemEntity(
                world,
                killedEntity.getX(),
                killedEntity.getY(),
                killedEntity.getZ(),
                stack
        );

        drop.setToDefaultPickupDelay();
        world.spawnEntity(drop);
    }
}
