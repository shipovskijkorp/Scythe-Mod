package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.entity.IceSpikeEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class IceSpikeEntityRenderer extends ProjectileEntityRenderer<IceSpikeEntity> {

    private static final Identifier TEXTURE = ScytheMod.id("textures/projectile/ice.png");

    public IceSpikeEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(IceSpikeEntity entity) {
        return TEXTURE;
    }
}
