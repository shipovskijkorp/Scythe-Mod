package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.WitheringAuraAbility;
import com.shipovskijkorp.scythes.mod.ability.WitheringAuraTracker;
import com.shipovskijkorp.scythes.mod.ability.WitheringMinionManager;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WitheringScytheItem extends ScytheSwordItem {

    private static final String SOULS_KEY = "Souls";

    public static final double WITHER_CHANCE = 0.30D;
    public static final int WITHER_TICKS = 20 * 10;
    public static final int WITHER_AMPLIFIER = 1;

    public static final int SOULS_PER_MOB_KILL = 1;
    public static final int SOULS_PER_PLAYER_KILL = 12;

    public static final int MINION_DURABILITY_COST = 10;
    public static final int MINION_SOUL_COST = 6;
    public static final int MAX_MINIONS = 6;

    public WitheringScytheItem(Properties settings) {
        super(settings);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        List<Component> tooltip = new ArrayList<>();
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_scythe.desc", ChatFormatting.GRAY, ChatFormatting.ITALIC);

        if (!TooltipUtil.isShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        boolean alt = TooltipUtil.isAltDown();

        if (alt) {
            tooltip.add(Component.empty());
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_scythe.base_stats", ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.passive").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.withering_scythe.passive", getSouls(stack)).withStyle(ChatFormatting.DARK_PURPLE));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_scythe.passive.desc", ChatFormatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.souls", String.valueOf(getSouls(stack))),
                    ChatFormatting.DARK_PURPLE
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.wither_chance_percent", TooltipUtil.fmtPercentValue(WITHER_CHANCE)),
                    ChatFormatting.DARK_PURPLE
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.wither_sec", TooltipUtil.fmtSecondsValue(WITHER_TICKS)),
                    ChatFormatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.souls_per_mob", String.valueOf(SOULS_PER_MOB_KILL)),
                    ChatFormatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.souls_per_player", String.valueOf(SOULS_PER_PLAYER_KILL)),
                    ChatFormatting.DARK_GRAY
            );
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.special").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.withering_minion").withStyle(ChatFormatting.DARK_PURPLE));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_minion.desc", ChatFormatting.GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(MINION_DURABILITY_COST)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.soul_cost", String.valueOf(MINION_SOUL_COST)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_cap", String.valueOf(getMaxMinions(context, stack))), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_health", TooltipUtil.fmtNumber(WitheringMinionEntity.MAX_HEALTH)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_armor", TooltipUtil.fmtNumber(WitheringMinionEntity.ARMOR)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_lifetime_sec", TooltipUtil.fmtSecondsValue(WitheringMinionEntity.LIFETIME_TICKS)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_regen_delay_sec", TooltipUtil.fmtSecondsValue(WitheringMinionEntity.REGEN_IDLE_TICKS)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_regen_cost", String.valueOf(WitheringMinionEntity.REGEN_DURABILITY_COST)), ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.active").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.withering_aura", TooltipUtil.getScytheAbilityKeyText(ChatFormatting.DARK_PURPLE)));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_aura.desc", ChatFormatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.duration_sec", TooltipUtil.fmtSecondsValue(WitheringAuraTracker.DURATION_TICKS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(WitheringAuraAbility.AURA_COOLDOWN_TICKS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(WitheringAuraAbility.AURA_DURABILITY_COST)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(WitheringAuraTracker.WITHER_RADIUS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.wither_sec", TooltipUtil.fmtSecondsValue(WitheringAuraTracker.WITHER_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_buff_radius", TooltipUtil.fmtNumber(WitheringAuraTracker.MINION_BUFF_RADIUS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_buff_sec", TooltipUtil.fmtSecondsValue(WitheringAuraTracker.MINION_BUFF_TICKS)), ChatFormatting.DARK_GRAY);

        TooltipUtil.flush(tooltip, textConsumer);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);

        if (attacker.level().isClientSide()) {
            return;
        }

        if (attacker.getRandom().nextDouble() < WITHER_CHANCE) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    MobEffects.WITHER,
                    WITHER_TICKS,
                    WITHER_AMPLIFIER,
                    false,
                    true,
                    true
            ));

            if (attacker instanceof ServerPlayer player) {
                ScytheAdvancementTracker.markWitheringPassive(player);
                DamageAttributionTracker.recordWithering(target, player, WITHER_TICKS);
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
            int dismissed = WitheringMinionManager.dismissMinions(player, (ServerLevel) player.level());
            player.sendOverlayMessage(Component.translatable("message.scythes.withering_minion.dismissed", dismissed));
            return InteractionResult.SUCCESS;
        }

        int maxMinions = getMaxMinions(player, stack);
        int ownedMinions = WitheringMinionManager.countMinions(player, (ServerLevel) player.level());
        if (ownedMinions >= maxMinions) {
            player.sendOverlayMessage(Component.translatable("message.scythes.withering_minion.cap", maxMinions));
            return InteractionResult.FAIL;
        }

        if (getSouls(stack) < MINION_SOUL_COST) {
            player.sendOverlayMessage(Component.translatable("message.scythes.withering_minion.no_souls", MINION_SOUL_COST));
            return InteractionResult.FAIL;
        }

        if (!hasEnoughDurability(stack, MINION_DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return InteractionResult.FAIL;
        }

        if (!WitheringMinionManager.spawnMinion(player, (ServerLevel) player.level())) {
            player.sendOverlayMessage(Component.translatable("message.scythes.withering_minion.no_space"));
            return InteractionResult.FAIL;
        }

        spendSouls(stack, MINION_SOUL_COST);
        stack.hurtAndBreak(MINION_DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
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

    private static int getMaxMinions(Item.TooltipContext context, ItemStack stack) {
        return getMaxMinions(context.registries(), stack);
    }

    private static int getMaxMinions(net.minecraft.core.HolderLookup.Provider registries, ItemStack stack) {
        return MAX_MINIONS + getAdditionalSlotBonus(getScytheEnchantmentLevel(registries, stack, ScytheMod.ADDITIONAL_SLOT));
    }

    public static int getAdditionalSlotLevel(ServerPlayer player, ItemStack stack) {
        return getScytheEnchantmentLevel(player.registryAccess(), stack, ScytheMod.ADDITIONAL_SLOT);
    }

    public static int getSoulSiphonLevel(ServerPlayer player, ItemStack stack) {
        return getScytheEnchantmentLevel(player.registryAccess(), stack, ScytheMod.SOUL_SIPHON);
    }

    private static int getAdditionalSlotBonus(int level) {
        return switch (Math.max(0, level)) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 4;
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
