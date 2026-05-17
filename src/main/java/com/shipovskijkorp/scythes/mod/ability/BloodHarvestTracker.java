package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.config.ScytheModConfig;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfigLoader;
import com.shipovskijkorp.scythes.mod.network.BloodHarvestHudS2CPacket;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BloodHarvestTracker {

    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    /** Сброс активки (например при DISCONNECT). Без пакетов — просто чистка. */
    public static void clear(ServerPlayerEntity player) {
        ACTIVE.remove(player.getUuid());
    }

    /** Сколько тиков осталось (для HUD/синхры) */
    public static int getTicksLeft(ServerPlayerEntity player) {
        return ACTIVE.getOrDefault(player.getUuid(), 0);
    }

    public static int start(ServerPlayerEntity player) {
        int ticks = 20 * 10;
        if (ScytheModConfigLoader.CONFIG != null) {
            ticks = Math.max(1, ScytheModConfigLoader.CONFIG.bloodHarvestKillWindowTicks);
        }
        ACTIVE.put(player.getUuid(), ticks);
        return ticks;
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
            BloodHarvestHudS2CPacket.sendStop(player);
            return;
        }

        time--;
        if (time <= 0) {
            ACTIVE.remove(id);

            // ✅ провал окна: наказание владельцу берём из конфига
            int debuffTicks = 20 * 5;
            int slowAmp = 1;
            int weakAmp = 1;

            if (ScytheModConfigLoader.CONFIG != null) {
                debuffTicks = Math.max(1, ScytheModConfigLoader.CONFIG.bloodHarvestFailureDebuffTicks);
                slowAmp = Math.max(0, ScytheModConfigLoader.CONFIG.bloodHarvestFailureSlownessAmplifier);
                weakAmp = Math.max(0, ScytheModConfigLoader.CONFIG.bloodHarvestFailureWeaknessAmplifier);
            }

            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, debuffTicks, slowAmp));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, debuffTicks, weakAmp));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, debuffTicks, 0));

            player.sendMessage(Text.translatable("message.scythes.blood_harvest.failed"), true);

            BloodHarvestHudS2CPacket.sendStop(player);
        } else {
            ACTIVE.put(id, time);
        }
    }

    /** Вызови это при успешном килле */
    public static void onKill(ServerPlayerEntity player) {
        if (!isActive(player)) return;

        ACTIVE.remove(player.getUuid());

        ScytheModConfig config = ScytheModConfigLoader.getConfig();
        int buffTicks = Math.max(1, config.bloodHarvestSuccessBuffTicks);
        int speedAmp = Math.max(0, config.bloodHarvestSuccessSpeedAmplifier);
        int strengthAmp = Math.max(0, config.bloodHarvestSuccessStrengthAmplifier);
        int regenAmp = Math.max(0, config.bloodHarvestSuccessRegenAmplifier);

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, buffTicks, speedAmp, false, true, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, buffTicks, strengthAmp, false, true, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, buffTicks, regenAmp, false, true, true));
        player.sendMessage(Text.translatable("message.scythes.blood_harvest.perfect"), true);

        BloodHarvestHudS2CPacket.sendStop(player);
    }
}
