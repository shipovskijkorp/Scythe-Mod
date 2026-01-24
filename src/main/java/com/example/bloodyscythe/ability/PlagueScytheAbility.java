package com.example.bloodyscythe.ability;

import com.example.bloodyscythe.config.BloodyScytheConfigLoader;
import com.example.bloodyscythe.item.PlagueScytheItem;
import com.example.bloodyscythe.network.PlagueHudS2CPacket;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

import java.util.List;

public class PlagueScytheAbility {

    public static void tryActivate(ServerPlayerEntity player) {

        Hand hand = getHeldPlagueScytheHand(player);
        if (hand == null) return;

        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();

        int cooldown = Math.max(1, BloodyScytheConfigLoader.CONFIG.plagueActiveCooldownTicks);
        if (player.getItemCooldownManager().isCoolingDown(item)) return;

        // ✅ Цена активации (общая для всех кос): берём из bloodHarvestDurabilityCost
        int cost = 100;
        if (BloodyScytheConfigLoader.CONFIG != null) {
            cost = Math.max(0, BloodyScytheConfigLoader.CONFIG.bloodHarvestDurabilityCost);
        }

        if (cost > 0) {
            int remaining = stack.getMaxDamage() - stack.getDamage();
            if (remaining < cost) {
                player.sendMessage(Text.translatable("message.bloodyscythe.scythe_ability.no_durability"), true);
                return;
            }
            stack.damage(cost, player, p -> p.sendToolBreakStatus(hand));
        }

        int ticks = PlagueScytheTracker.start(player);
        player.getItemCooldownManager().set(item, cooldown);
        PlagueHudS2CPacket.sendTicks(player, ticks);

        double radius = BloodyScytheConfigLoader.CONFIG.plagueActiveRadius;
        Box box = player.getBoundingBox().expand(radius);

        List<ServerPlayerEntity> targets =
                player.getWorld().getEntitiesByClass(
                        ServerPlayerEntity.class,
                        box,
                        p -> isEnemyPlayer(player, p)
                );

        int debuffTicks = Math.max(1, BloodyScytheConfigLoader.CONFIG.plagueActiveDebuffTicks);
        int slowAmp = Math.max(0, BloodyScytheConfigLoader.CONFIG.plagueActiveSlownessAmplifier);
        int weakAmp = Math.max(0, BloodyScytheConfigLoader.CONFIG.plagueActiveWeaknessAmplifier);

        for (ServerPlayerEntity target : targets) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, debuffTicks, 0));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, debuffTicks, slowAmp));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, debuffTicks, weakAmp));
        }
    }

    public static void tick(ServerPlayerEntity player) {
        if (!PlagueScytheTracker.isActive(player)) return;

        int tickRate = Math.max(1, BloodyScytheConfigLoader.CONFIG.plagueActiveTickRate);
        if (player.age % tickRate != 0) return;

        double radius = BloodyScytheConfigLoader.CONFIG.plagueActiveRadius;
        Box box = player.getBoundingBox().expand(radius);

        List<ServerPlayerEntity> targets =
                player.getWorld().getEntitiesByClass(
                        ServerPlayerEntity.class,
                        box,
                        p -> isEnemyPlayer(player, p)
                );

        double damage = Math.max(0.0, BloodyScytheConfigLoader.CONFIG.plagueActiveDamage);
        if (damage <= 0.0) return;

        for (ServerPlayerEntity target : targets) {
            target.damage(player.getDamageSources().indirectMagic(player, player), (float) damage);
        }
    }

    /**
     * ✅ Возвращаем множитель урона от недостающего HP (для PlagueScytheItem).
     *  - при полном HP → 1.0
     *  - при 0 HP → cap (по умолчанию 2.0)
     */
    public static float getDamageMultiplier(ServerPlayerEntity player) {
        float maxHp = player.getMaxHealth();
        if (maxHp <= 0.0f) return 1.0f;

        float missing = maxHp - player.getHealth();
        float missingFrac = missing / maxHp;

        // clamp 0..1
        if (missingFrac < 0.0f) missingFrac = 0.0f;
        if (missingFrac > 1.0f) missingFrac = 1.0f;

        double cap = 2.0;
        if (BloodyScytheConfigLoader.CONFIG != null) {
            cap = Math.max(1.0, BloodyScytheConfigLoader.CONFIG.plagueMissingHealthMultiplierCap);
        }

        // линейно от 1.0 до cap
        return (float) (1.0 + missingFrac * (cap - 1.0));
    }

    private static Hand getHeldPlagueScytheHand(ServerPlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof PlagueScytheItem) return Hand.MAIN_HAND;
        if (player.getOffHandStack().getItem() instanceof PlagueScytheItem) return Hand.OFF_HAND;
        return null;
    }

    private static boolean isEnemyPlayer(ServerPlayerEntity owner, ServerPlayerEntity other) {
        if (other == owner) return false;
        if (!other.isAlive() || other.isSpectator()) return false;
        return !owner.isTeammate(other);
    }
}
