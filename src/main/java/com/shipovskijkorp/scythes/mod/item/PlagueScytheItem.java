package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ability.PlagueScytheAbility;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfig;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfigLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlagueScytheItem extends SwordItem {


    public PlagueScytheItem(Settings settings) {
        super(ToolMaterials.NETHERITE, 4, -2.8F, settings);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.plague_scythe.desc", Formatting.GRAY, Formatting.ITALIC);

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
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.plague_scythe.passive", Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.plague_scythe.passive.desc", Formatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(cfg.plagueAuraRadius)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.tick_rate_sec", TooltipUtil.fmtSecondsValue(cfg.plagueAuraTickRate)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.effect_duration_sec", TooltipUtil.fmtSecondsValue(cfg.plagueAuraEffectTicks)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.refresh_threshold_sec", TooltipUtil.fmtSecondsValue(cfg.plagueAuraRefreshThresholdTicks)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.multiplier_cap", TooltipUtil.fmtNumber(cfg.plagueMissingHealthMultiplierCap)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.base_damage", TooltipUtil.fmtNumber(cfg.plagueMissingHealthBaseDamage)),
                    Formatting.DARK_GRAY
            );
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.active").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.plague_scythe.active"));

        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.plague_scythe.active.desc", Formatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            return;
        }

        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(cfg.plagueActiveRadius)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.mode_duration_sec", TooltipUtil.fmtSecondsValue(cfg.plagueActiveTicks)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.tick_rate_sec", TooltipUtil.fmtSecondsValue(cfg.plagueActiveTickRate)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.damage", TooltipUtil.fmtNumber(cfg.plagueActiveDamage)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.debuff_duration_sec", TooltipUtil.fmtSecondsValue(cfg.plagueActiveDebuffTicks)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(cfg.plagueActiveCooldownTicks)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(cfg.bloodHarvestDurabilityCost)),
                Formatting.GRAY
        );
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld().isClient) {
            return super.postHit(stack, target, attacker);
        }

        if (attacker instanceof ServerPlayerEntity player) {
            float multiplier = PlagueScytheAbility.getDamageMultiplier(player);

            if (multiplier > 1.0f) {
                double baseDamage = Math.max(0.0, ScytheModConfigLoader.getConfig().plagueMissingHealthBaseDamage);
                float extraDamage = (float) ((multiplier - 1.0f) * baseDamage);
                if (extraDamage > 0.0f) {
                    target.damage(player.getDamageSources().indirectMagic(player, player), extraDamage);
                }
            }
        }

        return super.postHit(stack, target, attacker);
    }
}
