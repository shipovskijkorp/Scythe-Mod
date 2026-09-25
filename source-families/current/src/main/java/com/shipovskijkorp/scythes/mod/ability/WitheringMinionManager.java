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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class WitheringMinionManager {
    private static final int RESTORE_MAX_ATTEMPTS = 5;
    private static final long RESTORE_RETRY_INTERVAL_TICKS = 20L;
    private static final Map<UUID, RestoreAttempt> RESTORE_ATTEMPTS = new HashMap<>();

    private WitheringMinionManager() {}

    public static int countMinions(ServerPlayer owner, ServerLevel world) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) return 0;
        return ScythePersistentStore.minionCount(root(server), owner.getUUID());
    }

    public static int dismissMinions(ServerPlayer owner, ServerLevel world, ItemStack refundScythe) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) return 0;
        Path root = root(server);
        RESTORE_ATTEMPTS.remove(owner.getUUID());
        int dismissed = 0;
        for (WitheringMinionEntity minion : findRegisteredMinions(owner)) {
            WitheringScytheItem.addSouls(refundScythe, calculateSoulRefund(minion));
            ScythePersistentStore.unregisterMinion(root, owner.getUUID(), minion.getUUID());
            minion.discard();
            dismissed++;
        }
        List<ScythePersistentStore.DormantMinion> dormant = ScythePersistentStore.takeDormantMinions(root, owner.getUUID());
        for (ScythePersistentStore.DormantMinion snapshot : dormant) {
            WitheringScytheItem.addSouls(refundScythe, calculateSoulRefund(snapshot.health()));
            dismissed++;
        }
        return dismissed;
    }

    public static int refundExpiredMinion(ServerPlayer owner, WitheringMinionEntity minion) {
        return returnMinion(owner, minion);
    }

    public static int returnMinion(ServerPlayer owner, WitheringMinionEntity minion) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) return 0;
        int refund = calculateSoulRefund(minion);
        ScythePersistentStore.unregisterMinion(root(server), owner.getUUID(), minion.getUUID());
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

    public static ItemStack findSoulStorageScythe(ServerPlayer owner) {
        ItemStack mainHand = owner.getMainHandItem();
        if (mainHand.getItem() instanceof WitheringScytheItem) return mainHand;
        ItemStack offHand = owner.getOffhandItem();
        if (offHand.getItem() instanceof WitheringScytheItem) return offHand;
        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            ItemStack stack = owner.getInventory().getItem(slot);
            if (stack.getItem() instanceof WitheringScytheItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static boolean spawnMinion(ServerPlayer owner, ServerLevel world) {
        WitheringMinionEntity minion = createMinion(owner, world);
        if (minion == null || !world.addFreshEntity(minion)) return false;
        MinecraftServer server = owner.level().getServer();
        if (server != null) ScythePersistentStore.registerMinion(root(server), owner.getUUID(), minion.getUUID());
        return true;
    }

    public static void suspendMinions(ServerPlayer owner) {
        RESTORE_ATTEMPTS.remove(owner.getUUID());
        for (WitheringMinionEntity minion : findRegisteredMinions(owner)) suspendOffline(minion);
    }

    public static void suspendOffline(WitheringMinionEntity minion) {
        UUID ownerId = minion.getOwnerUuid();
        if (!(minion.level() instanceof ServerLevel level) || ownerId == null) return;
        MinecraftServer server = level.getServer();
        ScythePersistentStore.storeDormantMinion(
                root(server), ownerId, minion.getUUID(), minion.getHealth(), minion.getLifeTicks());
        minion.discard();
    }

    public static void restoreMinions(ServerPlayer owner) {
        RESTORE_ATTEMPTS.remove(owner.getUUID());
        if (tryRestoreDormant(owner)) {
            RESTORE_ATTEMPTS.put(owner.getUUID(), new RestoreAttempt(1, owner.level().getGameTime() + RESTORE_RETRY_INTERVAL_TICKS));
        }
    }

    public static void tickRestore(ServerPlayer owner) {
        RestoreAttempt attempt = RESTORE_ATTEMPTS.get(owner.getUUID());
        long now = owner.level().getGameTime();
        if (attempt == null || now < attempt.nextAttemptAt()) return;
        int attempts = attempt.attempts() + 1;
        if (!tryRestoreDormant(owner)) {
            RESTORE_ATTEMPTS.remove(owner.getUUID());
        } else if (attempts >= RESTORE_MAX_ATTEMPTS) {
            refundDormantMinions(owner);
            RESTORE_ATTEMPTS.remove(owner.getUUID());
        } else {
            RESTORE_ATTEMPTS.put(owner.getUUID(), new RestoreAttempt(attempts, now + RESTORE_RETRY_INTERVAL_TICKS));
        }
    }

    private static boolean tryRestoreDormant(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        if (server == null || !(owner.level() instanceof ServerLevel world)) return false;
        Path root = root(server);
        List<ScythePersistentStore.DormantMinion> snapshots = ScythePersistentStore.dormantMinions(root, owner.getUUID());
        if (snapshots.isEmpty()) return false;
        for (ScythePersistentStore.DormantMinion snapshot : snapshots) {
            if (snapshot.lifeTicks() >= ScytheBalance.Minion.LIFETIME_TICKS) {
                if (ScythePersistentStore.removeDormantMinion(root, owner.getUUID(), snapshot)) {
                    creditSoulRefund(owner, calculateSoulRefund(snapshot.health()));
                }
                continue;
            }
            WitheringMinionEntity minion = createMinion(owner, world);
            if (minion == null) continue;
            minion.restoreRuntimeState(snapshot.health(), snapshot.lifeTicks());
            if (!world.addFreshEntity(minion)) continue;
            if (ScythePersistentStore.removeDormantMinion(root, owner.getUUID(), snapshot)) {
                ScythePersistentStore.registerMinion(root, owner.getUUID(), minion.getUUID());
            } else {
                minion.discard();
            }
        }
        return !ScythePersistentStore.dormantMinions(root, owner.getUUID()).isEmpty();
    }

    private static void refundDormantMinions(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) return;
        for (ScythePersistentStore.DormantMinion snapshot : ScythePersistentStore.takeDormantMinions(root(server), owner.getUUID())) {
            creditSoulRefund(owner, calculateSoulRefund(snapshot.health()));
        }
    }

    public static void clearRuntime() {
        RESTORE_ATTEMPTS.clear();
    }

    public static void flushPendingRefunds(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) return;
        Path root = root(server);
        int pending = ScythePersistentStore.pendingSoulRefund(root, owner.getUUID());
        if (pending <= 0) return;
        ItemStack scythe = findSoulStorageScythe(owner);
        if (scythe.isEmpty()) return;
        int before = WitheringScytheItem.getSouls(scythe);
        WitheringScytheItem.addSouls(scythe, pending);
        int accepted = Math.max(0, WitheringScytheItem.getSouls(scythe) - before);
        if (accepted > 0) ScythePersistentStore.consumePendingSoulRefund(root, owner.getUUID(), accepted);
    }

    public static void onMinionDeath(WitheringMinionEntity minion) {
        UUID ownerId = minion.getOwnerUuid();
        if (!(minion.level() instanceof ServerLevel level) || ownerId == null) return;
        ScythePersistentStore.unregisterMinion(root(level.getServer()), ownerId, minion.getUUID());
    }

    public static void ensureRegistered(WitheringMinionEntity minion) {
        UUID ownerId = minion.getOwnerUuid();
        if (!(minion.level() instanceof ServerLevel level) || ownerId == null) return;
        ScythePersistentStore.registerMinion(root(level.getServer()), ownerId, minion.getUUID());
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
            if (stack == mainHand || stack == offHand || !isUsableWitheringScythe(stack, durabilityCost)) continue;
            damageScytheExact(stack, durabilityCost);
            return true;
        }
        return false;
    }

    public static List<WitheringMinionEntity> findMinions(ServerPlayer owner, ServerLevel world) {
        return findRegisteredMinions(owner);
    }

    private static List<WitheringMinionEntity> findRegisteredMinions(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        if (server == null) return List.of();
        Set<UUID> ids = ScythePersistentStore.activeMinions(root(server), owner.getUUID());
        List<WitheringMinionEntity> result = new ArrayList<>();
        for (UUID id : ids) {
            boolean resolved = false;
            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(id);
                if (entity == null) continue;
                resolved = true;
                if (entity instanceof WitheringMinionEntity minion && minion.isAlive() && minion.isOwner(owner)) {
                    result.add(minion);
                } else {
                    ScythePersistentStore.unregisterMinion(root(server), owner.getUUID(), id);
                }
                break;
            }
            // Missing entities may simply live in unloaded chunks, so keep those registry entries intact.
            if (!resolved) continue;
        }
        return result;
    }

    private static WitheringMinionEntity createMinion(ServerPlayer owner, ServerLevel world) {
        WitheringMinionEntity minion = new WitheringMinionEntity(ScytheMod.WITHERING_MINION, world);
        minion.initializeForOwner(owner);
        double baseYaw = Math.toRadians(owner.getYRot());
        double[] yOffsets = {0.0D, 1.0D, -1.0D};
        for (int step = 0; step < 8; step++) {
            double angle = baseYaw + step * Math.PI / 4.0D;
            double x = owner.getX() - Math.sin(angle) * ScytheBalance.Minion.SPAWN_DISTANCE;
            double z = owner.getZ() + Math.cos(angle) * ScytheBalance.Minion.SPAWN_DISTANCE;
            for (double yOffset : yOffsets) {
                minion.snapTo(x, owner.getY() + yOffset, z, owner.getYRot(), 0.0F);
                if (world.noCollision(minion)) return minion;
            }
        }
        return null;
    }

    private static void creditSoulRefund(ServerPlayer owner, int refund) {
        if (refund <= 0) return;
        MinecraftServer server = owner.level().getServer();
        if (server == null) return;
        Path root = root(server);
        ItemStack scythe = findSoulStorageScythe(owner);
        if (scythe.isEmpty()) {
            ScythePersistentStore.addPendingSoulRefund(root, owner.getUUID(), refund);
            return;
        }
        int before = WitheringScytheItem.getSouls(scythe);
        WitheringScytheItem.addSouls(scythe, refund);
        int accepted = Math.max(0, WitheringScytheItem.getSouls(scythe) - before);
        if (accepted < refund) ScythePersistentStore.addPendingSoulRefund(root, owner.getUUID(), refund - accepted);
    }

    private static Path root(MinecraftServer server) { return ScytheRuntimeState.worldRoot(server); }

    private static boolean isUsableWitheringScythe(ItemStack stack, int durabilityCost) {
        return !stack.isEmpty() && stack.getItem() instanceof WitheringScytheItem
                && WitheringScytheItem.hasEnoughDurability(stack, durabilityCost);
    }

    private static void damageScytheExact(ItemStack stack, int durabilityCost) {
        if (durabilityCost <= 0 || stack.isEmpty() || !stack.isDamageableItem()) return;
        int newDamage = stack.getDamageValue() + durabilityCost;
        if (newDamage >= stack.getMaxDamage()) stack.shrink(1);
        else stack.setDamageValue(newDamage);
    }

    private record RestoreAttempt(int attempts, long nextAttemptAt) {}

}
