package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.network.BloodHarvestHudS2CPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BloodHarvestTracker {

    public static final int KILL_WINDOW_TICKS = 20 * 20;

    public static final int SUCCESS_BUFF_TICKS = 20 * 16;
    public static final int SUCCESS_SPEED_AMPLIFIER = 1;
    public static final int SUCCESS_STRENGTH_AMPLIFIER = 1;
    public static final int SUCCESS_REGEN_AMPLIFIER = 1;

    public static final int FAILURE_DEBUFF_TICKS = 150;
    public static final int FAILURE_SLOWNESS_AMPLIFIER = 1;
    public static final int FAILURE_WEAKNESS_AMPLIFIER = 1;

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
        ACTIVE.put(player.getUUID(), KILL_WINDOW_TICKS);
        return KILL_WINDOW_TICKS;
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
            BloodHarvestHudS2CPacket.sendStop(player);
            return;
        }

        time--;
        if (time <= 0) {
            ACTIVE.remove(id);

            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FAILURE_DEBUFF_TICKS, FAILURE_SLOWNESS_AMPLIFIER));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FAILURE_DEBUFF_TICKS, FAILURE_WEAKNESS_AMPLIFIER));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, FAILURE_DEBUFF_TICKS, 0));

            player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.failed"));

            BloodHarvestHudS2CPacket.sendStop(player);
        } else {
            ACTIVE.put(id, time);
        }
    }

    /** Вызови это при успешном килле */
    public static void onKill(ServerPlayer player) {
        if (!isActive(player)) return;

        ACTIVE.remove(player.getUUID());

        player.addEffect(new MobEffectInstance(MobEffects.SPEED, SUCCESS_BUFF_TICKS, SUCCESS_SPEED_AMPLIFIER, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, SUCCESS_BUFF_TICKS, SUCCESS_STRENGTH_AMPLIFIER, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, SUCCESS_BUFF_TICKS, SUCCESS_REGEN_AMPLIFIER, false, true, true));
        player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.perfect"));

        BloodHarvestHudS2CPacket.sendStop(player);
    }
}
