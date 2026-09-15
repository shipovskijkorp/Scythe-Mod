package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheCooldowns;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.entity.IceSpikeEntity;
import net.minecraft.enchantment.FrostWalkerEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class FrozenScytheItem extends ScytheSwordItem {

    public FrozenScytheItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (world.isClient || !(entity instanceof PlayerEntity player)) return;
        if (!isTheOnlyHeldStackToTick(player, stack)) return;

        FrostWalkerEnchantment.freezeWater(
                player,
                world,
                player.getBlockPos(),
                ScytheBalance.Frozen.COLD_MASTER_FROST_WALKER_LEVEL
        );
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (ScytheCombatUtil.isProtectedWitheringMinion(attacker, target)) return super.postHit(stack, target, attacker);
        boolean result = super.postHit(stack, target, attacker);

        if (!target.getWorld().isClient
                && target.isAlive()
                && attacker.getRandom().nextDouble() < ScytheBalance.Frozen.COLD_MASTER_FREEZING_CHANCE) {
            target.addStatusEffect(new StatusEffectInstance(
                    ScytheMod.FREEZING,
                    ScytheBalance.Frozen.COLD_MASTER_FREEZING_TICKS,
                    ScytheBalance.Frozen.COLD_MASTER_FREEZING_AMPLIFIER
            ));
            if (attacker instanceof ServerPlayerEntity player) {
                DamageAttributionTracker.recordFreezing(target, player, ScytheBalance.Frozen.COLD_MASTER_FREEZING_TICKS);
                ScytheAdvancementTracker.markFrozenPassive(player);
            }
        }

        return result;
    }

    public static boolean isInPlayerInventory(PlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (player.getInventory().getStack(slot).getItem() instanceof FrozenScytheItem) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static Hand getHeldFrozenScytheHand(PlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof FrozenScytheItem) {
            return Hand.MAIN_HAND;
        }
        if (player.getOffHandStack().getItem() instanceof FrozenScytheItem) {
            return Hand.OFF_HAND;
        }
        return null;
    }

    private static boolean isTheOnlyHeldStackToTick(PlayerEntity player, ItemStack stack) {
        if (player.getMainHandStack() == stack) return true;
        return !(player.getMainHandStack().getItem() instanceof FrozenScytheItem)
                && player.getOffHandStack() == stack;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.ICE_SPIKE);
        if (cooldownLeft > 0) {
            player.sendMessage(
                    Text.translatable("message.scythes.ice_spike.cooldown", Math.max(1, (cooldownLeft + 19) / 20)),
                    true
            );
            return TypedActionResult.fail(stack);
        }

        if (!hasEnoughDurability(stack, ScytheBalance.Frozen.ICE_SPIKE_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return TypedActionResult.fail(stack);
        }

        IceSpikeEntity spike = new IceSpikeEntity(world, player);
        spike.setPosition(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        spike.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, ScytheBalance.Frozen.ICE_SPIKE_SPEED, 0.0F);
        world.spawnEntity(spike);

        stack.damage(ScytheBalance.Frozen.ICE_SPIKE_DURABILITY_COST, player, p -> p.sendToolBreakStatus(hand));
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.ICE_SPIKE, ScytheBalance.Frozen.ICE_SPIKE_COOLDOWN_TICKS);

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_ARROW_SHOOT,
                SoundCategory.PLAYERS,
                1.0F,
                0.75F + player.getRandom().nextFloat() * 0.15F
        );

        return TypedActionResult.success(stack);
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageable()) return false;
        return stack.getMaxDamage() - stack.getDamage() >= cost;
    }
}
