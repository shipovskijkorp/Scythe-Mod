package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.platform.HudSync;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class BloodHarvestTracker {

    private static final Map<UUID, ActiveHarvest> ACTIVE = new HashMap<>();

    public static int getTicksLeft(ServerPlayerEntity player) {
        ActiveHarvest harvest = ACTIVE.get(player.getUuid());
        return harvest == null ? 0 : harvest.ticksLeft;
    }

    public static int start(ServerPlayerEntity player, Collection<UUID> markedTargets) {
        ACTIVE.put(player.getUuid(), new ActiveHarvest(
                ScytheBalance.BloodHarvest.KILL_WINDOW_TICKS,
                new HashSet<>(markedTargets)
        ));
        return ScytheBalance.BloodHarvest.KILL_WINDOW_TICKS;
    }

    public static boolean isActive(ServerPlayerEntity player) {
        return ACTIVE.containsKey(player.getUuid());
    }

    public static boolean isMarked(ServerPlayerEntity player, ServerPlayerEntity target) {
        ActiveHarvest harvest = ACTIVE.get(player.getUuid());
        return harvest != null && harvest.markedTargets.contains(target.getUuid());
    }

    public static void tick(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        ActiveHarvest harvest = ACTIVE.get(id);
        if (harvest == null) return;

        if (!player.isAlive() || player.isSpectator()) {
            ACTIVE.remove(id);
            HudSync.stop(player, HudTransport.Timer.BLOOD_HARVEST);
            return;
        }

        harvest.ticksLeft--;
        if (harvest.ticksLeft <= 0) {
            ACTIVE.remove(id);
            applyFailure(player);
            HudSync.stop(player, HudTransport.Timer.BLOOD_HARVEST);
        }
    }

    public static void onKill(ServerPlayerEntity player) {
        if (!isActive(player)) return;
        ACTIVE.remove(player.getUuid());

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, ScytheBalance.BloodHarvest.SUCCESS_BUFF_TICKS, ScytheBalance.BloodHarvest.SUCCESS_SPEED_AMPLIFIER, false, true, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, ScytheBalance.BloodHarvest.SUCCESS_BUFF_TICKS, ScytheBalance.BloodHarvest.SUCCESS_STRENGTH_AMPLIFIER, false, true, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, ScytheBalance.BloodHarvest.SUCCESS_BUFF_TICKS, ScytheBalance.BloodHarvest.SUCCESS_REGEN_AMPLIFIER, false, true, true));
        player.sendMessage(Text.translatable("message.scythes.blood_harvest.perfect"), true);
        HudSync.stop(player, HudTransport.Timer.BLOOD_HARVEST);
    }

    /** Logging out during the kill window is a failed Harvest, applied on the next login. */
    public static void disconnect(ServerPlayerEntity player) {
        if (ACTIVE.remove(player.getUuid()) == null) return;
//? if >=1.21.11 {
        MinecraftServer server = player.getEntityWorld() instanceof ServerWorld world ? world.getServer() : null;
//? } else {
        MinecraftServer server = player.getServerWorld().getServer();
//? }
        if (server != null) {
            ScythePersistentStore.queueBloodFailure(ScytheRuntimeState.worldRoot(server), player.getUuid());
        }
        HudSync.stop(player, HudTransport.Timer.BLOOD_HARVEST);
    }

    public static void connect(ServerPlayerEntity player) {
//? if >=1.21.11 {
        MinecraftServer server = player.getEntityWorld() instanceof ServerWorld world ? world.getServer() : null;
//? } else {
        MinecraftServer server = player.getServerWorld().getServer();
//? }
        if (server == null) return;
        if (ScythePersistentStore.consumeBloodFailure(ScytheRuntimeState.worldRoot(server), player.getUuid())) {
            applyFailure(player);
        }
    }

    private static void applyFailure(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ScytheBalance.BloodHarvest.FAILURE_DEBUFF_TICKS, ScytheBalance.BloodHarvest.FAILURE_SLOWNESS_AMPLIFIER));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, ScytheBalance.BloodHarvest.FAILURE_DEBUFF_TICKS, ScytheBalance.BloodHarvest.FAILURE_WEAKNESS_AMPLIFIER));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, ScytheBalance.BloodHarvest.FAILURE_DEBUFF_TICKS, ScytheBalance.BloodHarvest.FAILURE_BLINDNESS_AMPLIFIER));
        player.sendMessage(Text.translatable("message.scythes.blood_harvest.failed"), true);
    }

    private static final class ActiveHarvest {
        private int ticksLeft;
        private final Set<UUID> markedTargets;

        private ActiveHarvest(int ticksLeft, Set<UUID> markedTargets) {
            this.ticksLeft = ticksLeft;
            this.markedTargets = markedTargets;
        }
    }
}
