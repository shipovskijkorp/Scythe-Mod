package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.advancement.Advancement;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ScytheAdvancementTracker {

    private ScytheAdvancementTracker() {
    }

    private static final String MASTERY_ADVANCEMENT = "money_cards_god_scythe";
    private static final String SUPER_NECROMANCER_ADVANCEMENT = "super_necromancer";
    private static final String MERCILESS_ADVANCEMENT = "merciless";
    private static final String IMPOSSIBLE_INTOXICATION_ADVANCEMENT = "impossible_intoxication";
    private static final String THREE_DAYS_RAIN_ADVANCEMENT = "three_days_rain";

    private static final String CRITERION_BLOOD_PASSIVE = "blood_passive";
    private static final String CRITERION_BLOOD_ACTIVE = "blood_active";
    private static final String CRITERION_BLOOD_SPECIAL = "blood_special";
    private static final String CRITERION_TOXIC_PASSIVE = "toxic_passive";
    private static final String CRITERION_TOXIC_ACTIVE = "toxic_active";
    private static final String CRITERION_TOXIC_SPECIAL = "toxic_special";
    private static final String CRITERION_WITHERING_PASSIVE = "withering_passive";
    private static final String CRITERION_WITHERING_ACTIVE = "withering_active";
    private static final String CRITERION_WITHERING_SPECIAL = "withering_special";
    private static final String CRITERION_GOLDEN_PASSIVE = "golden_passive";
    private static final String CRITERION_GOLDEN_ACTIVE = "golden_active";
    private static final String CRITERION_GOLDEN_SPECIAL = "golden_special";
    private static final String CRITERION_FROZEN_PASSIVE = "frozen_passive";
    private static final String CRITERION_FROZEN_ACTIVE = "frozen_active";
    private static final String CRITERION_FROZEN_SPECIAL = "frozen_special";

    private static final int BLOOD_SKILL_PASSIVE = 1;
    private static final int BLOOD_SKILL_ACTIVE = 1 << 1;
    private static final int BLOOD_SKILL_SPECIAL = 1 << 2;
    private static final int BLOOD_SKILL_ALL = BLOOD_SKILL_PASSIVE | BLOOD_SKILL_ACTIVE | BLOOD_SKILL_SPECIAL;
    private static final int BLOOD_SKILL_EXPIRE_TICKS = 20 * 120;
    private static final int TOXIC_POISON_REQUIRED_TICKS = 20 * 22;

    private static final Map<UUID, Map<UUID, BloodSkillRecord>> BLOOD_TARGET_SKILLS = new HashMap<>();
    private static final Map<ToxicPoisonKey, ToxicPoisonRecord> TOXIC_POISON = new HashMap<>();

    public static void markBloodPassive(ServerPlayerEntity player, LivingEntity target) {
        grantMasteryCriterion(player, CRITERION_BLOOD_PASSIVE);
        markBloodSkill(player, target, BLOOD_SKILL_PASSIVE);
    }

    public static void markBloodActive(ServerPlayerEntity player, LivingEntity target) {
        grantMasteryCriterion(player, CRITERION_BLOOD_ACTIVE);
        markBloodSkill(player, target, BLOOD_SKILL_ACTIVE);
    }

    public static void markBloodSpecial(ServerPlayerEntity player, LivingEntity target) {
        grantMasteryCriterion(player, CRITERION_BLOOD_SPECIAL);
        markBloodSkill(player, target, BLOOD_SKILL_SPECIAL);
    }

    public static void markToxicPassive(ServerPlayerEntity player) {
        grantMasteryCriterion(player, CRITERION_TOXIC_PASSIVE);
    }

    public static void markToxicActive(ServerPlayerEntity player) {
        grantMasteryCriterion(player, CRITERION_TOXIC_ACTIVE);
    }

    public static void markToxicSpecial(ServerPlayerEntity player) {
        grantMasteryCriterion(player, CRITERION_TOXIC_SPECIAL);
    }

    public static void markWitheringPassive(ServerPlayerEntity player) {
        grantMasteryCriterion(player, CRITERION_WITHERING_PASSIVE);
    }

    public static void markWitheringActive(ServerPlayerEntity player) {
        grantMasteryCriterion(player, CRITERION_WITHERING_ACTIVE);
    }

    public static void markWitheringSpecial(ServerPlayerEntity player) {
        grantMasteryCriterion(player, CRITERION_WITHERING_SPECIAL);
    }

    public static void markGoldenPassive(ServerPlayerEntity player) {
        grantMasteryCriterion(player, CRITERION_GOLDEN_PASSIVE);
    }

    public static void markGoldenActive(ServerPlayerEntity player) {
        grantMasteryCriterion(player, CRITERION_GOLDEN_ACTIVE);
        if (GoldenRainDimensionDayTracker.recordActivation(player)) {
            grantCriterion(player, THREE_DAYS_RAIN_ADVANCEMENT, "activate_in_three_dimension_days");
        }
    }

    public static void markGoldenSpecial(ServerPlayerEntity player) {
        grantMasteryCriterion(player, CRITERION_GOLDEN_SPECIAL);
    }

    public static void markFrozenPassive(ServerPlayerEntity player) {
        grantMasteryCriterion(player, CRITERION_FROZEN_PASSIVE);
    }

    public static void markFrozenActive(ServerPlayerEntity player) {
        grantMasteryCriterion(player, CRITERION_FROZEN_ACTIVE);
    }

    public static void markFrozenSpecial(ServerPlayerEntity player) {
        grantMasteryCriterion(player, CRITERION_FROZEN_SPECIAL);
    }

    public static void recordToxicPoison(ServerPlayerEntity player, LivingEntity target, int durationTicks) {
        if (durationTicks <= 0 || target.getUuid().equals(player.getUuid())) return;

        long now = target.getWorld().getTime();
        ToxicPoisonKey key = new ToxicPoisonKey(player.getUuid(), target.getUuid());
        ToxicPoisonRecord record = TOXIC_POISON.get(key);

        if (record == null || now > record.expiresAt) {
            record = new ToxicPoisonRecord(now, now + durationTicks);
            TOXIC_POISON.put(key, record);
        } else {
            record.expiresAt = Math.max(record.expiresAt, now + durationTicks);
        }

        if (now - record.startTick >= TOXIC_POISON_REQUIRED_TICKS) {
            grantCriterion(player, IMPOSSIBLE_INTOXICATION_ADVANCEMENT, "poison_22_seconds");
        }

        if (TOXIC_POISON.size() > 256) {
            cleanupToxicPoison(now);
        }
    }

    public static void tryGrantSuperNecromancer(ServerPlayerEntity player, int minionCount) {
        if (minionCount >= 10) {
            grantCriterion(player, SUPER_NECROMANCER_ADVANCEMENT, "summon_10_minions");
        }
    }

    public static void tryGrantMercilessOnDeath(LivingEntity target, DamageSource source) {
        if (target.getWorld().isClient) return;

        Entity attacker = source.getAttacker();
        if (!(attacker instanceof ServerPlayerEntity killer)) return;
        if (target instanceof ServerPlayerEntity playerVictim && killer.isTeammate(playerVictim)) return;

        Map<UUID, BloodSkillRecord> byOwner = BLOOD_TARGET_SKILLS.remove(target.getUuid());
        if (byOwner == null) return;

        BloodSkillRecord record = byOwner.get(killer.getUuid());
        long now = target.getWorld().getTime();
        if (record != null && now <= record.expiresAt && (record.flags & BLOOD_SKILL_ALL) == BLOOD_SKILL_ALL) {
            grantCriterion(killer, MERCILESS_ADVANCEMENT, "kill_after_all_blood_skills");
        }
    }

    public static void clear(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        for (Iterator<Map.Entry<UUID, Map<UUID, BloodSkillRecord>>> targetIterator = BLOOD_TARGET_SKILLS.entrySet().iterator(); targetIterator.hasNext(); ) {
            Map<UUID, BloodSkillRecord> byOwner = targetIterator.next().getValue();
            byOwner.remove(playerId);
            if (byOwner.isEmpty()) {
                targetIterator.remove();
            }
        }
        TOXIC_POISON.keySet().removeIf(key -> key.ownerUuid().equals(playerId));
    }

    private static void grantMasteryCriterion(ServerPlayerEntity player, String criterion) {
        grantCriterion(player, MASTERY_ADVANCEMENT, criterion);
    }

    private static void markBloodSkill(ServerPlayerEntity player, LivingEntity target, int flag) {
        if (target.getWorld().isClient) return;
        if (!target.isAlive() || target.getUuid().equals(player.getUuid())) return;
        if (target instanceof ServerPlayerEntity playerTarget && player.isTeammate(playerTarget)) return;

        long now = target.getWorld().getTime();
        Map<UUID, BloodSkillRecord> byOwner = BLOOD_TARGET_SKILLS.computeIfAbsent(target.getUuid(), ignored -> new HashMap<>());
        BloodSkillRecord record = byOwner.get(player.getUuid());

        if (record == null || now > record.expiresAt) {
            record = new BloodSkillRecord(0, now + BLOOD_SKILL_EXPIRE_TICKS);
            byOwner.put(player.getUuid(), record);
        }

        record.flags |= flag;
        record.expiresAt = now + BLOOD_SKILL_EXPIRE_TICKS;
    }

    private static void cleanupToxicPoison(long now) {
        TOXIC_POISON.entrySet().removeIf(entry -> now > entry.getValue().expiresAt + 20L);
    }

    private static void grantCriterion(ServerPlayerEntity player, String advancementId, String criterion) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Advancement advancement = server.getAdvancementLoader().get(new Identifier(ScytheMod.MOD_ID, advancementId));
        if (advancement == null) return;

        player.getAdvancementTracker().grantCriterion(advancement, criterion);
    }

    private static final class BloodSkillRecord {
        private int flags;
        private long expiresAt;

        private BloodSkillRecord(int flags, long expiresAt) {
            this.flags = flags;
            this.expiresAt = expiresAt;
        }
    }

    private static final class ToxicPoisonRecord {
        private final long startTick;
        private long expiresAt;

        private ToxicPoisonRecord(long startTick, long expiresAt) {
            this.startTick = startTick;
            this.expiresAt = expiresAt;
        }
    }

    private record ToxicPoisonKey(UUID ownerUuid, UUID targetUuid) {
    }
}
