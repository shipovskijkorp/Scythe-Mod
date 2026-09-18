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
import net.minecraft.component.type.FoodComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ScytheMod.MOD_ID)
public class ScytheMod {

    public static final String MOD_ID = "scythes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /*
     * 1.21.11 requires registry keys on item/entity settings, while NeoForge
     * requires registrable objects to be constructed during its registry phase.
     */
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

    private static final FoodComponent FROZEN_HEART_FOOD = new FoodComponent.Builder()
            .nutrition(ScytheBalance.FrozenHeart.NUTRITION)
            .saturationModifier(ScytheBalance.FrozenHeart.SATURATION_MODIFIER)
            .alwaysEdible()
            .build();
    public static Item FROZEN_HEART;

    public static SpecialCraftingRecipe.SpecialRecipeSerializer<ToxicEssenceRecipe> TOXIC_ESSENCE_RECIPE_SERIALIZER;
    public static SpecialCraftingRecipe.SpecialRecipeSerializer<FireEssenceRecipe> FIRE_ESSENCE_RECIPE_SERIALIZER;

    public static final Identifier SCYTHE_ITEM_GROUP_ID = id("scythes");
    public static ItemGroup SCYTHE_ITEM_GROUP;

    public static RegistryEntry<StatusEffect> BLEEDING;
    public static RegistryEntry<StatusEffect> NO_JUMP;
    public static RegistryEntry<StatusEffect> FREEZING;
    public static RegistryEntry<StatusEffect> BURNS;

    public static final RegistryKey<Enchantment> SPIKED_BLADE = RegistryKey.of(RegistryKeys.ENCHANTMENT, id("spiked_blade"));
    public static final RegistryKey<Enchantment> ADDITIONAL_SLOT = RegistryKey.of(RegistryKeys.ENCHANTMENT, id("additional_slot"));
    public static final RegistryKey<Enchantment> SOUL_SIPHON = RegistryKey.of(RegistryKeys.ENCHANTMENT, id("soul_siphon"));
    public static final RegistryKey<Enchantment> ACIDITY = RegistryKey.of(RegistryKeys.ENCHANTMENT, id("acidity"));

    public ScytheMod(IEventBus modBus) {
        modBus.addListener(this::registerContent);
        modBus.addListener(this::registerAttributes);
        modBus.addListener(ModPackets::register);

        NeoForgeServerHooks.register();
        LOGGER.info("ScytheMod initialized successfully for Minecraft 1.21.11 NeoForge");
    }

    private void registerContent(RegisterEvent event) {
        event.register(RegistryKeys.ITEM, id("bloody_scythe"),
                () -> BLOODY_SCYTHE = new BloodScytheItem(scytheSettings("bloody_scythe")));
        event.register(RegistryKeys.ITEM, id("toxic_scythe"),
                () -> TOXIC_SCYTHE = new ToxicScytheItem(scytheSettings("toxic_scythe")));
        event.register(RegistryKeys.ITEM, id("plague_scythe"),
                () -> PLAGUE_SCYTHE = new Item(scytheSettings("plague_scythe")));
        event.register(RegistryKeys.ITEM, id("withering_scythe"),
                () -> WITHERING_SCYTHE = new WitheringScytheItem(scytheSettings("withering_scythe")));
        event.register(RegistryKeys.ITEM, id("golden_scythe"),
                () -> GOLDEN_SCYTHE = new GoldenScytheItem(scytheSettings("golden_scythe")));
        event.register(RegistryKeys.ITEM, id("frozen_scythe"),
                () -> FROZEN_SCYTHE = new FrozenScytheItem(scytheSettings("frozen_scythe")));
        event.register(RegistryKeys.ITEM, id("farmer_scythe"),
                () -> FARMER_SCYTHE = new FarmerScytheItem(ScytheMaterial.configureBase(itemSettings("farmer_scythe"))));
        event.register(RegistryKeys.ITEM, id("fire_scythe"),
                () -> FIRE_SCYTHE = new FireScytheItem(scytheSettings("fire_scythe")));
        event.register(RegistryKeys.ITEM, id("guide_book"),
                () -> GUIDE_BOOK = new GuideBookItem(itemSettings("guide_book").maxCount(1)));

        event.register(RegistryKeys.ITEM, id("bloody_essence"),
                () -> BLOODY_ESSENCE = new Item(itemSettings("bloody_essence")));
        event.register(RegistryKeys.ITEM, id("toxic_essence"),
                () -> TOXIC_ESSENCE = new Item(itemSettings("toxic_essence")));
        event.register(RegistryKeys.ITEM, id("withering_essence"),
                () -> WITHERING_ESSENCE = new Item(itemSettings("withering_essence")));
        event.register(RegistryKeys.ITEM, id("golden_essence"),
                () -> GOLDEN_ESSENCE = new Item(itemSettings("golden_essence")));
        event.register(RegistryKeys.ITEM, id("frozen_essence"),
                () -> FROZEN_ESSENCE = new Item(itemSettings("frozen_essence")));
        event.register(RegistryKeys.ITEM, id("farmer_essence"),
                () -> FARMER_ESSENCE = new Item(itemSettings("farmer_essence")));
        event.register(RegistryKeys.ITEM, id("fire_essence"),
                () -> FIRE_ESSENCE = new Item(itemSettings("fire_essence")));
        event.register(RegistryKeys.ITEM, id("frozen_heart"),
                () -> FROZEN_HEART = new FrozenHeartItem(itemSettings("frozen_heart").food(FROZEN_HEART_FOOD)));

        event.register(RegistryKeys.ENTITY_TYPE, id("toxic_orb"), () -> TOXIC_ORB = EntityType.Builder
                .<ToxicOrbEntity>create(ToxicOrbEntity::new, SpawnGroup.MISC)
                .dimensions(ScytheBalance.ToxicOrb.WIDTH, ScytheBalance.ToxicOrb.HEIGHT)
                .maxTrackingRange(ScytheBalance.ToxicOrb.TRACKING_RANGE)
                .trackingTickInterval(ScytheBalance.ToxicOrb.UPDATE_INTERVAL)
                .build(entityTypeKey("toxic_orb")));

        event.register(RegistryKeys.ENTITY_TYPE, id("fireball"), () -> FIREBALL = EntityType.Builder
                .<FireballEntity>create(FireballEntity::new, SpawnGroup.MISC)
                .dimensions(ScytheBalance.Fire.FIREBALL_WIDTH, ScytheBalance.Fire.FIREBALL_HEIGHT)
                .maxTrackingRange(ScytheBalance.Fire.FIREBALL_TRACKING_RANGE)
                .trackingTickInterval(ScytheBalance.Fire.FIREBALL_UPDATE_INTERVAL)
                .build(entityTypeKey("fireball")));

        event.register(RegistryKeys.ENTITY_TYPE, id("ice_spike"), () -> ICE_SPIKE = EntityType.Builder
                .<IceSpikeEntity>create(IceSpikeEntity::new, SpawnGroup.MISC)
                .dimensions(ScytheBalance.IceSpike.WIDTH, ScytheBalance.IceSpike.HEIGHT)
                .maxTrackingRange(ScytheBalance.IceSpike.TRACKING_RANGE)
                .trackingTickInterval(ScytheBalance.IceSpike.UPDATE_INTERVAL)
                .build(entityTypeKey("ice_spike")));

        event.register(RegistryKeys.ENTITY_TYPE, id("withering_minion"), () -> WITHERING_MINION = EntityType.Builder
                .<WitheringMinionEntity>create(WitheringMinionEntity::new, SpawnGroup.MONSTER)
                .dimensions(ScytheBalance.Minion.WIDTH, ScytheBalance.Minion.HEIGHT)
                .makeFireImmune()
                .maxTrackingRange(ScytheBalance.Minion.TRACKING_RANGE)
                .trackingTickInterval(ScytheBalance.Minion.UPDATE_INTERVAL)
                .build(entityTypeKey("withering_minion")));

        event.register(RegistryKeys.RECIPE_SERIALIZER, id("craft_toxic_essence"),
                () -> TOXIC_ESSENCE_RECIPE_SERIALIZER =
                        new SpecialCraftingRecipe.SpecialRecipeSerializer<>(ToxicEssenceRecipe::new));
        event.register(RegistryKeys.RECIPE_SERIALIZER, id("craft_fire_essence"),
                () -> FIRE_ESSENCE_RECIPE_SERIALIZER =
                        new SpecialCraftingRecipe.SpecialRecipeSerializer<>(FireEssenceRecipe::new));

        event.register(RegistryKeys.STATUS_EFFECT, helper -> {
            StatusEffect bleeding = new BleedingEffect();
            helper.register(id("bleeding"), bleeding);
            BLEEDING = Registries.STATUS_EFFECT.getEntry(bleeding);

            StatusEffect noJump = new NoJumpEffect();
            helper.register(id("no_jump"), noJump);
            NO_JUMP = Registries.STATUS_EFFECT.getEntry(noJump);

            StatusEffect freezing = new FreezingEffect();
            helper.register(id("freezing"), freezing);
            FREEZING = Registries.STATUS_EFFECT.getEntry(freezing);

            StatusEffect burns = new BurnsEffect();
            helper.register(id("burns"), burns);
            BURNS = Registries.STATUS_EFFECT.getEntry(burns);
        });

        event.register(RegistryKeys.ITEM_GROUP, SCYTHE_ITEM_GROUP_ID,
                () -> SCYTHE_ITEM_GROUP = createItemGroup());
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(WITHERING_MINION, WitheringMinionEntity.createAttributes().build());
    }

    private static ItemGroup createItemGroup() {
        return ItemGroup.builder()
                .icon(ScytheMod::createRotatingTabIcon)
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
                .build();
    }

    public static ItemStack createRotatingTabIcon() {
        Item[] tabIconItems = new Item[] {
                BLOODY_SCYTHE, TOXIC_SCYTHE, WITHERING_SCYTHE, GOLDEN_SCYTHE,
                FROZEN_SCYTHE, FARMER_SCYTHE, FIRE_SCYTHE
        };
        int index = (int) ((System.currentTimeMillis() / ScytheBalance.Presentation.TAB_ICON_INTERVAL_MS) % tabIconItems.length);
        Item item = tabIconItems[index];
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    private static RegistryKey<Item> itemKey(String name) {
        return RegistryKey.of(RegistryKeys.ITEM, id(name));
    }

    private static RegistryKey<EntityType<?>> entityTypeKey(String name) {
        return RegistryKey.of(RegistryKeys.ENTITY_TYPE, id(name));
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
}
