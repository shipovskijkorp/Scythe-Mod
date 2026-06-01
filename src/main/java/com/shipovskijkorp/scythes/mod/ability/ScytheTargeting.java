package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class ScytheTargeting {

    private ScytheTargeting() {
    }

    public static boolean canHit(ServerPlayerEntity owner, LivingEntity target) {
        if (target == owner) return false;
        if (!target.isAlive()) return false;
        if (target instanceof ServerPlayerEntity player && player.isSpectator()) return false;
        if (owner.isTeammate(target)) return false;
        return !isProtectedPet(owner, target);
    }

    private static boolean isProtectedPet(ServerPlayerEntity owner, LivingEntity target) {
        if (!(target instanceof TameableEntity tameable)) return false;
        if (!tameable.isTamed()) return false;
        if (tameable.isOwner(owner)) return true;

        UUID petOwnerUuid = tameable.getOwnerUuid();
        if (petOwnerUuid == null) return false;

        Entity petOwner = owner.getServerWorld().getEntity(petOwnerUuid);
        return petOwner != null && owner.isTeammate(petOwner);
    }
}
