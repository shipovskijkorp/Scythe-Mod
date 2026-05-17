package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameRules;
import net.minecraft.server.network.ServerPlayerEntity;

public class BloodyEssenceDropHandler {

    private static final double VILLAGER_CHANCE = 0.05; // 5%
    private static final double PLAYER_CHANCE = 0.20;   // 20%

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {

            // Дропаем только если убил игрок
            if (!(entity instanceof ServerPlayerEntity killer)) return;

            // Уважаем gamerule doMobLoot
            if (!world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) return;

            double chance;
            if (killedEntity instanceof VillagerEntity) {
                chance = VILLAGER_CHANCE;
            } else if (killedEntity instanceof ServerPlayerEntity) {
                chance = PLAYER_CHANCE;
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
