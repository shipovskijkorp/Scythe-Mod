package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ability.GoldenLootMarkTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheCooldowns;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GoldenScytheItem extends ScytheSwordItem {

    public GoldenScytheItem(Settings settings) {
        super(settings);
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

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.MIDAS);
        if (cooldownLeft > 0) {
            player.sendMessage(Text.translatable("message.scythes.midas_touch.cooldown", Math.max(1, cooldownLeft / 20)), true);
            return TypedActionResult.fail(stack);
        }

        if (!hasEnoughDurability(stack, ScytheBalance.Golden.MIDAS_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return TypedActionResult.fail(stack);
        }

        LivingEntity target = findMidasTarget(player);
        if (target == null) {
            player.sendMessage(Text.translatable("message.scythes.midas_touch.no_target"), true);
            return TypedActionResult.fail(stack);
        }

        GoldenLootMarkTracker.mark(target, player);

        float damage = MathHelper.clamp(
                target.getMaxHealth() * ScytheBalance.Golden.MIDAS_DAMAGE_FRACTION,
                ScytheBalance.Golden.MIDAS_MIN_DAMAGE,
                ScytheBalance.Golden.MIDAS_MAX_DAMAGE
        );
        target.damage(ScytheDamageTypes.midasTouch(world, player), damage);

        stack.damage(ScytheBalance.Golden.MIDAS_DURABILITY_COST, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.MIDAS, ScytheBalance.Golden.MIDAS_COOLDOWN_TICKS);
        ScytheAdvancementTracker.markGoldenSpecial(player);

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS,
                0.9F,
                0.7F + player.getRandom().nextFloat() * 0.25F
        );

        player.sendMessage(
                target.isAlive()
                        ? Text.translatable("message.scythes.midas_touch.marked")
                        : Text.translatable("message.scythes.midas_touch.killed"),
                true
        );

        return TypedActionResult.success(stack);
    }

    @Nullable
    private static LivingEntity findMidasTarget(ServerPlayerEntity player) {
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(ScytheBalance.Golden.MIDAS_REACH));
        Box searchBox = player.getBoundingBox().stretch(direction.multiply(ScytheBalance.Golden.MIDAS_REACH)).expand(ScytheBalance.Golden.MIDAS_SEARCH_PADDING);

        EntityHitResult hitResult = ProjectileUtil.raycast(
                player,
                start,
                end,
                searchBox,
                entity -> entity instanceof LivingEntity living && isValidGoldenTarget(player, living),
                ScytheBalance.Golden.MIDAS_REACH * ScytheBalance.Golden.MIDAS_REACH
        );

        Entity entity = hitResult == null ? null : hitResult.getEntity();
        return entity instanceof LivingEntity living ? living : null;
    }

    public static boolean isValidGoldenTarget(ServerPlayerEntity player, LivingEntity target) {
        if (target == player) return false;
        if (!target.isAlive()) return false;
        if (target.isSpectator()) return false;
        if (player.isTeammate(target)) return false;
        if (target instanceof TameableEntity tameable && tameable.isTamed()) return false;
        if (target instanceof AbstractHorseEntity horse && horse.isTame()) return false;
        return true;
    }

    public static boolean hasGoldenScythe(PlayerEntity player) {
        return getHeldGoldenScytheHand(player) != null;
    }

    @Nullable
    public static Hand getHeldGoldenScytheHand(PlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof GoldenScytheItem) {
            return Hand.MAIN_HAND;
        }
        if (player.getOffHandStack().getItem() instanceof GoldenScytheItem) {
            return Hand.OFF_HAND;
        }
        return null;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageable()) return false;
        return stack.getMaxDamage() - stack.getDamage() >= cost;
    }
}
