package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfig;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfigLoader;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import com.shipovskijkorp.scythes.mod.network.WitheringHudS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class WitheringScytheAbility {

    public static void tryActivate(ServerPlayerEntity player) {

        Hand hand = getHeldWitheringScytheHand(player);
        if (hand == null) return;

        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();

        ScytheModConfig config = ScytheModConfigLoader.getConfig();

        int cooldown = Math.max(0, config.witheringActiveCooldownTicks);
        if (cooldown > 0 && player.getItemCooldownManager().isCoolingDown(item)) return;

        double range = config.witheringActiveRadius;
        LivingEntity target = findTarget(player, range);
        if (target == null) {
            player.sendMessage(Text.translatable("message.scythes.withering.no_target"), true);
            return;
        }

        int cost = Math.max(0, config.scytheAbilityDurabilityCost);

        if (cost > 0) {
            int remaining = stack.getMaxDamage() - stack.getDamage();
            if (remaining < cost) {
                player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
                return;
            }
            stack.damage(cost, player, p -> p.sendToolBreakStatus(hand));
        }

        int debuffTicks = Math.max(1, config.witheringDebuffTicks);
        int slowAmp = Math.max(0, config.witheringDebuffSlownessAmplifier);
        int witherAmp = Math.max(0, config.witheringDebuffWitherAmplifier);

        target.addStatusEffect(new StatusEffectInstance(
                ScytheMod.NO_JUMP,
                debuffTicks,
                0,
                false,
                true
        ));

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, debuffTicks, slowAmp));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, debuffTicks, witherAmp));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, debuffTicks, 0));
        DamageAttributionTracker.recordWithering(target, player, debuffTicks);

        int ticks = WitheringScytheTracker.start(player);
        WitheringHudS2CPacket.sendTicks(player, ticks);

        if (cooldown > 0) {
            player.getItemCooldownManager().set(item, cooldown);
        }
    }

    public static void tick(ServerPlayerEntity player) {
        if (!WitheringScytheTracker.isActive(player)) return;

        if (getHeldWitheringScytheHand(player) == null) {
            WitheringScytheTracker.stop(player);
            return;
        }

        ScytheModConfig config = ScytheModConfigLoader.getConfig();
        int rate = Math.max(1, config.witheringActiveTickRate);
        if (player.age % rate != 0) return;

        double radius = config.witheringActiveRadius;
        Box box = player.getBoundingBox().expand(radius);

        List<LivingEntity> targets =
                player.getWorld().getEntitiesByClass(
                        LivingEntity.class,
                        box,
                        target -> ScytheTargeting.canHit(player, target)
                );

        double damage = Math.max(0.0, config.witheringActiveDamage);
        if (damage <= 0.0) return;

        for (LivingEntity target : targets) {
            target.damage(player.getDamageSources().indirectMagic(player, player), (float) damage);
        }
    }

    private static Hand getHeldWitheringScytheHand(ServerPlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof WitheringScytheItem) return Hand.MAIN_HAND;
        if (player.getOffHandStack().getItem() instanceof WitheringScytheItem) return Hand.OFF_HAND;
        return null;
    }

    private static LivingEntity findTarget(ServerPlayerEntity player, double range) {

        Vec3d start = player.getCameraPosVec(1.0f);
        Vec3d dir = player.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(range));

        Box box = player.getBoundingBox()
                .stretch(dir.multiply(range))
                .expand(1.0);

        EntityHitResult hit = ProjectileUtil.raycast(
                player,
                start,
                end,
                box,
                e -> e instanceof LivingEntity target && ScytheTargeting.canHit(player, target),
                range * range
        );

        if (hit == null) return null;

        Entity entity = hit.getEntity();
        return entity instanceof LivingEntity target ? target : null;
    }
}
