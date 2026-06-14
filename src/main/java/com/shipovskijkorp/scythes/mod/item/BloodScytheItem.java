package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.BloodHarvestAbility;
import com.shipovskijkorp.scythes.mod.ability.BloodHarvestTracker;
import com.shipovskijkorp.scythes.mod.ability.BloodScytheVampirism;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.effect.BleedingEffect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BloodScytheItem extends SwordItem {

    public static final double BLEEDING_CHANCE = 0.40D;
    public static final double DEFENSE_PIERCE_CHANCE = 0.20D;
    public static final double DEFENSE_PIERCE_MITIGATION_IGNORED = 0.50D;
    public static final int BLEEDING_BASE_DURATION_TICKS = 20 * 2;
    public static final int BLEEDING_EXTEND_TICKS = 20 * 2;

    public BloodScytheItem(Settings settings) {
        super(ToolMaterials.NETHERITE, 4, -2.8F, settings);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
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
                            TooltipUtil.fmtPercentValue(BLEEDING_CHANCE)
                    ),
                    Formatting.DARK_RED
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.bleed_base_sec",
                            TooltipUtil.fmtSecondsValue(BLEEDING_BASE_DURATION_TICKS)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.bleed_extend_sec",
                            TooltipUtil.fmtSecondsValue(BLEEDING_EXTEND_TICKS)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.bleeding_damage_per_second",
                            TooltipUtil.fmtNumber(BleedingEffect.DAMAGE_PER_SECOND)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.vampirism_chance_percent",
                            TooltipUtil.fmtPercentValue(BloodScytheVampirism.VAMPIRISM_CHANCE)
                    ),
                    Formatting.DARK_RED
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.vampirism_heal_percent",
                            TooltipUtil.fmtPercentValue(BloodScytheVampirism.HEAL_FRACTION)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.vampirism_cooldown_sec",
                            TooltipUtil.fmtSecondsValue(BloodScytheVampirism.COOLDOWN_TICKS)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.defense_pierce",
                            TooltipUtil.fmtPercentValue(DEFENSE_PIERCE_CHANCE),
                            TooltipUtil.fmtPercentValue(DEFENSE_PIERCE_MITIGATION_IGNORED)
                    ),
                    Formatting.DARK_RED
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
                Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(BloodHarvestAbility.RADIUS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.COOLDOWN_TICKS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(BloodHarvestAbility.DURABILITY_COST)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.kill_window_sec", TooltipUtil.fmtSecondsValue(BloodHarvestTracker.KILL_WINDOW_TICKS)),
                Formatting.GRAY
        );

        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.slowness_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.SLOWNESS_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.blindness_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.BLINDNESS_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.weakness_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.WEAKNESS_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.glowing_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.GLOWING_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.success_buffs_sec", TooltipUtil.fmtSecondsValue(BloodHarvestTracker.SUCCESS_BUFF_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.failure_debuffs_sec", TooltipUtil.fmtSecondsValue(BloodHarvestTracker.FAILURE_DEBUFF_TICKS)),
                Formatting.DARK_GRAY
        );
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld().isClient) {
            return super.postHit(stack, target, attacker);
        }

        if (attacker.getRandom().nextDouble() > BLEEDING_CHANCE) {
            return super.postHit(stack, target, attacker);
        }

        int spikedLevel = EnchantmentHelper.getLevel(ScytheMod.SPIKED_BLADE, stack);
        int duration = BLEEDING_BASE_DURATION_TICKS * (1 + Math.max(0, spikedLevel));

        StatusEffectInstance current = target.getStatusEffect(ScytheMod.BLEEDING);
        if (current != null) {
            duration = Math.max(duration, current.getDuration() + BLEEDING_EXTEND_TICKS);
        }

        target.addStatusEffect(new StatusEffectInstance(
                ScytheMod.BLEEDING,
                duration,
                0,
                false,
                true
        ));

        if (attacker instanceof ServerPlayerEntity player) {
            DamageAttributionTracker.recordBleeding(target, player, duration);
        }

        return super.postHit(stack, target, attacker);
    }
}
