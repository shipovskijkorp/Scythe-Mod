package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.platform.HudSync;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class BloodHarvestTracker {

    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    /** Сброс активной способности (например при DISCONNECT). Без пакетов — просто чистка. */
    public static void clear(ServerPlayerEntity player) {
        ACTIVE.remove(player.getUuid());
    }

    /** Сколько тиков осталось (для HUD/синхры) */
    public static int getTicksLeft(ServerPlayerEntity player) {
        return ACTIVE.getOrDefault(player.getUuid(), 0);
    }

    public static int start(ServerPlayerEntity player) {
        ACTIVE.put(player.getUuid(), ScytheBalance.BloodHarvest.KILL_WINDOW_TICKS);
        return ScytheBalance.BloodHarvest.KILL_WINDOW_TICKS;
    }

    public static boolean isActive(ServerPlayerEntity player) {
        return ACTIVE.containsKey(player.getUuid());
    }

    public static void tick(ServerPlayerEntity player) {
        UUID id = player.getUuid();
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

            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ScytheBalance.BloodHarvest.FAILURE_DEBUFF_TICKS, ScytheBalance.BloodHarvest.FAILURE_SLOWNESS_AMPLIFIER));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, ScytheBalance.BloodHarvest.FAILURE_DEBUFF_TICKS, ScytheBalance.BloodHarvest.FAILURE_WEAKNESS_AMPLIFIER));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, ScytheBalance.BloodHarvest.FAILURE_DEBUFF_TICKS, ScytheBalance.BloodHarvest.FAILURE_BLINDNESS_AMPLIFIER));

            player.sendMessage(Text.translatable("message.scythes.blood_harvest.failed"), true);

            HudSync.stop(player, HudTransport.Timer.BLOOD_HARVEST);
        } else {
            ACTIVE.put(id, time);
        }
    }

    /** Вызови это при успешном килле */
    public static void onKill(ServerPlayerEntity player) {
        if (!isActive(player)) return;

        ACTIVE.remove(player.getUuid());

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, ScytheBalance.BloodHarvest.SUCCESS_BUFF_TICKS, ScytheBalance.BloodHarvest.SUCCESS_SPEED_AMPLIFIER, false, true, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, ScytheBalance.BloodHarvest.SUCCESS_BUFF_TICKS, ScytheBalance.BloodHarvest.SUCCESS_STRENGTH_AMPLIFIER, false, true, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, ScytheBalance.BloodHarvest.SUCCESS_BUFF_TICKS, ScytheBalance.BloodHarvest.SUCCESS_REGEN_AMPLIFIER, false, true, true));
        player.sendMessage(Text.translatable("message.scythes.blood_harvest.perfect"), true);

        HudSync.stop(player, HudTransport.Timer.BLOOD_HARVEST);
    }
}
