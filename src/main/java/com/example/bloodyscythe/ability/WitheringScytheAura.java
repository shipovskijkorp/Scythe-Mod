package com.example.bloodyscythe.ability;

import com.example.bloodyscythe.config.BloodyScytheConfig;
import com.example.bloodyscythe.config.BloodyScytheConfigLoader;
import com.example.bloodyscythe.item.WitheringScytheItem;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.List;

public class WitheringScytheAura {

    public static void apply(ServerPlayerEntity player) {

        boolean hasScytheInHand =
                player.getMainHandStack().getItem() instanceof WitheringScytheItem
                        || player.getOffHandStack().getItem() instanceof WitheringScytheItem;

        if (!hasScytheInHand) return;

        double radius = 5.0;
        int witherAdd = 40;
        int slowAdd = 40;
        int threshold = 20;

        BloodyScytheConfig config = BloodyScytheConfigLoader.getConfig();
        radius = config.witheringAuraRadius;
        witherAdd = Math.max(1, config.witheringAuraWitherTicks);
        slowAdd = Math.max(1, config.witheringAuraSlownessTicks);
        threshold = Math.max(1, config.witheringAuraRefreshThresholdTicks);

        Box box = player.getBoundingBox().expand(radius);
        List<ServerPlayerEntity> targets =
                player.getWorld().getEntitiesByClass(
                        ServerPlayerEntity.class,
                        box,
                        // ✅ иссушающая коса игнорит тиммейтов
                        p -> p != player && p.isAlive() && !p.isSpectator() && !player.isTeammate(p)
                );

        for (ServerPlayerEntity target : targets) {
            extendOrApply(target, StatusEffects.WITHER, witherAdd, 0, threshold);
            extendOrApply(target, StatusEffects.SLOWNESS, slowAdd, 0, threshold);
        }
    }

    private static void extendOrApply(ServerPlayerEntity target,
                                      net.minecraft.entity.effect.StatusEffect effect,
                                      int addTicks,
                                      int amplifier,
                                      int thresholdTicks) {

        StatusEffectInstance cur = target.getStatusEffect(effect);

        if (cur == null) {
            target.addStatusEffect(new StatusEffectInstance(effect, addTicks, amplifier));
            return;
        }

        if (cur.getDuration() <= thresholdTicks) {
            target.addStatusEffect(new StatusEffectInstance(
                    effect,
                    cur.getDuration() + addTicks,
                    Math.max(cur.getAmplifier(), amplifier)
            ));
        }
    }
}
