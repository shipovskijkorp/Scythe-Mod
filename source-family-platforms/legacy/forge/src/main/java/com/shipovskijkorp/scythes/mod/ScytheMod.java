package com.shipovskijkorp.scythes.mod;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.effect.BleedingEffect;
import com.shipovskijkorp.scythes.mod.effect.BurnsEffect;
import com.shipovskijkorp.scythes.mod.effect.FreezingEffect;
import com.shipovskijkorp.scythes.mod.effect.NoJumpEffect;
import com.shipovskijkorp.scythes.mod.enchantment.ScytheEnchantment;
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
import com.shipovskijkorp.scythes.mod.network.ScytheAbilityC2SPacket;
import com.shipovskijkorp.scythes.mod.platform.forge.ForgeServerHooks;
import com.shipovskijkorp.scythes.mod.recipe.FireEssenceRecipe;
import com.shipovskijkorp.scythes.mod.recipe.ToxicEssenceRecipe;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ScytheMod.MOD_ID)
public class ScytheMod {

    public static final String MOD_ID = "scythes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /*
     * Forge 1.20.1 freezes registries before the mod class may safely construct
     * registrable objects. Keep the public gameplay-facing fields, but populate
     * them from RegisterEvent suppliers while the matching registry is unfrozen.
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

    public static EntityType<IceSpikeEntity> ICE_SPIKE;
    public static EntityType<ToxicOrbEntity> TOXIC_ORB;
    public static EntityType<FireballEntity> FIREBALL;
    public static EntityType<WitheringMinionEntity> WITHERING_MINION;

    public static Item BLOODY_ESSENCE;
    public static Item TOXIC_ESSENCE;
    public static Item WITHERING_ESSENCE;
    public static Item GOLDEN_ESSENCE;
    public static Item FROZEN_ESSENCE;
    public static Item FARMER_ESSENCE;
    public static Item FIRE_ESSENCE;

    private static final FoodComponent FROZEN_HEART_FOOD = new FoodComponent.Builder()
            .hunger(ScytheBalance.FrozenHeart.NUTRITION)
            .saturationModifier(ScytheBalance.FrozenHeart.SATURATION_MODIFIER)
            .meat()
            .alwaysEdible()
            .build();
    public static Item FROZEN_HEART;

    public static RecipeSerializer<ToxicEssenceRecipe> TOXIC_ESSENCE_RECIPE_SERIALIZER;
    public static RecipeSerializer<FireEssenceRecipe> FIRE_ESSENCE_RECIPE_SERIALIZER;

    public static final Identifier SCYTHE_ITEM_GROUP_ID = id("scythes");
    public static ItemGroup SCYTHE_ITEM_GROUP;

    public static StatusEffect BLEEDING;
    public static StatusEffect NO_JUMP;
    public static StatusEffect FREEZING;
    public static StatusEffect BURNS;

    public static Enchantment SPIKED_BLADE;
    public static Enchantment ADDITIONAL_SLOT;
    public static Enchantment SOUL_SIPHON;
    public static Enchantment ACIDITY;

    public ScytheMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::registerContent);
        modBus.addListener(this::registerAttributes);

        ScytheAbilityC2SPacket.register();
        ForgeServerHooks.register();

        LOGGER.info("ScytheMod initialized successfully");
    }

    private void registerContent(RegisterEvent event) {
        event.register(RegistryKeys.ITEM, id("bloody_scythe"),
                () -> BLOODY_SCYTHE = new BloodScytheItem(ScytheMaterial.configure(new Item.Settings())));
        event.register(RegistryKeys.ITEM, id("toxic_scythe"),
                () -> TOXIC_SCYTHE = new ToxicScytheItem(ScytheMaterial.configure(new Item.Settings())));
        event.register(RegistryKeys.ITEM, id("plague_scythe"),
                () -> PLAGUE_SCYTHE = new ScytheSwordItem(ScytheMaterial.configure(new Item.Settings())));
        event.register(RegistryKeys.ITEM, id("withering_scythe"),
                () -> WITHERING_SCYTHE = new WitheringScytheItem(ScytheMaterial.configure(new Item.Settings())));
        event.register(RegistryKeys.ITEM, id("golden_scythe"),
                () -> GOLDEN_SCYTHE = new GoldenScytheItem(ScytheMaterial.configure(new Item.Settings())));
        event.register(RegistryKeys.ITEM, id("frozen_scythe"),
                () -> FROZEN_SCYTHE = new FrozenScytheItem(ScytheMaterial.configure(new Item.Settings())));
        event.register(RegistryKeys.ITEM, id("farmer_scythe"),
                () -> FARMER_SCYTHE = new FarmerScytheItem(ScytheMaterial.configureBase(new Item.Settings())));
        event.register(RegistryKeys.ITEM, id("fire_scythe"),
                () -> FIRE_SCYTHE = new FireScytheItem(ScytheMaterial.configure(new Item.Settings())));
        event.register(RegistryKeys.ITEM, id("guide_book"),
                () -> GUIDE_BOOK = new GuideBookItem(new Item.Settings().maxCount(1)));

        event.register(RegistryKeys.ITEM, id("bloody_essence"),
                () -> BLOODY_ESSENCE = new Item(new Item.Settings()));
        event.register(RegistryKeys.ITEM, id("toxic_essence"),
                () -> TOXIC_ESSENCE = new Item(new Item.Settings()));
        event.register(RegistryKeys.ITEM, id("withering_essence"),
                () -> WITHERING_ESSENCE = new Item(new Item.Settings()));
        event.register(RegistryKeys.ITEM, id("golden_essence"),
                () -> GOLDEN_ESSENCE = new Item(new Item.Settings()));
        event.register(RegistryKeys.ITEM, id("frozen_essence"),
                () -> FROZEN_ESSENCE = new Item(new Item.Settings()));
        event.register(RegistryKeys.ITEM, id("farmer_essence"),
                () -> FARMER_ESSENCE = new Item(new Item.Settings()));
        event.register(RegistryKeys.ITEM, id("fire_essence"),
                () -> FIRE_ESSENCE = new Item(new Item.Settings()));
        event.register(RegistryKeys.ITEM, id("frozen_heart"),
                () -> FROZEN_HEART = new FrozenHeartItem(new Item.Settings().food(FROZEN_HEART_FOOD)));

        event.register(RegistryKeys.ENTITY_TYPE, id("ice_spike"), () -> ICE_SPIKE = EntityType.Builder
                .<IceSpikeEntity>create(IceSpikeEntity::new, SpawnGroup.MISC)
                .setDimensions(ScytheBalance.IceSpike.WIDTH, ScytheBalance.IceSpike.HEIGHT)
                .maxTrackingRange(ScytheBalance.IceSpike.TRACKING_RANGE)
                .trackingTickInterval(ScytheBalance.IceSpike.UPDATE_INTERVAL)
                .build(id("ice_spike").toString()));

        event.register(RegistryKeys.ENTITY_TYPE, id("toxic_orb"), () -> TOXIC_ORB = EntityType.Builder
                .<ToxicOrbEntity>create(ToxicOrbEntity::new, SpawnGroup.MISC)
                .setDimensions(ScytheBalance.ToxicOrb.WIDTH, ScytheBalance.ToxicOrb.HEIGHT)
                .maxTrackingRange(ScytheBalance.ToxicOrb.TRACKING_RANGE)
                .trackingTickInterval(ScytheBalance.ToxicOrb.UPDATE_INTERVAL)
                .build(id("toxic_orb").toString()));

        event.register(RegistryKeys.ENTITY_TYPE, id("fireball"), () -> FIREBALL = EntityType.Builder
                .<FireballEntity>create(FireballEntity::new, SpawnGroup.MISC)
                .setDimensions(ScytheBalance.Fire.FIREBALL_WIDTH, ScytheBalance.Fire.FIREBALL_HEIGHT)
                .maxTrackingRange(ScytheBalance.Fire.FIREBALL_TRACKING_RANGE)
                .trackingTickInterval(ScytheBalance.Fire.FIREBALL_UPDATE_INTERVAL)
                .build(id("fireball").toString()));

        event.register(RegistryKeys.ENTITY_TYPE, id("withering_minion"), () -> WITHERING_MINION = EntityType.Builder
                .<WitheringMinionEntity>create(WitheringMinionEntity::new, SpawnGroup.MONSTER)
                .setDimensions(ScytheBalance.Minion.WIDTH, ScytheBalance.Minion.HEIGHT)
                .makeFireImmune()
                .maxTrackingRange(ScytheBalance.Minion.TRACKING_RANGE)
                .trackingTickInterval(ScytheBalance.Minion.UPDATE_INTERVAL)
                .build(id("withering_minion").toString()));

        event.register(RegistryKeys.RECIPE_SERIALIZER, id("craft_toxic_essence"),
                () -> TOXIC_ESSENCE_RECIPE_SERIALIZER = new SpecialRecipeSerializer<>(ToxicEssenceRecipe::new));
        event.register(RegistryKeys.RECIPE_SERIALIZER, id("craft_fire_essence"),
                () -> FIRE_ESSENCE_RECIPE_SERIALIZER = new SpecialRecipeSerializer<>(FireEssenceRecipe::new));

        event.register(RegistryKeys.STATUS_EFFECT, id("bleeding"),
                () -> BLEEDING = new BleedingEffect());
        event.register(RegistryKeys.STATUS_EFFECT, id("no_jump"),
                () -> NO_JUMP = new NoJumpEffect());
        event.register(RegistryKeys.STATUS_EFFECT, id("freezing"),
                () -> FREEZING = new FreezingEffect());
        event.register(RegistryKeys.STATUS_EFFECT, id("burns"),
                () -> BURNS = new BurnsEffect());

        event.register(RegistryKeys.ENCHANTMENT, id("spiked_blade"),
                () -> SPIKED_BLADE = ScytheEnchantment.spikedBlade());
        event.register(RegistryKeys.ENCHANTMENT, id("additional_slot"),
                () -> ADDITIONAL_SLOT = ScytheEnchantment.additionalSlot());
        event.register(RegistryKeys.ENCHANTMENT, id("soul_siphon"),
                () -> SOUL_SIPHON = ScytheEnchantment.soulSiphon());
        event.register(RegistryKeys.ENCHANTMENT, id("acidity"),
                () -> ACIDITY = ScytheEnchantment.acidity());

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

    private static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
