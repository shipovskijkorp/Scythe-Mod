package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.ability.GoldenLootMarkTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheCooldowns;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GoldenScytheItem extends ScytheSwordItem {

    public GoldenScytheItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (!(world instanceof ServerLevel serverWorld)) {
            return InteractionResult.SUCCESS;
        }

        if (!(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.MIDAS);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.midas_touch.cooldown", Math.max(1, cooldownLeft / 20)));
            return InteractionResult.FAIL;
        }

        if (!hasEnoughDurability(stack, ScytheBalance.Golden.MIDAS_DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return InteractionResult.FAIL;
        }

        LivingEntity target = findMidasTarget(player);
        if (target == null) {
            player.sendOverlayMessage(Component.translatable("message.scythes.midas_touch.no_target"));
            return InteractionResult.FAIL;
        }

        GoldenLootMarkTracker.mark(target, player);

        float damage = Mth.clamp(
                target.getMaxHealth() * ScytheBalance.Golden.MIDAS_DAMAGE_FRACTION,
                ScytheBalance.Golden.MIDAS_MIN_DAMAGE,
                ScytheBalance.Golden.MIDAS_MAX_DAMAGE
        );
        target.hurtServer(serverWorld, ScytheDamageTypes.midasTouch(serverWorld, player), damage);

        stack.hurtAndBreak(ScytheBalance.Golden.MIDAS_DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.MIDAS, ScytheBalance.Golden.MIDAS_COOLDOWN_TICKS);
        ScytheAdvancementTracker.markGoldenSpecial(player);

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.9F,
                0.7F + player.getRandom().nextFloat() * 0.25F
        );

        player.sendOverlayMessage(
                target.isAlive()
                        ? Component.translatable("message.scythes.midas_touch.marked")
                        : Component.translatable("message.scythes.midas_touch.killed")
        );

        return InteractionResult.SUCCESS;
    }

    private static LivingEntity findMidasTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(ScytheBalance.Golden.MIDAS_REACH));
        AABB searchBox = player.getBoundingBox().expandTowards(direction.scale(ScytheBalance.Golden.MIDAS_REACH)).inflate(ScytheBalance.Golden.MIDAS_SEARCH_PADDING);

        return player.level().getEntitiesOfClass(
                        LivingEntity.class,
                        searchBox,
                        target -> isValidGoldenTarget(player, target)
                )
                .stream()
                .map(target -> new MidasCandidate(target, getHitDistance(start, end, target)))
                .filter(candidate -> candidate.hitDistance().isPresent())
                .min(Comparator.comparingDouble(candidate -> candidate.hitDistance().orElse(Double.MAX_VALUE)))
                .map(MidasCandidate::target)
                .orElse(null);
    }

    private static Optional<Double> getHitDistance(Vec3 start, Vec3 end, LivingEntity target) {
        AABB box = target.getBoundingBox().inflate(ScytheBalance.Golden.MIDAS_HITBOX_PADDING);
        return box.clip(start, end).map(hit -> hit.distanceToSqr(start));
    }

    public static boolean isValidGoldenTarget(ServerPlayer player, LivingEntity target) {
        if (ScytheCombatUtil.isProtectedWitheringMinion(player, target)) return false;
        if (target == player) return false;
        if (!target.isAlive()) return false;
        if (target.isSpectator()) return false;
        if (player.isAlliedTo(target)) return false;
        if (target instanceof TamableAnimal tameable && tameable.isTame()) return false;
        if (target instanceof AbstractHorse horse && horse.isTamed()) return false;
//? if >=26.2 {
        // Golden Scythe is loot-focused: other peaceful mobs and villagers remain valid targets.
//? } else {
//? }
        return true;
    }

    public static boolean hasGoldenScythe(Player player) {
        return player.getMainHandItem().getItem() instanceof GoldenScytheItem
                || player.getOffhandItem().getItem() instanceof GoldenScytheItem;
    }

    public static InteractionHand getHeldGoldenScytheHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof GoldenScytheItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof GoldenScytheItem) return InteractionHand.OFF_HAND;
        return null;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageableItem()) return false;
        return stack.getMaxDamage() - stack.getDamageValue() >= cost;
    }

    private record MidasCandidate(LivingEntity target, Optional<Double> hitDistance) {
    }
}
