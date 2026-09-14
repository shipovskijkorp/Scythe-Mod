package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WitheringSoulHandler {

    private WitheringSoulHandler() {
    }

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            ServerPlayerEntity owner = findSoulOwner(entity);
            if (owner == null) return;
            awardSouls(owner, killedEntity);
        });
    }

    public static void tryAwardTrackedWitheringDeath(LivingEntity killedEntity, DamageSource source) {
        Entity attacker = source.getAttacker();
        if (attacker instanceof ServerPlayerEntity || attacker instanceof WitheringMinionEntity) {
            return;
        }

        ServerPlayerEntity owner = DamageAttributionTracker.getWitheringOwner(killedEntity);
        if (owner != null) {
            awardSouls(owner, killedEntity);
        }
    }

    private static ServerPlayerEntity findSoulOwner(Entity killerEntity) {
        if (killerEntity instanceof ServerPlayerEntity player) {
            if (findWitheringScythe(player).isEmpty()) return null;
            return player;
        }

        if (killerEntity instanceof WitheringMinionEntity minion) {
            return minion.getOwnerPlayer();
        }

        return null;
    }

    private static void awardSouls(ServerPlayerEntity owner, LivingEntity killedEntity) {
        if (killedEntity instanceof ServerPlayerEntity playerVictim && owner.isTeammate(playerVictim)) return;

        ItemStack scythe = findWitheringScythe(owner);
        if (scythe.isEmpty()) return;

        int amount = killedEntity instanceof ServerPlayerEntity
                ? WitheringScytheItem.SOULS_PER_PLAYER_KILL
                : WitheringScytheItem.SOULS_PER_MOB_KILL;

        if (hasSoulSiphon(owner, scythe) && owner.getRandom().nextDouble() < 0.50D) {
            amount *= 2;
        }

        WitheringScytheItem.addSouls(scythe, amount);
    }


    private static boolean hasSoulSiphon(ServerPlayerEntity owner, ItemStack scythe) {
        return owner.getRegistryManager()
                .getWrapperOrThrow(RegistryKeys.ENCHANTMENT)
                .getOptional(ScytheMod.SOUL_SIPHON)
                .map(enchantment -> EnchantmentHelper.getLevel(enchantment, scythe) > 0)
                .orElse(false);
    }

    private static ItemStack findWitheringScythe(ServerPlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof WitheringScytheItem) {
            return player.getMainHandStack();
        }
        if (player.getOffHandStack().getItem() instanceof WitheringScytheItem) {
            return player.getOffHandStack();
        }
        return ItemStack.EMPTY;
    }
}
