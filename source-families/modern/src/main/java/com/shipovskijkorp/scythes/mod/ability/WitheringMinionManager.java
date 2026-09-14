package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.List;

public final class WitheringMinionManager {

    private WitheringMinionManager() {
    }

    private static final double SEARCH_RADIUS = 160.0D;

    public static int countMinions(ServerPlayerEntity owner) {
        return findMinions(owner).size();
    }

    public static int dismissMinions(ServerPlayerEntity owner) {
        List<WitheringMinionEntity> minions = findMinions(owner);
        for (WitheringMinionEntity minion : minions) {
            minion.discard();
        }
        return minions.size();
    }

    public static boolean spawnMinion(ServerPlayerEntity owner) {
        ServerWorld world = owner.getServerWorld();
        WitheringMinionEntity minion = new WitheringMinionEntity(ScytheMod.WITHERING_MINION, world);
        minion.initializeForOwner(owner);

        double yaw = Math.toRadians(owner.getYaw());
        double x = owner.getX() - Math.sin(yaw) * 1.6D;
        double y = owner.getY();
        double z = owner.getZ() + Math.cos(yaw) * 1.6D;

        minion.refreshPositionAndAngles(x, y, z, owner.getYaw(), 0.0F);
        return world.spawnEntity(minion);
    }

    public static boolean damageOwnerScytheForMinionRegen(ServerPlayerEntity owner, int durabilityCost) {
        if (durabilityCost <= 0) return true;

        ItemStack mainHand = owner.getMainHandStack();
        if (isUsableWitheringScythe(mainHand, durabilityCost)) {
            Item brokenItem = mainHand.getItem();
            damageScytheExact(mainHand, durabilityCost);
            if (mainHand.isEmpty()) {
                owner.sendEquipmentBreakStatus(brokenItem, EquipmentSlot.MAINHAND);
            }
            return true;
        }

        ItemStack offHand = owner.getOffHandStack();
        if (isUsableWitheringScythe(offHand, durabilityCost)) {
            Item brokenItem = offHand.getItem();
            damageScytheExact(offHand, durabilityCost);
            if (offHand.isEmpty()) {
                owner.sendEquipmentBreakStatus(brokenItem, EquipmentSlot.OFFHAND);
            }
            return true;
        }

        for (int slot = 0; slot < owner.getInventory().size(); slot++) {
            ItemStack stack = owner.getInventory().getStack(slot);
            if (stack == mainHand || stack == offHand) continue;
            if (!isUsableWitheringScythe(stack, durabilityCost)) continue;

            damageScytheExact(stack, durabilityCost);
            return true;
        }

        return false;
    }

    public static List<WitheringMinionEntity> findMinions(ServerPlayerEntity owner) {
        Box box = owner.getBoundingBox().expand(SEARCH_RADIUS);
        return owner.getServerWorld().getEntitiesByClass(
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
        if (durabilityCost <= 0 || stack.isEmpty() || !stack.isDamageable()) return;

        int newDamage = stack.getDamage() + durabilityCost;
        if (newDamage >= stack.getMaxDamage()) {
            stack.decrement(1);
        } else {
            stack.setDamage(newDamage);
        }
    }
}
