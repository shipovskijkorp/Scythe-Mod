package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.item.*;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Client-only presentation. Installed by the client entry point, never by the server. */
public final class ScytheTooltips {
    private ScytheTooltips() {}

    public static void append(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (stack.getItem() instanceof BloodScytheItem) blood(stack, context, tooltip, type);
        if (stack.getItem() instanceof ToxicScytheItem) toxic(stack, context, tooltip, type);
        if (stack.getItem() instanceof WitheringScytheItem) withering(stack, context, tooltip, type);
        if (stack.getItem() instanceof GoldenScytheItem) golden(stack, context, tooltip, type);
        if (stack.getItem() instanceof FrozenScytheItem) frozen(stack, context, tooltip, type);
        if (stack.getItem() instanceof FarmerScytheItem) farmer(stack, context, tooltip, type);
    }

    private static void blood(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.bloody_scythe.desc", Formatting.GRAY, Formatting.ITALIC);

        if (!Screen.hasShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            return;
        }

        boolean alt = Screen.hasAltDown();

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.passive").formatted(Formatting.GRAY));

        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.bleeding_chance", Formatting.DARK_RED);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.bloody_scythe.passive.desc", Formatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.bleed_chance_percent",
                            TooltipUtil.fmtPercentValue(ScytheBalance.Blood.BLEEDING_CHANCE)
                    ),
                    Formatting.DARK_RED
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.bleed_base_sec",
                            TooltipUtil.fmtSecondsValue(ScytheBalance.Blood.BLEEDING_BASE_DURATION_TICKS)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.bleed_extend_sec",
                            TooltipUtil.fmtSecondsValue(ScytheBalance.Blood.BLEEDING_EXTEND_TICKS)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.bleeding_damage_per_second",
                            TooltipUtil.fmtNumber(ScytheBalance.Bleeding.DAMAGE_PER_SECOND)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.vampirism_chance_percent",
                            TooltipUtil.fmtPercentValue(ScytheBalance.Vampirism.VAMPIRISM_CHANCE)
                    ),
                    Formatting.DARK_RED
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.vampirism_heal_percent",
                            TooltipUtil.fmtPercentValue(ScytheBalance.Vampirism.HEAL_FRACTION)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.vampirism_cooldown_sec",
                            TooltipUtil.fmtSecondsValue(ScytheBalance.Vampirism.COOLDOWN_TICKS)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.defense_pierce",
                            TooltipUtil.fmtPercentValue(ScytheBalance.Blood.DEFENSE_PIERCE_CHANCE),
                            TooltipUtil.fmtPercentValue(ScytheBalance.Blood.DEFENSE_PIERCE_MITIGATION_IGNORED)
                    ),
                    Formatting.DARK_RED
            );
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.special").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.blood_blender").formatted(Formatting.DARK_RED));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.blood_blender.desc", Formatting.GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.Blood.BLENDER_RADIUS)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Blood.BLENDER_COOLDOWN_TICKS)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.Blood.BLENDER_DURABILITY_COST)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.basic_hit_count", String.valueOf(ScytheBalance.Blood.BLENDER_HIT_COUNT)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.slowness_ii_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Blood.BLENDER_SLOWNESS_TICKS)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.bleeding_ii_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Blood.BLENDER_BLEEDING_TICKS)),
                    Formatting.DARK_GRAY
            );
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.active").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.blood_harvest", TooltipUtil.getScytheAbilityKeyText(Formatting.DARK_RED)));

        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.blood_harvest.desc", Formatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            return;
        }

        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.BloodHarvest.RADIUS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.COOLDOWN_TICKS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.BloodHarvest.DURABILITY_COST)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.kill_window_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.KILL_WINDOW_TICKS)),
                Formatting.GRAY
        );

        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.slowness_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.SLOWNESS_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.blindness_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.BLINDNESS_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.weakness_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.WEAKNESS_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.glowing_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.GLOWING_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.success_buffs_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.SUCCESS_BUFF_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.failure_debuffs_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.BloodHarvest.FAILURE_DEBUFF_TICKS)),
                Formatting.DARK_GRAY
        );
    }

    private static void toxic(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.toxic_scythe.desc", Formatting.GRAY, Formatting.ITALIC);

        if (!Screen.hasShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            return;
        }

        boolean alt = Screen.hasAltDown();

        if (alt) {
            tooltip.add(Text.empty());
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.toxic_scythe.base_stats", Formatting.DARK_GRAY);
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.passive").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.toxic_scythe.passive").formatted(Formatting.GREEN));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.toxic_scythe.passive.desc", Formatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.armor_damage_chance_percent",
                            TooltipUtil.fmtPercentValue(ScytheBalance.Toxic.PASSIVE_ARMOR_DAMAGE_CHANCE)
                    ),
                    Formatting.GREEN
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.armor_damage_full_set", String.valueOf(ScytheBalance.Toxic.PASSIVE_ARMOR_DAMAGE)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.poison_chance_percent",
                            TooltipUtil.fmtPercentValue(ScytheBalance.Toxic.PASSIVE_POISON_CHANCE)
                    ),
                    Formatting.GREEN
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.poison_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Toxic.PASSIVE_POISON_TICKS)),
                    Formatting.DARK_GRAY
            );
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.special").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.toxic_orb").formatted(Formatting.GREEN));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.toxic_orb.desc", Formatting.GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Toxic.ORB_COOLDOWN_TICKS)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.Toxic.ORB_DURABILITY_COST)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.ToxicOrb.DAMAGE_RADIUS)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.pure_damage", TooltipUtil.fmtNumber(ScytheBalance.ToxicOrb.PURE_DAMAGE)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.poison_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.ToxicOrb.POISON_TICKS)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.armor_damage_full_set", String.valueOf(ScytheBalance.ToxicOrb.ARMOR_DAMAGE)),
                    Formatting.DARK_GRAY
            );
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.active").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.toxic_aura", TooltipUtil.getScytheAbilityKeyText(Formatting.GREEN)));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.toxic_aura.desc", Formatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            return;
        }

        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.duration_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.ToxicAura.DURATION_TICKS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.ToxicAura.AURA_COOLDOWN_TICKS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.ToxicAura.AURA_DURABILITY_COST)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.ToxicAura.RADIUS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.poison_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.ToxicAura.POISON_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.armor_damage_per_tick", String.valueOf(ScytheBalance.ToxicAura.ARMOR_DAMAGE_PER_TICK)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable(
                        "tooltip.scythes.stat.pure_damage_chance_percent",
                        TooltipUtil.fmtPercentValue(ScytheBalance.ToxicAura.PURE_DAMAGE_CHANCE)
                ),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.pure_damage", TooltipUtil.fmtNumber(ScytheBalance.ToxicAura.PURE_DAMAGE)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable(
                        "tooltip.scythes.stat.nausea_chance_percent",
                        TooltipUtil.fmtPercentValue(ScytheBalance.ToxicAura.NAUSEA_CHANCE)
                ),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.nausea_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.ToxicAura.NAUSEA_TICKS)),
                Formatting.DARK_GRAY
        );
    }

    private static void withering(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_scythe.desc", Formatting.GRAY, Formatting.ITALIC);

        if (!Screen.hasShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            return;
        }

        boolean alt = Screen.hasAltDown();

        if (alt) {
            tooltip.add(Text.empty());
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_scythe.base_stats", Formatting.DARK_GRAY);
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.passive").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.withering_scythe.passive", WitheringScytheItem.getSouls(stack)).formatted(Formatting.DARK_PURPLE));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_scythe.passive.desc", Formatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.souls", String.valueOf(WitheringScytheItem.getSouls(stack))),
                    Formatting.DARK_PURPLE
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.wither_chance_percent", TooltipUtil.fmtPercentValue(ScytheBalance.Withering.WITHER_CHANCE)),
                    Formatting.DARK_PURPLE
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.wither_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Withering.WITHER_TICKS)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.souls_per_mob", String.valueOf(ScytheBalance.Withering.SOULS_PER_MOB_KILL)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.souls_per_player", String.valueOf(ScytheBalance.Withering.SOULS_PER_PLAYER_KILL)),
                    Formatting.DARK_GRAY
            );
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.special").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.withering_minion").formatted(Formatting.DARK_PURPLE));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_minion.desc", Formatting.GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.Withering.MINION_DURABILITY_COST)), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.soul_cost", String.valueOf(ScytheBalance.Withering.MINION_SOUL_COST)), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_cap", String.valueOf(WitheringScytheItem.getMaxMinions(context.getRegistryLookup(), stack))), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_health", TooltipUtil.fmtNumber(ScytheBalance.Minion.MAX_HEALTH)), Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_armor", TooltipUtil.fmtNumber(ScytheBalance.Minion.ARMOR)), Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_lifetime_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Minion.LIFETIME_TICKS)), Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_regen_delay_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Minion.REGEN_IDLE_TICKS)), Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_regen_cost", String.valueOf(ScytheBalance.Minion.REGEN_DURABILITY_COST)), Formatting.DARK_GRAY);
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.active").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.withering_aura", TooltipUtil.getScytheAbilityKeyText(Formatting.DARK_PURPLE)));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_aura.desc", Formatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            return;
        }

        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.duration_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.WitheringAura.DURATION_TICKS)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.WitheringAura.AURA_COOLDOWN_TICKS)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.WitheringAura.AURA_DURABILITY_COST)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.WitheringAura.WITHER_RADIUS)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.wither_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.WitheringAura.WITHER_TICKS)), Formatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_buff_radius", TooltipUtil.fmtNumber(ScytheBalance.WitheringAura.MINION_BUFF_RADIUS)), Formatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_buff_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.WitheringAura.MINION_BUFF_TICKS)), Formatting.DARK_GRAY);
    }

    private static void golden(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_scythe.desc", Formatting.GRAY, Formatting.ITALIC);

        if (!Screen.hasShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            return;
        }

        boolean alt = Screen.hasAltDown();

        if (alt) {
            tooltip.add(Text.empty());
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_scythe.base_stats", Formatting.DARK_GRAY);
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.passive").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.golden_scythe.passive").formatted(Formatting.GOLD));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_scythe.passive.desc", Formatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.looting_bonus", String.valueOf(ScytheBalance.Golden.PASSIVE_LOOTING_BONUS)),
                    Formatting.GOLD
            );
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.piglin_neutrality", Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.no_golden_scythe_stacking", Formatting.DARK_GRAY);
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.special").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.midas_touch").formatted(Formatting.GOLD));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.midas_touch.desc", Formatting.GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Golden.MIDAS_COOLDOWN_TICKS)), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.Golden.MIDAS_DURABILITY_COST)), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.reach_blocks", TooltipUtil.fmtNumber(ScytheBalance.Golden.MIDAS_REACH)), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.max_health_damage_percent", TooltipUtil.fmtPercentValue(ScytheBalance.Golden.MIDAS_DAMAGE_FRACTION)), Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.damage_clamp", TooltipUtil.fmtNumber(ScytheBalance.Golden.MIDAS_MIN_DAMAGE), TooltipUtil.fmtNumber(ScytheBalance.Golden.MIDAS_MAX_DAMAGE)), Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.mark_looting_bonus", String.valueOf(ScytheBalance.Golden.MARK_LOOTING_BONUS)), Formatting.GOLD);
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.active").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.golden_rain", TooltipUtil.getScytheAbilityKeyText(Formatting.GOLD)));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_rain.desc", Formatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            return;
        }

        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.GoldenRain.RADIUS)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.GoldenRain.COOLDOWN_TICKS)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.GoldenRain.DURABILITY_COST)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.mark_looting_bonus", String.valueOf(ScytheBalance.Golden.MARK_LOOTING_BONUS)), Formatting.GOLD);
    }

    private static void frozen(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.frozen_scythe.desc", Formatting.GRAY, Formatting.ITALIC);

        if (!Screen.hasShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            return;
        }

        boolean alt = Screen.hasAltDown();
        if (alt) {
            tooltip.add(Text.empty());
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.frozen_scythe.base_stats", Formatting.DARK_GRAY);
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.passive").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.frozen_scythe.passive").formatted(Formatting.AQUA));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.frozen_scythe.passive.desc", Formatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.powder_snow_walk", Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.frost_walker_level", ScytheBalance.Frozen.COLD_MASTER_FROST_WALKER_LEVEL),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.freezing_chance_percent",
                            TooltipUtil.fmtPercentValue(ScytheBalance.Frozen.COLD_MASTER_FREEZING_CHANCE)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.freezing_sec",
                            TooltipUtil.fmtSecondsValue(ScytheBalance.Frozen.COLD_MASTER_FREEZING_TICKS)
                    ),
                    Formatting.DARK_GRAY
            );
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.special").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.ice_spike").formatted(Formatting.AQUA));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.ice_spike.desc", Formatting.GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Frozen.ICE_SPIKE_COOLDOWN_TICKS)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.Frozen.ICE_SPIKE_DURABILITY_COST)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.damage", TooltipUtil.fmtNumber(ScytheBalance.IceSpike.HIT_DAMAGE)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.freezing_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.IceSpike.FREEZING_TICKS)),
                    Formatting.DARK_GRAY
            );
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.active").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(
                "tooltip.scythes.frozen_storm",
                TooltipUtil.getScytheAbilityKeyText(Formatting.AQUA)
        ));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.frozen_storm.desc", Formatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            return;
        }

        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.FrozenStorm.RADIUS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.FrozenStorm.COOLDOWN_TICKS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.FrozenStorm.DURABILITY_COST)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.freezing_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.FrozenStorm.FREEZING_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.slowness_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.FrozenStorm.SLOWNESS_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.weakness_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.FrozenStorm.WEAKNESS_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.ignores_pets_and_teammates", Formatting.DARK_GRAY);
    }

    private static void farmer(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.farmer_scythe.desc", Formatting.GRAY, Formatting.ITALIC);

        if (!Screen.hasShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            return;
        }

        boolean alt = Screen.hasAltDown();
        if (alt) {
            tooltip.add(Text.empty());
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.farmer_scythe.base_stats", Formatting.DARK_GRAY);
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.passive").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.farmer_scythe.passive").formatted(Formatting.GREEN));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.farmer_scythe.passive.desc", Formatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.double_drop_chance_percent", TooltipUtil.fmtPercentValue(ScytheBalance.Farmer.DOUBLE_DROP_CHANCE)), Formatting.GREEN);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.fortune_stacks", Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.reap_replants", Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.area_tilling", String.valueOf(ScytheBalance.Farmer.TILLING_DIAMETER), String.valueOf(ScytheBalance.Farmer.TILLING_DIAMETER)), Formatting.DARK_GRAY);
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.special").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.farmer_harvest").formatted(Formatting.GREEN));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.farmer_harvest.desc", Formatting.GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.Farmer.MASS_HARVEST_RADIUS)), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Farmer.MASS_HARVEST_COOLDOWN_TICKS)), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.Farmer.MASS_HARVEST_DURABILITY_COST)), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.direct_inventory", Formatting.DARK_GRAY);
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.active").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.farmer_growth", TooltipUtil.getScytheAbilityKeyText(Formatting.GREEN)));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.farmer_growth.desc", Formatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            return;
        }

        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ScytheBalance.Farmer.GROWTH_ACCELERATION_RADIUS)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ScytheBalance.Farmer.GROWTH_COOLDOWN_TICKS)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ScytheBalance.Farmer.GROWTH_DURABILITY_COST)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.bone_meal_cost", String.valueOf(ScytheBalance.Farmer.GROWTH_BONE_MEAL_COST)), Formatting.DARK_GREEN);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.growth_reduction_percent", TooltipUtil.fmtPercentValue(ScytheBalance.Farmer.GROWTH_REDUCTION_FRACTION)), Formatting.DARK_GREEN);
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.once_per_crop", Formatting.DARK_GRAY);
    }
}
