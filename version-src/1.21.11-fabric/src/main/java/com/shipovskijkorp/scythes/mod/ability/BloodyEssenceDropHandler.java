package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.rule.GameRules;

public class BloodyEssenceDropHandler {

    public static final double VILLAGER_DROP_CHANCE = 0.05D;
    public static final double PLAYER_DROP_CHANCE = 0.20D;

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity, damageSource) -> {

            if (!(entity instanceof ServerPlayerEntity killer)) return;

            if (!Boolean.TRUE.equals(world.getGameRules().getValue(GameRules.DO_MOB_LOOT))) return;

            double chance;
            if (killedEntity instanceof VillagerEntity) {
                chance = VILLAGER_DROP_CHANCE;
            } else if (killedEntity instanceof ServerPlayerEntity victim) {
                if (killer.isTeammate(victim)) return;
                chance = PLAYER_DROP_CHANCE;
            } else {
                return;
            }

            if (world.getRandom().nextDouble() >= chance) return;

            ItemStack stack = new ItemStack(ScytheMod.BLOODY_ESSENCE);

            ItemEntity drop = new ItemEntity(
                    world,
                    killedEntity.getX(),
                    killedEntity.getY(),
                    killedEntity.getZ(),
                    stack
            );

            drop.setToDefaultPickupDelay();
            world.spawnEntity(drop);
        });
    }
}
