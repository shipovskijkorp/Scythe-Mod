package com.shipovskijkorp.scythes.mod;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.effect.BleedingEffect;
import com.shipovskijkorp.scythes.mod.effect.BurnsEffect;
import com.shipovskijkorp.scythes.mod.effect.FreezingEffect;
import com.shipovskijkorp.scythes.mod.effect.NoJumpEffect;
import com.shipovskijkorp.scythes.mod.entity.FireballEntity;
import com.shipovskijkorp.scythes.mod.entity.IceSpikeEntity;
import com.shipovskijkorp.scythes.mod.entity.ToxicOrbEntity;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import com.shipovskijkorp.scythes.mod.item.FireScytheItem;
import com.shipovskijkorp.scythes.mod.item.FrozenHeartItem;
import com.shipovskijkorp.scythes.mod.item.FrozenScytheItem;
import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import com.shipovskijkorp.scythes.mod.item.GuideBookItem;
import com.shipovskijkorp.scythes.mod.item.ScytheMaterial;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import com.shipovskijkorp.scythes.mod.network.ModPackets;
import com.shipovskijkorp.scythes.mod.platform.neoforge.NeoForgeServerHooks;
import com.shipovskijkorp.scythes.mod.recipe.FireEssenceRecipe;
import com.shipovskijkorp.scythes.mod.recipe.ToxicEssenceRecipe;
import net.minecraft.core.Holder;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ScytheMod.MOD_ID)
public class ScytheMod {

    public static final String MOD_ID = "scythes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MOD_ID);

    public static Item BLOODY_SCYTHE;
    public static Item TOXIC_SCYTHE;
    /** Legacy placeholder. Kept registered so old scythes:plague_scythe stacks survive load and can be migrated. */
    public static Item PLAGUE_SCYTHE;
    public static Item WITHERING_SCYTHE;
    public static Item GOLDEN_SCYTHE;
    public static Item FROZEN_SCYTHE;
    public static Item FARMER_SCYTHE;
    public static Item FIRE_SCYTHE;
    public static Item GUIDE_BOOK;

    public static EntityType<ToxicOrbEntity> TOXIC_ORB;
    public static EntityType<FireballEntity> FIREBALL;
    public static EntityType<IceSpikeEntity> ICE_SPIKE;
    public static EntityType<WitheringMinionEntity> WITHERING_MINION;

    public static Item BLOODY_ESSENCE;
    public static Item TOXIC_ESSENCE;
    public static Item WITHERING_ESSENCE;
    public static Item GOLDEN_ESSENCE;
    public static Item FROZEN_ESSENCE;
    public static Item FARMER_ESSENCE;
    public static Item FIRE_ESSENCE;
    public static Item FROZEN_HEART;

    public static final RecipeSerializer<ToxicEssenceRecipe> TOXIC_ESSENCE_RECIPE_SERIALIZER =
            new RecipeSerializer<>(ToxicEssenceRecipe.CODEC, ToxicEssenceRecipe.STREAM_CODEC);
    public static final RecipeSerializer<FireEssenceRecipe> FIRE_ESSENCE_RECIPE_SERIALIZER =
            new RecipeSerializer<>(FireEssenceRecipe.CODEC, FireEssenceRecipe.STREAM_CODEC);

    public static final Identifier SCYTHE_ITEM_GROUP_ID = id("scythes");
    public static CreativeModeTab SCYTHE_ITEM_GROUP;

    public static final Holder<MobEffect> BLEEDING = MOB_EFFECTS.register("bleeding", BleedingEffect::new);
    public static final Holder<MobEffect> NO_JUMP = MOB_EFFECTS.register("no_jump", NoJumpEffect::new);
    public static final Holder<MobEffect> FREEZING = MOB_EFFECTS.register("freezing", FreezingEffect::new);
    public static final Holder<MobEffect> BURNS = MOB_EFFECTS.register("burns", BurnsEffect::new);

    public static final ResourceKey<Enchantment> SPIKED_BLADE =
            ResourceKey.create(Registries.ENCHANTMENT, id("spiked_blade"));
    public static final ResourceKey<Enchantment> ADDITIONAL_SLOT =
            ResourceKey.create(Registries.ENCHANTMENT, id("additional_slot"));
    public static final ResourceKey<Enchantment> SOUL_SIPHON =
            ResourceKey.create(Registries.ENCHANTMENT, id("soul_siphon"));
    public static final ResourceKey<Enchantment> ACIDITY =
            ResourceKey.create(Registries.ENCHANTMENT, id("acidity"));

    public ScytheMod(IEventBus modBus) {
        MOB_EFFECTS.register(modBus);
        modBus.addListener(this::registerContent);
        modBus.addListener(this::registerAttributes);
        modBus.addListener(ModPackets::register);
        NeoForgeServerHooks.register();
        LOGGER.info("ScytheMod 5.1 initialized successfully for Minecraft 26.1.2 NeoForge");
    }

    private void registerContent(RegisterEvent event) {
        event.register(Registries.ITEM, id("bloody_scythe"),
                () -> BLOODY_SCYTHE = new BloodScytheItem(scytheSettings("bloody_scythe")));
        event.register(Registries.ITEM, id("toxic_scythe"),
                () -> TOXIC_SCYTHE = new ToxicScytheItem(scytheSettings("toxic_scythe")));
        event.register(Registries.ITEM, id("plague_scythe"),
                () -> PLAGUE_SCYTHE = new Item(scytheSettings("plague_scythe")));
        event.register(Registries.ITEM, id("withering_scythe"),
                () -> WITHERING_SCYTHE = new WitheringScytheItem(scytheSettings("withering_scythe")));
        event.register(Registries.ITEM, id("golden_scythe"),
                () -> GOLDEN_SCYTHE = new GoldenScytheItem(scytheSettings("golden_scythe")));
        event.register(Registries.ITEM, id("frozen_scythe"),
                () -> FROZEN_SCYTHE = new FrozenScytheItem(scytheSettings("frozen_scythe")));
        event.register(Registries.ITEM, id("farmer_scythe"),
                () -> FARMER_SCYTHE = new FarmerScytheItem(ScytheMaterial.configureBase(itemSettings("farmer_scythe"))));
        event.register(Registries.ITEM, id("fire_scythe"),
                () -> FIRE_SCYTHE = new FireScytheItem(scytheSettings("fire_scythe")));
        event.register(Registries.ITEM, id("guide_book"),
                () -> GUIDE_BOOK = new GuideBookItem(itemSettings("guide_book").stacksTo(1)));

        event.register(Registries.ITEM, id("bloody_essence"),
                () -> BLOODY_ESSENCE = new Item(itemSettings("bloody_essence")));
        event.register(Registries.ITEM, id("toxic_essence"),
                () -> TOXIC_ESSENCE = new Item(itemSettings("toxic_essence")));
        event.register(Registries.ITEM, id("withering_essence"),
                () -> WITHERING_ESSENCE = new Item(itemSettings("withering_essence")));
        event.register(Registries.ITEM, id("golden_essence"),
                () -> GOLDEN_ESSENCE = new Item(itemSettings("golden_essence")));
        event.register(Registries.ITEM, id("frozen_essence"),
                () -> FROZEN_ESSENCE = new Item(itemSettings("frozen_essence")));
        event.register(Registries.ITEM, id("farmer_essence"),
                () -> FARMER_ESSENCE = new Item(itemSettings("farmer_essence")));
        event.register(Registries.ITEM, id("fire_essence"),
                () -> FIRE_ESSENCE = new Item(itemSettings("fire_essence")));
        event.register(Registries.ITEM, id("frozen_heart"),
                () -> FROZEN_HEART = new FrozenHeartItem(itemSettings("frozen_heart").food(new FoodProperties.Builder()
                        .nutrition(ScytheBalance.FrozenHeart.NUTRITION)
                        .saturationModifier(ScytheBalance.FrozenHeart.SATURATION_MODIFIER)
                        .alwaysEdible()
                        .build())));

        event.register(Registries.ENTITY_TYPE, id("toxic_orb"), () -> TOXIC_ORB = EntityType.Builder
                .<ToxicOrbEntity>of(ToxicOrbEntity::new, MobCategory.MISC)
                .sized(ScytheBalance.ToxicOrb.WIDTH, ScytheBalance.ToxicOrb.HEIGHT)
                .clientTrackingRange(ScytheBalance.ToxicOrb.TRACKING_RANGE)
                .updateInterval(ScytheBalance.ToxicOrb.UPDATE_INTERVAL)
                .build(entityTypeKey("toxic_orb")));
        event.register(Registries.ENTITY_TYPE, id("fireball"), () -> FIREBALL = EntityType.Builder
                .<FireballEntity>of(FireballEntity::new, MobCategory.MISC)
                .sized(ScytheBalance.Fire.FIREBALL_WIDTH, ScytheBalance.Fire.FIREBALL_HEIGHT)
                .clientTrackingRange(ScytheBalance.Fire.FIREBALL_TRACKING_RANGE)
                .updateInterval(ScytheBalance.Fire.FIREBALL_UPDATE_INTERVAL)
                .build(entityTypeKey("fireball")));
        event.register(Registries.ENTITY_TYPE, id("ice_spike"), () -> ICE_SPIKE = EntityType.Builder
                .<IceSpikeEntity>of(IceSpikeEntity::new, MobCategory.MISC)
                .sized(ScytheBalance.IceSpike.WIDTH, ScytheBalance.IceSpike.HEIGHT)
                .clientTrackingRange(ScytheBalance.IceSpike.TRACKING_RANGE)
                .updateInterval(ScytheBalance.IceSpike.UPDATE_INTERVAL)
                .build(entityTypeKey("ice_spike")));
        event.register(Registries.ENTITY_TYPE, id("withering_minion"), () -> WITHERING_MINION = EntityType.Builder
                .<WitheringMinionEntity>of(WitheringMinionEntity::new, MobCategory.MONSTER)
                .sized(ScytheBalance.Minion.WIDTH, ScytheBalance.Minion.HEIGHT)
                .fireImmune()
                .clientTrackingRange(ScytheBalance.Minion.TRACKING_RANGE)
                .updateInterval(ScytheBalance.Minion.UPDATE_INTERVAL)
                .build(entityTypeKey("withering_minion")));

        event.register(Registries.RECIPE_SERIALIZER, id("craft_toxic_essence"), () -> TOXIC_ESSENCE_RECIPE_SERIALIZER);
        event.register(Registries.RECIPE_SERIALIZER, id("craft_fire_essence"), () -> FIRE_ESSENCE_RECIPE_SERIALIZER);
        event.register(Registries.CREATIVE_MODE_TAB, SCYTHE_ITEM_GROUP_ID,
                () -> SCYTHE_ITEM_GROUP = createItemGroup());
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(WITHERING_MINION, WitheringMinionEntity.createAttributes().build());
    }

    private static CreativeModeTab createItemGroup() {
        return CreativeModeTab.builder()
                .icon(ScytheMod::createRotatingTabIcon)
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
                .build();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, id(name));
    }

    private static ResourceKey<EntityType<?>> entityTypeKey(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, id(name));
    }

    private static Item.Properties itemSettings(String name) {
        return new Item.Properties().setId(itemKey(name));
    }

    private static Item.Properties scytheSettings(String name) {
        return ScytheMaterial.configure(itemSettings(name));
    }

    public static ItemStack createRotatingTabIcon() {
        Item[] items = {
                BLOODY_SCYTHE, TOXIC_SCYTHE, WITHERING_SCYTHE, GOLDEN_SCYTHE,
                FROZEN_SCYTHE, FARMER_SCYTHE, FIRE_SCYTHE
        };
        int index = (int) ((System.currentTimeMillis() / ScytheBalance.Presentation.TAB_ICON_INTERVAL_MS) % items.length);
        Item item = items[index];
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
