package com.shipovskijkorp.scythes.mod;

import com.shipovskijkorp.scythes.mod.ability.*;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.effect.BleedingEffect;
import com.shipovskijkorp.scythes.mod.effect.FreezingEffect;
import com.shipovskijkorp.scythes.mod.effect.NoJumpEffect;
import com.shipovskijkorp.scythes.mod.enchantment.ScytheEnchantment;
import com.shipovskijkorp.scythes.mod.entity.IceSpikeEntity;
import com.shipovskijkorp.scythes.mod.entity.ToxicOrbEntity;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import com.shipovskijkorp.scythes.mod.item.FrozenHeartItem;
import com.shipovskijkorp.scythes.mod.item.FrozenScytheItem;
import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import com.shipovskijkorp.scythes.mod.item.ScytheMaterial;
import com.shipovskijkorp.scythes.mod.item.ScytheSwordItem;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import com.shipovskijkorp.scythes.mod.network.ScytheAbilityC2SPacket;
import com.shipovskijkorp.scythes.mod.platform.fabric.FabricServerHooks;
import com.shipovskijkorp.scythes.mod.recipe.ToxicEssenceRecipe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScytheMod implements ModInitializer {

	public static final String MOD_ID = "scythes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Item BLOODY_SCYTHE = new BloodScytheItem(ScytheMaterial.configure(new Item.Settings()));
	public static final Item TOXIC_SCYTHE = new ToxicScytheItem(ScytheMaterial.configure(new Item.Settings()));
	/** Legacy placeholder. Kept registered so old scythes:plague_scythe stacks survive load and can be migrated. */
	public static final Item PLAGUE_SCYTHE = new ScytheSwordItem(ScytheMaterial.configure(new Item.Settings()));
	public static final Item WITHERING_SCYTHE = new WitheringScytheItem(ScytheMaterial.configure(new Item.Settings()));
	public static final Item GOLDEN_SCYTHE = new GoldenScytheItem(ScytheMaterial.configure(new Item.Settings()));
	public static final Item FROZEN_SCYTHE = new FrozenScytheItem(ScytheMaterial.configure(new Item.Settings()));
	public static final Item FARMER_SCYTHE = new FarmerScytheItem(ScytheMaterial.configureBase(new Item.Settings()));

	public static final EntityType<IceSpikeEntity> ICE_SPIKE = FabricEntityTypeBuilder
			.<IceSpikeEntity>create(SpawnGroup.MISC, IceSpikeEntity::new)
			.dimensions(EntityDimensions.fixed(ScytheBalance.IceSpike.WIDTH, ScytheBalance.IceSpike.HEIGHT))
			.trackRangeBlocks(ScytheBalance.IceSpike.TRACKING_RANGE)
			.trackedUpdateRate(ScytheBalance.IceSpike.UPDATE_INTERVAL)
			.build();

	public static final EntityType<ToxicOrbEntity> TOXIC_ORB = FabricEntityTypeBuilder
			.<ToxicOrbEntity>create(SpawnGroup.MISC, ToxicOrbEntity::new)
			.dimensions(EntityDimensions.fixed(ScytheBalance.ToxicOrb.WIDTH, ScytheBalance.ToxicOrb.HEIGHT))
			.trackRangeBlocks(ScytheBalance.ToxicOrb.TRACKING_RANGE)
			.trackedUpdateRate(ScytheBalance.ToxicOrb.UPDATE_INTERVAL)
			.build();

	public static final EntityType<WitheringMinionEntity> WITHERING_MINION = FabricEntityTypeBuilder
			.<WitheringMinionEntity>create(SpawnGroup.MONSTER, WitheringMinionEntity::new)
			.dimensions(EntityDimensions.fixed(ScytheBalance.Minion.WIDTH, ScytheBalance.Minion.HEIGHT))
			.trackRangeBlocks(ScytheBalance.Minion.TRACKING_RANGE)
			.trackedUpdateRate(ScytheBalance.Minion.UPDATE_INTERVAL)
			.build();

	public static final Item BLOODY_ESSENCE = new Item(new Item.Settings());
	public static final Item TOXIC_ESSENCE = new Item(new Item.Settings());
	public static final Item WITHERING_ESSENCE = new Item(new Item.Settings());
	public static final Item GOLDEN_ESSENCE = new Item(new Item.Settings());
	public static final Item FROZEN_ESSENCE = new Item(new Item.Settings());
	public static final Item FARMER_ESSENCE = new Item(new Item.Settings());
	private static final FoodComponent FROZEN_HEART_FOOD = new FoodComponent.Builder()
			.hunger(ScytheBalance.FrozenHeart.NUTRITION)
			.saturationModifier(ScytheBalance.FrozenHeart.SATURATION_MODIFIER)
			.meat()
			.alwaysEdible()
			.build();
	public static final Item FROZEN_HEART = new FrozenHeartItem(
			new Item.Settings().food(FROZEN_HEART_FOOD)
	);

	public static final RecipeSerializer<ToxicEssenceRecipe> TOXIC_ESSENCE_RECIPE_SERIALIZER =
			new SpecialRecipeSerializer<>(ToxicEssenceRecipe::new);

	public static final Identifier SCYTHE_ITEM_GROUP_ID = new Identifier(MOD_ID, "scythes");
	public static ItemGroup SCYTHE_ITEM_GROUP;

	private static final Item[] TAB_ICON_ITEMS = new Item[] { BLOODY_SCYTHE, TOXIC_SCYTHE, WITHERING_SCYTHE, GOLDEN_SCYTHE, FROZEN_SCYTHE, FARMER_SCYTHE };

	public static final StatusEffect BLEEDING = new BleedingEffect();
	public static final StatusEffect NO_JUMP = new NoJumpEffect();
	public static final StatusEffect FREEZING = new FreezingEffect();

	public static final Enchantment SPIKED_BLADE = ScytheEnchantment.spikedBlade();
	public static final Enchantment ADDITIONAL_SLOT = ScytheEnchantment.additionalSlot();
	public static final Enchantment SOUL_SIPHON = ScytheEnchantment.soulSiphon();
	public static final Enchantment ACIDITY = ScytheEnchantment.acidity();

	@Override
	public void onInitialize() {
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "bloody_scythe"), BLOODY_SCYTHE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "toxic_scythe"), TOXIC_SCYTHE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "plague_scythe"), PLAGUE_SCYTHE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "withering_scythe"), WITHERING_SCYTHE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "golden_scythe"), GOLDEN_SCYTHE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "frozen_scythe"), FROZEN_SCYTHE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "farmer_scythe"), FARMER_SCYTHE);
		Registry.register(Registries.ENTITY_TYPE, new Identifier(MOD_ID, "ice_spike"), ICE_SPIKE);
		Registry.register(Registries.ENTITY_TYPE, new Identifier(MOD_ID, "toxic_orb"), TOXIC_ORB);
		Registry.register(Registries.ENTITY_TYPE, new Identifier(MOD_ID, "withering_minion"), WITHERING_MINION);
		FabricDefaultAttributeRegistry.register(WITHERING_MINION, WitheringMinionEntity.createAttributes());
		Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(MOD_ID, "craft_toxic_essence"), TOXIC_ESSENCE_RECIPE_SERIALIZER);

		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "bloody_essence"), BLOODY_ESSENCE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "toxic_essence"), TOXIC_ESSENCE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "withering_essence"), WITHERING_ESSENCE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "golden_essence"), GOLDEN_ESSENCE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "frozen_essence"), FROZEN_ESSENCE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "farmer_essence"), FARMER_ESSENCE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "frozen_heart"), FROZEN_HEART);

		Registry.register(Registries.STATUS_EFFECT, new Identifier(MOD_ID, "bleeding"), BLEEDING);
		Registry.register(Registries.STATUS_EFFECT, new Identifier(MOD_ID, "no_jump"), NO_JUMP);
		Registry.register(Registries.STATUS_EFFECT, new Identifier(MOD_ID, "freezing"), FREEZING);

		Registry.register(Registries.ENCHANTMENT, new Identifier(MOD_ID, "spiked_blade"), SPIKED_BLADE);
		Registry.register(Registries.ENCHANTMENT, new Identifier(MOD_ID, "additional_slot"), ADDITIONAL_SLOT);
		Registry.register(Registries.ENCHANTMENT, new Identifier(MOD_ID, "soul_siphon"), SOUL_SIPHON);
		Registry.register(Registries.ENCHANTMENT, new Identifier(MOD_ID, "acidity"), ACIDITY);

		SCYTHE_ITEM_GROUP = Registry.register(
				Registries.ITEM_GROUP,
				SCYTHE_ITEM_GROUP_ID,
				FabricItemGroup.builder()
						.icon(() -> new ItemStack(BLOODY_SCYTHE))
						.displayName(Text.translatable("itemGroup." + MOD_ID + ".scythes"))
						.entries((displayContext, entries) -> {
							entries.add(BLOODY_SCYTHE);
							entries.add(TOXIC_SCYTHE);
							entries.add(WITHERING_SCYTHE);
							entries.add(GOLDEN_SCYTHE);
							entries.add(FROZEN_SCYTHE);
							entries.add(FARMER_SCYTHE);
							entries.add(BLOODY_ESSENCE);
							entries.add(TOXIC_ESSENCE);
							entries.add(WITHERING_ESSENCE);
							entries.add(GOLDEN_ESSENCE);
							entries.add(FROZEN_ESSENCE);
							entries.add(FARMER_ESSENCE);
							entries.add(FROZEN_HEART);
						})
						.build()
		);

		ScytheAbilityC2SPacket.register();
        FabricServerHooks.register();

		LOGGER.info("ScytheMod initialized successfully");
	}

	public static ItemStack createRotatingTabIcon() {
		int index = (int) ((System.currentTimeMillis() / ScytheBalance.Presentation.TAB_ICON_INTERVAL_MS) % TAB_ICON_ITEMS.length);
		return new ItemStack(TAB_ICON_ITEMS[index]);
	}

}
