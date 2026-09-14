package com.shipovskijkorp.scythes.mod.network;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public final class ModPackets {

    private ModPackets() {}

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playC2S().register(ScytheAbilityPayload.ID, ScytheAbilityPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(BloodHarvestStartPayload.ID, BloodHarvestStartPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BloodHarvestStopPayload.ID, BloodHarvestStopPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ToxicAuraStartPayload.ID, ToxicAuraStartPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ToxicAuraStopPayload.ID, ToxicAuraStopPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WitheringAuraStartPayload.ID, WitheringAuraStartPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WitheringAuraStopPayload.ID, WitheringAuraStopPayload.CODEC);
    }

    public record ScytheAbilityPayload() implements CustomPayload {
        public static final CustomPayload.Id<ScytheAbilityPayload> ID =
                new CustomPayload.Id<>(ScytheMod.id("scythe_ability"));
        public static final PacketCodec<RegistryByteBuf, ScytheAbilityPayload> CODEC =
                PacketCodec.unit(new ScytheAbilityPayload());

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BloodHarvestStartPayload(int ticksLeft) implements CustomPayload {
        public static final CustomPayload.Id<BloodHarvestStartPayload> ID =
                new CustomPayload.Id<>(ScytheMod.id("blood_harvest_start"));
        public static final PacketCodec<RegistryByteBuf, BloodHarvestStartPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.INTEGER, BloodHarvestStartPayload::ticksLeft, BloodHarvestStartPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BloodHarvestStopPayload() implements CustomPayload {
        public static final CustomPayload.Id<BloodHarvestStopPayload> ID =
                new CustomPayload.Id<>(ScytheMod.id("blood_harvest_stop"));
        public static final PacketCodec<RegistryByteBuf, BloodHarvestStopPayload> CODEC =
                PacketCodec.unit(new BloodHarvestStopPayload());

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ToxicAuraStartPayload(int ticksLeft) implements CustomPayload {
        public static final CustomPayload.Id<ToxicAuraStartPayload> ID =
                new CustomPayload.Id<>(ScytheMod.id("toxic_aura_start"));
        public static final PacketCodec<RegistryByteBuf, ToxicAuraStartPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.INTEGER, ToxicAuraStartPayload::ticksLeft, ToxicAuraStartPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ToxicAuraStopPayload() implements CustomPayload {
        public static final CustomPayload.Id<ToxicAuraStopPayload> ID =
                new CustomPayload.Id<>(ScytheMod.id("toxic_aura_stop"));
        public static final PacketCodec<RegistryByteBuf, ToxicAuraStopPayload> CODEC =
                PacketCodec.unit(new ToxicAuraStopPayload());

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record WitheringAuraStartPayload(int ticksLeft) implements CustomPayload {
        public static final CustomPayload.Id<WitheringAuraStartPayload> ID =
                new CustomPayload.Id<>(ScytheMod.id("withering_aura_start"));
        public static final PacketCodec<RegistryByteBuf, WitheringAuraStartPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.INTEGER, WitheringAuraStartPayload::ticksLeft, WitheringAuraStartPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record WitheringAuraStopPayload() implements CustomPayload {
        public static final CustomPayload.Id<WitheringAuraStopPayload> ID =
                new CustomPayload.Id<>(ScytheMod.id("withering_aura_stop"));
        public static final PacketCodec<RegistryByteBuf, WitheringAuraStopPayload> CODEC =
                PacketCodec.unit(new WitheringAuraStopPayload());

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
