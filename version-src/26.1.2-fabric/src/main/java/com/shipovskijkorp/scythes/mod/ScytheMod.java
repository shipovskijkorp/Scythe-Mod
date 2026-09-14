package com.shipovskijkorp.scythes.mod;

import com.shipovskijkorp.scythes.mod.ability.*;
import com.shipovskijkorp.scythes.mod.effect.BleedingEffect;
import com.shipovskijkorp.scythes.mod.effect.FreezingEffect;
import com.shipovskijkorp.scythes.mod.effect.NoJumpEffect;
import com.shipovskijkorp.scythes.mod.entity.IceSpikeEntity;
import com.shipovskijkorp.scythes.mod.entity.ToxicOrbEntity;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.item.*;
import com.shipovskijkorp.scythes.mod.network.ModPackets;
import com.shipovskijkorp.scythes.mod.network.ScytheAbilityC2SPacket;
import com.shipovskijkorp.scythes.mod.recipe.ToxicEssenceRecipe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class ScytheMod implements ModInitializer {

    public static final String MOD_ID = "scythes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final ResourceKey<EntityType<?>> TOXIC_ORB_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("toxic_orb"));
    private static final ResourceKey<EntityType<?>> WITHERING_MINION_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("withering_minion"));
    private static final ResourceKey<EntityType<?>> ICE_SPIKE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("ice_spike"));

    public static final Item BLOODY_SCYTHE = registerItem("bloody_scythe", BloodScytheItem::new, scytheSettings("bloody_scythe"));
    public static final Item TOXIC_SCYTHE = registerItem("toxic_scythe", ToxicScytheItem::new, scytheSettings("toxic_scythe"));
    /** Legacy placeholder. Kept registered so old scythes:plague_scythe stacks survive load and can be migrated. */
    public static final Item PLAGUE_SCYTHE = registerItem("plague_scythe", Item::new, scytheSettings("plague_scythe"));
    public static final Item WITHERING_SCYTHE = registerItem("withering_scythe", WitheringScytheItem::new, scytheSettings("withering_scythe"));
    public static final Item GOLDEN_SCYTHE = registerItem("golden_scythe", GoldenScytheItem::new, scytheSettings("golden_scythe"));
    public static final Item FROZEN_SCYTHE = registerItem("frozen_scythe", FrozenScytheItem::new, scytheSettings("frozen_scythe"));

    public static final EntityType<ToxicOrbEntity> TOXIC_ORB = EntityType.Builder
            .<ToxicOrbEntity>of(ToxicOrbEntity::new, MobCategory.MISC)
            .sized(0.35F, 0.35F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build(TOXIC_ORB_KEY);

    public static final EntityType<WitheringMinionEntity> WITHERING_MINION = EntityType.Builder
            .<WitheringMinionEntity>of(WitheringMinionEntity::new, MobCategory.MONSTER)
            .sized(0.7F, 2.4F)
            .clientTrackingRange(8)
            .updateInterval(3)
            .build(WITHERING_MINION_KEY);

    public static final EntityType<IceSpikeEntity> ICE_SPIKE = EntityType.Builder
            .<IceSpikeEntity>of(IceSpikeEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .clientTrackingRange(8)
            .updateInterval(10)
            .build(ICE_SPIKE_KEY);

    public static final Item BLOODY_ESSENCE = registerItem("bloody_essence", Item::new, itemSettings("bloody_essence"));
    public static final Item TOXIC_ESSENCE = registerItem("toxic_essence", Item::new, itemSettings("toxic_essence"));
    public static final Item WITHERING_ESSENCE = registerItem("withering_essence", Item::new, itemSettings("withering_essence"));
    public static final Item GOLDEN_ESSENCE = registerItem("golden_essence", Item::new, itemSettings("golden_essence"));
    public static final Item FROZEN_ESSENCE = registerItem("frozen_essence", Item::new, itemSettings("frozen_essence"));
    public static final Item FROZEN_HEART = registerItem(
            "frozen_heart",
            FrozenHeartItem::new,
            itemSettings("frozen_heart").food(new FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0.3F)
                    .alwaysEdible()
                    .build())
    );


    public static final RecipeSerializer<ToxicEssenceRecipe> TOXIC_ESSENCE_RECIPE_SERIALIZER =
            new RecipeSerializer<>(ToxicEssenceRecipe.CODEC, ToxicEssenceRecipe.STREAM_CODEC);

    public static final Identifier SCYTHE_ITEM_GROUP_ID = id("scythes");
    public static CreativeModeTab SCYTHE_ITEM_GROUP;

    private static final Item[] TAB_ICON_ITEMS = new Item[] {
            BLOODY_SCYTHE, TOXIC_SCYTHE, WITHERING_SCYTHE, GOLDEN_SCYTHE, FROZEN_SCYTHE
    };
    private static final long TAB_ICON_INTERVAL_MS = 1200L;

    public static final Holder<MobEffect> BLEEDING =
            Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id("bleeding"), new BleedingEffect());
    public static final Holder<MobEffect> NO_JUMP =
            Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id("no_jump"), new NoJumpEffect());
    public static final Holder<MobEffect> FREEZING =
            Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id("freezing"), new FreezingEffect());

    public static final ResourceKey<Enchantment> SPIKED_BLADE =
            ResourceKey.create(Registries.ENCHANTMENT, id("spiked_blade"));
    public static final ResourceKey<Enchantment> ADDITIONAL_SLOT =
            ResourceKey.create(Registries.ENCHANTMENT, id("additional_slot"));
    public static final ResourceKey<Enchantment> SOUL_SIPHON =
            ResourceKey.create(Registries.ENCHANTMENT, id("soul_siphon"));
    public static final ResourceKey<Enchantment> ACIDITY =
            ResourceKey.create(Registries.ENCHANTMENT, id("acidity"));

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id("toxic_orb"), TOXIC_ORB);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id("withering_minion"), WITHERING_MINION);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id("ice_spike"), ICE_SPIKE);
        FabricDefaultAttributeRegistry.register(WITHERING_MINION, WitheringMinionEntity.createAttributes());
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("craft_toxic_essence"), TOXIC_ESSENCE_RECIPE_SERIALIZER);
        ModPackets.registerPayloadTypes();

        SCYTHE_ITEM_GROUP = Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                SCYTHE_ITEM_GROUP_ID,
                FabricCreativeModeTab.builder()
                        .icon(() -> new ItemStack(BLOODY_SCYTHE))
                        .title(Component.translatable("itemGroup." + MOD_ID + ".scythes"))
                        .displayItems((displayContext, entries) -> {
                            entries.accept(BLOODY_SCYTHE);
                            entries.accept(TOXIC_SCYTHE);
                            entries.accept(WITHERING_SCYTHE);
                            entries.accept(GOLDEN_SCYTHE);
                            entries.accept(FROZEN_SCYTHE);
                            entries.accept(BLOODY_ESSENCE);
                            entries.accept(TOXIC_ESSENCE);
                            entries.accept(WITHERING_ESSENCE);
                            entries.accept(GOLDEN_ESSENCE);
                            entries.accept(FROZEN_ESSENCE);
                            entries.accept(FROZEN_HEART);
                        })
                        .build()
        );

        ScytheAbilityC2SPacket.register();
        PlagueScytheMigrationHandler.register();
        WelcomeAdvancementHandler.register();
        BloodHarvestKillHandler.register();
        WitheringSoulHandler.register();
        BloodyEssenceDropHandler.register();
        FrozenHeartDropHandler.register();

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            server.execute(() -> {
                BloodHarvestTracker.clear(player);
                BloodScytheCooldowns.clear(player);
                BloodScytheVampirism.clear(player);
                ToxicAuraTracker.clear(player);
                ToxicScytheCooldowns.clear(player);
                WitheringAuraTracker.clear(player);
                WitheringScytheCooldowns.clear(player);
                GoldenScytheCooldowns.clear(player);
                FrozenScytheCooldowns.clear(player);
                GoldenLootMarkTracker.clearOwner(player);
                GoldenRainDimensionDayTracker.clear(player);
                ScytheAdvancementTracker.clear(player);
            });
        });

        ServerTickEvents.END_SERVER_TICK.register(this::tickServer);

        LOGGER.info("ScytheMod 4.0 initialized successfully for Minecraft 26.1.2");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, id(name));
    }

    private static Item.Properties itemSettings(String name) {
        return new Item.Properties().setId(itemKey(name));
    }

    private static Item.Properties scytheSettings(String name) {
        return itemSettings(name).stacksTo(1).fireResistant().sword(ToolMaterial.NETHERITE, 4.0F, -2.8F);
    }

    private static <T extends Item> T registerItem(String name, Function<Item.Properties, T> factory, Item.Properties settings) {
        T item = factory.apply(settings);
        return Registry.register(BuiltInRegistries.ITEM, id(name), item);
    }

    public static ItemStack createRotatingTabIcon() {
        int index = (int) ((System.currentTimeMillis() / TAB_ICON_INTERVAL_MS) % TAB_ICON_ITEMS.length);
        return new ItemStack(TAB_ICON_ITEMS[index]);
    }

    private void tickServer(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BloodHarvestTracker.tick(player);
            ToxicAuraTracker.tick(player);
            WitheringAuraTracker.tick(player);
            PlagueScytheMigrationHandler.migratePlayer(player);
        }
        GoldenLootMarkTracker.tick(server);
    }
}
