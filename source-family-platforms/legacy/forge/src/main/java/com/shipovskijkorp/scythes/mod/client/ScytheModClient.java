package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.client.guide.GuideScreen;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import com.shipovskijkorp.scythes.mod.item.GuideBookItem;
import com.shipovskijkorp.scythes.mod.item.ScytheSwordItem;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.WitherSkeletonEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ScytheMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ScytheModClient {
    private static KeyBinding scytheAbilityKey;

    private ScytheModClient() {}

    public static Text getScytheAbilityKeyBoundText() {
        return scytheAbilityKey == null
                ? Text.literal("R")
                : scytheAbilityKey.getBoundKeyLocalizedText();
    }

    public static KeyBinding getScytheAbilityKey() {
        return scytheAbilityKey;
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            GuideBookItem.installClientOpener(GuideScreen::open);
            ScytheSwordItem.installTooltipAppender(ScytheTooltips::append);
            FarmerScytheItem.installTooltipAppender(ScytheTooltips::append);
        });
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        scytheAbilityKey = new KeyBinding(
                "key.scythes.scythe_ability",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.scythes"
        );
        event.register(scytheAbilityKey);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ScytheMod.ICE_SPIKE, IceSpikeEntityRenderer::new);
        event.registerEntityRenderer(ScytheMod.TOXIC_ORB, FlyingItemEntityRenderer::new);
        event.registerEntityRenderer(ScytheMod.FIREBALL, FlyingItemEntityRenderer::new);
        event.registerEntityRenderer(ScytheMod.WITHERING_MINION, WitherSkeletonEntityRenderer::new);
    }
}
