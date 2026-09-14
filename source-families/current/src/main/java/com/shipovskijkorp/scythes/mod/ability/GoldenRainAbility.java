package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class GoldenRainAbility {

    private GoldenRainAbility() {
    }

    public static final double RADIUS = 20.0D;
    public static final int COOLDOWN_TICKS = 20 * 150;
    public static final int DURABILITY_COST = 100;

    public static void tryActivate(ServerPlayer player) {
        InteractionHand hand = GoldenScytheItem.getHeldGoldenScytheHand(player);
        if (hand == null) {
            player.sendOverlayMessage(Component.translatable("message.scythes.golden_rain.no_scythe"));
            return;
        }

        ItemStack stack = player.getItemInHand(hand);

        int cooldownLeft = GoldenScytheCooldowns.getRainTicksLeft(player);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.golden_rain.cooldown", Math.max(1, cooldownLeft / 20)));
            return;
        }

        if (!GoldenScytheItem.hasEnoughDurability(stack, DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return;
        }

        int marked = GoldenLootMarkTracker.markAround(player, RADIUS);
        if (marked <= 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.golden_rain.no_targets"));
            return;
        }

        stack.hurtAndBreak(DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        GoldenScytheCooldowns.setRainCooldown(player, COOLDOWN_TICKS);
        ScytheAdvancementTracker.markGoldenActive(player);

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS,
                0.8F,
                1.45F
        );
        player.sendOverlayMessage(Component.translatable("message.scythes.golden_rain.success", marked));
    }
}
