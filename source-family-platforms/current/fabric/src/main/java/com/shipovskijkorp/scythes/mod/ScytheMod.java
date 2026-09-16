package com.shipovskijkorp.scythes.mod;

import com.shipovskijkorp.scythes.mod.ability.*;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.effect.BleedingEffect;
import com.shipovskijkorp.scythes.mod.effect.BurnsEffect;
import com.shipovskijkorp.scythes.mod.effect.FreezingEffect;
import com.shipovskijkorp.scythes.mod.effect.NoJumpEffect;
import com.shipovskijkorp.scythes.mod.entity.FireballEntity;
import com.shipovskijkorp.scythes.mod.entity.IceSpikeEntity;
import com.shipovskijkorp.scythes.mod.entity.ToxicOrbEntity;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.item.*;
import com.shipovskijkorp.scythes.mod.item.ScytheMaterial;
import com.shipovskijkorp.scythes.mod.network.ModPackets;
import com.shipovskijkorp.scythes.mod.network.ScytheAbilityC2SPacket;
import com.shipovskijkorp.scythes.mod.platform.fabric.FabricServerHooks;
import com.shipovskijkorp.scythes.mod.recipe.FireEssenceRecipe;
import com.shipovskijkorp.scythes.mod.recipe.ToxicEssenceRecipe;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScytheMod implements ModInitializer {

    public static final String MOD_ID = "scythes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final ResourceKey<EntityType<?>> TOXIC_ORB_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("toxic_orb"));
    private static final ResourceKey<EntityType<?>> FIREBALL_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("fireball"));
    private static final ResourceKey<EntityType<?>> WITHERING_MINION_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("withering_minion"));
    private static final ResourceKey<EntityType<?>> ICE_SPIKE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("ice_spike"));

    public static final Item BLOODY_SCYTHE = registerItem("bloody_scythe", BloodScytheItem::new, scytheSettings("bloody_scythe"));
    public static final Item TOXIC_SCYTHE = registerItem("toxic_scythe", ToxicScytheItem::new, scytheSettings("toxic_scythe"));
    /** Legacy placeholder. Kept registered so old scythes:plague_scythe stacks survive load and can be migrated. */
    public static final Item PLAGUE_SCYTHE = registerItem("plague_scythe", Item::new, scytheSettings("plague_scythe"));
    public static final Item WITHERING_SCYTHE = registerItem("withering_scythe", WitheringScytheItem::new, scytheSettings("withering_scythe"));
    public static final Item GOLDEN_SCYTHE = registerItem("golden_scythe", GoldenScytheItem::new, scytheSettings("golden_scythe"));
    public static final Item FROZEN_SCYTHE = registerItem("frozen_scythe", FrozenScytheItem::new, scytheSettings("frozen_scythe"));
    public static final Item FARMER_SCYTHE = registerItem("farmer_scythe", FarmerScytheItem::new,
            ScytheMaterial.configureBase(itemSettings("farmer_scythe")));
    public static final Item FIRE_SCYTHE = registerItem("fire_scythe", FireScytheItem::new, scytheSettings("fire_scythe"));
    public static final Item GUIDE_BOOK = registerItem("guide_book", GuideBookItem::new, itemSettings("guide_book").stacksTo(1));

    public static final EntityType<ToxicOrbEntity> TOXIC_ORB = EntityType.Builder
            .<ToxicOrbEntity>of(ToxicOrbEntity::new, MobCategory.MISC)
            .sized(ScytheBalance.ToxicOrb.WIDTH, ScytheBalance.ToxicOrb.HEIGHT)
            .clientTrackingRange(ScytheBalance.ToxicOrb.TRACKING_RANGE)
            .updateInterval(ScytheBalance.ToxicOrb.UPDATE_INTERVAL)
            .build(TOXIC_ORB_KEY);

    public static final EntityType<FireballEntity> FIREBALL = EntityType.Builder
            .<FireballEntity>of(FireballEntity::new, MobCategory.MISC)
            .sized(ScytheBalance.Fire.FIREBALL_WIDTH, ScytheBalance.Fire.FIREBALL_HEIGHT)
            .clientTrackingRange(ScytheBalance.Fire.FIREBALL_TRACKING_RANGE)
            .updateInterval(ScytheBalance.Fire.FIREBALL_UPDATE_INTERVAL)
            .build(FIREBALL_KEY);

    public static final EntityType<WitheringMinionEntity> WITHERING_MINION = EntityType.Builder
            .<WitheringMinionEntity>of(WitheringMinionEntity::new, MobCategory.MONSTER)
            .sized(ScytheBalance.Minion.WIDTH, ScytheBalance.Minion.HEIGHT)
            .fireImmune()
            .clientTrackingRange(ScytheBalance.Minion.TRACKING_RANGE)
            .updateInterval(ScytheBalance.Minion.UPDATE_INTERVAL)
            .build(WITHERING_MINION_KEY);

    public static final EntityType<IceSpikeEntity> ICE_SPIKE = EntityType.Builder
            .<IceSpikeEntity>of(IceSpikeEntity::new, MobCategory.MISC)
            .sized(ScytheBalance.IceSpike.WIDTH, ScytheBalance.IceSpike.HEIGHT)
            .clientTrackingRange(ScytheBalance.IceSpike.TRACKING_RANGE)
            .updateInterval(ScytheBalance.IceSpike.UPDATE_INTERVAL)
            .build(ICE_SPIKE_KEY);

    public static final Item BLOODY_ESSENCE = registerItem("bloody_essence", Item::new, itemSettings("bloody_essence"));
    public static final Item TOXIC_ESSENCE = registerItem("toxic_essence", Item::new, itemSettings("toxic_essence"));
    public static final Item WITHERING_ESSENCE = registerItem("withering_essence", Item::new, itemSettings("withering_essence"));
    public static final Item GOLDEN_ESSENCE = registerItem("golden_essence", Item::new, itemSettings("golden_essence"));
    public static final Item FROZEN_ESSENCE = registerItem("frozen_essence", Item::new, itemSettings("frozen_essence"));
        public static final Item FARMER_ESSENCE = registerItem("farmer_essence", Item::new, itemSettings("farmer_essence"));
    public static final Item FIRE_ESSENCE = registerItem("fire_essence", Item::new, itemSettings("fire_essence"));
    public static final Item FROZEN_HEART = registerItem(
            "frozen_heart",
            FrozenHeartItem::new,
            itemSettings("frozen_heart").food(new FoodProperties.Builder()
                    .nutrition(ScytheBalance.FrozenHeart.NUTRITION)
                    .saturationModifier(ScytheBalance.FrozenHeart.SATURATION_MODIFIER)
                    .alwaysEdible()
                    .build())
    );

    public static final RecipeSerializer<ToxicEssenceRecipe> TOXIC_ESSENCE_RECIPE_SERIALIZER =
            new RecipeSerializer<>(ToxicEssenceRecipe.CODEC, ToxicEssenceRecipe.STREAM_CODEC);
    public static final RecipeSerializer<FireEssenceRecipe> FIRE_ESSENCE_RECIPE_SERIALIZER =
            new RecipeSerializer<>(FireEssenceRecipe.CODEC, FireEssenceRecipe.STREAM_CODEC);

    public static final Identifier SCYTHE_ITEM_GROUP_ID = id("scythes");
    public static CreativeModeTab SCYTHE_ITEM_GROUP;

    private static final Item[] TAB_ICON_ITEMS = new Item[] {
            BLOODY_SCYTHE, TOXIC_SCYTHE, WITHERING_SCYTHE, GOLDEN_SCYTHE, FROZEN_SCYTHE, FARMER_SCYTHE, FIRE_SCYTHE
    };

    public static final Holder<MobEffect> BLEEDING =
            Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id("bleeding"), new BleedingEffect());
    public static final Holder<MobEffect> NO_JUMP =
            Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id("no_jump"), new NoJumpEffect());
    public static final Holder<MobEffect> FREEZING =
            Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id("freezing"), new FreezingEffect());
    public static final Holder<MobEffect> BURNS =
            Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id("burns"), new BurnsEffect());

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
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id("fireball"), FIREBALL);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id("withering_minion"), WITHERING_MINION);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id("ice_spike"), ICE_SPIKE);
        FabricDefaultAttributeRegistry.register(WITHERING_MINION, WitheringMinionEntity.createAttributes());
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("craft_toxic_essence"), TOXIC_ESSENCE_RECIPE_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("craft_fire_essence"), FIRE_ESSENCE_RECIPE_SERIALIZER);
        ModPackets.registerPayloadTypes();

        SCYTHE_ITEM_GROUP = Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                SCYTHE_ITEM_GROUP_ID,
                FabricCreativeModeTab.builder()
                        .icon(() -> new ItemStack(BLOODY_SCYTHE))
                        .title(Component.translatable("itemGroup." + MOD_ID + ".scythes"))
                        .displayItems((displayContext, entries) -> {
                            entries.accept(GUIDE_BOOK);
                            entries.accept(BLOODY_SCYTHE);
                            entries.accept(TOXIC_SCYTHE);
                            entries.accept(WITHERING_SCYTHE);
                            entries.accept(GOLDEN_SCYTHE);
                            entries.accept(FROZEN_SCYTHE);
                            entries.accept(FARMER_SCYTHE);
                            entries.accept(FIRE_SCYTHE);
                            entries.accept(BLOODY_ESSENCE);
                            entries.accept(TOXIC_ESSENCE);
                            entries.accept(WITHERING_ESSENCE);
                            entries.accept(GOLDEN_ESSENCE);
                            entries.accept(FROZEN_ESSENCE);
                            entries.accept(FARMER_ESSENCE);
                            entries.accept(FIRE_ESSENCE);
                            entries.accept(FROZEN_HEART);
                        })
                        .build()
        );

        ScytheAbilityC2SPacket.register();
        FabricServerHooks.register();

//? if >=26.3 {
        LOGGER.info("ScytheMod 5.0 initialized successfully for Minecraft 26.3");
//? } else if >=26.2 {
        LOGGER.info("ScytheMod 5.0 initialized successfully for Minecraft 26.2");
//? } else {
        LOGGER.info("ScytheMod 5.0 initialized successfully for Minecraft 26.1.2");
//? }
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
        return ScytheMaterial.configure(itemSettings(name));
    }

    private static <T extends Item> T registerItem(String name, Function<Item.Properties, T> factory, Item.Properties settings) {
        T item = factory.apply(settings);
        return Registry.register(BuiltInRegistries.ITEM, id(name), item);
    }

    public static ItemStack createRotatingTabIcon() {
        int index = (int) ((System.currentTimeMillis() / ScytheBalance.Presentation.TAB_ICON_INTERVAL_MS) % TAB_ICON_ITEMS.length);
        return new ItemStack(TAB_ICON_ITEMS[index]);
    }

}
