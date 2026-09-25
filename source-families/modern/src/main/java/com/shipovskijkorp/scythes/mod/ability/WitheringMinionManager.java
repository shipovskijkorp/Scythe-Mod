package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class WitheringMinionManager {
    private static final int RESTORE_MAX_ATTEMPTS = 5;
    private static final long RESTORE_RETRY_INTERVAL_TICKS = 20L;
    private static final Map<UUID, RestoreAttempt> RESTORE_ATTEMPTS = new HashMap<>();

    private WitheringMinionManager() {}

//? if >=1.21.11 {
    public static int countMinions(ServerPlayerEntity owner, ServerWorld world) {
//? } else {
    public static int countMinions(ServerPlayerEntity owner) {
//? }
        MinecraftServer server = server(owner);
        if (server == null) return 0;
        return ScythePersistentStore.minionCount(root(server), owner.getUuid());
    }

//? if >=1.21.11 {
    public static int dismissMinions(ServerPlayerEntity owner, ServerWorld world, ItemStack refundScythe) {
//? } else {
    public static int dismissMinions(ServerPlayerEntity owner, ItemStack refundScythe) {
//? }
        MinecraftServer server = server(owner);
        if (server == null) return 0;
        Path root = root(server);
        RESTORE_ATTEMPTS.remove(owner.getUuid());
        int dismissed = 0;
        for (WitheringMinionEntity minion : findRegisteredMinions(owner)) {
            WitheringScytheItem.addSouls(refundScythe, calculateSoulRefund(minion));
            ScythePersistentStore.unregisterMinion(root, owner.getUuid(), minion.getUuid());
            minion.discard();
            dismissed++;
        }
        List<ScythePersistentStore.DormantMinion> dormant = ScythePersistentStore.takeDormantMinions(root, owner.getUuid());
        for (ScythePersistentStore.DormantMinion snapshot : dormant) {
            WitheringScytheItem.addSouls(refundScythe, calculateSoulRefund(snapshot.health()));
            dismissed++;
        }
        return dismissed;
    }

    public static int refundExpiredMinion(ServerPlayerEntity owner, WitheringMinionEntity minion) {
        return returnMinion(owner, minion);
    }

    public static int returnMinion(ServerPlayerEntity owner, WitheringMinionEntity minion) {
        MinecraftServer server = server(owner);
        if (server == null) return 0;
        int refund = calculateSoulRefund(minion);
        ScythePersistentStore.unregisterMinion(root(server), owner.getUuid(), minion.getUuid());
        creditSoulRefund(owner, refund);
        return refund;
    }

    public static int calculateSoulRefund(WitheringMinionEntity minion) {
        return calculateSoulRefund(minion.getHealth());
    }

    private static int calculateSoulRefund(float health) {
        double fraction = Math.max(0.0D, Math.min(1.0D, health / ScytheBalance.Minion.MAX_HEALTH));
        return (int) Math.floor(ScytheBalance.Withering.MINION_SOUL_COST * fraction);
    }

    public static ItemStack findSoulStorageScythe(ServerPlayerEntity owner) {
        ItemStack mainHand = owner.getMainHandStack();
        if (mainHand.getItem() instanceof WitheringScytheItem) return mainHand;
        ItemStack offHand = owner.getOffHandStack();
        if (offHand.getItem() instanceof WitheringScytheItem) return offHand;
        for (int slot = 0; slot < owner.getInventory().size(); slot++) {
            ItemStack stack = owner.getInventory().getStack(slot);
            if (stack.getItem() instanceof WitheringScytheItem) return stack;
        }
        return ItemStack.EMPTY;
    }

//? if >=1.21.11 {
    public static boolean spawnMinion(ServerPlayerEntity owner, ServerWorld world) {
        WitheringMinionEntity minion = createMinion(owner, world);
//? } else {
    public static boolean spawnMinion(ServerPlayerEntity owner) {
        ServerWorld world = owner.getServerWorld();
        WitheringMinionEntity minion = createMinion(owner, world);
//? }
        if (minion == null || !world.spawnEntity(minion)) return false;
        MinecraftServer server = server(owner);
        if (server != null) ScythePersistentStore.registerMinion(root(server), owner.getUuid(), minion.getUuid());
        return true;
    }

    public static void suspendMinions(ServerPlayerEntity owner) {
        RESTORE_ATTEMPTS.remove(owner.getUuid());
        for (WitheringMinionEntity minion : findRegisteredMinions(owner)) suspendOffline(minion);
    }

    public static void suspendOffline(WitheringMinionEntity minion) {
        UUID ownerId = minion.getOwnerUuid();
//? if >=1.21.11 {
        if (!(minion.getEntityWorld() instanceof ServerWorld world)) return;
        MinecraftServer server = world.getServer();
//? } else {
        MinecraftServer server = minion.getWorld().getServer();
//? }
        if (ownerId == null || server == null) return;
        ScythePersistentStore.storeDormantMinion(
                root(server), ownerId, minion.getUuid(), minion.getHealth(), minion.getLifeTicks());
        minion.discard();
    }

    public static void restoreMinions(ServerPlayerEntity owner) {
        RESTORE_ATTEMPTS.remove(owner.getUuid());
        if (tryRestoreDormant(owner)) {
            RESTORE_ATTEMPTS.put(owner.getUuid(), new RestoreAttempt(1, currentGameTime(owner) + RESTORE_RETRY_INTERVAL_TICKS));
        }
    }

    public static void tickRestore(ServerPlayerEntity owner) {
        RestoreAttempt attempt = RESTORE_ATTEMPTS.get(owner.getUuid());
        long now = currentGameTime(owner);
        if (attempt == null || now < attempt.nextAttemptAt()) return;
        int attempts = attempt.attempts() + 1;
        if (!tryRestoreDormant(owner)) {
            RESTORE_ATTEMPTS.remove(owner.getUuid());
        } else if (attempts >= RESTORE_MAX_ATTEMPTS) {
            refundDormantMinions(owner);
            RESTORE_ATTEMPTS.remove(owner.getUuid());
        } else {
            RESTORE_ATTEMPTS.put(owner.getUuid(), new RestoreAttempt(attempts, now + RESTORE_RETRY_INTERVAL_TICKS));
        }
    }

    private static boolean tryRestoreDormant(ServerPlayerEntity owner) {
        MinecraftServer server = server(owner);
        if (server == null) return false;
        Path root = root(server);
        List<ScythePersistentStore.DormantMinion> snapshots = ScythePersistentStore.dormantMinions(root, owner.getUuid());
        if (snapshots.isEmpty()) return false;
//? if >=1.21.11 {
        if (!(owner.getEntityWorld() instanceof ServerWorld world)) return true;
//? } else {
        ServerWorld world = owner.getServerWorld();
//? }
        for (ScythePersistentStore.DormantMinion snapshot : snapshots) {
            if (snapshot.lifeTicks() >= ScytheBalance.Minion.LIFETIME_TICKS) {
                if (ScythePersistentStore.removeDormantMinion(root, owner.getUuid(), snapshot)) {
                    creditSoulRefund(owner, calculateSoulRefund(snapshot.health()));
                }
                continue;
            }
            WitheringMinionEntity minion = createMinion(owner, world);
            if (minion == null) continue;
            minion.restoreRuntimeState(snapshot.health(), snapshot.lifeTicks());
            if (!world.spawnEntity(minion)) continue;
            if (ScythePersistentStore.removeDormantMinion(root, owner.getUuid(), snapshot)) {
                ScythePersistentStore.registerMinion(root, owner.getUuid(), minion.getUuid());
            } else {
                minion.discard();
            }
        }
        return !ScythePersistentStore.dormantMinions(root, owner.getUuid()).isEmpty();
    }

    private static void refundDormantMinions(ServerPlayerEntity owner) {
        MinecraftServer server = server(owner);
        if (server == null) return;
        for (ScythePersistentStore.DormantMinion snapshot : ScythePersistentStore.takeDormantMinions(root(server), owner.getUuid())) {
            creditSoulRefund(owner, calculateSoulRefund(snapshot.health()));
        }
    }

    private static long currentGameTime(ServerPlayerEntity owner) {
//? if >=1.21.11 {
        return owner.getEntityWorld().getTime();
//? } else {
        return owner.getServerWorld().getTime();
//? }
    }

    public static void clearRuntime() {
        RESTORE_ATTEMPTS.clear();
    }

    public static void flushPendingRefunds(ServerPlayerEntity owner) {
        MinecraftServer server = server(owner);
        if (server == null) return;
        Path root = root(server);
        int pending = ScythePersistentStore.pendingSoulRefund(root, owner.getUuid());
        if (pending <= 0) return;
        ItemStack scythe = findSoulStorageScythe(owner);
        if (scythe.isEmpty()) return;
        int before = WitheringScytheItem.getSouls(scythe);
        WitheringScytheItem.addSouls(scythe, pending);
        int accepted = Math.max(0, WitheringScytheItem.getSouls(scythe) - before);
        if (accepted > 0) ScythePersistentStore.consumePendingSoulRefund(root, owner.getUuid(), accepted);
    }

    public static void onMinionDeath(WitheringMinionEntity minion) {
        UUID ownerId = minion.getOwnerUuid();
//? if >=1.21.11 {
        if (!(minion.getEntityWorld() instanceof ServerWorld world)) return;
        MinecraftServer server = world.getServer();
//? } else {
        MinecraftServer server = minion.getWorld().getServer();
//? }
        if (ownerId != null && server != null) {
            ScythePersistentStore.unregisterMinion(root(server), ownerId, minion.getUuid());
        }
    }

    public static void ensureRegistered(WitheringMinionEntity minion) {
        UUID ownerId = minion.getOwnerUuid();
//? if >=1.21.11 {
        if (!(minion.getEntityWorld() instanceof ServerWorld world)) return;
        MinecraftServer server = world.getServer();
//? } else {
        MinecraftServer server = minion.getWorld().getServer();
//? }
        if (ownerId != null && server != null) {
            ScythePersistentStore.registerMinion(root(server), ownerId, minion.getUuid());
        }
    }

    public static boolean damageOwnerScytheForMinionRegen(ServerPlayerEntity owner, int durabilityCost) {
        if (durabilityCost <= 0) return true;
        ItemStack mainHand = owner.getMainHandStack();
        if (isUsableWitheringScythe(mainHand, durabilityCost)) {
            Item brokenItem = mainHand.getItem();
            damageScytheExact(mainHand, durabilityCost);
            if (mainHand.isEmpty()) owner.sendEquipmentBreakStatus(brokenItem, EquipmentSlot.MAINHAND);
            return true;
        }
        ItemStack offHand = owner.getOffHandStack();
        if (isUsableWitheringScythe(offHand, durabilityCost)) {
            Item brokenItem = offHand.getItem();
            damageScytheExact(offHand, durabilityCost);
            if (offHand.isEmpty()) owner.sendEquipmentBreakStatus(brokenItem, EquipmentSlot.OFFHAND);
            return true;
        }
        for (int slot = 0; slot < owner.getInventory().size(); slot++) {
            ItemStack stack = owner.getInventory().getStack(slot);
            if (stack == mainHand || stack == offHand || !isUsableWitheringScythe(stack, durabilityCost)) continue;
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
        return findRegisteredMinions(owner);
    }

    private static List<WitheringMinionEntity> findRegisteredMinions(ServerPlayerEntity owner) {
        MinecraftServer server = server(owner);
        if (server == null) return List.of();
        Set<UUID> ids = ScythePersistentStore.activeMinions(root(server), owner.getUuid());
        List<WitheringMinionEntity> result = new ArrayList<>();
        for (UUID id : ids) {
            boolean resolved = false;
            for (ServerWorld world : server.getWorlds()) {
                Entity entity = world.getEntity(id);
                if (entity == null) continue;
                resolved = true;
                if (entity instanceof WitheringMinionEntity minion && minion.isAlive() && minion.isOwner(owner)) {
                    result.add(minion);
                } else {
                    ScythePersistentStore.unregisterMinion(root(server), owner.getUuid(), id);
                }
                break;
            }
            // Missing entities may simply live in unloaded chunks, so keep those registry entries intact.
            if (!resolved) continue;
        }
        return result;
    }

    private static WitheringMinionEntity createMinion(ServerPlayerEntity owner, ServerWorld world) {
        WitheringMinionEntity minion = new WitheringMinionEntity(ScytheMod.WITHERING_MINION, world);
        minion.initializeForOwner(owner);
        double baseYaw = Math.toRadians(owner.getYaw());
        double[] yOffsets = {0.0D, 1.0D, -1.0D};
        for (int step = 0; step < 8; step++) {
            double angle = baseYaw + step * Math.PI / 4.0D;
            double x = owner.getX() - Math.sin(angle) * ScytheBalance.Minion.SPAWN_DISTANCE;
            double z = owner.getZ() + Math.cos(angle) * ScytheBalance.Minion.SPAWN_DISTANCE;
            for (double yOffset : yOffsets) {
                minion.refreshPositionAndAngles(x, owner.getY() + yOffset, z, owner.getYaw(), 0.0F);
                if (world.isSpaceEmpty(minion)) return minion;
            }
        }
        return null;
    }

    private static void creditSoulRefund(ServerPlayerEntity owner, int refund) {
        if (refund <= 0) return;
        MinecraftServer server = server(owner);
        if (server == null) return;
        Path root = root(server);
        ItemStack scythe = findSoulStorageScythe(owner);
        if (scythe.isEmpty()) {
            ScythePersistentStore.addPendingSoulRefund(root, owner.getUuid(), refund);
            return;
        }
        int before = WitheringScytheItem.getSouls(scythe);
        WitheringScytheItem.addSouls(scythe, refund);
        int accepted = Math.max(0, WitheringScytheItem.getSouls(scythe) - before);
        if (accepted < refund) ScythePersistentStore.addPendingSoulRefund(root, owner.getUuid(), refund - accepted);
    }

    private static MinecraftServer server(ServerPlayerEntity owner) {
//? if >=1.21.11 {
        return owner.getEntityWorld() instanceof ServerWorld world ? world.getServer() : null;
//? } else {
        return owner.getServerWorld().getServer();
//? }
    }

    private static Path root(MinecraftServer server) { return ScytheRuntimeState.worldRoot(server); }

    private static boolean isUsableWitheringScythe(ItemStack stack, int durabilityCost) {
        return !stack.isEmpty() && stack.getItem() instanceof WitheringScytheItem
                && WitheringScytheItem.hasEnoughDurability(stack, durabilityCost);
    }

    private static void damageScytheExact(ItemStack stack, int durabilityCost) {
        if (durabilityCost <= 0 || stack.isEmpty() || !stack.isDamageable()) return;
        int newDamage = stack.getDamage() + durabilityCost;
        if (newDamage >= stack.getMaxDamage()) stack.decrement(1);
        else stack.setDamage(newDamage);
    }

    private record RestoreAttempt(int attempts, long nextAttemptAt) {}

}
