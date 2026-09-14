package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.WitheringAuraAbility;
import com.shipovskijkorp.scythes.mod.ability.WitheringAuraTracker;
import com.shipovskijkorp.scythes.mod.ability.WitheringMinionManager;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
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

public class WitheringScytheItem extends SwordItem {

    private static final String SOULS_KEY = "Souls";

    public static final double WITHER_CHANCE = 0.30D;
    public static final int WITHER_TICKS = 20 * 10;
    public static final int WITHER_AMPLIFIER = 1;

    public static final int SOULS_PER_MOB_KILL = 1;
    public static final int SOULS_PER_PLAYER_KILL = 12;

    public static final int MINION_DURABILITY_COST = 10;
    public static final int MINION_SOUL_COST = 6;
    public static final int MAX_MINIONS = 6;

    public WitheringScytheItem(Settings settings) {
        super(ToolMaterials.NETHERITE, settings.attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 4, -2.8F)));
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
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
        tooltip.add(Text.translatable("tooltip.scythes.withering_scythe.passive", getSouls(stack)).formatted(Formatting.DARK_PURPLE));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_scythe.passive.desc", Formatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.souls", String.valueOf(getSouls(stack))),
                    Formatting.DARK_PURPLE
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.wither_chance_percent", TooltipUtil.fmtPercentValue(WITHER_CHANCE)),
                    Formatting.DARK_PURPLE
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.wither_sec", TooltipUtil.fmtSecondsValue(WITHER_TICKS)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.souls_per_mob", String.valueOf(SOULS_PER_MOB_KILL)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.souls_per_player", String.valueOf(SOULS_PER_PLAYER_KILL)),
                    Formatting.DARK_GRAY
            );
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.special").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.withering_minion").formatted(Formatting.DARK_PURPLE));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_minion.desc", Formatting.GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(MINION_DURABILITY_COST)), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.soul_cost", String.valueOf(MINION_SOUL_COST)), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_cap", String.valueOf(getMaxMinions(context.getRegistryLookup(), stack))), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_health", TooltipUtil.fmtNumber(WitheringMinionEntity.MAX_HEALTH)), Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_armor", TooltipUtil.fmtNumber(WitheringMinionEntity.ARMOR)), Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_lifetime_sec", TooltipUtil.fmtSecondsValue(WitheringMinionEntity.LIFETIME_TICKS)), Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_regen_delay_sec", TooltipUtil.fmtSecondsValue(WitheringMinionEntity.REGEN_IDLE_TICKS)), Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_regen_cost", String.valueOf(WitheringMinionEntity.REGEN_DURABILITY_COST)), Formatting.DARK_GRAY);
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.active").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.withering_aura", TooltipUtil.getScytheAbilityKeyText(Formatting.DARK_PURPLE)));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.withering_aura.desc", Formatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            return;
        }

        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.duration_sec", TooltipUtil.fmtSecondsValue(WitheringAuraTracker.DURATION_TICKS)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(WitheringAuraAbility.AURA_COOLDOWN_TICKS)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(WitheringAuraAbility.AURA_DURABILITY_COST)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(WitheringAuraTracker.WITHER_RADIUS)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.wither_sec", TooltipUtil.fmtSecondsValue(WitheringAuraTracker.WITHER_TICKS)), Formatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_buff_radius", TooltipUtil.fmtNumber(WitheringAuraTracker.MINION_BUFF_RADIUS)), Formatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.minion_buff_sec", TooltipUtil.fmtSecondsValue(WitheringAuraTracker.MINION_BUFF_TICKS)), Formatting.DARK_GRAY);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient && attacker.getRandom().nextDouble() < WITHER_CHANCE) {
            target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    StatusEffects.WITHER,
                    WITHER_TICKS,
                    WITHER_AMPLIFIER,
                    false,
                    true,
                    true
            ));

            if (attacker instanceof ServerPlayerEntity player) {
                ScytheAdvancementTracker.markWitheringPassive(player);
                DamageAttributionTracker.recordWithering(target, player, WITHER_TICKS);
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

        if (player.isSneaking()) {
            int dismissed = WitheringMinionManager.dismissMinions(player);
            player.sendMessage(Text.translatable("message.scythes.withering_minion.dismissed", dismissed), true);
            return TypedActionResult.success(stack);
        }

        int ownedMinions = WitheringMinionManager.countMinions(player);
        int maxMinions = getMaxMinions(player, stack);
        if (ownedMinions >= maxMinions) {
            player.sendMessage(Text.translatable("message.scythes.withering_minion.cap", maxMinions), true);
            return TypedActionResult.fail(stack);
        }

        if (getSouls(stack) < MINION_SOUL_COST) {
            player.sendMessage(Text.translatable("message.scythes.withering_minion.no_souls", MINION_SOUL_COST), true);
            return TypedActionResult.fail(stack);
        }

        if (!hasEnoughDurability(stack, MINION_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return TypedActionResult.fail(stack);
        }

        if (!WitheringMinionManager.spawnMinion(player)) {
            player.sendMessage(Text.translatable("message.scythes.withering_minion.no_space"), true);
            return TypedActionResult.fail(stack);
        }

        spendSouls(stack, MINION_SOUL_COST);
        stack.damage(MINION_DURABILITY_COST, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheAdvancementTracker.markWitheringSpecial(player);
        ScytheAdvancementTracker.tryGrantSuperNecromancer(player, WitheringMinionManager.countMinions(player));
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT, SoundCategory.PLAYERS, 0.8F, 0.75F);
        player.sendMessage(Text.translatable("message.scythes.withering_minion.spawned", getSouls(stack)), true);
        return TypedActionResult.success(stack);
    }

    public static int getSouls(ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = component.copyNbt();
        return Math.max(0, nbt.getInt(SOULS_KEY));
    }

    public static void addSouls(ItemStack stack, int amount) {
        if (stack.isEmpty() || !(stack.getItem() instanceof WitheringScytheItem) || amount <= 0) return;
        setSouls(stack, getSouls(stack) + amount);
    }

    public static boolean spendSouls(ItemStack stack, int amount) {
        if (amount <= 0) return true;
        int current = getSouls(stack);
        if (current < amount) return false;
        setSouls(stack, current - amount);
        return true;
    }

    private static void setSouls(ItemStack stack, int souls) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = component.copyNbt();
        nbt.putInt(SOULS_KEY, Math.max(0, souls));
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }


    public static int getMaxMinions(LivingEntity holder, ItemStack stack) {
        return getMaxMinions(holder.getRegistryManager(), stack);
    }

    public static int getMaxMinions(RegistryWrapper.WrapperLookup registries, ItemStack stack) {
        int enchantmentLevel = registries
                .getWrapperOrThrow(RegistryKeys.ENCHANTMENT)
                .getOptional(ScytheMod.ADDITIONAL_SLOT)
                .map(enchantment -> EnchantmentHelper.getLevel(enchantment, stack))
                .orElse(0);
        return MAX_MINIONS + getAdditionalMinionSlots(enchantmentLevel);
    }

    private static int getAdditionalMinionSlots(int level) {
        if (level <= 0) return 0;
        if (level == 1) return 1;
        if (level == 2) return 2;
        return 4;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageable()) return false;
        return stack.getMaxDamage() - stack.getDamage() >= cost;
    }
}
