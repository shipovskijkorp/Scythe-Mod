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
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
//? if >=1.21.11 {
import net.minecraft.item.consume.UseAction;
//? } else {
import net.minecraft.util.UseAction;
//? }
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
//? if >=1.21.11 {
import net.minecraft.util.ActionResult;
//? } else {
import net.minecraft.util.TypedActionResult;
//? }
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class FireScytheItem extends ScytheSwordItem {
    private record Lock(LivingEntity target, Hand hand) {}
    private static final Map<ServerPlayerEntity, Lock> LOCKS = new WeakHashMap<>();

    public FireScytheItem(Settings settings) { super(settings); }

//? if >=1.21.11 {
    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHit(stack, target, attacker);
        if (ScytheCombatUtil.isProtectedWitheringMinion(attacker, target)) return;
        if (attacker.getEntityWorld() instanceof ServerWorld) {
            target.setOnFireFor(ScytheBalance.Fire.PASSIVE_FIRE_SECONDS);
            if (attacker.getRandom().nextDouble() < ScytheBalance.Fire.PASSIVE_BURNS_CHANCE) BurnsUtil.tryApplyPassive(target);
            if (attacker instanceof ServerPlayerEntity player) ScytheAdvancementTracker.markFirePassive(player);
        }
    }
//? } else {
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (ScytheCombatUtil.isProtectedWitheringMinion(attacker, target)) return super.postHit(stack, target, attacker);
        if (!attacker.getWorld().isClient) {
            target.setOnFireFor(ScytheBalance.Fire.PASSIVE_FIRE_SECONDS);
            if (attacker.getRandom().nextDouble() < ScytheBalance.Fire.PASSIVE_BURNS_CHANCE) BurnsUtil.tryApplyPassive(target);
        }
        return super.postHit(stack, target, attacker);
    }
//? }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) { return 72000; }

    @Override
    public UseAction getUseAction(ItemStack stack) { return UseAction.BOW; }

//? if >=1.21.11 {
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        LivingEntity target = FireTargeting.findLockTarget(user);
        if (target == null) {
            if (user instanceof ServerPlayerEntity player) player.sendMessage(Text.translatable("message.scythes.fireball.no_target"), true);
            return ActionResult.FAIL;
        }
        if (user instanceof ServerPlayerEntity player) {
            int cooldown = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.FIREBALL);
            if (cooldown > 0) {
                player.sendMessage(Text.translatable("message.scythes.fireball.cooldown", Math.max(1, (cooldown + 19) / 20)), true);
                return ActionResult.FAIL;
            }
            if (!hasEnoughDurability(stack, ScytheBalance.Fire.FIREBALL_DURABILITY_COST)) {
                player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
                return ActionResult.FAIL;
            }
            LOCKS.put(player, new Lock(target, hand));
        }
        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        release(stack, world, user, remainingUseTicks);
        return true;
    }
//? } else {
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        LivingEntity target = FireTargeting.findLockTarget(user);
        if (target == null) {
            if (!world.isClient && user instanceof ServerPlayerEntity player) player.sendMessage(Text.translatable("message.scythes.fireball.no_target"), true);
            return TypedActionResult.fail(stack);
        }
        if (user instanceof ServerPlayerEntity player) {
            int cooldown = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.FIREBALL);
            if (cooldown > 0) {
                player.sendMessage(Text.translatable("message.scythes.fireball.cooldown", Math.max(1, (cooldown + 19) / 20)), true);
                return TypedActionResult.fail(stack);
            }
            if (!hasEnoughDurability(stack, ScytheBalance.Fire.FIREBALL_DURABILITY_COST)) {
                player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
                return TypedActionResult.fail(stack);
            }
            LOCKS.put(player, new Lock(target, hand));
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        release(stack, world, user, remainingUseTicks);
    }
//? }

    private void release(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof ServerPlayerEntity player) || !(world instanceof ServerWorld serverWorld)) return;
        Lock lock = LOCKS.remove(player);
        int used = getMaxUseTime(stack, user) - remainingUseTicks;
        if (used < ScytheBalance.Fire.FIREBALL_CHARGE_TICKS || lock == null) return;
        LivingEntity target = lock.target();
//? if >=1.21.11 {
        if (!FireTargeting.isValidTarget(player, target) || target.getEntityWorld() != world) {
//? } else {
        if (!FireTargeting.isValidTarget(player, target) || target.getWorld() != world) {
//? }
            player.sendMessage(Text.translatable("message.scythes.fireball.target_lost"), true);
            return;
        }
        FireballEntity fireball = new FireballEntity(world, player, target);
        fireball.refreshPositionAndAngles(player.getX(), player.getEyeY() - ScytheBalance.Fire.FIREBALL_SPAWN_EYE_OFFSET, player.getZ(), player.getYaw(), player.getPitch());
        fireball.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, ScytheBalance.Fire.FIREBALL_SPEED, 0.0F);
        serverWorld.spawnEntity(fireball);
        ScytheAdvancementTracker.markFireSpecial(player);
        stack.damage(ScytheBalance.Fire.FIREBALL_DURABILITY_COST, player, lock.hand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.FIREBALL, ScytheBalance.Fire.FIREBALL_COOLDOWN_TICKS);
        serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.75F);
    }

    public static Hand getHeldFireScytheHand(PlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof FireScytheItem) return Hand.MAIN_HAND;
        if (player.getOffHandStack().getItem() instanceof FireScytheItem) return Hand.OFF_HAND;
        return null;
    }
    public static boolean isHeld(PlayerEntity player) { return getHeldFireScytheHand(player) != null; }
    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        return cost <= 0 || (stack.isDamageable() && stack.getMaxDamage() - stack.getDamage() >= cost);
    }
}
