package com.shipovskijkorp.scythes.mod.network;

import com.shipovskijkorp.scythes.mod.ability.BloodHarvestAbility;
import com.shipovskijkorp.scythes.mod.ability.ToxicAuraAbility;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;

public class ScytheAbilityC2SPacket {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
                ModPackets.SCYTHE_ABILITY_C2S,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    if (!player.isAlive()) return;

                    ItemStack stack = player.getMainHandStack();
                    if (stack.getItem() instanceof BloodScytheItem) {
                        BloodHarvestAbility.tryActivate(player);
                        return;
                    }
                    if (stack.getItem() instanceof ToxicScytheItem) {
                        ToxicAuraAbility.tryActivate(player);
                        return;
                    }

                    stack = player.getOffHandStack();
                    if (stack.getItem() instanceof BloodScytheItem) {
                        BloodHarvestAbility.tryActivate(player);
                        return;
                    }
                    if (stack.getItem() instanceof ToxicScytheItem) {
                        ToxicAuraAbility.tryActivate(player);
                    }
                })
        );
    }
}
