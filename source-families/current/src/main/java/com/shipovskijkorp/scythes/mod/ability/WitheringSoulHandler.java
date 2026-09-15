package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class WitheringSoulHandler {

    private WitheringSoulHandler() {
    }

    /** Called once by the loader after a credited kill, on the server thread. */
    public static void onKill(ServerLevel world, Entity entity, LivingEntity killedEntity) {
        if (entity instanceof WitheringMinionEntity minion) {
            ServerPlayer owner = minion.getOwnerPlayer();
            if (owner == null) return;

            ItemStack scythe = WitheringMinionManager.findSoulStorageScythe(owner);
            if (scythe.isEmpty()) return;
            awardSouls(owner, killedEntity, scythe);
            return;
        }

        ServerPlayer owner = findSoulOwner(entity);
        if (owner == null) return;
        awardSouls(owner, killedEntity);
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
        ItemStack scythe = findWitheringScythe(owner);
        if (scythe.isEmpty()) return;
        awardSouls(owner, killedEntity, scythe);
    }

    private static void awardSouls(ServerPlayer owner, LivingEntity killedEntity, ItemStack scythe) {
        if (killedEntity instanceof ServerPlayer playerVictim && owner.isAlliedTo(playerVictim)) return;

        int amount = killedEntity instanceof ServerPlayer
                ? ScytheBalance.Withering.SOULS_PER_PLAYER_KILL
                : ScytheBalance.Withering.SOULS_PER_MOB_KILL;

        if (WitheringScytheItem.getSoulSiphonLevel(owner, scythe) > 0 && owner.getRandom().nextDouble() < ScytheBalance.Withering.SOUL_SIPHON_CHANCE) {
            amount *= ScytheBalance.Withering.SOUL_SIPHON_MULTIPLIER;
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
