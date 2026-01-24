package com.example.bloodyscythe.network;

import com.example.bloodyscythe.BleedingMod;
import com.example.bloodyscythe.ability.BloodHarvestAbility;
import com.example.bloodyscythe.ability.PlagueScytheAbility;
import com.example.bloodyscythe.ability.WitheringScytheAbility;
import com.example.bloodyscythe.item.BloodScytheItem;
import com.example.bloodyscythe.item.PlagueScytheItem;
import com.example.bloodyscythe.item.WitheringScytheItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ScytheAbilityC2SPacket {

    public static final Identifier ID =
            new Identifier(BleedingMod.MOD_ID, "scythe_ability");

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
