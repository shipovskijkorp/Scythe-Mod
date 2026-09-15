package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheCooldowns;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.entity.FireballEntity;
import com.shipovskijkorp.scythes.mod.util.BurnsUtil;
import com.shipovskijkorp.scythes.mod.util.FireTargeting;
import java.util.WeakHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

public final class FireScytheItem extends ScytheSwordItem {
    private record Lock(LivingEntity target, InteractionHand hand) {}
    private static final Map<ServerPlayer, Lock> LOCKS = new WeakHashMap<>();

    public FireScytheItem(Properties properties) { super(properties); }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (ScytheCombatUtil.isProtectedWitheringMinion(attacker, target)) return;
        if (!attacker.level().isClientSide()) {
            target.igniteForSeconds(ScytheBalance.Fire.PASSIVE_FIRE_SECONDS);
            if (attacker.getRandom().nextDouble() < ScytheBalance.Fire.PASSIVE_BURNS_CHANCE) BurnsUtil.tryApplyPassive(target);
            if (attacker instanceof ServerPlayer player) ScytheAdvancementTracker.markFirePassive(player);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) { return 72000; }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.BOW; }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        LivingEntity target = FireTargeting.findLockTarget(user);
        if (target == null) {
            if (user instanceof ServerPlayer player) player.sendOverlayMessage(Component.translatable("message.scythes.fireball.no_target"));
            return InteractionResult.FAIL;
        }
        if (user instanceof ServerPlayer player) {
            int cooldown = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.FIREBALL);
            if (cooldown > 0) {
                player.sendOverlayMessage(Component.translatable("message.scythes.fireball.cooldown", Math.max(1, (cooldown + 19) / 20)));
                return InteractionResult.FAIL;
            }
            if (!hasEnoughDurability(stack, ScytheBalance.Fire.FIREBALL_DURABILITY_COST)) {
                player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
                return InteractionResult.FAIL;
            }
            LOCKS.put(player, new Lock(target, hand));
        }
        user.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingTime) {
        if (!(user instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) return true;
        Lock lock = LOCKS.remove(player);
        int used = getUseDuration(stack, user) - remainingTime;
        if (used < ScytheBalance.Fire.FIREBALL_CHARGE_TICKS || lock == null) return true;
        LivingEntity target = lock.target();
        if (!FireTargeting.isValidTarget(player, target) || target.level() != level) {
            player.sendOverlayMessage(Component.translatable("message.scythes.fireball.target_lost"));
            return true;
        }
        FireballEntity fireball = new FireballEntity(level, player, target);
        fireball.snapTo(player.getX(), player.getEyeY() - ScytheBalance.Fire.FIREBALL_SPAWN_EYE_OFFSET, player.getZ(), player.getYRot(), player.getXRot());
        fireball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, ScytheBalance.Fire.FIREBALL_SPEED, 0.0F);
        level.addFreshEntity(fireball);
        ScytheAdvancementTracker.markFireSpecial(player);
        stack.hurtAndBreak(ScytheBalance.Fire.FIREBALL_DURABILITY_COST, player, lock.hand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.FIREBALL, ScytheBalance.Fire.FIREBALL_COOLDOWN_TICKS);
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.75F);
        return true;
    }

    public static InteractionHand getHeldFireScytheHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof FireScytheItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof FireScytheItem) return InteractionHand.OFF_HAND;
        return null;
    }
    public static boolean isHeld(Player player) { return getHeldFireScytheHand(player) != null; }
    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        return cost <= 0 || (stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() >= cost);
    }
}
