package com.example.bloodyscythe;

import com.example.bloodyscythe.ability.*;
import com.example.bloodyscythe.config.BloodyScytheConfigLoader;
import com.example.bloodyscythe.effect.BleedingEffect;
import com.example.bloodyscythe.effect.NoJumpEffect;
import com.example.bloodyscythe.enchantment.SpikedBladeEnchantment;
import com.example.bloodyscythe.item.BloodScytheItem;
import com.example.bloodyscythe.item.PlagueScytheItem;
import com.example.bloodyscythe.item.WitheringScytheItem;
import com.example.bloodyscythe.network.ScytheAbilityC2SPacket;
import com.example.bloodyscythe.network.WitheringHudS2CPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BleedingMod implements ModInitializer {

	public static final String MOD_ID = "bloodyscythe";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Item BLOODY_SCYTHE = new BloodScytheItem(new Item.Settings().maxCount(1).fireproof());
	public static final Item PLAGUE_SCYTHE = new PlagueScytheItem(new Item.Settings().maxCount(1).fireproof());
	public static final Item WITHERING_SCYTHE = new WitheringScytheItem(new Item.Settings().maxCount(1).fireproof());

	// ✅ новый предмет
	public static final Item BLOODY_ESSENCE = new Item(new Item.Settings());

	public static final Identifier SCYTHE_ITEM_GROUP_ID = new Identifier(MOD_ID, "scythes");
	public static ItemGroup SCYTHE_ITEM_GROUP;

	private static final Item[] TAB_ICON_ITEMS = new Item[] { BLOODY_SCYTHE, PLAGUE_SCYTHE, WITHERING_SCYTHE };
	private static final long TAB_ICON_INTERVAL_MS = 1200L;

	public static final StatusEffect BLEEDING = new BleedingEffect();
	public static final StatusEffect NO_JUMP = new NoJumpEffect();

	public static final Enchantment SPIKED_BLADE = new SpikedBladeEnchantment();

	@Override
	public void onInitialize() {

		BloodyScytheConfigLoader.load();

		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "bloody_scythe"), BLOODY_SCYTHE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "plague_scythe"), PLAGUE_SCYTHE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "withering_scythe"), WITHERING_SCYTHE);

		// ✅ регистрация bloody essence
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "bloody_essence"), BLOODY_ESSENCE);

		Registry.register(Registries.STATUS_EFFECT, new Identifier(MOD_ID, "bleeding"), BLEEDING);
		Registry.register(Registries.STATUS_EFFECT, new Identifier(MOD_ID, "no_jump"), NO_JUMP);

		Registry.register(Registries.ENCHANTMENT, new Identifier(MOD_ID, "spiked_blade"), SPIKED_BLADE);

		// ✅ отдельная вкладка под предметы мода (иконка крутится косами через миксин)
		SCYTHE_ITEM_GROUP = Registry.register(
				Registries.ITEM_GROUP,
				SCYTHE_ITEM_GROUP_ID,
				FabricItemGroup.builder()
						.icon(() -> new ItemStack(BLOODY_SCYTHE)) // fallback, если вдруг миксин не применился
						.displayName(Text.translatable("itemGroup." + MOD_ID + ".scythes"))
						.entries((displayContext, entries) -> {
							entries.add(BLOODY_SCYTHE);
							entries.add(PLAGUE_SCYTHE);
							entries.add(WITHERING_SCYTHE);

							// ✅ добавляем эссенцию в эту же вкладку
							entries.add(BLOODY_ESSENCE);
						})
						.build()
		);

		ScytheAbilityC2SPacket.register();

		WelcomeAdvancementHandler.register();

		// Perfect Harvest milestones
		BloodHarvestKillHandler.register();

		// ✅ общий килл-хендлер для 1/5/10/20 убийств косами
		ScytheKillMilestoneHandler.register();

		// ✅ дроп Bloody Essence (5% житель, 20% игрок)
		BloodyEssenceDropHandler.register();

		// ✅ чистим активки при DISCONNECT (не залипают UUID в Map)
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.player;
			server.execute(() -> {
				BloodHarvestTracker.clear(player);
				PlagueScytheTracker.clear(player);
				WitheringScytheTracker.clear(player);
			});
		});

		ServerTickEvents.END_SERVER_TICK.register(this::tickServer);

		LOGGER.info("BloodyScythe initialized successfully");
	}

	// Вызывается из миксина (ItemGroupMixin) чтобы иконка вкладки менялась со временем
	public static ItemStack createRotatingTabIcon() {
		int index = (int) ((System.currentTimeMillis() / TAB_ICON_INTERVAL_MS) % TAB_ICON_ITEMS.length);
		return new ItemStack(TAB_ICON_ITEMS[index]);
	}

	private void tickServer(MinecraftServer server) {
		int ticks = server.getTicks();

		int plagueRate = 20;
		int witheringRate = 20;

		if (BloodyScytheConfigLoader.CONFIG != null) {
			plagueRate = Math.max(1, BloodyScytheConfigLoader.CONFIG.plagueAuraTickRate);
			witheringRate = Math.max(1, BloodyScytheConfigLoader.CONFIG.witheringAuraTickRate);
		}

		boolean runPlagueAuraThisTick = (ticks % plagueRate) == 0;
		boolean runWitheringAuraThisTick = (ticks % witheringRate) == 0;

		boolean everySecond = (ticks % 20) == 0;

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

			BloodHarvestTracker.tick(player);

			PlagueScytheTracker.tick(player);
			PlagueScytheAbility.tick(player);

			WitheringScytheTracker.tick(player);
			WitheringScytheAbility.tick(player);

			if (runPlagueAuraThisTick) {
				PlagueScytheAura.apply(player);
			}

			if (runWitheringAuraThisTick) {
				WitheringScytheAura.apply(player);
			}

			// HUD sync раз в секунду (страховка от рассинхрона)
			if (everySecond && WitheringScytheTracker.isActive(player)) {
				WitheringHudS2CPacket.sendTicks(player, WitheringScytheTracker.getTicksLeft(player));
			}
		}
	}
}
