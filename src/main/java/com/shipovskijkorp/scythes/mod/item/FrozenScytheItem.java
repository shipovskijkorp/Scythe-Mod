package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.FrozenScytheCooldowns;
import com.shipovskijkorp.scythes.mod.ability.FrozenStormAbility;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.entity.IceSpikeEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.FrostWalkerEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
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

public class FrozenScytheItem extends SwordItem {

    public static final int ICE_SPIKE_DURABILITY_COST = 30;
    public static final int ICE_SPIKE_COOLDOWN_TICKS = 20 * 20;
    public static final float ICE_SPIKE_SPEED = 3.0F;

    public static final double COLD_MASTER_FREEZING_CHANCE = 0.33D;
    public static final int COLD_MASTER_FREEZING_TICKS = 20;
    public static final int COLD_MASTER_FROST_WALKER_LEVEL = 2;

    public FrozenScytheItem(Settings settings) {
        super(ToolMaterials.NETHERITE, 4, -2.8F, settings);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
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
                    Text.translatable("tooltip.scythes.stat.frost_walker_level", COLD_MASTER_FROST_WALKER_LEVEL),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.freezing_chance_percent",
                            TooltipUtil.fmtPercentValue(COLD_MASTER_FREEZING_CHANCE)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.freezing_sec",
                            TooltipUtil.fmtSecondsValue(COLD_MASTER_FREEZING_TICKS)
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
                    Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ICE_SPIKE_COOLDOWN_TICKS)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ICE_SPIKE_DURABILITY_COST)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.damage", TooltipUtil.fmtNumber(IceSpikeEntity.HIT_DAMAGE)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.freezing_sec", TooltipUtil.fmtSecondsValue(IceSpikeEntity.FREEZING_TICKS)),
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
                Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(FrozenStormAbility.RADIUS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(FrozenStormAbility.COOLDOWN_TICKS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(FrozenStormAbility.DURABILITY_COST)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.freezing_sec", TooltipUtil.fmtSecondsValue(FrozenStormAbility.FREEZING_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.slowness_sec", TooltipUtil.fmtSecondsValue(FrozenStormAbility.SLOWNESS_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.weakness_sec", TooltipUtil.fmtSecondsValue(FrozenStormAbility.WEAKNESS_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.ignores_pets_and_teammates", Formatting.DARK_GRAY);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (world.isClient || !(entity instanceof PlayerEntity player)) return;
        if (!isTheOnlyHeldStackToTick(player, stack)) return;

        FrostWalkerEnchantment.freezeWater(
                player,
                world,
                player.getBlockPos(),
                COLD_MASTER_FROST_WALKER_LEVEL
        );
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.postHit(stack, target, attacker);

        if (!target.getWorld().isClient
                && target.isAlive()
                && attacker.getRandom().nextDouble() < COLD_MASTER_FREEZING_CHANCE) {
            target.addStatusEffect(new StatusEffectInstance(
                    ScytheMod.FREEZING,
                    COLD_MASTER_FREEZING_TICKS,
                    0
            ));
            if (attacker instanceof ServerPlayerEntity player) {
                ScytheAdvancementTracker.markFrozenPassive(player);
            }
        }

        return result;
    }

    public static boolean isInPlayerInventory(PlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (player.getInventory().getStack(slot).getItem() instanceof FrozenScytheItem) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static Hand getHeldFrozenScytheHand(PlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof FrozenScytheItem) {
            return Hand.MAIN_HAND;
        }
        if (player.getOffHandStack().getItem() instanceof FrozenScytheItem) {
            return Hand.OFF_HAND;
        }
        return null;
    }

    private static boolean isTheOnlyHeldStackToTick(PlayerEntity player, ItemStack stack) {
        if (player.getMainHandStack() == stack) return true;
        return !(player.getMainHandStack().getItem() instanceof FrozenScytheItem)
                && player.getOffHandStack() == stack;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }

        int cooldownLeft = FrozenScytheCooldowns.getIceSpikeTicksLeft(player);
        if (cooldownLeft > 0) {
            player.sendMessage(
                    Text.translatable("message.scythes.ice_spike.cooldown", Math.max(1, (cooldownLeft + 19) / 20)),
                    true
            );
            return TypedActionResult.fail(stack);
        }

        if (!hasEnoughDurability(stack, ICE_SPIKE_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return TypedActionResult.fail(stack);
        }

        IceSpikeEntity spike = new IceSpikeEntity(world, player);
        spike.setPosition(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        spike.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, ICE_SPIKE_SPEED, 0.0F);
        world.spawnEntity(spike);

        stack.damage(ICE_SPIKE_DURABILITY_COST, player, p -> p.sendToolBreakStatus(hand));
        FrozenScytheCooldowns.setIceSpikeCooldown(player, ICE_SPIKE_COOLDOWN_TICKS);

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_ARROW_SHOOT,
                SoundCategory.PLAYERS,
                1.0F,
                0.75F + player.getRandom().nextFloat() * 0.15F
        );

        return TypedActionResult.success(stack);
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageable()) return false;
        return stack.getMaxDamage() - stack.getDamage() >= cost;
    }
}
