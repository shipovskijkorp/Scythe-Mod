package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.ToxicAuraAbility;
import com.shipovskijkorp.scythes.mod.ability.ToxicAuraTracker;
import com.shipovskijkorp.scythes.mod.ability.ToxicScytheCooldowns;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.entity.ToxicOrbEntity;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
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

public class ToxicScytheItem extends ScytheSwordItem {

    public static final double PASSIVE_ARMOR_DAMAGE_CHANCE = 0.30D;
    public static final int PASSIVE_ARMOR_DAMAGE = 20;
    public static final double PASSIVE_POISON_CHANCE = 0.25D;
    public static final int PASSIVE_POISON_TICKS = 20 * 2;
    public static final int PASSIVE_POISON_AMPLIFIER = 1;

    public static final int ORB_COOLDOWN_TICKS = 20 * 5;
    public static final int ORB_DURABILITY_COST = 20;
    public static final float ORB_SPEED = 1.5F;

    public ToxicScytheItem(Properties settings) {
        super(settings);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        List<Component> tooltip = new ArrayList<>();
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.toxic_scythe.desc", ChatFormatting.GRAY, ChatFormatting.ITALIC);

        if (!TooltipUtil.isShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        boolean alt = TooltipUtil.isAltDown();

        if (alt) {
            tooltip.add(Component.empty());
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.toxic_scythe.base_stats", ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.passive").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.toxic_scythe.passive").withStyle(ChatFormatting.GREEN));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.toxic_scythe.passive.desc", ChatFormatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable(
                            "tooltip.scythes.stat.armor_damage_chance_percent",
                            TooltipUtil.fmtPercentValue(PASSIVE_ARMOR_DAMAGE_CHANCE)
                    ),
                    ChatFormatting.GREEN
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.armor_damage_full_set", String.valueOf(PASSIVE_ARMOR_DAMAGE)),
                    ChatFormatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable(
                            "tooltip.scythes.stat.poison_chance_percent",
                            TooltipUtil.fmtPercentValue(PASSIVE_POISON_CHANCE)
                    ),
                    ChatFormatting.GREEN
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.poison_sec", TooltipUtil.fmtSecondsValue(PASSIVE_POISON_TICKS)),
                    ChatFormatting.DARK_GRAY
            );
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.special").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.toxic_orb").withStyle(ChatFormatting.GREEN));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.toxic_orb.desc", ChatFormatting.GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ORB_COOLDOWN_TICKS)),
                    ChatFormatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ORB_DURABILITY_COST)),
                    ChatFormatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ToxicOrbEntity.DAMAGE_RADIUS)),
                    ChatFormatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.pure_damage", TooltipUtil.fmtNumber(ToxicOrbEntity.PURE_DAMAGE)),
                    ChatFormatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.poison_sec", TooltipUtil.fmtSecondsValue(ToxicOrbEntity.POISON_TICKS)),
                    ChatFormatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.armor_damage_full_set", String.valueOf(ToxicOrbEntity.ARMOR_DAMAGE)),
                    ChatFormatting.DARK_GRAY
            );
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.active").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.toxic_aura", TooltipUtil.getScytheAbilityKeyText(ChatFormatting.GREEN)));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.toxic_aura.desc", ChatFormatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.duration_sec", TooltipUtil.fmtSecondsValue(ToxicAuraTracker.DURATION_TICKS)),
                ChatFormatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ToxicAuraAbility.AURA_COOLDOWN_TICKS)),
                ChatFormatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ToxicAuraAbility.AURA_DURABILITY_COST)),
                ChatFormatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ToxicAuraTracker.RADIUS)),
                ChatFormatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.poison_sec", TooltipUtil.fmtSecondsValue(ToxicAuraTracker.POISON_TICKS)),
                ChatFormatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.armor_damage_per_tick", String.valueOf(ToxicAuraTracker.ARMOR_DAMAGE_PER_TICK)),
                ChatFormatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable(
                        "tooltip.scythes.stat.pure_damage_chance_percent",
                        TooltipUtil.fmtPercentValue(ToxicAuraTracker.PURE_DAMAGE_CHANCE)
                ),
                ChatFormatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.pure_damage", TooltipUtil.fmtNumber(ToxicAuraTracker.PURE_DAMAGE)),
                ChatFormatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable(
                        "tooltip.scythes.stat.nausea_chance_percent",
                        TooltipUtil.fmtPercentValue(ToxicAuraTracker.NAUSEA_CHANCE)
                ),
                ChatFormatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.nausea_sec", TooltipUtil.fmtSecondsValue(ToxicAuraTracker.NAUSEA_TICKS)),
                ChatFormatting.DARK_GRAY
        );

        TooltipUtil.flush(tooltip, textConsumer);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);

        boolean passiveTriggered = false;
        int acidityLevel = getAcidityLevel(attacker, stack);

        if (attacker.getRandom().nextDouble() < PASSIVE_ARMOR_DAMAGE_CHANCE) {
            ScytheCombatUtil.damageArmorSet(target, applyAcidityBonus(attacker.getRandom(), PASSIVE_ARMOR_DAMAGE, acidityLevel));
            passiveTriggered = true;
        }

        if (attacker.getRandom().nextDouble() < PASSIVE_POISON_CHANCE) {
            ScytheCombatUtil.refreshStatus(target, MobEffects.POISON, PASSIVE_POISON_TICKS, PASSIVE_POISON_AMPLIFIER);
            if (attacker instanceof ServerPlayer player) {
                ScytheAdvancementTracker.recordToxicPoison(player, target, PASSIVE_POISON_TICKS);
            }
            passiveTriggered = true;
        }

        if (passiveTriggered && attacker instanceof ServerPlayer player) {
            ScytheAdvancementTracker.markToxicPassive(player);
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

        int cooldownLeft = ToxicScytheCooldowns.getOrbTicksLeft(player);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.toxic_orb.cooldown", Math.max(1, cooldownLeft / 20)));
            return InteractionResult.FAIL;
        }

        if (!hasEnoughDurability(stack, ORB_DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return InteractionResult.FAIL;
        }

        ToxicOrbEntity orb = new ToxicOrbEntity(world, player);
        orb.setAcidityLevel(getAcidityLevel(player, stack));
        orb.snapTo(player.getX(), player.getEyeY() - 0.15D, player.getZ(), player.getYRot(), player.getXRot());
        orb.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, ORB_SPEED, 0.0F);
        world.addFreshEntity(orb);

        stack.hurtAndBreak(ORB_DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ToxicScytheCooldowns.setOrbCooldown(player, ORB_COOLDOWN_TICKS);
        ScytheAdvancementTracker.markToxicSpecial(player);

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS,
                0.75F,
                0.85F + player.getRandom().nextFloat() * 0.3F
        );

        return InteractionResult.SUCCESS;
    }

    public static int getAcidityLevel(LivingEntity holder, ItemStack stack) {
        java.util.Optional<net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment>> entry = holder.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ScytheMod.ACIDITY);
        return entry.map(enchantment -> net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack)).orElse(0);
    }

    public static int applyAcidityBonus(RandomSource random, int baseArmorDamage, int acidityLevel) {
        if (baseArmorDamage <= 0 || acidityLevel <= 0) return baseArmorDamage;

        double exactBonus = baseArmorDamage * 0.11D * Math.min(3, acidityLevel);
        int wholeBonus = (int) Math.floor(exactBonus);
        double fractionalBonus = exactBonus - wholeBonus;
        if (fractionalBonus > 0.0D && random.nextDouble() < fractionalBonus) {
            wholeBonus++;
        }

        return baseArmorDamage + wholeBonus;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageableItem()) return false;
        return stack.getMaxDamage() - stack.getDamageValue() >= cost;
    }
}
