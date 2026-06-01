package com.shipovskijkorp.scythes.mod.network;

import com.shipovskijkorp.scythes.mod.ability.BloodHarvestAbility;
import com.shipovskijkorp.scythes.mod.ability.PlagueScytheAbility;
import com.shipovskijkorp.scythes.mod.ability.WitheringScytheAbility;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.item.PlagueScytheItem;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;

public class ScytheAbilityC2SPacket {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
                ModPackets.SCYTHE_ABILITY_C2S,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    if (!player.isAlive()) return;

                    ItemStack stack = player.getMainHandStack();
                    if (!(stack.getItem() instanceof BloodScytheItem)
                            && !(stack.getItem() instanceof PlagueScytheItem)
                            && !(stack.getItem() instanceof WitheringScytheItem)) {
                        stack = player.getOffHandStack();
                    }

                    if (stack.getItem() instanceof BloodScytheItem) {
                        BloodHarvestAbility.tryActivate(player);
                        return;
                    }

                    if (stack.getItem() instanceof PlagueScytheItem) {
                        PlagueScytheAbility.tryActivate(player);
                        return;
                    }

                    if (stack.getItem() instanceof WitheringScytheItem) {
                        WitheringScytheAbility.tryActivate(player);
                    }
                })
        );
    }
}
