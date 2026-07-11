package com.shipovskijkorp.scythes.mod;

import com.shipovskijkorp.scythes.mod.ability.*;
import com.shipovskijkorp.scythes.mod.effect.BleedingEffect;
import com.shipovskijkorp.scythes.mod.effect.NoJumpEffect;
import com.shipovskijkorp.scythes.mod.enchantment.AcidityEnchantment;
import com.shipovskijkorp.scythes.mod.enchantment.AdditionalSlotEnchantment;
import com.shipovskijkorp.scythes.mod.enchantment.SoulSiphonEnchantment;
import com.shipovskijkorp.scythes.mod.enchantment.SpikedBladeEnchantment;
import com.shipovskijkorp.scythes.mod.entity.ToxicOrbEntity;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import com.shipovskijkorp.scythes.mod.item.FrozenScytheItem;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import com.shipovskijkorp.scythes.mod.network.ScytheAbilityC2SPacket;
import com.shipovskijkorp.scythes.mod.recipe.ToxicEssenceRecipe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
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
	public static final Item PLAGUE_SCYTHE = new SwordItem(ToolMaterials.NETHERITE, 4, -2.8F, new Item.Settings().maxCount(1).fireproof());
	public static final Item WITHERING_SCYTHE = new WitheringScytheItem(new Item.Settings().maxCount(1).fireproof());
	public static final Item GOLDEN_SCYTHE = new GoldenScytheItem(new Item.Settings().maxCount(1).fireproof());
	public static final Item FROZEN_SCYTHE = new FrozenScytheItem(new Item.Settings().maxCount(1).fireproof());

	public static final EntityType<ToxicOrbEntity> TOXIC_ORB = FabricEntityTypeBuilder
			.<ToxicOrbEntity>create(SpawnGroup.MISC, ToxicOrbEntity::new)
			.dimensions(EntityDimensions.fixed(0.35F, 0.35F))
			.trackRangeBlocks(4)
			.trackedUpdateRate(10)
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
	public static final Item FROZEN_HEART = new Item(new Item.Settings());

	public static final RecipeSerializer<ToxicEssenceRecipe> TOXIC_ESSENCE_RECIPE_SERIALIZER =
			new SpecialRecipeSerializer<>(ToxicEssenceRecipe::new);

	public static final Identifier SCYTHE_ITEM_GROUP_ID = new Identifier(MOD_ID, "scythes");
	public static ItemGroup SCYTHE_ITEM_GROUP;

	private static final Item[] TAB_ICON_ITEMS = new Item[] { BLOODY_SCYTHE, TOXIC_SCYTHE, WITHERING_SCYTHE, GOLDEN_SCYTHE, FROZEN_SCYTHE };
	private static final long TAB_ICON_INTERVAL_MS = 1200L;

	public static final StatusEffect BLEEDING = new BleedingEffect();
	public static final StatusEffect NO_JUMP = new NoJumpEffect();

	public static final Enchantment SPIKED_BLADE = new SpikedBladeEnchantment();
	public static final Enchantment ADDITIONAL_SLOT = new AdditionalSlotEnchantment();
	public static final Enchantment SOUL_SIPHON = new SoulSiphonEnchantment();
	public static final Enchantment ACIDITY = new AcidityEnchantment();

	@Override
	public void onInitialize() {
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "bloody_scythe"), BLOODY_SCYTHE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "toxic_scythe"), TOXIC_SCYTHE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "plague_scythe"), PLAGUE_SCYTHE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "withering_scythe"), WITHERING_SCYTHE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "golden_scythe"), GOLDEN_SCYTHE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "frozen_scythe"), FROZEN_SCYTHE);
		Registry.register(Registries.ENTITY_TYPE, new Identifier(MOD_ID, "toxic_orb"), TOXIC_ORB);
		Registry.register(Registries.ENTITY_TYPE, new Identifier(MOD_ID, "withering_minion"), WITHERING_MINION);
		FabricDefaultAttributeRegistry.register(WITHERING_MINION, WitheringMinionEntity.createAttributes());
		Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(MOD_ID, "craft_toxic_essence"), TOXIC_ESSENCE_RECIPE_SERIALIZER);

		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "bloody_essence"), BLOODY_ESSENCE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "toxic_essence"), TOXIC_ESSENCE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "withering_essence"), WITHERING_ESSENCE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "golden_essence"), GOLDEN_ESSENCE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "frozen_essence"), FROZEN_ESSENCE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "frozen_heart"), FROZEN_HEART);

		Registry.register(Registries.STATUS_EFFECT, new Identifier(MOD_ID, "bleeding"), BLEEDING);
		Registry.register(Registries.STATUS_EFFECT, new Identifier(MOD_ID, "no_jump"), NO_JUMP);

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
							entries.add(BLOODY_ESSENCE);
							entries.add(TOXIC_ESSENCE);
							entries.add(WITHERING_ESSENCE);
							entries.add(GOLDEN_ESSENCE);
							entries.add(FROZEN_ESSENCE);
							entries.add(FROZEN_HEART);
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
