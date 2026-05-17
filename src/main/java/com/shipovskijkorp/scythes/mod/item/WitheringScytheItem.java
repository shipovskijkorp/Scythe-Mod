package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ability.WitheringScytheTracker;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfig;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfigLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WitheringScytheItem extends SwordItem {

    public WitheringScytheItem(Settings settings) {
        super(ToolMaterials.NETHERITE, 4, -2.8F, settings);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_scythe.desc", Formatting.GRAY, Formatting.ITALIC);

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
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_scythe.passive", Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_scythe.passive.desc", Formatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(cfg.witheringAuraRadius)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.tick_rate_sec", TooltipUtil.fmtSecondsValue(cfg.witheringAuraTickRate)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.wither_sec", TooltipUtil.fmtSecondsValue(cfg.witheringAuraWitherTicks)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.slowness_sec", TooltipUtil.fmtSecondsValue(cfg.witheringAuraSlownessTicks)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.refresh_threshold_sec", TooltipUtil.fmtSecondsValue(cfg.witheringAuraRefreshThresholdTicks)),
                    Formatting.DARK_GRAY
            );
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.active").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.withering_scythe.active"));

        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_scythe.active.desc", Formatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            return;
        }

        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(cfg.witheringActiveRadius)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.mode_duration_sec", TooltipUtil.fmtSecondsValue(cfg.witheringActiveTicks)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.tick_rate_sec", TooltipUtil.fmtSecondsValue(cfg.witheringActiveTickRate)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.damage", TooltipUtil.fmtNumber(cfg.witheringActiveDamage)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.debuff_duration_sec", TooltipUtil.fmtSecondsValue(cfg.witheringDebuffTicks)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(cfg.witheringActiveCooldownTicks)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(cfg.bloodHarvestDurabilityCost)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.armor_ignore_percent", TooltipUtil.fmtPercentValue(cfg.witheringArmorIgnoreFraction)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.armor_ignore_base_damage", TooltipUtil.fmtNumber(cfg.witheringArmorIgnoreBaseDamage)),
                Formatting.DARK_GRAY
        );
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayerEntity player)) {
            return super.postHit(stack, target, attacker);
        }

        if (target instanceof ServerPlayerEntity victim && player.isTeammate(victim)) {
            return super.postHit(stack, target, attacker);
        }

        if (!WitheringScytheTracker.isActive(player)) {
            return super.postHit(stack, target, attacker);
        }

        if (!target.isAlive()) {
            return super.postHit(stack, target, attacker);
        }

        double baseDamageOnly = 8.0;
        double ignoreArmorFraction = 0.20;

        if (ScytheModConfigLoader.CONFIG != null) {
            baseDamageOnly = Math.max(0.0, ScytheModConfigLoader.CONFIG.witheringArmorIgnoreBaseDamage);
            ignoreArmorFraction = ScytheModConfigLoader.CONFIG.witheringArmorIgnoreFraction;
        }

        ignoreArmorFraction = Math.max(0.0, Math.min(1.0, ignoreArmorFraction));
        if (baseDamageOnly <= 0.0 || ignoreArmorFraction <= 0.0) {
            return super.postHit(stack, target, attacker);
        }

        float armor = target.getArmor();
        float toughness = (float) target.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);

        float armorFactor = Math.min(
                20.0f,
                Math.max(armor / 5.0f, armor - (float) baseDamageOnly / (2.0f + toughness / 4.0f))
        ) / 25.0f;

        float reducedByArmor = (float) baseDamageOnly * armorFactor;
        float extraDamage = (float) (reducedByArmor * ignoreArmorFraction);

        if (extraDamage > 0.01f) {
            target.damage(player.getDamageSources().indirectMagic(player, player), extraDamage);
        }

        return super.postHit(stack, target, attacker);
    }
}
