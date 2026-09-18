package com.shipovskijkorp.scythes.mod.network;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.ScytheAbilityHandler;
import com.shipovskijkorp.scythes.mod.client.BloodHarvestHudState;
import com.shipovskijkorp.scythes.mod.client.ToxicAuraHudState;
import com.shipovskijkorp.scythes.mod.client.WitheringAuraHudState;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** NeoForge payload registration for Minecraft 1.21.11. */
public final class ModPackets {

    private ModPackets() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(ScytheAbilityPayload.ID, ScytheAbilityPayload.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayerEntity player) {
                ScytheAbilityHandler.activate(player);
            }
        });

        registrar.playToClient(BloodHarvestStartPayload.ID, BloodHarvestStartPayload.CODEC,
                (payload, context) -> BloodHarvestHudState.startOrUpdate(payload.ticksLeft()));
        registrar.playToClient(BloodHarvestStopPayload.ID, BloodHarvestStopPayload.CODEC,
                (payload, context) -> BloodHarvestHudState.stop());
        registrar.playToClient(ToxicAuraStartPayload.ID, ToxicAuraStartPayload.CODEC,
                (payload, context) -> ToxicAuraHudState.startOrUpdate(payload.ticksLeft()));
        registrar.playToClient(ToxicAuraStopPayload.ID, ToxicAuraStopPayload.CODEC,
                (payload, context) -> ToxicAuraHudState.stop());
        registrar.playToClient(WitheringAuraStartPayload.ID, WitheringAuraStartPayload.CODEC,
                (payload, context) -> WitheringAuraHudState.startOrUpdate(payload.ticksLeft()));
        registrar.playToClient(WitheringAuraStopPayload.ID, WitheringAuraStopPayload.CODEC,
                (payload, context) -> WitheringAuraHudState.stop());
    }

    public record ScytheAbilityPayload() implements CustomPayload {
        public static final Id<ScytheAbilityPayload> ID = new Id<>(ScytheMod.id("scythe_ability"));
        public static final PacketCodec<RegistryByteBuf, ScytheAbilityPayload> CODEC = PacketCodec.unit(new ScytheAbilityPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BloodHarvestStartPayload(int ticksLeft) implements CustomPayload {
        public static final Id<BloodHarvestStartPayload> ID = new Id<>(ScytheMod.id("blood_harvest_start"));
        public static final PacketCodec<RegistryByteBuf, BloodHarvestStartPayload> CODEC = PacketCodec.ofStatic(
                (buf, payload) -> buf.writeInt(Math.max(0, payload.ticksLeft())),
                buf -> new BloodHarvestStartPayload(buf.readInt())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BloodHarvestStopPayload() implements CustomPayload {
        public static final Id<BloodHarvestStopPayload> ID = new Id<>(ScytheMod.id("blood_harvest_stop"));
        public static final PacketCodec<RegistryByteBuf, BloodHarvestStopPayload> CODEC = PacketCodec.unit(new BloodHarvestStopPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ToxicAuraStartPayload(int ticksLeft) implements CustomPayload {
        public static final Id<ToxicAuraStartPayload> ID = new Id<>(ScytheMod.id("toxic_aura_start"));
        public static final PacketCodec<RegistryByteBuf, ToxicAuraStartPayload> CODEC = PacketCodec.ofStatic(
                (buf, payload) -> buf.writeInt(Math.max(0, payload.ticksLeft())),
                buf -> new ToxicAuraStartPayload(buf.readInt())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ToxicAuraStopPayload() implements CustomPayload {
        public static final Id<ToxicAuraStopPayload> ID = new Id<>(ScytheMod.id("toxic_aura_stop"));
        public static final PacketCodec<RegistryByteBuf, ToxicAuraStopPayload> CODEC = PacketCodec.unit(new ToxicAuraStopPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record WitheringAuraStartPayload(int ticksLeft) implements CustomPayload {
        public static final Id<WitheringAuraStartPayload> ID = new Id<>(ScytheMod.id("withering_aura_start"));
        public static final PacketCodec<RegistryByteBuf, WitheringAuraStartPayload> CODEC = PacketCodec.ofStatic(
                (buf, payload) -> buf.writeInt(Math.max(0, payload.ticksLeft())),
                buf -> new WitheringAuraStartPayload(buf.readInt())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record WitheringAuraStopPayload() implements CustomPayload {
        public static final Id<WitheringAuraStopPayload> ID = new Id<>(ScytheMod.id("withering_aura_stop"));
        public static final PacketCodec<RegistryByteBuf, WitheringAuraStopPayload> CODEC = PacketCodec.unit(new WitheringAuraStopPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
