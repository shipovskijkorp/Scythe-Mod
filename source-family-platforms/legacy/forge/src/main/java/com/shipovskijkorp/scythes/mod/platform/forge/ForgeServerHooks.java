package com.shipovskijkorp.scythes.mod.platform.forge;

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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Forge wiring only. Gameplay must not subscribe to loader events directly. */
public final class ForgeServerHooks {
    private static final ForgeServerHooks INSTANCE = new ForgeServerHooks();
    private static boolean registered;

    private ForgeServerHooks() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        HudSync.install(new ForgeHudTransport());
        MinecraftForge.EVENT_BUS.register(INSTANCE);
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity killedEntity = event.getEntity();
        if (!(killedEntity.getWorld() instanceof ServerWorld world)) return;
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerWorld world)) return;
        if (!(event.getPlayer() instanceof ServerPlayerEntity player)) return;
        FarmerHarvestHandler.prepareCropBreak(world, player, event.getPos(), event.getState());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ScytheLifecycle.tickServer(event.getServer());
        }
    }
}
