package com.shipovskijkorp.scythes.mod.network;

import com.shipovskijkorp.scythes.mod.ability.BloodHarvestAbility;
import com.shipovskijkorp.scythes.mod.ability.ToxicAuraAbility;
import com.shipovskijkorp.scythes.mod.ability.WitheringAuraAbility;
import com.shipovskijkorp.scythes.mod.ability.GoldenRainAbility;
import com.shipovskijkorp.scythes.mod.ability.FrozenStormAbility;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import com.shipovskijkorp.scythes.mod.item.FrozenScytheItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.ItemStack;

public class ScytheAbilityC2SPacket {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
                ModPackets.ScytheAbilityPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    var player = context.player();
                    if (!player.isAlive()) return;

                    ItemStack stack = player.getMainHandItem();
                    if (stack.getItem() instanceof BloodScytheItem) {
                        BloodHarvestAbility.tryActivate(player);
                        return;
                    }
                    if (stack.getItem() instanceof ToxicScytheItem) {
                        ToxicAuraAbility.tryActivate(player);
                        return;
                    }
                    if (stack.getItem() instanceof WitheringScytheItem) {
                        WitheringAuraAbility.tryActivate(player);
                        return;
                    }
                    if (stack.getItem() instanceof GoldenScytheItem) {
                        GoldenRainAbility.tryActivate(player);
                        return;
                    }
                    if (stack.getItem() instanceof FrozenScytheItem) {
                        FrozenStormAbility.tryActivate(player);
                        return;
                    }

                    stack = player.getOffhandItem();
                    if (stack.getItem() instanceof BloodScytheItem) {
                        BloodHarvestAbility.tryActivate(player);
                        return;
                    }
                    if (stack.getItem() instanceof ToxicScytheItem) {
                        ToxicAuraAbility.tryActivate(player);
                        return;
                    }
                    if (stack.getItem() instanceof WitheringScytheItem) {
                        WitheringAuraAbility.tryActivate(player);
                        return;
                    }
                    if (stack.getItem() instanceof GoldenScytheItem) {
                        GoldenRainAbility.tryActivate(player);
                        return;
                    }
                    if (stack.getItem() instanceof FrozenScytheItem) {
                        FrozenStormAbility.tryActivate(player);
                    }
                })
        );
    }
}
