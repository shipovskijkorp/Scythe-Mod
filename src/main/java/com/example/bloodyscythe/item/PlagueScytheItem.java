package com.example.bloodyscythe.item;

import com.example.bloodyscythe.ability.PlagueScytheAbility;
import com.example.bloodyscythe.client.TooltipUtil;
import com.example.bloodyscythe.config.BloodyScytheConfig;
import com.example.bloodyscythe.config.BloodyScytheConfigLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlagueScytheItem extends SwordItem {

    private static final float BASE_WEAPON_DAMAGE = 8.0f;

    public PlagueScytheItem(ToolMaterial material, Settings settings) {
        super(material, 8, -3.2f, settings);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        TooltipUtil.addWrapped(tooltip, "tooltip.bloodyscythe.plague_scythe.desc", Formatting.GRAY, Formatting.ITALIC);

        if (!Screen.hasShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            return;
        }

        boolean alt = Screen.hasAltDown();
        BloodyScytheConfig cfg = (BloodyScytheConfigLoader.CONFIG != null)
                ? BloodyScytheConfigLoader.CONFIG
                : new BloodyScytheConfig();

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.bloodyscythe.section.passive").formatted(Formatting.GRAY));

        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.bloodyscythe.plague_scythe.passive", Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, "tooltip.bloodyscythe.plague_scythe.passive.desc", Formatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.bloodyscythe.stat.radius_blocks", TooltipUtil.fmtNumber(cfg.plagueAuraRadius)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.bloodyscythe.stat.tick_rate_sec", TooltipUtil.fmtSecondsValue(cfg.plagueAuraTickRate)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.bloodyscythe.stat.effect_duration_sec", TooltipUtil.fmtSecondsValue(cfg.plagueAuraEffectTicks)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.bloodyscythe.stat.refresh_threshold_sec", TooltipUtil.fmtSecondsValue(cfg.plagueAuraRefreshThresholdTicks)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.bloodyscythe.stat.multiplier_cap", TooltipUtil.fmtNumber(cfg.plagueMissingHealthMultiplierCap)),
                    Formatting.DARK_GRAY
            );
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.bloodyscythe.section.active").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.bloodyscythe.plague_scythe.active"));

        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.bloodyscythe.plague_scythe.active.desc", Formatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            return;
        }

        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.bloodyscythe.stat.radius_blocks", TooltipUtil.fmtNumber(cfg.plagueActiveRadius)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.bloodyscythe.stat.mode_duration_sec", TooltipUtil.fmtSecondsValue(cfg.plagueActiveTicks)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.bloodyscythe.stat.tick_rate_sec", TooltipUtil.fmtSecondsValue(cfg.plagueActiveTickRate)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.bloodyscythe.stat.damage", TooltipUtil.fmtNumber(cfg.plagueActiveDamage)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.bloodyscythe.stat.debuff_duration_sec", TooltipUtil.fmtSecondsValue(cfg.plagueActiveDebuffTicks)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.bloodyscythe.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(cfg.plagueActiveCooldownTicks)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.bloodyscythe.stat.durability_cost", String.valueOf(cfg.bloodHarvestDurabilityCost)),
                Formatting.GRAY
        );
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        // Важно: дополнительный урон наносим только на сервере
        if (attacker.getWorld().isClient) {
            return super.postHit(stack, target, attacker);
        }

        if (attacker instanceof ServerPlayerEntity player) {

            float multiplier = PlagueScytheAbility.getDamageMultiplier(player);

            // защита от странных значений
            if (multiplier > 1.0f) {
                multiplier = Math.min(multiplier, 2.0f); // кап +100% (как в описании)

                float extraDamage = (multiplier - 1.0f) * BASE_WEAPON_DAMAGE;

                // Урон с “атакующим”, чтобы киллы/триггеры нормально засчитывались
                target.damage(
                        player.getDamageSources().indirectMagic(player, player),
                        extraDamage
                );
            }
        }

        return super.postHit(stack, target, attacker);
    }
}
