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
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.WitheringMinionManager;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.platform.HudSync;
import com.shipovskijkorp.scythes.mod.util.BurnsUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.loot.LootPool;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

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
        DamageAttributionTracker.clearAll();
        WitheringMinionManager.clearRuntime();
        BurnsUtil.clearAll();
        FireLaunchTracker.clearAll();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) return;
        ScytheLifecycle.connect(player);
        PlagueScytheMigrationHandler.migratePlayer(player);
        WelcomeAdvancementHandler.grantRoot(player);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity killedEntity = event.getEntity();
        if (!(killedEntity.getEntityWorld() instanceof ServerWorld world)) return;
        var source = event.getSource();
        world.getServer().execute(() -> {
            if (event.isCanceled() || killedEntity.isAlive()) return;
            if (killedEntity instanceof WitheringMinionEntity minion) WitheringMinionManager.onMinionDeath(minion);
            FrozenHeartDropHandler.onDeath(killedEntity, source);
            Entity killer = source.getAttacker();
            if (killer == null) return;
            BloodHarvestKillHandler.onKill(world, killer, killedEntity);
            WitheringSoulHandler.onKill(world, killer, killedEntity);
            BloodyEssenceDropHandler.onKill(world, killer, killedEntity);
        });
    }

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        LootPool.Builder pool = FrozenHeartDropHandler.createLootPool(event.getName(), true);
        if (pool != null) {
            event.getTable().addPool(pool.build());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            ScytheLifecycle.disconnect(player);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerWorld world)) return;
        if (!(event.getPlayer() instanceof ServerPlayerEntity player)) return;
        FarmerHarvestHandler.prepareCropBreak(world, player, event.getPos(), event.getState());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        ScytheLifecycle.tickServer(event.getServer());
    }
}
