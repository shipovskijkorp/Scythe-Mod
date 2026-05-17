package com.shipovskijkorp.scythes.mod.network;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.BloodHarvestAbility;
import com.shipovskijkorp.scythes.mod.ability.PlagueScytheAbility;
import com.shipovskijkorp.scythes.mod.ability.WitheringScytheAbility;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.item.PlagueScytheItem;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ScytheAbilityC2SPacket {

    public static final Identifier ID =
            new Identifier(ScytheMod.MOD_ID, "scythe_ability");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
                ID,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    if (!player.isAlive()) return;

                    // main hand -> offhand fallback
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
