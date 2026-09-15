package com.shipovskijkorp.scythes.mod.platform.fabric;

import com.shipovskijkorp.scythes.mod.ability.*;
import com.shipovskijkorp.scythes.mod.platform.HudSync;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Fabric wiring only. Gameplay must not subscribe to loader events directly. */
public final class FabricServerHooks {
    private FabricServerHooks() {}

    public static void register() {
        HudSync.install(new FabricHudTransport());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ScytheCooldowns.clearAll());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> PlagueScytheMigrationHandler.migratePlayer(handler.player));
            server.execute(() -> WelcomeAdvancementHandler.grantRoot(handler.player));
        });
//? if >=1.21.11 {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity, damageSource) -> {
//? } else {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
//? }
            BloodHarvestKillHandler.onKill(world, entity, killedEntity);
            WitheringSoulHandler.onKill(world, entity, killedEntity);
            BloodyEssenceDropHandler.onKill(world, entity, killedEntity);
        });
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) ->
                FrozenHeartDropHandler.modifyLoot(key.getValue(), tableBuilder, source.isBuiltin()));
        ServerLivingEntityEvents.AFTER_DEATH.register(FrozenHeartDropHandler::onDeath);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                server.execute(() -> ScytheLifecycle.disconnect(handler.player)));
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld
                    && player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                FarmerHarvestHandler.afterCropBroken(serverWorld, serverPlayer, pos, state);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(ScytheLifecycle::tickServer);
    }
}
