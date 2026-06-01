package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfig;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfigLoader;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;

public class BloodyEssenceDropHandler {

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {

            if (!(entity instanceof ServerPlayerEntity killer)) return;

            if (!world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) return;

            ScytheModConfig config = ScytheModConfigLoader.getConfig();
            double chance;
            if (killedEntity instanceof VillagerEntity) {
                chance = config.bloodyEssenceVillagerDropChance;
            } else if (killedEntity instanceof ServerPlayerEntity victim) {
                if (killer.isTeammate(victim)) return;
                chance = config.bloodyEssencePlayerDropChance;
            } else {
                return;
            }

            chance = Math.max(0.0, Math.min(1.0, chance));
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
