package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

public final class ScytheAdvancementTracker {

    private ScytheAdvancementTracker() {
    }

    private static final String MASTERY_ADVANCEMENT = "money_cards_god_scythe";
    private static final String SUPER_NECROMANCER_ADVANCEMENT = "super_necromancer";
    private static final String MERCILESS_ADVANCEMENT = "merciless";
    private static final String IMPOSSIBLE_INTOXICATION_ADVANCEMENT = "impossible_intoxication";
    private static final String THREE_DAYS_RAIN_ADVANCEMENT = "three_days_rain";
    private static final String FARMER_DRAGON_ADVANCEMENT = "dumb_and_dumber";
    private static final String FIREBALL_GHAST_ADVANCEMENT = "return_to_returner";
    private static final String SUPERCOOLED_SNOW_ADVANCEMENT = "supercooled_snow";

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
    private static final String CRITERION_FIRE_PASSIVE = "fire_passive";
    private static final String CRITERION_FIRE_ACTIVE = "fire_active";
    private static final String CRITERION_FIRE_SPECIAL = "fire_special";
    private static final String CRITERION_FARMER_PASSIVE = "farmer_passive";
    private static final String CRITERION_FARMER_ACTIVE = "farmer_active";
    private static final String CRITERION_FARMER_SPECIAL = "farmer_special";

    private static final int BLOOD_SKILL_PASSIVE = 1;
    private static final int BLOOD_SKILL_ACTIVE = 1 << 1;
    private static final int BLOOD_SKILL_SPECIAL = 1 << 2;
    private static final int BLOOD_SKILL_ALL = BLOOD_SKILL_PASSIVE | BLOOD_SKILL_ACTIVE | BLOOD_SKILL_SPECIAL;

    private static final Map<UUID, Map<UUID, BloodSkillRecord>> BLOOD_TARGET_SKILLS = new HashMap<>();
    private static final Map<ToxicPoisonKey, ToxicPoisonRecord> TOXIC_POISON = new HashMap<>();

    public static void markBloodPassive(ServerPlayer player, LivingEntity target) {
        grantMasteryCriterion(player, CRITERION_BLOOD_PASSIVE);
        markBloodSkill(player, target, BLOOD_SKILL_PASSIVE);
    }

    public static void markBloodActive(ServerPlayer player, LivingEntity target) {
        grantMasteryCriterion(player, CRITERION_BLOOD_ACTIVE);
        markBloodSkill(player, target, BLOOD_SKILL_ACTIVE);
    }

    public static void markBloodSpecial(ServerPlayer player, LivingEntity target) {
        grantMasteryCriterion(player, CRITERION_BLOOD_SPECIAL);
        markBloodSkill(player, target, BLOOD_SKILL_SPECIAL);
    }

    public static void markToxicPassive(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_TOXIC_PASSIVE);
    }

    public static void markToxicActive(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_TOXIC_ACTIVE);
    }

    public static void markToxicSpecial(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_TOXIC_SPECIAL);
    }

    public static void markWitheringPassive(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_WITHERING_PASSIVE);
    }

    public static void markWitheringActive(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_WITHERING_ACTIVE);
    }

    public static void markWitheringSpecial(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_WITHERING_SPECIAL);
    }

    public static void markGoldenPassive(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_GOLDEN_PASSIVE);
    }

    public static void markGoldenActive(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_GOLDEN_ACTIVE);
        if (GoldenRainDimensionDayTracker.recordActivation(player)) {
            grantCriterion(player, THREE_DAYS_RAIN_ADVANCEMENT, "activate_in_three_dimension_days");
        }
    }

    public static void markGoldenSpecial(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_GOLDEN_SPECIAL);
    }

    public static void markFrozenPassive(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_FROZEN_PASSIVE);
    }

    public static void markFrozenActive(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_FROZEN_ACTIVE);
    }

    public static void markFrozenSpecial(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_FROZEN_SPECIAL);
    }

    public static void markFirePassive(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_FIRE_PASSIVE);
    }

    public static void markFireActive(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_FIRE_ACTIVE);
    }

    public static void markFireSpecial(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_FIRE_SPECIAL);
    }

    public static void markFarmerPassive(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_FARMER_PASSIVE);
    }

    public static void markFarmerActive(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_FARMER_ACTIVE);
    }

    public static void markFarmerSpecial(ServerPlayer player) {
        grantMasteryCriterion(player, CRITERION_FARMER_SPECIAL);
    }

    public static void tryGrantFireballGhastKill(ServerPlayer player) {
        grantCriterion(player, FIREBALL_GHAST_ADVANCEMENT, "kill_ghast_with_fire_scythe_special");
    }

    public static void tryGrantSupercooledSnow(ServerPlayer player) {
        grantCriterion(player, SUPERCOOLED_SNOW_ADVANCEMENT, "freeze_snow_golem_to_death");
    }

    public static void recordToxicPoison(ServerPlayer player, LivingEntity target, int durationTicks) {
        if (durationTicks <= 0 || target.getUUID().equals(player.getUUID())) return;

        long now = player.level().getGameTime();
        ToxicPoisonKey key = new ToxicPoisonKey(player.getUUID(), target.getUUID());
        ToxicPoisonRecord record = TOXIC_POISON.get(key);

        if (record == null || now > record.expiresAt) {
            record = new ToxicPoisonRecord(now, now + durationTicks);
            TOXIC_POISON.put(key, record);
        } else {
            record.expiresAt = Math.max(record.expiresAt, now + durationTicks);
        }

        if (now - record.startTick >= ScytheBalance.Progression.TOXIC_POISON_REQUIRED_TICKS) {
            grantCriterion(player, IMPOSSIBLE_INTOXICATION_ADVANCEMENT, "poison_22_seconds");
        }

        if (TOXIC_POISON.size() > 256) {
            cleanupToxicPoison(now);
        }
    }

    public static void tryGrantSuperNecromancer(ServerPlayer player, int minionCount) {
        if (minionCount >= ScytheBalance.Progression.MINION_ARMY_SIZE) {
            grantCriterion(player, SUPER_NECROMANCER_ADVANCEMENT, "summon_10_minions");
        }
    }

    public static void tryGrantMercilessOnDeath(LivingEntity target, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer killer)) return;
        if (target instanceof ServerPlayer playerVictim && killer.isAlliedTo(playerVictim)) return;

        Map<UUID, BloodSkillRecord> byOwner = BLOOD_TARGET_SKILLS.remove(target.getUUID());
        if (byOwner == null) return;

        BloodSkillRecord record = byOwner.get(killer.getUUID());
        long now = killer.level().getGameTime();
        if (record != null && now <= record.expiresAt && (record.flags & BLOOD_SKILL_ALL) == BLOOD_SKILL_ALL) {
            grantCriterion(killer, MERCILESS_ADVANCEMENT, "kill_after_all_blood_skills");
        }
    }


    public static void tryGrantFarmerDragonKill(LivingEntity target, DamageSource source) {
        if (!(target instanceof EnderDragon)) return;
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return;

        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;
        if (!(player.getMainHandItem().getItem() instanceof FarmerScytheItem)) return;

        grantCriterion(player, FARMER_DRAGON_ADVANCEMENT, "kill_ender_dragon_with_farmer_scythe");
    }

    public static void clear(ServerPlayer player) {
        UUID playerId = player.getUUID();
        for (Iterator<Map.Entry<UUID, Map<UUID, BloodSkillRecord>>> targetIterator = BLOOD_TARGET_SKILLS.entrySet().iterator(); targetIterator.hasNext(); ) {
            Map<UUID, BloodSkillRecord> byOwner = targetIterator.next().getValue();
            byOwner.remove(playerId);
            if (byOwner.isEmpty()) {
                targetIterator.remove();
            }
        }
        TOXIC_POISON.keySet().removeIf(key -> key.ownerUuid().equals(playerId));
    }

    private static void grantMasteryCriterion(ServerPlayer player, String criterion) {
        grantCriterion(player, MASTERY_ADVANCEMENT, criterion);
    }

    private static void markBloodSkill(ServerPlayer player, LivingEntity target, int flag) {
        if (!target.isAlive() || target.getUUID().equals(player.getUUID())) return;
        if (target instanceof ServerPlayer playerTarget && player.isAlliedTo(playerTarget)) return;

        long now = player.level().getGameTime();
        Map<UUID, BloodSkillRecord> byOwner = BLOOD_TARGET_SKILLS.computeIfAbsent(target.getUUID(), ignored -> new HashMap<>());
        BloodSkillRecord record = byOwner.get(player.getUUID());

        if (record == null || now > record.expiresAt) {
            record = new BloodSkillRecord(0, now + ScytheBalance.Progression.BLOOD_SKILL_EXPIRE_TICKS);
            byOwner.put(player.getUUID(), record);
        }

        record.flags |= flag;
        record.expiresAt = now + ScytheBalance.Progression.BLOOD_SKILL_EXPIRE_TICKS;
    }

    private static void cleanupToxicPoison(long now) {
        TOXIC_POISON.entrySet().removeIf(entry -> now > entry.getValue().expiresAt + 20L);
    }

    private static void grantCriterion(ServerPlayer player, String advancementId, String criterion) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        AdvancementHolder advancement = server.getAdvancements().get(ScytheMod.id(advancementId));
        if (advancement == null) return;

        player.getAdvancements().award(advancement, criterion);
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
