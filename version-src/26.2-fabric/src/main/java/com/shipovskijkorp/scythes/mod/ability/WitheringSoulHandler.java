package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

public final class WitheringSoulHandler {

    private WitheringSoulHandler() {
    }

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity, damageSource) -> {
            ServerPlayer owner = findSoulOwner(entity);
            if (owner == null) return;
            awardSouls(owner, killedEntity);
        });
    }

    public static void tryAwardTrackedWitheringDeath(LivingEntity killedEntity, DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer || attacker instanceof WitheringMinionEntity) {
            return;
        }

        ServerPlayer owner = DamageAttributionTracker.getWitheringOwner(killedEntity);
        if (owner != null) {
            awardSouls(owner, killedEntity);
        }
    }

    private static ServerPlayer findSoulOwner(Entity killerEntity) {
        if (killerEntity instanceof ServerPlayer player) {
            if (findWitheringScythe(player).isEmpty()) return null;
            return player;
        }

        if (killerEntity instanceof WitheringMinionEntity minion) {
            return minion.getOwnerPlayer();
        }

        return null;
    }

    private static void awardSouls(ServerPlayer owner, LivingEntity killedEntity) {
        if (killedEntity instanceof ServerPlayer playerVictim && owner.isAlliedTo(playerVictim)) return;

        ItemStack scythe = findWitheringScythe(owner);
        if (scythe.isEmpty()) return;

        int amount = killedEntity instanceof ServerPlayer
                ? WitheringScytheItem.SOULS_PER_PLAYER_KILL
                : WitheringScytheItem.SOULS_PER_MOB_KILL;

        if (WitheringScytheItem.getSoulSiphonLevel(owner, scythe) > 0 && owner.getRandom().nextDouble() < 0.50D) {
            amount *= 2;
        }

        WitheringScytheItem.addSouls(scythe, amount);
    }

    private static ItemStack findWitheringScythe(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof WitheringScytheItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof WitheringScytheItem) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }
}
