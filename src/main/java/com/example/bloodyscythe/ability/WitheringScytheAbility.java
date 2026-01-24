package com.example.bloodyscythe.ability;

import com.example.bloodyscythe.BleedingMod;
import com.example.bloodyscythe.config.BloodyScytheConfig;
import com.example.bloodyscythe.config.BloodyScytheConfigLoader;
import com.example.bloodyscythe.item.WitheringScytheItem;
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

        BloodyScytheConfig config = BloodyScytheConfigLoader.getConfig();

        int cooldown = Math.max(0, config.witheringActiveCooldownTicks);
        if (cooldown > 0 && player.getItemCooldownManager().isCoolingDown(item)) return;

        // ✅ Цена активации (общая для всех кос): берём из bloodHarvestDurabilityCost
        int cost = Math.max(0, config.bloodHarvestDurabilityCost);

        if (cost > 0) {
            int remaining = stack.getMaxDamage() - stack.getDamage();
            if (remaining < cost) {
                player.sendMessage(Text.translatable("message.bloodyscythe.scythe_ability.no_durability"), true);
                return;
            }
            stack.damage(cost, player, p -> p.sendToolBreakStatus(hand));
        }

        double range = config.witheringActiveRadius;
        ServerPlayerEntity target = findTargetPlayer(player, range);

        if (target != null) {
            int debuffTicks = Math.max(1, config.witheringDebuffTicks);
            int slowAmp = Math.max(0, config.witheringDebuffSlownessAmplifier);
            int witherAmp = Math.max(0, config.witheringDebuffWitherAmplifier);

            target.addStatusEffect(new StatusEffectInstance(
                    BleedingMod.NO_JUMP,
                    debuffTicks,
                    0,
                    false,
                    true
            ));

            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, debuffTicks, slowAmp));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, debuffTicks, witherAmp));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, debuffTicks, 0));
        }

        WitheringScytheTracker.start(player);

        if (cooldown > 0) {
            player.getItemCooldownManager().set(item, cooldown);
        }
    }

    public static void tick(ServerPlayerEntity player) {
        if (!WitheringScytheTracker.isActive(player)) return;

        BloodyScytheConfig config = BloodyScytheConfigLoader.getConfig();
        int rate = Math.max(1, config.witheringActiveTickRate);
        if (player.age % rate != 0) return;

        double radius = config.witheringActiveRadius;
        Box box = player.getBoundingBox().expand(radius);

        List<ServerPlayerEntity> targets =
                player.getWorld().getEntitiesByClass(
                        ServerPlayerEntity.class,
                        box,
                        p -> isEnemyPlayer(player, p)
                );

        double damage = Math.max(0.0, config.witheringActiveDamage);
        if (damage <= 0.0) return;

        for (ServerPlayerEntity target : targets) {
            target.damage(player.getDamageSources().indirectMagic(player, player), (float) damage);
        }
    }

    private static Hand getHeldWitheringScytheHand(ServerPlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof WitheringScytheItem) return Hand.MAIN_HAND;
        if (player.getOffHandStack().getItem() instanceof WitheringScytheItem) return Hand.OFF_HAND;
        return null;
    }

    private static boolean isEnemyPlayer(ServerPlayerEntity owner, ServerPlayerEntity other) {
        if (other == owner) return false;
        if (!other.isAlive() || other.isSpectator()) return false;
        return !owner.isTeammate(other);
    }

    private static ServerPlayerEntity findTargetPlayer(ServerPlayerEntity player, double range) {

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
                e -> e instanceof ServerPlayerEntity sp && isEnemyPlayer(player, sp),
                range * range
        );

        if (hit == null) return null;
        return (ServerPlayerEntity) hit.getEntity();
    }
}
