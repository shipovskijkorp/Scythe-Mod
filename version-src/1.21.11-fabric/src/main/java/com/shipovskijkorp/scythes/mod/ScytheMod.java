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
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import com.shipovskijkorp.scythes.mod.item.FireScytheItem;
import com.shipovskijkorp.scythes.mod.item.FrozenHeartItem;
import com.shipovskijkorp.scythes.mod.item.FrozenScytheItem;
import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import com.shipovskijkorp.scythes.mod.item.GuideBookItem;
import com.shipovskijkorp.scythes.mod.item.ScytheMaterial;
import com.shipovskijkorp.scythes.mod.item.ScytheSwordItem;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import com.shipovskijkorp.scythes.mod.network.ModPackets;
import com.shipovskijkorp.scythes.mod.network.ScytheAbilityC2SPacket;
import com.shipovskijkorp.scythes.mod.platform.fabric.FabricServerHooks;
import com.shipovskijkorp.scythes.mod.recipe.FireEssenceRecipe;
import com.shipovskijkorp.scythes.mod.recipe.ToxicEssenceRecipe;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScytheMod implements ModInitializer {

    public static final String MOD_ID = "scythes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

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
    public static final Item GUIDE_BOOK = registerItem("guide_book", GuideBookItem::new, itemSettings("guide_book").maxCount(1));

    private static final RegistryKey<EntityType<?>> TOXIC_ORB_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("toxic_orb"));
    private static final RegistryKey<EntityType<?>> FIREBALL_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("fireball"));
    private static final RegistryKey<EntityType<?>> ICE_SPIKE_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("ice_spike"));
    private static final RegistryKey<EntityType<?>> WITHERING_MINION_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("withering_minion"));

    public static final EntityType<ToxicOrbEntity> TOXIC_ORB = FabricEntityTypeBuilder
            .<ToxicOrbEntity>create(SpawnGroup.MISC, ToxicOrbEntity::new)
            .dimensions(EntityDimensions.fixed(ScytheBalance.ToxicOrb.WIDTH, ScytheBalance.ToxicOrb.HEIGHT))
            .trackRangeBlocks(ScytheBalance.ToxicOrb.TRACKING_RANGE)
            .trackedUpdateRate(ScytheBalance.ToxicOrb.UPDATE_INTERVAL)
            .build(TOXIC_ORB_KEY);

    public static final EntityType<FireballEntity> FIREBALL = FabricEntityTypeBuilder
            .<FireballEntity>create(SpawnGroup.MISC, FireballEntity::new)
            .dimensions(EntityDimensions.fixed(ScytheBalance.Fire.FIREBALL_WIDTH, ScytheBalance.Fire.FIREBALL_HEIGHT))
            .trackRangeBlocks(ScytheBalance.Fire.FIREBALL_TRACKING_RANGE)
            .trackedUpdateRate(ScytheBalance.Fire.FIREBALL_UPDATE_INTERVAL)
            .build(FIREBALL_KEY);

    public static final EntityType<IceSpikeEntity> ICE_SPIKE = FabricEntityTypeBuilder
            .<IceSpikeEntity>create(SpawnGroup.MISC, IceSpikeEntity::new)
            .dimensions(EntityDimensions.fixed(ScytheBalance.IceSpike.WIDTH, ScytheBalance.IceSpike.HEIGHT))
            .trackRangeBlocks(ScytheBalance.IceSpike.TRACKING_RANGE)
            .trackedUpdateRate(ScytheBalance.IceSpike.UPDATE_INTERVAL)
            .build(ICE_SPIKE_KEY);

    public static final EntityType<WitheringMinionEntity> WITHERING_MINION = FabricEntityTypeBuilder
            .<WitheringMinionEntity>create(SpawnGroup.MONSTER, WitheringMinionEntity::new)
            .dimensions(EntityDimensions.fixed(ScytheBalance.Minion.WIDTH, ScytheBalance.Minion.HEIGHT))
            .fireImmune()
            .trackRangeBlocks(ScytheBalance.Minion.TRACKING_RANGE)
            .trackedUpdateRate(ScytheBalance.Minion.UPDATE_INTERVAL)
            .build(WITHERING_MINION_KEY);

    public static final Item BLOODY_ESSENCE = registerItem("bloody_essence", Item::new, itemSettings("bloody_essence"));
    public static final Item TOXIC_ESSENCE = registerItem("toxic_essence", Item::new, itemSettings("toxic_essence"));
    public static final Item WITHERING_ESSENCE = registerItem("withering_essence", Item::new, itemSettings("withering_essence"));
    public static final Item GOLDEN_ESSENCE = registerItem("golden_essence", Item::new, itemSettings("golden_essence"));
    public static final Item FROZEN_ESSENCE = registerItem("frozen_essence", Item::new, itemSettings("frozen_essence"));
    public static final Item FARMER_ESSENCE = registerItem("farmer_essence", Item::new, itemSettings("farmer_essence"));
    public static final Item FIRE_ESSENCE = registerItem("fire_essence", Item::new, itemSettings("fire_essence"));

    private static final FoodComponent FROZEN_HEART_FOOD = new FoodComponent.Builder()
            .nutrition(ScytheBalance.FrozenHeart.NUTRITION)
            .saturationModifier(ScytheBalance.FrozenHeart.SATURATION_MODIFIER)
            .alwaysEdible()
            .build();
    public static final Item FROZEN_HEART = registerItem(
            "frozen_heart",
            FrozenHeartItem::new,
            itemSettings("frozen_heart").food(FROZEN_HEART_FOOD)
    );

    public static final SpecialCraftingRecipe.SpecialRecipeSerializer<ToxicEssenceRecipe> TOXIC_ESSENCE_RECIPE_SERIALIZER =
            new SpecialCraftingRecipe.SpecialRecipeSerializer<>(ToxicEssenceRecipe::new);
    public static final SpecialCraftingRecipe.SpecialRecipeSerializer<FireEssenceRecipe> FIRE_ESSENCE_RECIPE_SERIALIZER =
            new SpecialCraftingRecipe.SpecialRecipeSerializer<>(FireEssenceRecipe::new);

    public static final Identifier SCYTHE_ITEM_GROUP_ID = id("scythes");
    public static ItemGroup SCYTHE_ITEM_GROUP;

    private static final Item[] TAB_ICON_ITEMS = new Item[] {
            BLOODY_SCYTHE, TOXIC_SCYTHE, WITHERING_SCYTHE, GOLDEN_SCYTHE, FROZEN_SCYTHE, FARMER_SCYTHE, FIRE_SCYTHE
    };

    public static final RegistryEntry<StatusEffect> BLEEDING =
            Registry.registerReference(Registries.STATUS_EFFECT, id("bleeding"), new BleedingEffect());
    public static final RegistryEntry<StatusEffect> NO_JUMP =
            Registry.registerReference(Registries.STATUS_EFFECT, id("no_jump"), new NoJumpEffect());
    public static final RegistryEntry<StatusEffect> FREEZING =
            Registry.registerReference(Registries.STATUS_EFFECT, id("freezing"), new FreezingEffect());
    public static final RegistryEntry<StatusEffect> BURNS =
            Registry.registerReference(Registries.STATUS_EFFECT, id("burns"), new BurnsEffect());

    public static final RegistryKey<Enchantment> SPIKED_BLADE = RegistryKey.of(RegistryKeys.ENCHANTMENT, id("spiked_blade"));
    public static final RegistryKey<Enchantment> ADDITIONAL_SLOT = RegistryKey.of(RegistryKeys.ENCHANTMENT, id("additional_slot"));
    public static final RegistryKey<Enchantment> SOUL_SIPHON = RegistryKey.of(RegistryKeys.ENCHANTMENT, id("soul_siphon"));
    public static final RegistryKey<Enchantment> ACIDITY = RegistryKey.of(RegistryKeys.ENCHANTMENT, id("acidity"));

    @Override
    public void onInitialize() {
        Registry.register(Registries.ENTITY_TYPE, TOXIC_ORB_KEY, TOXIC_ORB);
        Registry.register(Registries.ENTITY_TYPE, FIREBALL_KEY, FIREBALL);
        Registry.register(Registries.ENTITY_TYPE, ICE_SPIKE_KEY, ICE_SPIKE);
        Registry.register(Registries.ENTITY_TYPE, WITHERING_MINION_KEY, WITHERING_MINION);
        FabricDefaultAttributeRegistry.register(WITHERING_MINION, WitheringMinionEntity.createAttributes());
        Registry.register(Registries.RECIPE_SERIALIZER, id("craft_toxic_essence"), TOXIC_ESSENCE_RECIPE_SERIALIZER);
        Registry.register(Registries.RECIPE_SERIALIZER, id("craft_fire_essence"), FIRE_ESSENCE_RECIPE_SERIALIZER);
        ModPackets.register();

        SCYTHE_ITEM_GROUP = Registry.register(
                Registries.ITEM_GROUP,
                SCYTHE_ITEM_GROUP_ID,
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(BLOODY_SCYTHE))
                        .displayName(Text.translatable("itemGroup." + MOD_ID + ".scythes"))
                        .entries((displayContext, entries) -> {
                            entries.add(GUIDE_BOOK);
                            entries.add(BLOODY_SCYTHE);
                            entries.add(TOXIC_SCYTHE);
                            entries.add(WITHERING_SCYTHE);
                            entries.add(GOLDEN_SCYTHE);
                            entries.add(FROZEN_SCYTHE);
                            entries.add(FARMER_SCYTHE);
							entries.add(FIRE_SCYTHE);
                            entries.add(BLOODY_ESSENCE);
                            entries.add(TOXIC_ESSENCE);
                            entries.add(WITHERING_ESSENCE);
                            entries.add(GOLDEN_ESSENCE);
                            entries.add(FROZEN_ESSENCE);
                            entries.add(FARMER_ESSENCE);
							entries.add(FIRE_ESSENCE);
                            entries.add(FROZEN_HEART);
                        })
                        .build()
        );

        ScytheAbilityC2SPacket.register();
        FabricServerHooks.register();

        LOGGER.info("ScytheMod 4.0 initialized successfully for Minecraft 1.21.11");
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    private static RegistryKey<Item> itemKey(String name) {
        return RegistryKey.of(RegistryKeys.ITEM, id(name));
    }

    private static Item.Settings itemSettings(String name) {
        return new Item.Settings().registryKey(itemKey(name));
    }

    private static Item.Settings scytheSettings(String name) {
        return ToolMaterial.NETHERITE.applySwordSettings(
                new Item.Settings().maxCount(1).fireproof().registryKey(itemKey(name)),
                4.0F,
                -2.8F
        );
    }

    private static <T extends Item> T registerItem(String name, Function<Item.Settings, T> factory, Item.Settings settings) {
        RegistryKey<Item> key = itemKey(name);
        T item = factory.apply(settings.registryKey(key));
        return Registry.register(Registries.ITEM, key, item);
    }

    public static ItemStack createRotatingTabIcon() {
        int index = (int) ((System.currentTimeMillis() / ScytheBalance.Presentation.TAB_ICON_INTERVAL_MS) % TAB_ICON_ITEMS.length);
        return new ItemStack(TAB_ICON_ITEMS[index]);
    }

}
