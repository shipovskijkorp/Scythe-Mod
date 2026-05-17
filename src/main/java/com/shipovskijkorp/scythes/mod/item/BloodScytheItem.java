package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfig;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfigLoader;
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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BloodScytheItem extends SwordItem {

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
        ScytheModConfig cfg = (ScytheModConfigLoader.CONFIG != null)
                ? ScytheModConfigLoader.CONFIG
                : new ScytheModConfig();

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
                            TooltipUtil.fmtPercentValue(cfg.bloodBleedingChance)
                    ),
                    Formatting.DARK_RED
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.bleed_base_sec",
                            TooltipUtil.fmtSecondsValue(cfg.bloodBleedingBaseDurationTicks)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.bleed_extend_sec",
                            TooltipUtil.fmtSecondsValue(cfg.bloodBleedingExtendTicks)
                    ),
                    Formatting.DARK_GRAY
            );
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.active").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.blood_harvest"));

        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.blood_harvest.desc", Formatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            return;
        }

        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(cfg.bloodHarvestRadius)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(cfg.bloodHarvestCooldownTicks)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(cfg.bloodHarvestDurabilityCost)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.kill_window_sec", TooltipUtil.fmtSecondsValue(cfg.bloodHarvestKillWindowTicks)),
                Formatting.GRAY
        );

        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.slowness_sec", TooltipUtil.fmtSecondsValue(cfg.bloodHarvestSlownessTicks)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.blindness_sec", TooltipUtil.fmtSecondsValue(cfg.bloodHarvestBlindnessTicks)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.weakness_sec", TooltipUtil.fmtSecondsValue(cfg.bloodHarvestWeaknessTicks)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.glowing_sec", TooltipUtil.fmtSecondsValue(cfg.bloodHarvestGlowingTicks)),
                Formatting.DARK_GRAY
        );
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld().isClient) {
            return super.postHit(stack, target, attacker);
        }

        double chance = 0.35;
        int baseTicks = 40;
        int extendTicks = 40;

        if (ScytheModConfigLoader.CONFIG != null) {
            chance = ScytheModConfigLoader.CONFIG.bloodBleedingChance;
            baseTicks = ScytheModConfigLoader.CONFIG.bloodBleedingBaseDurationTicks;
            extendTicks = ScytheModConfigLoader.CONFIG.bloodBleedingExtendTicks;
        }

        chance = Math.max(0.0, Math.min(1.0, chance));
        baseTicks = Math.max(1, baseTicks);
        extendTicks = Math.max(1, extendTicks);

        if (attacker.getRandom().nextDouble() > chance) {
            return super.postHit(stack, target, attacker);
        }

        int spikedLevel = EnchantmentHelper.getLevel(ScytheMod.SPIKED_BLADE, stack);
        int duration = baseTicks * (1 + Math.max(0, spikedLevel));

        StatusEffectInstance current = target.getStatusEffect(ScytheMod.BLEEDING);
        if (current != null) {
            duration = Math.max(duration, current.getDuration() + extendTicks);
        }

        target.addStatusEffect(new StatusEffectInstance(
                ScytheMod.BLEEDING,
                duration,
                0,
                false,
                true
        ));

        return super.postHit(stack, target, attacker);
    }
}
