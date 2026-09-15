package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.WitheringMinionManager;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class WitheringScytheItem extends ScytheSwordItem {

    private static final String SOULS_KEY = "Souls";

    public WitheringScytheItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient && attacker.getRandom().nextDouble() < ScytheBalance.Withering.WITHER_CHANCE) {
            target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    StatusEffects.WITHER,
                    ScytheBalance.Withering.WITHER_TICKS,
                    ScytheBalance.Withering.WITHER_AMPLIFIER,
                    false,
                    true,
                    true
            ));

            if (attacker instanceof ServerPlayerEntity player) {
                ScytheAdvancementTracker.markWitheringPassive(player);
                DamageAttributionTracker.recordWithering(target, player, ScytheBalance.Withering.WITHER_TICKS);
            }
        }

        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }

        if (player.isSneaking()) {
            int dismissed = WitheringMinionManager.dismissMinions(player);
            player.sendMessage(Text.translatable("message.scythes.withering_minion.dismissed", dismissed), true);
            return TypedActionResult.success(stack);
        }

        int maxMinions = getMaxMinions(stack);
        int ownedMinions = WitheringMinionManager.countMinions(player);
        if (ownedMinions >= maxMinions) {
            player.sendMessage(Text.translatable("message.scythes.withering_minion.cap", maxMinions), true);
            return TypedActionResult.fail(stack);
        }

        if (getSouls(stack) < ScytheBalance.Withering.MINION_SOUL_COST) {
            player.sendMessage(Text.translatable("message.scythes.withering_minion.no_souls", ScytheBalance.Withering.MINION_SOUL_COST), true);
            return TypedActionResult.fail(stack);
        }

        if (!hasEnoughDurability(stack, ScytheBalance.Withering.MINION_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return TypedActionResult.fail(stack);
        }

        if (!WitheringMinionManager.spawnMinion(player)) {
            player.sendMessage(Text.translatable("message.scythes.withering_minion.no_space"), true);
            return TypedActionResult.fail(stack);
        }

        spendSouls(stack, ScytheBalance.Withering.MINION_SOUL_COST);
        stack.damage(ScytheBalance.Withering.MINION_DURABILITY_COST, player, p -> p.sendToolBreakStatus(hand));
        ScytheAdvancementTracker.markWitheringSpecial(player);
        ScytheAdvancementTracker.tryGrantSuperNecromancer(player, WitheringMinionManager.countMinions(player));
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT, SoundCategory.PLAYERS, 0.8F, 0.75F);
        player.sendMessage(Text.translatable("message.scythes.withering_minion.spawned", getSouls(stack)), true);
        return TypedActionResult.success(stack);
    }

    public static int getMaxMinions(ItemStack stack) {
        return ScytheBalance.Withering.MAX_MINIONS + getAdditionalSlotBonus(stack);
    }

    public static int getAdditionalSlotBonus(ItemStack stack) {
        int level = EnchantmentHelper.getLevel(ScytheMod.ADDITIONAL_SLOT, stack);
        return switch (level) {
            case 1 -> ScytheBalance.Withering.ADDITIONAL_SLOT_LEVEL_ONE;
            case 2 -> ScytheBalance.Withering.ADDITIONAL_SLOT_LEVEL_TWO;
            case 3 -> ScytheBalance.Withering.ADDITIONAL_SLOT_LEVEL_THREE;
            default -> 0;
        };
    }

    public static int getSouls(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0 : Math.max(0, nbt.getInt(SOULS_KEY));
    }

    public static void addSouls(ItemStack stack, int amount) {
        if (stack.isEmpty() || !(stack.getItem() instanceof WitheringScytheItem) || amount <= 0) return;
        stack.getOrCreateNbt().putInt(SOULS_KEY, getSouls(stack) + amount);
    }

    public static boolean spendSouls(ItemStack stack, int amount) {
        if (amount <= 0) return true;
        int current = getSouls(stack);
        if (current < amount) return false;
        stack.getOrCreateNbt().putInt(SOULS_KEY, current - amount);
        return true;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageable()) return false;
        return stack.getMaxDamage() - stack.getDamage() >= cost;
    }
}
