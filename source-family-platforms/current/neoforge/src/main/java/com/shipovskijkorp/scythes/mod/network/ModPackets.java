package com.shipovskijkorp.scythes.mod.network;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.ScytheAbilityHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.UUID;

public final class ModPackets {
    private ModPackets() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(ScytheAbilityPayload.ID, ScytheAbilityPayload.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                ScytheAbilityHandler.activate(player);
            }
        });
        registrar.playToClient(BloodHarvestStartPayload.ID, BloodHarvestStartPayload.CODEC);
        registrar.playToClient(BloodHarvestStopPayload.ID, BloodHarvestStopPayload.CODEC);
        registrar.playToClient(ToxicAuraStartPayload.ID, ToxicAuraStartPayload.CODEC);
        registrar.playToClient(ToxicAuraStopPayload.ID, ToxicAuraStopPayload.CODEC);
        registrar.playToClient(WitheringAuraStartPayload.ID, WitheringAuraStartPayload.CODEC);
        registrar.playToClient(WitheringAuraStopPayload.ID, WitheringAuraStopPayload.CODEC);
        registrar.playToClient(FreezingVisualPayload.ID, FreezingVisualPayload.CODEC);
    }

    public record ScytheAbilityPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ScytheAbilityPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("scythe_ability"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ScytheAbilityPayload> CODEC =
                StreamCodec.unit(new ScytheAbilityPayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record BloodHarvestStartPayload(int ticksLeft) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BloodHarvestStartPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("blood_harvest_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BloodHarvestStartPayload> CODEC =
                intPayload(BloodHarvestStartPayload::new, BloodHarvestStartPayload::ticksLeft);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record BloodHarvestStopPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BloodHarvestStopPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("blood_harvest_stop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BloodHarvestStopPayload> CODEC =
                StreamCodec.unit(new BloodHarvestStopPayload());
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record ToxicAuraStartPayload(int ticksLeft) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ToxicAuraStartPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("toxic_aura_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToxicAuraStartPayload> CODEC =
                intPayload(ToxicAuraStartPayload::new, ToxicAuraStartPayload::ticksLeft);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record ToxicAuraStopPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ToxicAuraStopPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("toxic_aura_stop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToxicAuraStopPayload> CODEC =
                StreamCodec.unit(new ToxicAuraStopPayload());
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record WitheringAuraStartPayload(int ticksLeft) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<WitheringAuraStartPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("withering_aura_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WitheringAuraStartPayload> CODEC =
                intPayload(WitheringAuraStartPayload::new, WitheringAuraStartPayload::ticksLeft);
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record WitheringAuraStopPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<WitheringAuraStopPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("withering_aura_stop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WitheringAuraStopPayload> CODEC =
                StreamCodec.unit(new WitheringAuraStopPayload());
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record FreezingVisualPayload(UUID entityUuid, boolean frozen) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<FreezingVisualPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("freezing_visual"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FreezingVisualPayload> CODEC = new StreamCodec<>() {
            @Override
            public FreezingVisualPayload decode(RegistryFriendlyByteBuf buf) {
                return new FreezingVisualPayload(
                        new UUID(buf.readLong(), buf.readLong()),
                        buf.readBoolean()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, FreezingVisualPayload payload) {
                buf.writeLong(payload.entityUuid().getMostSignificantBits());
                buf.writeLong(payload.entityUuid().getLeastSignificantBits());
                buf.writeBoolean(payload.frozen());
            }
        };

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    private static <T> StreamCodec<RegistryFriendlyByteBuf, T> intPayload(IntPayloadFactory<T> factory, IntPayloadValue<T> value) {
        return new StreamCodec<>() {
            @Override public T decode(RegistryFriendlyByteBuf buf) { return factory.create(buf.readInt()); }
            @Override public void encode(RegistryFriendlyByteBuf buf, T payload) { buf.writeInt(Math.max(0, value.get(payload))); }
        };
    }

    private interface IntPayloadFactory<T> { T create(int value); }
    private interface IntPayloadValue<T> { int get(T payload); }
}
