package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.config.ScytheModConfig;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfigLoader;
import com.shipovskijkorp.scythes.mod.item.PlagueScytheItem;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.List;

public class PlagueScytheAura {

    public static void apply(ServerPlayerEntity player) {

        boolean hasScytheInHand =
                player.getMainHandStack().getItem() instanceof PlagueScytheItem
                        || player.getOffHandStack().getItem() instanceof PlagueScytheItem;

        if (!hasScytheInHand) return;

        double radius = 6.0;
        int addTicks = 60;
        int thresholdTicks = 20;

        ScytheModConfig config = ScytheModConfigLoader.getConfig();
        radius = config.plagueAuraRadius;
        addTicks = Math.max(1, config.plagueAuraEffectTicks);
        thresholdTicks = Math.max(1, config.plagueAuraRefreshThresholdTicks);

        float maxHealth = player.getMaxHealth();
        float healthRatio = maxHealth <= 0.0f ? 0.0f : (player.getHealth() / maxHealth);
        int amplifier = healthRatio <= 0.25f ? 2 : healthRatio <= 0.5f ? 1 : 0;

        Box box = player.getBoundingBox().expand(radius);

        List<ServerPlayerEntity> targets = player.getWorld().getEntitiesByClass(
                ServerPlayerEntity.class,
                box,
                p -> p != player && p.isAlive() && !p.isSpectator() && !player.isTeammate(p)
        );

        for (ServerPlayerEntity target : targets) {

            StatusEffectInstance current = target.getStatusEffect(StatusEffects.POISON);

            if (current == null) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, addTicks, amplifier, false, true));
                continue;
            }

            if (current.getDuration() <= thresholdTicks) {
                int newDuration = current.getDuration() + addTicks;
                int newAmplifier = Math.max(current.getAmplifier(), amplifier);

                target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, newDuration, newAmplifier, false, true));
            }
        }
    }
}
