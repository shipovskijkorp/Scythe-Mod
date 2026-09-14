package com.shipovskijkorp.scythes.mod.network;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ModPackets {

    private ModPackets() {}

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.serverboundPlay().register(ScytheAbilityPayload.ID, ScytheAbilityPayload.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(BloodHarvestStartPayload.ID, BloodHarvestStartPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BloodHarvestStopPayload.ID, BloodHarvestStopPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ToxicAuraStartPayload.ID, ToxicAuraStartPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ToxicAuraStopPayload.ID, ToxicAuraStopPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WitheringAuraStartPayload.ID, WitheringAuraStartPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WitheringAuraStopPayload.ID, WitheringAuraStopPayload.CODEC);
    }

    public static void register() {
        registerPayloadTypes();
    }

    public record ScytheAbilityPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ScytheAbilityPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("scythe_ability"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ScytheAbilityPayload> CODEC =
                StreamCodec.unit(new ScytheAbilityPayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record BloodHarvestStartPayload(int ticksLeft) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BloodHarvestStartPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("blood_harvest_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BloodHarvestStartPayload> CODEC = intPayload(BloodHarvestStartPayload::new, BloodHarvestStartPayload::ticksLeft);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record BloodHarvestStopPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BloodHarvestStopPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("blood_harvest_stop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BloodHarvestStopPayload> CODEC =
                StreamCodec.unit(new BloodHarvestStopPayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record ToxicAuraStartPayload(int ticksLeft) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ToxicAuraStartPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("toxic_aura_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToxicAuraStartPayload> CODEC = intPayload(ToxicAuraStartPayload::new, ToxicAuraStartPayload::ticksLeft);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record ToxicAuraStopPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ToxicAuraStopPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("toxic_aura_stop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToxicAuraStopPayload> CODEC =
                StreamCodec.unit(new ToxicAuraStopPayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record WitheringAuraStartPayload(int ticksLeft) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<WitheringAuraStartPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("withering_aura_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WitheringAuraStartPayload> CODEC = intPayload(WitheringAuraStartPayload::new, WitheringAuraStartPayload::ticksLeft);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record WitheringAuraStopPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<WitheringAuraStopPayload> ID =
                new CustomPacketPayload.Type<>(ScytheMod.id("withering_aura_stop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WitheringAuraStopPayload> CODEC =
                StreamCodec.unit(new WitheringAuraStopPayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    private static <T> StreamCodec<RegistryFriendlyByteBuf, T> intPayload(IntPayloadFactory<T> factory, IntPayloadValue<T> value) {
        return new StreamCodec<>() {
            @Override
            public T decode(RegistryFriendlyByteBuf buf) {
                return factory.create(buf.readInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, T payload) {
                buf.writeInt(Math.max(0, value.get(payload)));
            }
        };
    }

    private interface IntPayloadFactory<T> {
        T create(int value);
    }

    private interface IntPayloadValue<T> {
        int get(T payload);
    }
}
