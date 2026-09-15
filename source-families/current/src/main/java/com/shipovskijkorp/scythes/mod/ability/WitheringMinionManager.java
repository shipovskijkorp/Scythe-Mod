package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public final class WitheringMinionManager {

    private WitheringMinionManager() {
    }

    public static int countMinions(ServerPlayer owner, ServerLevel world) {
        return findMinions(owner, world).size();
    }

    public static int dismissMinions(ServerPlayer owner, ServerLevel world) {
        List<WitheringMinionEntity> minions = findMinions(owner, world);
        for (WitheringMinionEntity minion : minions) {
            minion.discard();
        }
        return minions.size();
    }

    public static boolean spawnMinion(ServerPlayer owner, ServerLevel world) {
        WitheringMinionEntity minion = new WitheringMinionEntity(ScytheMod.WITHERING_MINION, world);
        minion.initializeForOwner(owner);

        double yaw = Math.toRadians(owner.getYRot());
        double x = owner.getX() - Math.sin(yaw) * ScytheBalance.Minion.SPAWN_DISTANCE;
        double y = owner.getY();
        double z = owner.getZ() + Math.cos(yaw) * ScytheBalance.Minion.SPAWN_DISTANCE;

        minion.snapTo(x, y, z, owner.getYRot(), 0.0F);
        return world.addFreshEntity(minion);
    }

    public static boolean damageOwnerScytheForMinionRegen(ServerPlayer owner, int durabilityCost) {
        if (durabilityCost <= 0) return true;

        ItemStack mainHand = owner.getMainHandItem();
        if (isUsableWitheringScythe(mainHand, durabilityCost)) {
            damageScytheExact(mainHand, durabilityCost);
            return true;
        }

        ItemStack offHand = owner.getOffhandItem();
        if (isUsableWitheringScythe(offHand, durabilityCost)) {
            damageScytheExact(offHand, durabilityCost);
            return true;
        }

        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            ItemStack stack = owner.getInventory().getItem(slot);
            if (stack == mainHand || stack == offHand) continue;
            if (!isUsableWitheringScythe(stack, durabilityCost)) continue;

            damageScytheExact(stack, durabilityCost);
            return true;
        }

        return false;
    }

    public static List<WitheringMinionEntity> findMinions(ServerPlayer owner, ServerLevel world) {
        AABB box = owner.getBoundingBox().inflate(ScytheBalance.Minion.SEARCH_RADIUS);
        return world.getEntitiesOfClass(
                WitheringMinionEntity.class,
                box,
                minion -> minion.isAlive() && minion.isOwner(owner)
        );
    }

    private static boolean isUsableWitheringScythe(ItemStack stack, int durabilityCost) {
        return !stack.isEmpty()
                && stack.getItem() instanceof WitheringScytheItem
                && WitheringScytheItem.hasEnoughDurability(stack, durabilityCost);
    }

    private static void damageScytheExact(ItemStack stack, int durabilityCost) {
        if (durabilityCost <= 0 || stack.isEmpty() || !stack.isDamageableItem()) return;

        int newDamage = stack.getDamageValue() + durabilityCost;
        if (newDamage >= stack.getMaxDamage()) {
            stack.shrink(1);
        } else {
            stack.setDamageValue(newDamage);
        }
    }
}
