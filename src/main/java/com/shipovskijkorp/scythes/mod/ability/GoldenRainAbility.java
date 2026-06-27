package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public final class GoldenRainAbility {

    private GoldenRainAbility() {
    }

    public static final double RADIUS = 20.0D;
    public static final int COOLDOWN_TICKS = 20 * 150;
    public static final int DURABILITY_COST = 200;

    public static void tryActivate(ServerPlayerEntity player) {
        Hand hand = GoldenScytheItem.getHeldGoldenScytheHand(player);
        if (hand == null) {
            player.sendMessage(Text.translatable("message.scythes.golden_rain.no_scythe"), true);
            return;
        }

        ItemStack stack = player.getStackInHand(hand);

        int cooldownLeft = GoldenScytheCooldowns.getRainTicksLeft(player);
        if (cooldownLeft > 0) {
            player.sendMessage(Text.translatable("message.scythes.golden_rain.cooldown", Math.max(1, cooldownLeft / 20)), true);
            return;
        }

        if (!GoldenScytheItem.hasEnoughDurability(stack, DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return;
        }

        int marked = GoldenLootMarkTracker.markAround(player, RADIUS);
        if (marked <= 0) {
            player.sendMessage(Text.translatable("message.scythes.golden_rain.no_targets"), true);
            return;
        }

        stack.damage(DURABILITY_COST, player, p -> p.sendToolBreakStatus(hand));
        GoldenScytheCooldowns.setRainCooldown(player, COOLDOWN_TICKS);
        ScytheAdvancementTracker.markGoldenActive(player);

        player.getWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS,
                0.8F,
                1.45F
        );
        player.sendMessage(Text.translatable("message.scythes.golden_rain.success", marked), true);
    }
}
