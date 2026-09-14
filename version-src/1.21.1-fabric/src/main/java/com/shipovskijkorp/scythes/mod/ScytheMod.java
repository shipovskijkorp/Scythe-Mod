package com.shipovskijkorp.scythes.mod;

import com.shipovskijkorp.scythes.mod.ability.*;
import com.shipovskijkorp.scythes.mod.effect.BleedingEffect;
import com.shipovskijkorp.scythes.mod.effect.FreezingEffect;
import com.shipovskijkorp.scythes.mod.effect.NoJumpEffect;
import com.shipovskijkorp.scythes.mod.entity.IceSpikeEntity;
import com.shipovskijkorp.scythes.mod.entity.ToxicOrbEntity;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.item.FrozenHeartItem;
import com.shipovskijkorp.scythes.mod.item.FrozenScytheItem;
import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import com.shipovskijkorp.scythes.mod.network.ModPackets;
import com.shipovskijkorp.scythes.mod.network.ScytheAbilityC2SPacket;
import com.shipovskijkorp.scythes.mod.recipe.ToxicEssenceRecipe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScytheMod implements ModInitializer {

	public static final String MOD_ID = "scythes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Item BLOODY_SCYTHE = new BloodScytheItem(new Item.Settings().maxCount(1).fireproof());
	public static final Item TOXIC_SCYTHE = new ToxicScytheItem(new Item.Settings().maxCount(1).fireproof());
	/** Legacy placeholder. Kept registered so old scythes:plague_scythe stacks survive load and can be migrated. */
	public static final Item PLAGUE_SCYTHE = new SwordItem(ToolMaterials.NETHERITE, new Item.Settings().maxCount(1).fireproof().attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 4, -2.8F)));
	public static final Item WITHERING_SCYTHE = new WitheringScytheItem(new Item.Settings().maxCount(1).fireproof());
	public static final Item GOLDEN_SCYTHE = new GoldenScytheItem(new Item.Settings().maxCount(1).fireproof());
	public static final Item FROZEN_SCYTHE = new FrozenScytheItem(new Item.Settings().maxCount(1).fireproof());

	public static final EntityType<ToxicOrbEntity> TOXIC_ORB = FabricEntityTypeBuilder
			.<ToxicOrbEntity>create(SpawnGroup.MISC, ToxicOrbEntity::new)
			.dimensions(EntityDimensions.fixed(0.35F, 0.35F))
			.trackRangeBlocks(4)
			.trackedUpdateRate(10)
			.build();

	public static final EntityType<IceSpikeEntity> ICE_SPIKE = FabricEntityTypeBuilder
			.<IceSpikeEntity>create(SpawnGroup.MISC, IceSpikeEntity::new)
			.dimensions(EntityDimensions.fixed(0.5F, 0.5F))
			.trackRangeBlocks(8)
			.trackedUpdateRate(1)
			.build();

	public static final EntityType<WitheringMinionEntity> WITHERING_MINION = FabricEntityTypeBuilder
			.<WitheringMinionEntity>create(SpawnGroup.MONSTER, WitheringMinionEntity::new)
			.dimensions(EntityDimensions.fixed(0.7F, 2.4F))
			.trackRangeBlocks(8)
			.trackedUpdateRate(3)
			.build();

	public static final Item BLOODY_ESSENCE = new Item(new Item.Settings());
	public static final Item TOXIC_ESSENCE = new Item(new Item.Settings());
	public static final Item WITHERING_ESSENCE = new Item(new Item.Settings());
	public static final Item GOLDEN_ESSENCE = new Item(new Item.Settings());
	public static final Item FROZEN_ESSENCE = new Item(new Item.Settings());
	private static final FoodComponent FROZEN_HEART_FOOD = new FoodComponent.Builder()
			.nutrition(3)
			.saturationModifier(0.3F)
			.alwaysEdible()
			.build();
	public static final Item FROZEN_HEART = new FrozenHeartItem(new Item.Settings().food(FROZEN_HEART_FOOD));

	public static final RecipeSerializer<ToxicEssenceRecipe> TOXIC_ESSENCE_RECIPE_SERIALIZER =
			new SpecialRecipeSerializer<>(ToxicEssenceRecipe::new);

	public static final Identifier SCYTHE_ITEM_GROUP_ID = id("scythes");
	public static ItemGroup SCYTHE_ITEM_GROUP;

	private static final Item[] TAB_ICON_ITEMS = new Item[] { BLOODY_SCYTHE, TOXIC_SCYTHE, WITHERING_SCYTHE, GOLDEN_SCYTHE, FROZEN_SCYTHE };
	private static final long TAB_ICON_INTERVAL_MS = 1200L;

	public static final RegistryEntry<StatusEffect> BLEEDING =
			Registry.registerReference(Registries.STATUS_EFFECT, id("bleeding"), new BleedingEffect());
	public static final RegistryEntry<StatusEffect> NO_JUMP =
			Registry.registerReference(Registries.STATUS_EFFECT, id("no_jump"), new NoJumpEffect());
	public static final RegistryEntry<StatusEffect> FREEZING =
			Registry.registerReference(Registries.STATUS_EFFECT, id("freezing"), new FreezingEffect());

	public static final RegistryKey<Enchantment> SPIKED_BLADE = RegistryKey.of(RegistryKeys.ENCHANTMENT, id("spiked_blade"));
	public static final RegistryKey<Enchantment> ADDITIONAL_SLOT = RegistryKey.of(RegistryKeys.ENCHANTMENT, id("additional_slot"));
	public static final RegistryKey<Enchantment> SOUL_SIPHON = RegistryKey.of(RegistryKeys.ENCHANTMENT, id("soul_siphon"));
	public static final RegistryKey<Enchantment> ACIDITY = RegistryKey.of(RegistryKeys.ENCHANTMENT, id("acidity"));

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		Registry.register(Registries.ITEM, id("bloody_scythe"), BLOODY_SCYTHE);
		Registry.register(Registries.ITEM, id("toxic_scythe"), TOXIC_SCYTHE);
		Registry.register(Registries.ITEM, id("plague_scythe"), PLAGUE_SCYTHE);
		Registry.register(Registries.ITEM, id("withering_scythe"), WITHERING_SCYTHE);
		Registry.register(Registries.ITEM, id("golden_scythe"), GOLDEN_SCYTHE);
		Registry.register(Registries.ITEM, id("frozen_scythe"), FROZEN_SCYTHE);
		Registry.register(Registries.ENTITY_TYPE, id("toxic_orb"), TOXIC_ORB);
		Registry.register(Registries.ENTITY_TYPE, id("ice_spike"), ICE_SPIKE);
		Registry.register(Registries.ENTITY_TYPE, id("withering_minion"), WITHERING_MINION);
		FabricDefaultAttributeRegistry.register(WITHERING_MINION, WitheringMinionEntity.createAttributes());
		Registry.register(Registries.RECIPE_SERIALIZER, id("craft_toxic_essence"), TOXIC_ESSENCE_RECIPE_SERIALIZER);

		Registry.register(Registries.ITEM, id("bloody_essence"), BLOODY_ESSENCE);
		Registry.register(Registries.ITEM, id("toxic_essence"), TOXIC_ESSENCE);
		Registry.register(Registries.ITEM, id("withering_essence"), WITHERING_ESSENCE);
		Registry.register(Registries.ITEM, id("golden_essence"), GOLDEN_ESSENCE);
		Registry.register(Registries.ITEM, id("frozen_essence"), FROZEN_ESSENCE);
		Registry.register(Registries.ITEM, id("frozen_heart"), FROZEN_HEART);



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
							entries.add(BLOODY_ESSENCE);
							entries.add(TOXIC_ESSENCE);
							entries.add(WITHERING_ESSENCE);
							entries.add(GOLDEN_ESSENCE);
							entries.add(FROZEN_ESSENCE);
							entries.add(FROZEN_HEART);
						})
						.build()
		);

		ModPackets.registerPayloadTypes();
		ScytheAbilityC2SPacket.register();
		PlagueScytheMigrationHandler.register();
		WelcomeAdvancementHandler.register();
		BloodHarvestKillHandler.register();
		WitheringSoulHandler.register();
		BloodyEssenceDropHandler.register();
		FrozenHeartDropHandler.register();

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.player;
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
				ScytheAdvancementTracker.clear(player);
			});
		});

		ServerTickEvents.END_SERVER_TICK.register(this::tickServer);

		LOGGER.info("ScytheMod initialized successfully");
	}

	public static ItemStack createRotatingTabIcon() {
		int index = (int) ((System.currentTimeMillis() / TAB_ICON_INTERVAL_MS) % TAB_ICON_ITEMS.length);
		return new ItemStack(TAB_ICON_ITEMS[index]);
	}

	private void tickServer(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			BloodHarvestTracker.tick(player);
			ToxicAuraTracker.tick(player);
			WitheringAuraTracker.tick(player);
			PlagueScytheMigrationHandler.migratePlayer(player);
		}
		GoldenLootMarkTracker.tick(server);
	}
}
