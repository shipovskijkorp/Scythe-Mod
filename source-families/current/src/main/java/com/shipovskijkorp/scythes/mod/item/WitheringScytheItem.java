package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.WitheringMinionManager;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class WitheringScytheItem extends ScytheSwordItem {

    private static final String SOULS_KEY = "Souls";

    public WitheringScytheItem(Properties settings) {
        super(settings);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (ScytheCombatUtil.isProtectedWitheringMinion(attacker, target)) return;

        if (attacker.level().isClientSide()) {
            return;
        }

        if (attacker.getRandom().nextDouble() < ScytheBalance.Withering.WITHER_CHANCE) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    MobEffects.WITHER,
                    ScytheBalance.Withering.WITHER_TICKS,
                    ScytheBalance.Withering.WITHER_AMPLIFIER,
                    false,
                    true,
                    true
            ));

            if (attacker instanceof ServerPlayer player) {
                ScytheAdvancementTracker.markWitheringPassive(player);
                DamageAttributionTracker.recordWithering(target, player, ScytheBalance.Withering.WITHER_TICKS);
            }
        }
    }

    @Override
    public InteractionResult use(Level world, net.minecraft.world.entity.player.Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (!(world instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        }

        if (!(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            int soulsBefore = getSouls(stack);
            int dismissed = WitheringMinionManager.dismissMinions(player, (ServerLevel) player.level(), stack);
            int soulsReturned = getSouls(stack) - soulsBefore;
            player.sendOverlayMessage(Component.translatable("message.scythes.withering_minion.dismissed", dismissed, soulsReturned));
            return InteractionResult.SUCCESS;
        }

        int maxMinions = getMaxMinions(player, stack);
        int ownedMinions = WitheringMinionManager.countMinions(player, (ServerLevel) player.level());
        if (ownedMinions >= maxMinions) {
            player.sendOverlayMessage(Component.translatable("message.scythes.withering_minion.cap", maxMinions));
            return InteractionResult.FAIL;
        }

        if (getSouls(stack) < ScytheBalance.Withering.MINION_SOUL_COST) {
            player.sendOverlayMessage(Component.translatable("message.scythes.withering_minion.no_souls", ScytheBalance.Withering.MINION_SOUL_COST));
            return InteractionResult.FAIL;
        }

        if (!hasEnoughDurability(stack, ScytheBalance.Withering.MINION_DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return InteractionResult.FAIL;
        }

        if (!WitheringMinionManager.spawnMinion(player, (ServerLevel) player.level())) {
            player.sendOverlayMessage(Component.translatable("message.scythes.withering_minion.no_space"));
            return InteractionResult.FAIL;
        }

        spendSouls(stack, ScytheBalance.Withering.MINION_SOUL_COST);
        stack.hurtAndBreak(ScytheBalance.Withering.MINION_DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheAdvancementTracker.markWitheringSpecial(player);
        ScytheAdvancementTracker.tryGrantSuperNecromancer(player, WitheringMinionManager.countMinions(player, (ServerLevel) player.level()));
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.PLAYERS, 0.8F, 0.75F);
        player.sendOverlayMessage(Component.translatable("message.scythes.withering_minion.spawned", getSouls(stack)));
        return InteractionResult.SUCCESS;
    }

    public static int getSouls(ItemStack stack) {
        CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0, nbt.getInt(SOULS_KEY).orElse(0));
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
        CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        nbt.putInt(SOULS_KEY, Math.max(0, souls));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    public static int getMaxMinions(ServerPlayer player, ItemStack stack) {
        return getMaxMinions(player.registryAccess(), stack);
    }

    public static int getMaxMinions(Item.TooltipContext context, ItemStack stack) {
        return getMaxMinions(context.registries(), stack);
    }

    private static int getMaxMinions(net.minecraft.core.HolderLookup.Provider registries, ItemStack stack) {
        return ScytheBalance.Withering.MAX_MINIONS + getAdditionalSlotBonus(getScytheEnchantmentLevel(registries, stack, ScytheMod.ADDITIONAL_SLOT));
    }

    public static int getAdditionalSlotLevel(ServerPlayer player, ItemStack stack) {
        return getScytheEnchantmentLevel(player.registryAccess(), stack, ScytheMod.ADDITIONAL_SLOT);
    }

    public static int getSoulSiphonLevel(ServerPlayer player, ItemStack stack) {
        return getScytheEnchantmentLevel(player.registryAccess(), stack, ScytheMod.SOUL_SIPHON);
    }

    private static int getAdditionalSlotBonus(int level) {
        return switch (Math.max(0, level)) {
            case 1 -> ScytheBalance.Withering.ADDITIONAL_SLOT_LEVEL_ONE;
            case 2 -> ScytheBalance.Withering.ADDITIONAL_SLOT_LEVEL_TWO;
            case 3 -> ScytheBalance.Withering.ADDITIONAL_SLOT_LEVEL_THREE;
            default -> 0;
        };
    }

    private static int getScytheEnchantmentLevel(net.minecraft.core.HolderLookup.Provider registries, ItemStack stack, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantmentKey) {
        java.util.Optional<net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment>> entry = registries
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(enchantmentKey);
        return entry.map(enchantment -> net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack)).orElse(0);
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageableItem()) return false;
        return stack.getMaxDamage() - stack.getDamageValue() >= cost;
    }
}
