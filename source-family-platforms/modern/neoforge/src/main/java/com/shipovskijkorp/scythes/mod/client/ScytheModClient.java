package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.ScytheMod;
//? if >=1.21.11 {
import com.google.common.reflect.TypeToken;
import com.shipovskijkorp.scythes.mod.util.FreezingRenderState;
//? }
import com.shipovskijkorp.scythes.mod.client.guide.GuideScreen;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import com.shipovskijkorp.scythes.mod.item.GuideBookItem;
import com.shipovskijkorp.scythes.mod.item.ScytheSwordItem;
import com.shipovskijkorp.scythes.mod.network.ScytheAbilityC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.WitherSkeletonEntityRenderer;
//? if >=1.21.11 {
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
//? }
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
//? if >=1.21.11 {
import net.minecraft.util.Formatting;
//? }
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
//? if >=1.21.11 {
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
//? }
import org.lwjgl.glfw.GLFW;

/** Client-only NeoForge entry point. */
@Mod(value = ScytheMod.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ScytheMod.MOD_ID, value = Dist.CLIENT)
public final class ScytheModClient {
    private static KeyBinding scytheAbilityKey;

    public ScytheModClient(IEventBus modBus) {
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::registerKeyMappings);
        modBus.addListener(this::registerEntityRenderers);
        modBus.addListener(this::registerGuiLayers);
//? if >=1.21.11 {
        modBus.addListener(this::registerRenderStateModifiers);
//? }
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            GuideBookItem.installClientOpener(GuideScreen::open);
            ScytheSwordItem.installTooltipAppender(ScytheTooltips::append);
            FarmerScytheItem.installTooltipAppender(ScytheTooltips::append);
        });
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
//? if >=1.21.11 {
        KeyBinding.Category category = KeyBinding.Category.create(ScytheMod.id("scythes"));
        event.registerCategory(category);
//? }
        scytheAbilityKey = new KeyBinding(
                "key.scythes.scythe_ability",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
//? if >=1.21.11 {
                category
//? } else {
                "category.scythes"
//? }
        );
        event.register(scytheAbilityKey);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ScytheMod.TOXIC_ORB, FlyingItemEntityRenderer::new);
        event.registerEntityRenderer(ScytheMod.FIREBALL, FlyingItemEntityRenderer::new);
        event.registerEntityRenderer(ScytheMod.ICE_SPIKE, IceSpikeEntityRenderer::new);
        event.registerEntityRenderer(ScytheMod.WITHERING_MINION, WitherSkeletonEntityRenderer::new);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ScytheMod.id("blood_harvest_hud"), BloodHarvestHudRenderer::render);
        event.registerAboveAll(ScytheMod.id("toxic_aura_hud"), ToxicAuraHudRenderer::render);
        event.registerAboveAll(ScytheMod.id("withering_aura_hud"), WitheringAuraHudRenderer::render);
    }

//? if >=1.21.11 {
    private void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                (entity, state) -> ((FreezingRenderState) state).scythes$setFrozenForRendering(
                        FreezingOverlayRenderer.shouldRenderFor(entity)
                )
        );
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?, ?> event) {
        FreezingOverlayRenderer.render(
                event.getRenderState(),
                event.getPoseStack(),
                event.getSubmitNodeCollector()
        );
    }
//? }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        MinecraftClient client = MinecraftClient.getInstance();
        BloodHarvestHudState.tick();
        ToxicAuraHudState.tick();
        WitheringAuraHudState.tick();
        FireScytheLockHighlighter.tick(client);

        if (scytheAbilityKey == null) return;
        while (scytheAbilityKey.wasPressed()) {
            if (client.getNetworkHandler() != null) {
                ScytheAbilityC2SPacket.send();
            }
        }
    }

//? if >=1.21.11 {
    public static Text getScytheAbilityKeyText(Formatting formatting) {
        if (scytheAbilityKey == null) {
            return Text.literal("R").formatted(formatting);
        }
        return scytheAbilityKey.getBoundKeyLocalizedText().copy().formatted(formatting);
    }
//? } else {
    public static Text getScytheAbilityKeyBoundText() {
        return scytheAbilityKey == null
                ? Text.literal("R")
                : scytheAbilityKey.getBoundKeyLocalizedText();
    }
//? }
}
