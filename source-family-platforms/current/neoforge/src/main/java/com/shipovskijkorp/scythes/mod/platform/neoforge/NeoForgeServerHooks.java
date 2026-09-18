package com.shipovskijkorp.scythes.mod.platform.neoforge;

import com.shipovskijkorp.scythes.mod.ability.BloodHarvestKillHandler;
import com.shipovskijkorp.scythes.mod.ability.BloodyEssenceDropHandler;
import com.shipovskijkorp.scythes.mod.ability.FarmerHarvestHandler;
import com.shipovskijkorp.scythes.mod.ability.FireLaunchTracker;
import com.shipovskijkorp.scythes.mod.ability.FrozenHeartDropHandler;
import com.shipovskijkorp.scythes.mod.ability.PlagueScytheMigrationHandler;
import com.shipovskijkorp.scythes.mod.ability.ScytheCooldowns;
import com.shipovskijkorp.scythes.mod.ability.ScytheLifecycle;
import com.shipovskijkorp.scythes.mod.ability.WelcomeAdvancementHandler;
import com.shipovskijkorp.scythes.mod.ability.WitheringSoulHandler;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.network.ModPackets;
import com.shipovskijkorp.scythes.mod.platform.HudSync;
import com.shipovskijkorp.scythes.mod.util.BurnsUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootPool;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** NeoForge wiring only. Gameplay must not subscribe to loader events directly. */
public final class NeoForgeServerHooks {
    private static final NeoForgeServerHooks INSTANCE = new NeoForgeServerHooks();
    private static boolean registered;

    private NeoForgeServerHooks() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        HudSync.install(new NeoForgeHudTransport());
        NeoForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        ScytheCooldowns.clearAll();
        BurnsUtil.clearAll();
        FireLaunchTracker.clearAll();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlagueScytheMigrationHandler.migratePlayer(player);
        WelcomeAdvancementHandler.grantRoot(player);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity killedEntity = event.getEntity();
        if (!(killedEntity.level() instanceof ServerLevel level)) return;

        FrozenHeartDropHandler.onDeath(killedEntity, event.getSource());
        Entity killer = event.getSource().getEntity();
        if (killer == null) return;
        BloodHarvestKillHandler.onKill(level, killer, killedEntity);
        WitheringSoulHandler.onKill(level, killer, killedEntity);
        BloodyEssenceDropHandler.onKill(level, killer, killedEntity);
    }

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        LootPool.Builder pool = FrozenHeartDropHandler.createLootPool(event.getKey(), true);
        if (pool != null) {
            event.getTable().addPool(pool.build());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ScytheLifecycle.disconnect(player);
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        PacketDistributor.sendToPlayer(
                player,
                new ModPackets.FreezingVisualPayload(
                        target.getUUID(),
                        target.isAlive() && target.hasEffect(ScytheMod.FREEZING)
                )
        );
    }

    @SubscribeEvent
    public void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        FarmerHarvestHandler.afterCropBroken(event.getLevel(), player, event.getPos(), event.getState());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        ScytheLifecycle.tickServer(event.getServer());
    }
}
