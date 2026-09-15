package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import java.util.List;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

public final class WitheringMinionManager {

    private WitheringMinionManager() {
    }

//? if >=1.21.11 {
    public static int countMinions(ServerPlayerEntity owner, ServerWorld world) {
        return findMinions(owner, world).size();
//? } else {
    public static int countMinions(ServerPlayerEntity owner) {
        return findMinions(owner).size();
//? }
    }

//? if >=1.21.11 {
    public static int dismissMinions(ServerPlayerEntity owner, ServerWorld world) {
        List<WitheringMinionEntity> minions = findMinions(owner, world);
//? } else {
    public static int dismissMinions(ServerPlayerEntity owner) {
        List<WitheringMinionEntity> minions = findMinions(owner);
//? }
        for (WitheringMinionEntity minion : minions) {
            minion.discard();
        }
        return minions.size();
    }

//? if >=1.21.11 {
    public static boolean spawnMinion(ServerPlayerEntity owner, ServerWorld world) {
//? } else {
    public static boolean spawnMinion(ServerPlayerEntity owner) {
        ServerWorld world = owner.getServerWorld();
//? }
        WitheringMinionEntity minion = new WitheringMinionEntity(ScytheMod.WITHERING_MINION, world);
        minion.initializeForOwner(owner);

        double yaw = Math.toRadians(owner.getYaw());
        double x = owner.getX() - Math.sin(yaw) * ScytheBalance.Minion.SPAWN_DISTANCE;
        double y = owner.getY();
        double z = owner.getZ() + Math.cos(yaw) * ScytheBalance.Minion.SPAWN_DISTANCE;

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

//? if >=1.21.11 {
    public static List<WitheringMinionEntity> findMinions(ServerPlayerEntity owner, ServerWorld world) {
//? } else {
    public static List<WitheringMinionEntity> findMinions(ServerPlayerEntity owner) {
//? }
        Box box = owner.getBoundingBox().expand(ScytheBalance.Minion.SEARCH_RADIUS);
//? if >=1.21.11 {
        return world.getEntitiesByClass(
//? } else {
        return owner.getServerWorld().getEntitiesByClass(
//? }
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
