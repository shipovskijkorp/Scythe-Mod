package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import com.shipovskijkorp.scythes.mod.platform.HudSync;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public final class ToxicAuraAbility {

    private ToxicAuraAbility() {
    }

    public static void tryActivate(ServerPlayerEntity player) {
        Hand hand = getHeldToxicScytheHand(player);
        if (hand == null) {
            player.sendMessage(Text.translatable("message.scythes.toxic_aura.no_scythe"), true);
            return;
        }

        if (ToxicAuraTracker.isActive(player)) {
            player.sendMessage(Text.translatable("message.scythes.toxic_aura.already_active"), true);
            return;
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.TOXIC_AURA);
        if (cooldownLeft > 0) {
            player.sendMessage(Text.translatable("message.scythes.toxic_aura.cooldown", Math.max(1, cooldownLeft / 20)), true);
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (!ToxicScytheItem.hasEnoughDurability(stack, ScytheBalance.ToxicAura.AURA_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return;
        }

        if (ScytheBalance.ToxicAura.AURA_DURABILITY_COST > 0) {
            stack.damage(ScytheBalance.ToxicAura.AURA_DURABILITY_COST, player, p -> p.sendToolBreakStatus(hand));
        }

        int auraTicks = ToxicAuraTracker.start(player, stack);
        HudSync.start(player, HudTransport.Timer.TOXIC_AURA, auraTicks);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.TOXIC_AURA, ScytheBalance.ToxicAura.AURA_COOLDOWN_TICKS);
        ScytheAdvancementTracker.markToxicActive(player);

        player.getWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                SoundCategory.PLAYERS,
                0.8F,
                0.65F
        );
        player.sendMessage(Text.translatable("message.scythes.toxic_aura.success"), true);
    }

    private static Hand getHeldToxicScytheHand(ServerPlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof ToxicScytheItem) return Hand.MAIN_HAND;
        if (player.getOffHandStack().getItem() instanceof ToxicScytheItem) return Hand.OFF_HAND;
        return null;
    }
}
