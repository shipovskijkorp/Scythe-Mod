package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class WitheringSoulHandler {

    private WitheringSoulHandler() {
    }

    /** Called once by the loader after a credited kill, on the server thread. */
    public static void onKill(ServerWorld world, Entity entity, LivingEntity killedEntity) {
        ServerPlayerEntity owner = findSoulOwner(entity);
        if (owner == null) return;
        awardSouls(owner, killedEntity);
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
                ? ScytheBalance.Withering.SOULS_PER_PLAYER_KILL
                : ScytheBalance.Withering.SOULS_PER_MOB_KILL;
        if (EnchantmentHelper.getLevel(ScytheMod.SOUL_SIPHON, scythe) > 0 && owner.getRandom().nextDouble() < ScytheBalance.Withering.SOUL_SIPHON_CHANCE) {
            amount *= ScytheBalance.Withering.SOUL_SIPHON_MULTIPLIER;
        }
        WitheringScytheItem.addSouls(scythe, amount);
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
