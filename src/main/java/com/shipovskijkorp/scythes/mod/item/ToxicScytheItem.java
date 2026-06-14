package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ability.ToxicAuraAbility;
import com.shipovskijkorp.scythes.mod.ability.ToxicAuraTracker;
import com.shipovskijkorp.scythes.mod.ability.ToxicScytheCooldowns;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.entity.ToxicOrbEntity;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ToxicScytheItem extends SwordItem {

    public static final double PASSIVE_ARMOR_DAMAGE_CHANCE = 0.30D;
    public static final int PASSIVE_ARMOR_DAMAGE = 20;
    public static final double PASSIVE_POISON_CHANCE = 0.25D;
    public static final int PASSIVE_POISON_TICKS = 20 * 2;
    public static final int PASSIVE_POISON_AMPLIFIER = 1;

    public static final int ORB_COOLDOWN_TICKS = 20 * 5;
    public static final int ORB_DURABILITY_COST = 20;
    public static final float ORB_SPEED = 1.5F;

    public ToxicScytheItem(Settings settings) {
        super(ToolMaterials.NETHERITE, 4, -2.8F, settings);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
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
                            TooltipUtil.fmtPercentValue(PASSIVE_ARMOR_DAMAGE_CHANCE)
                    ),
                    Formatting.GREEN
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.armor_damage_full_set", String.valueOf(PASSIVE_ARMOR_DAMAGE)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.poison_chance_percent",
                            TooltipUtil.fmtPercentValue(PASSIVE_POISON_CHANCE)
                    ),
                    Formatting.GREEN
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.poison_sec", TooltipUtil.fmtSecondsValue(PASSIVE_POISON_TICKS)),
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
                    Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ORB_COOLDOWN_TICKS)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ORB_DURABILITY_COST)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ToxicOrbEntity.DAMAGE_RADIUS)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.pure_damage", TooltipUtil.fmtNumber(ToxicOrbEntity.PURE_DAMAGE)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.poison_sec", TooltipUtil.fmtSecondsValue(ToxicOrbEntity.POISON_TICKS)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.armor_damage_full_set", String.valueOf(ToxicOrbEntity.ARMOR_DAMAGE)),
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
                Text.translatable("tooltip.scythes.stat.duration_sec", TooltipUtil.fmtSecondsValue(ToxicAuraTracker.DURATION_TICKS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ToxicAuraAbility.AURA_COOLDOWN_TICKS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ToxicAuraAbility.AURA_DURABILITY_COST)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(ToxicAuraTracker.RADIUS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.poison_sec", TooltipUtil.fmtSecondsValue(ToxicAuraTracker.POISON_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.armor_damage_per_tick", String.valueOf(ToxicAuraTracker.ARMOR_DAMAGE_PER_TICK)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable(
                        "tooltip.scythes.stat.pure_damage_chance_percent",
                        TooltipUtil.fmtPercentValue(ToxicAuraTracker.PURE_DAMAGE_CHANCE)
                ),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.pure_damage", TooltipUtil.fmtNumber(ToxicAuraTracker.PURE_DAMAGE)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable(
                        "tooltip.scythes.stat.nausea_chance_percent",
                        TooltipUtil.fmtPercentValue(ToxicAuraTracker.NAUSEA_CHANCE)
                ),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.nausea_sec", TooltipUtil.fmtSecondsValue(ToxicAuraTracker.NAUSEA_TICKS)),
                Formatting.DARK_GRAY
        );
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient) {
            if (attacker.getRandom().nextDouble() < PASSIVE_ARMOR_DAMAGE_CHANCE) {
                ScytheCombatUtil.damageArmorSet(target, PASSIVE_ARMOR_DAMAGE);
            }

            if (attacker.getRandom().nextDouble() < PASSIVE_POISON_CHANCE) {
                ScytheCombatUtil.refreshStatus(target, StatusEffects.POISON, PASSIVE_POISON_TICKS, PASSIVE_POISON_AMPLIFIER);
            }
        }

        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }

        int cooldownLeft = ToxicScytheCooldowns.getOrbTicksLeft(player);
        if (cooldownLeft > 0) {
            player.sendMessage(Text.translatable("message.scythes.toxic_orb.cooldown", Math.max(1, cooldownLeft / 20)), true);
            return TypedActionResult.fail(stack);
        }

        if (!hasEnoughDurability(stack, ORB_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return TypedActionResult.fail(stack);
        }

        ToxicOrbEntity orb = new ToxicOrbEntity(world, player);
        orb.refreshPositionAndAngles(player.getX(), player.getEyeY() - 0.15D, player.getZ(), player.getYaw(), player.getPitch());
        orb.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, ORB_SPEED, 0.0F);
        world.spawnEntity(orb);

        stack.damage(ORB_DURABILITY_COST, player, p -> p.sendToolBreakStatus(hand));
        ToxicScytheCooldowns.setOrbCooldown(player, ORB_COOLDOWN_TICKS);

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_BLAZE_SHOOT,
                SoundCategory.PLAYERS,
                0.75F,
                0.85F + player.getRandom().nextFloat() * 0.3F
        );

        return TypedActionResult.success(stack);
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageable()) return false;
        return stack.getMaxDamage() - stack.getDamage() >= cost;
    }
}
