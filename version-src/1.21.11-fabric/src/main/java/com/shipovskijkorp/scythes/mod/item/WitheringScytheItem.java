package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.WitheringMinionManager;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class WitheringScytheItem extends ScytheSwordItem {

    private static final String SOULS_KEY = "Souls";

    public WitheringScytheItem(Settings settings) {
        super(settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHit(stack, target, attacker);
        if (ScytheCombatUtil.isProtectedWitheringMinion(attacker, target)) return;

        if (attacker.getEntityWorld().isClient()) {
            return;
        }

        if (attacker.getRandom().nextDouble() < ScytheBalance.Withering.WITHER_CHANCE) {
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
    }

    @Override
    public ActionResult use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!(world instanceof ServerWorld)) {
            return ActionResult.SUCCESS;
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        if (player.isSneaking()) {
            int soulsBefore = getSouls(stack);
            int dismissed = WitheringMinionManager.dismissMinions(player, (ServerWorld) player.getEntityWorld(), stack);
            int soulsReturned = getSouls(stack) - soulsBefore;
            player.sendMessage(Text.translatable("message.scythes.withering_minion.dismissed", dismissed, soulsReturned), true);
            return ActionResult.SUCCESS;
        }

        int ownedMinions = WitheringMinionManager.countMinions(player, (ServerWorld) player.getEntityWorld());
        int maxMinions = getMaxMinions(player, stack);
        if (ownedMinions >= maxMinions) {
            player.sendMessage(Text.translatable("message.scythes.withering_minion.cap", maxMinions), true);
            return ActionResult.FAIL;
        }

        if (getSouls(stack) < ScytheBalance.Withering.MINION_SOUL_COST) {
            player.sendMessage(Text.translatable("message.scythes.withering_minion.no_souls", ScytheBalance.Withering.MINION_SOUL_COST), true);
            return ActionResult.FAIL;
        }

        if (!hasEnoughDurability(stack, ScytheBalance.Withering.MINION_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return ActionResult.FAIL;
        }

        if (!WitheringMinionManager.spawnMinion(player, (ServerWorld) player.getEntityWorld())) {
            player.sendMessage(Text.translatable("message.scythes.withering_minion.no_space"), true);
            return ActionResult.FAIL;
        }

        spendSouls(stack, ScytheBalance.Withering.MINION_SOUL_COST);
        stack.damage(ScytheBalance.Withering.MINION_DURABILITY_COST, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheAdvancementTracker.markWitheringSpecial(player);
        ScytheAdvancementTracker.tryGrantSuperNecromancer(player, WitheringMinionManager.countMinions(player, (ServerWorld) player.getEntityWorld()));
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT, SoundCategory.PLAYERS, 0.8F, 0.75F);
        player.sendMessage(Text.translatable("message.scythes.withering_minion.spawned", getSouls(stack)), true);
        return ActionResult.SUCCESS;
    }

    public static int getSouls(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        return Math.max(0, nbt.getInt(SOULS_KEY, 0));
    }

    public static void addSouls(ItemStack stack, int amount) {
        if (stack.isEmpty() || !(stack.getItem() instanceof WitheringScytheItem) || amount <= 0) return;
        setSouls(stack, getSouls(stack) + amount);
    }

    public static boolean spendSouls(ItemStack stack, int amount) {
        if (amount <= 0) return true;
        int current = getSouls(stack);
        if (current < amount) return false;
        setSouls(stack, current - amount);
        return true;
    }

    private static void setSouls(ItemStack stack, int souls) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.putInt(SOULS_KEY, Math.max(0, souls));
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static int getMaxMinions(LivingEntity holder, ItemStack stack) {
        int enchantmentLevel = holder.getEntityWorld()
                .getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOptional(ScytheMod.ADDITIONAL_SLOT)
                .map(enchantment -> EnchantmentHelper.getLevel(enchantment, stack))
                .orElse(0);
        return ScytheBalance.Withering.MAX_MINIONS + getAdditionalMinionSlots(enchantmentLevel);
    }

    public static int getMaxMinions(TooltipContext context, ItemStack stack) {
        int enchantmentLevel = context.getRegistryLookup()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOptional(ScytheMod.ADDITIONAL_SLOT)
                .map(enchantment -> EnchantmentHelper.getLevel(enchantment, stack))
                .orElse(0);
        return ScytheBalance.Withering.MAX_MINIONS + getAdditionalMinionSlots(enchantmentLevel);
    }

    private static int getAdditionalMinionSlots(int level) {
        if (level <= 0) return 0;
        if (level == 1) return ScytheBalance.Withering.ADDITIONAL_SLOT_LEVEL_ONE;
        if (level == 2) return ScytheBalance.Withering.ADDITIONAL_SLOT_LEVEL_TWO;
        return ScytheBalance.Withering.ADDITIONAL_SLOT_LEVEL_THREE;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageable()) return false;
        return stack.getMaxDamage() - stack.getDamage() >= cost;
    }
}
