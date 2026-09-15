package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.item.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Client-only presentation. Installed by the client entry point, never by the server. */
public final class ScytheTooltips {
    private ScytheTooltips() {}

    public static void append(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        if (stack.getItem() instanceof BloodScytheItem) blood(stack, context, displayComponent, textConsumer, type);
        if (stack.getItem() instanceof ToxicScytheItem) toxic(stack, context, displayComponent, textConsumer, type);
        if (stack.getItem() instanceof WitheringScytheItem) withering(stack, context, displayComponent, textConsumer, type);
        if (stack.getItem() instanceof GoldenScytheItem) golden(stack, context, displayComponent, textConsumer, type);
        if (stack.getItem() instanceof FrozenScytheItem) frozen(stack, context, displayComponent, textConsumer, type);
    }

    private static void blood(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        List<Component> tooltip = new ArrayList<>();
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.bloody_scythe.desc", ChatFormatting.GRAY, ChatFormatting.ITALIC);

        if (!TooltipUtil.isShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        boolean alt = TooltipUtil.isAltDown();

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.passive").withStyle(ChatFormatting.GRAY));

        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.bleeding_chance", ChatFormatting.DARK_RED);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.bloody_scythe.passive.desc", ChatFormatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.bleed_chance_percent", TooltipUtil.fmtPercentValue(ScytheBalance.Blood.BLEEDING_CHANCE)), ChatFormatting.DARK_RED);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.bleed_base_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Blood.BLEEDING_BASE_DURATION_TICKS)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.bleed_extend_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Blood.BLEEDING_EXTEND_TICKS)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.bleeding_damage_per_second", TooltipUtil.fmtNumber(ScytheBalance.Bleeding.DAMAGE_PER_SECOND)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.vampirism_chance_percent", TooltipUtil.fmtPercentValue(ScytheBalance.Vampirism.VAMPIRISM_CHANCE)), ChatFormatting.DARK_RED);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.vampirism_heal_percent", TooltipUtil.fmtPercentValue(ScytheBalance.Vampirism.HEAL_FRACTION)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.vampirism_cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Vampirism.COOLDOWN_TICKS)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.defense_pierce", TooltipUtil.fmtPercentValue(ScytheBalance.Blood.DEFENSE_PIERCE_CHANCE), TooltipUtil.fmtPercentValue(ScytheBalance.Blood.DEFENSE_PIERCE_MITIGATION_IGNORED)), ChatFormatting.DARK_RED);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.special").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.blood_blender").withStyle(ChatFormatting.DARK_RED));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.blood_blender.desc", ChatFormatting.GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.Blood.BLENDER_RADIUS)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Blood.BLENDER_COOLDOWN_TICKS)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.Blood.BLENDER_DURABILITY_COST)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.basic_hit_count", String.valueOf(ScytheBalance.Blood.BLENDER_HIT_COUNT)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.slowness_ii_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Blood.BLENDER_SLOWNESS_TICKS)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.bleeding_ii_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Blood.BLENDER_BLEEDING_TICKS)), ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.active").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.blood_harvest", TooltipUtil.getScytheAbilityKeyText(ChatFormatting.DARK_RED)));

        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.blood_harvest.desc", ChatFormatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.BloodHarvest.RADIUS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.COOLDOWN_TICKS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.BloodHarvest.DURABILITY_COST)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.kill_window_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.KILL_WINDOW_TICKS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.slowness_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.SLOWNESS_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.blindness_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.BLINDNESS_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.weakness_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.WEAKNESS_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.glowing_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.GLOWING_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.success_buffs_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.SUCCESS_BUFF_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.failure_debuffs_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.FAILURE_DEBUFF_TICKS)), ChatFormatting.DARK_GRAY);

        TooltipUtil.flush(tooltip, textConsumer);
    }

    private static void toxic(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
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
                            TooltipUtil.fmtPercentValue(ScytheBalance.Toxic.PASSIVE_ARMOR_DAMAGE_CHANCE)
                    ),
                    ChatFormatting.GREEN
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.armor_damage_full_set", String.valueOf(ScytheBalance.Toxic.PASSIVE_ARMOR_DAMAGE)),
                    ChatFormatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable(
                            "tooltip.scythes.stat.poison_chance_percent",
                            TooltipUtil.fmtPercentValue(ScytheBalance.Toxic.PASSIVE_POISON_CHANCE)
                    ),
                    ChatFormatting.GREEN
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.poison_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Toxic.PASSIVE_POISON_TICKS)),
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
                    Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Toxic.ORB_COOLDOWN_TICKS)),
                    ChatFormatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.Toxic.ORB_DURABILITY_COST)),
                    ChatFormatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.ToxicOrb.DAMAGE_RADIUS)),
                    ChatFormatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.pure_damage", TooltipUtil.fmtNumber(ScytheBalance.ToxicOrb.PURE_DAMAGE)),
                    ChatFormatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.poison_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.ToxicOrb.POISON_TICKS)),
                    ChatFormatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.armor_damage_full_set", String.valueOf(ScytheBalance.ToxicOrb.ARMOR_DAMAGE)),
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
                Component.translatable("tooltip.scythes.stat.duration_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.ToxicAura.DURATION_TICKS)),
                ChatFormatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.ToxicAura.AURA_COOLDOWN_TICKS)),
                ChatFormatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.ToxicAura.AURA_DURABILITY_COST)),
                ChatFormatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.ToxicAura.RADIUS)),
                ChatFormatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.poison_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.ToxicAura.POISON_TICKS)),
                ChatFormatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.armor_damage_per_tick", String.valueOf(ScytheBalance.ToxicAura.ARMOR_DAMAGE_PER_TICK)),
                ChatFormatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable(
                        "tooltip.scythes.stat.pure_damage_chance_percent",
                        TooltipUtil.fmtPercentValue(ScytheBalance.ToxicAura.PURE_DAMAGE_CHANCE)
                ),
                ChatFormatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.pure_damage", TooltipUtil.fmtNumber(ScytheBalance.ToxicAura.PURE_DAMAGE)),
                ChatFormatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable(
                        "tooltip.scythes.stat.nausea_chance_percent",
                        TooltipUtil.fmtPercentValue(ScytheBalance.ToxicAura.NAUSEA_CHANCE)
                ),
                ChatFormatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Component.translatable("tooltip.scythes.stat.nausea_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.ToxicAura.NAUSEA_TICKS)),
                ChatFormatting.DARK_GRAY
        );

        TooltipUtil.flush(tooltip, textConsumer);
    }

    private static void withering(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
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
        tooltip.add(Component.translatable("tooltip.scythes.withering_scythe.passive", WitheringScytheItem.getSouls(stack)).withStyle(ChatFormatting.DARK_PURPLE));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_scythe.passive.desc", ChatFormatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.souls", String.valueOf(WitheringScytheItem.getSouls(stack))),
                    ChatFormatting.DARK_PURPLE
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.wither_chance_percent", TooltipUtil.fmtPercentValue(ScytheBalance.Withering.WITHER_CHANCE)),
                    ChatFormatting.DARK_PURPLE
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.wither_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Withering.WITHER_TICKS)),
                    ChatFormatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.souls_per_mob", String.valueOf(ScytheBalance.Withering.SOULS_PER_MOB_KILL)),
                    ChatFormatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Component.translatable("tooltip.scythes.stat.souls_per_player", String.valueOf(ScytheBalance.Withering.SOULS_PER_PLAYER_KILL)),
                    ChatFormatting.DARK_GRAY
            );
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.special").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.withering_minion").withStyle(ChatFormatting.DARK_PURPLE));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_minion.desc", ChatFormatting.GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.Withering.MINION_DURABILITY_COST)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.soul_cost", String.valueOf(ScytheBalance.Withering.MINION_SOUL_COST)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_cap", String.valueOf(WitheringScytheItem.getMaxMinions(context, stack))), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_health", TooltipUtil.fmtNumber(ScytheBalance.Minion.MAX_HEALTH)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_armor", TooltipUtil.fmtNumber(ScytheBalance.Minion.ARMOR)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_lifetime_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Minion.LIFETIME_TICKS)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_regen_delay_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Minion.REGEN_IDLE_TICKS)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_regen_cost", String.valueOf(ScytheBalance.Minion.REGEN_DURABILITY_COST)), ChatFormatting.DARK_GRAY);
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

        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.duration_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.WitheringAura.DURATION_TICKS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.WitheringAura.AURA_COOLDOWN_TICKS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.WitheringAura.AURA_DURABILITY_COST)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.WitheringAura.WITHER_RADIUS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.wither_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.WitheringAura.WITHER_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_buff_radius", TooltipUtil.fmtNumber(ScytheBalance.WitheringAura.MINION_BUFF_RADIUS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.minion_buff_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.WitheringAura.MINION_BUFF_TICKS)), ChatFormatting.DARK_GRAY);

        TooltipUtil.flush(tooltip, textConsumer);
    }

    private static void golden(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        List<Component> tooltip = new ArrayList<>();
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_scythe.desc", ChatFormatting.GRAY, ChatFormatting.ITALIC);

        if (!TooltipUtil.isShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        boolean alt = TooltipUtil.isAltDown();

        if (alt) {
            tooltip.add(Component.empty());
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_scythe.base_stats", ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.passive").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.golden_scythe.passive").withStyle(ChatFormatting.GOLD));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_scythe.passive.desc", ChatFormatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.looting_bonus", String.valueOf(ScytheBalance.Golden.PASSIVE_LOOTING_BONUS)), ChatFormatting.GOLD);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.piglin_neutrality", ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.no_golden_scythe_stacking", ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.special").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.midas_touch").withStyle(ChatFormatting.GOLD));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.midas_touch.desc", ChatFormatting.GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Golden.MIDAS_COOLDOWN_TICKS)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.Golden.MIDAS_DURABILITY_COST)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.reach_blocks", TooltipUtil.fmtNumber(ScytheBalance.Golden.MIDAS_REACH)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.max_health_damage_percent", TooltipUtil.fmtPercentValue(ScytheBalance.Golden.MIDAS_DAMAGE_FRACTION)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.damage_clamp", TooltipUtil.fmtNumber(ScytheBalance.Golden.MIDAS_MIN_DAMAGE), TooltipUtil.fmtNumber(ScytheBalance.Golden.MIDAS_MAX_DAMAGE)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.mark_looting_bonus", String.valueOf(ScytheBalance.Golden.MARK_LOOTING_BONUS)), ChatFormatting.GOLD);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.active").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.golden_rain", TooltipUtil.getScytheAbilityKeyText(ChatFormatting.GOLD)));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_rain.desc", ChatFormatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.GoldenRain.RADIUS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.GoldenRain.COOLDOWN_TICKS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.GoldenRain.DURABILITY_COST)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.mark_looting_bonus", String.valueOf(ScytheBalance.Golden.MARK_LOOTING_BONUS)), ChatFormatting.GOLD);

        TooltipUtil.flush(tooltip, textConsumer);
    }

    private static void frozen(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> textConsumer, TooltipFlag type) {
        List<Component> tooltip = new ArrayList<>();
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.frozen_scythe.desc", ChatFormatting.GRAY, ChatFormatting.ITALIC);

        if (!TooltipUtil.isShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        boolean alt = TooltipUtil.isAltDown();
        if (alt) {
            tooltip.add(Component.empty());
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.frozen_scythe.base_stats", ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.passive").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.frozen_scythe.passive").withStyle(ChatFormatting.AQUA));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.frozen_scythe.passive.desc", ChatFormatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.powder_snow_walk", ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.frost_walker_level", ScytheBalance.Frozen.COLD_MASTER_FROST_WALKER_LEVEL), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.freezing_chance_percent", TooltipUtil.fmtPercentValue(ScytheBalance.Frozen.COLD_MASTER_FREEZING_CHANCE)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.freezing_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Frozen.COLD_MASTER_FREEZING_TICKS)), ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.special").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.ice_spike").withStyle(ChatFormatting.AQUA));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.ice_spike.desc", ChatFormatting.GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Frozen.ICE_SPIKE_COOLDOWN_TICKS)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.Frozen.ICE_SPIKE_DURABILITY_COST)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.damage", TooltipUtil.fmtNumber(ScytheBalance.IceSpike.HIT_DAMAGE)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.freezing_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.IceSpike.FREEZING_TICKS)), ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.active").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.frozen_storm", TooltipUtil.getScytheAbilityKeyText(ChatFormatting.AQUA)));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.frozen_storm.desc", ChatFormatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.FrozenStorm.RADIUS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.FrozenStorm.COOLDOWN_TICKS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.FrozenStorm.DURABILITY_COST)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.freezing_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.FrozenStorm.FREEZING_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.slowness_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.FrozenStorm.SLOWNESS_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.weakness_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.FrozenStorm.WEAKNESS_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.ignores_pets_and_teammates", ChatFormatting.DARK_GRAY);
        TooltipUtil.flush(tooltip, textConsumer);
    }
}
