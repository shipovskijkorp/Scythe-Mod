package com.shipovskijkorp.scythes.mod.network;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public final class ModPackets {

    private ModPackets() {}

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ScytheAbilityPayload.ID, ScytheAbilityPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(BloodHarvestStartPayload.ID, BloodHarvestStartPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BloodHarvestStopPayload.ID, BloodHarvestStopPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ToxicAuraStartPayload.ID, ToxicAuraStartPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ToxicAuraStopPayload.ID, ToxicAuraStopPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WitheringAuraStartPayload.ID, WitheringAuraStartPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WitheringAuraStopPayload.ID, WitheringAuraStopPayload.CODEC);
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
