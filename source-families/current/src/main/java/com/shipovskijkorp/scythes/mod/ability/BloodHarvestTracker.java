package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.platform.HudSync;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class BloodHarvestTracker {

    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    /** Сброс активной способности (например при DISCONNECT). Без пакетов — просто чистка. */
    public static void clear(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
    }

    /** Сколько тиков осталось (для HUD/синхры) */
    public static int getTicksLeft(ServerPlayer player) {
        return ACTIVE.getOrDefault(player.getUUID(), 0);
    }

    public static int start(ServerPlayer player) {
        ACTIVE.put(player.getUUID(), ScytheBalance.BloodHarvest.KILL_WINDOW_TICKS);
        return ScytheBalance.BloodHarvest.KILL_WINDOW_TICKS;
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        Integer time = ACTIVE.get(id);
        if (time == null) return;

        // если владелец мёртв/спектатор — закрываем окно без "провала" (это не честно наказывать за смерть)
        if (!player.isAlive() || player.isSpectator()) {
            ACTIVE.remove(id);
            HudSync.stop(player, HudTransport.Timer.BLOOD_HARVEST);
            return;
        }

        time--;
        if (time <= 0) {
            ACTIVE.remove(id);

            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ScytheBalance.BloodHarvest.FAILURE_DEBUFF_TICKS, ScytheBalance.BloodHarvest.FAILURE_SLOWNESS_AMPLIFIER));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ScytheBalance.BloodHarvest.FAILURE_DEBUFF_TICKS, ScytheBalance.BloodHarvest.FAILURE_WEAKNESS_AMPLIFIER));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ScytheBalance.BloodHarvest.FAILURE_DEBUFF_TICKS, ScytheBalance.BloodHarvest.FAILURE_BLINDNESS_AMPLIFIER));

            player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.failed"));

            HudSync.stop(player, HudTransport.Timer.BLOOD_HARVEST);
        } else {
            ACTIVE.put(id, time);
        }
    }

    /** Вызови это при успешном килле */
    public static void onKill(ServerPlayer player) {
        if (!isActive(player)) return;

        ACTIVE.remove(player.getUUID());

        player.addEffect(new MobEffectInstance(MobEffects.SPEED, ScytheBalance.BloodHarvest.SUCCESS_BUFF_TICKS, ScytheBalance.BloodHarvest.SUCCESS_SPEED_AMPLIFIER, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ScytheBalance.BloodHarvest.SUCCESS_BUFF_TICKS, ScytheBalance.BloodHarvest.SUCCESS_STRENGTH_AMPLIFIER, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ScytheBalance.BloodHarvest.SUCCESS_BUFF_TICKS, ScytheBalance.BloodHarvest.SUCCESS_REGEN_AMPLIFIER, false, true, true));
        player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.perfect"));

        HudSync.stop(player, HudTransport.Timer.BLOOD_HARVEST);
    }
}
