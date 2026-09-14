package com.shipovskijkorp.scythes.mod.network;

import com.shipovskijkorp.scythes.mod.ability.BloodHarvestAbility;
import com.shipovskijkorp.scythes.mod.ability.FrozenStormAbility;
import com.shipovskijkorp.scythes.mod.ability.GoldenRainAbility;
import com.shipovskijkorp.scythes.mod.ability.ToxicAuraAbility;
import com.shipovskijkorp.scythes.mod.ability.WitheringAuraAbility;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.item.FrozenScytheItem;
import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

public final class ScytheAbilityC2SPacket {

    private ScytheAbilityC2SPacket() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
                ModPackets.ScytheAbilityPayload.ID,
                (payload, context) -> context.server().execute(() -> handle(context.player()))
        );
    }

    private static void handle(ServerPlayerEntity player) {
        Hand hand = findAbilityHand(player);
        if (hand == null) {
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() instanceof BloodScytheItem) {
            BloodHarvestAbility.tryActivate(player);
        } else if (stack.getItem() instanceof ToxicScytheItem) {
            ToxicAuraAbility.tryActivate(player);
        } else if (stack.getItem() instanceof WitheringScytheItem) {
            WitheringAuraAbility.tryActivate(player);
        } else if (stack.getItem() instanceof GoldenScytheItem) {
            GoldenRainAbility.tryActivate(player);
        } else if (stack.getItem() instanceof FrozenScytheItem) {
            FrozenStormAbility.tryActivate(player);
        }
    }

    private static Hand findAbilityHand(ServerPlayerEntity player) {
        if (isAbilityScythe(player.getMainHandStack())) {
            return Hand.MAIN_HAND;
        }
        if (isAbilityScythe(player.getOffHandStack())) {
            return Hand.OFF_HAND;
        }
        return null;
    }

    private static boolean isAbilityScythe(ItemStack stack) {
        return stack.getItem() instanceof BloodScytheItem
                || stack.getItem() instanceof ToxicScytheItem
                || stack.getItem() instanceof WitheringScytheItem
                || stack.getItem() instanceof GoldenScytheItem
                || stack.getItem() instanceof FrozenScytheItem;
    }
}
