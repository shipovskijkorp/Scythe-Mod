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
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class BloodHarvestTracker {

    private static final Map<UUID, ActiveHarvest> ACTIVE = new HashMap<>();

    public static int getTicksLeft(ServerPlayer player) {
        ActiveHarvest harvest = ACTIVE.get(player.getUUID());
        return harvest == null ? 0 : harvest.ticksLeft;
    }

    public static int start(ServerPlayer player, Collection<UUID> markedTargets) {
        ACTIVE.put(player.getUUID(), new ActiveHarvest(
                ScytheBalance.BloodHarvest.KILL_WINDOW_TICKS,
                new HashSet<>(markedTargets)
        ));
        return ScytheBalance.BloodHarvest.KILL_WINDOW_TICKS;
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static boolean isMarked(ServerPlayer player, ServerPlayer target) {
        ActiveHarvest harvest = ACTIVE.get(player.getUUID());
        return harvest != null && harvest.markedTargets.contains(target.getUUID());
    }

    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
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

    public static void onKill(ServerPlayer player) {
        if (!isActive(player)) return;
        ACTIVE.remove(player.getUUID());

        player.addEffect(new MobEffectInstance(MobEffects.SPEED, ScytheBalance.BloodHarvest.SUCCESS_BUFF_TICKS, ScytheBalance.BloodHarvest.SUCCESS_SPEED_AMPLIFIER, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ScytheBalance.BloodHarvest.SUCCESS_BUFF_TICKS, ScytheBalance.BloodHarvest.SUCCESS_STRENGTH_AMPLIFIER, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ScytheBalance.BloodHarvest.SUCCESS_BUFF_TICKS, ScytheBalance.BloodHarvest.SUCCESS_REGEN_AMPLIFIER, false, true, true));
        player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.perfect"));
        HudSync.stop(player, HudTransport.Timer.BLOOD_HARVEST);
    }

    public static void disconnect(ServerPlayer player) {
        if (ACTIVE.remove(player.getUUID()) == null) return;
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            ScythePersistentStore.queueBloodFailure(ScytheRuntimeState.worldRoot(server), player.getUUID());
        }
        HudSync.stop(player, HudTransport.Timer.BLOOD_HARVEST);
    }

    public static void connect(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        if (ScythePersistentStore.consumeBloodFailure(ScytheRuntimeState.worldRoot(server), player.getUUID())) {
            applyFailure(player);
        }
    }

    private static void applyFailure(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ScytheBalance.BloodHarvest.FAILURE_DEBUFF_TICKS, ScytheBalance.BloodHarvest.FAILURE_SLOWNESS_AMPLIFIER));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ScytheBalance.BloodHarvest.FAILURE_DEBUFF_TICKS, ScytheBalance.BloodHarvest.FAILURE_WEAKNESS_AMPLIFIER));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ScytheBalance.BloodHarvest.FAILURE_DEBUFF_TICKS, ScytheBalance.BloodHarvest.FAILURE_BLINDNESS_AMPLIFIER));
        player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.failed"));
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
