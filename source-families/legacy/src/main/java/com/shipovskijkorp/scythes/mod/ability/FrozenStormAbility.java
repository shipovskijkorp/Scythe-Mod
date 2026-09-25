package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.FrozenScytheItem;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

/** Active ability of the Frozen Scythe. */
public final class FrozenStormAbility {

    private FrozenStormAbility() {
    }

    public static void tryActivate(ServerPlayerEntity player) {
        Hand hand = FrozenScytheItem.getHeldFrozenScytheHand(player);
        if (hand == null) {
            player.sendMessage(Text.translatable("message.scythes.frozen_storm.no_scythe"), true);
            return;
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.STORM);
        if (cooldownLeft > 0) {
            player.sendMessage(
                    Text.translatable("message.scythes.frozen_storm.cooldown", Math.max(1, (cooldownLeft + 19) / 20)),
                    true
            );
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (!FrozenScytheItem.hasEnoughDurability(stack, ScytheBalance.FrozenStorm.DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return;
        }

        Box searchBox = player.getBoundingBox().expand(ScytheBalance.FrozenStorm.RADIUS);
        List<LivingEntity> targets = player.getWorld().getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                target -> ScytheCombatUtil.isValidCombatTargetWithin(player, target, ScytheBalance.FrozenStorm.RADIUS)
        );

        if (targets.isEmpty()) {
            player.sendMessage(Text.translatable("message.scythes.frozen_storm.no_targets"), true);
            return;
        }

        for (LivingEntity target : targets) {
            target.addStatusEffect(new StatusEffectInstance(
                    ScytheMod.FREEZING,
                    ScytheBalance.FrozenStorm.FREEZING_TICKS,
                    ScytheBalance.FrozenStorm.FREEZING_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            DamageAttributionTracker.recordFreezing(target, player, ScytheBalance.FrozenStorm.FREEZING_TICKS);
            target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS,
                    ScytheBalance.FrozenStorm.SLOWNESS_TICKS,
                    ScytheBalance.FrozenStorm.SLOWNESS_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.WEAKNESS,
                    ScytheBalance.FrozenStorm.WEAKNESS_TICKS,
                    ScytheBalance.FrozenStorm.WEAKNESS_AMPLIFIER,
                    false,
                    true,
                    true
            ));
        }

        stack.damage(ScytheBalance.FrozenStorm.DURABILITY_COST, player, p -> p.sendToolBreakStatus(hand));
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.STORM, ScytheBalance.FrozenStorm.COOLDOWN_TICKS);
        ScytheAdvancementTracker.markFrozenActive(player);

        player.getWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                SoundCategory.PLAYERS,
                1.25F,
                0.65F
        );
        player.sendMessage(Text.translatable("message.scythes.frozen_storm.success", targets.size()), true);
    }


}
