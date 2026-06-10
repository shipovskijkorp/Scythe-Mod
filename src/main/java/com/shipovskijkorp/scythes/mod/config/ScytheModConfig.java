package com.shipovskijkorp.scythes.mod.config;

public class ScytheModConfig {

    /* ===================== BLOODY SCYTHE ===================== */

    /** Шанс наложить кровотечение при ударе (0..1). */
    public double bloodBleedingChance = 0.35;

    /** Базовая длительность кровотечения (тики). По умолчанию 2 секунды. */
    public int bloodBleedingBaseDurationTicks = 40;

    /** На сколько продлевать кровотечение, если оно уже есть (тики). */
    public int bloodBleedingExtendTicks = 40;

    /* ===================== BLEEDING EFFECT ===================== */

    /** Как часто кровотечение наносит урон (тики). По умолчанию 20 = 1 раз/сек. */
    public int bleedingTickRate = 20;

    /** Урон в секунду (на amplifier=0). При изменении tickRate DPS сохраняется. */
    public double bleedingDamagePerSecond = 1.5;

    /** Шанс вампиризма Кровавой косы при фактическом уроне ближней атакой (0..1). */
    public double bloodVampirismChance = 0.20;

    /** Доля фактически нанесённого урона, которая возвращается здоровьем. */
    public double bloodVampirismHealFraction = 0.40;

    /** Минимальная задержка между успешными срабатываниями вампиризма (тики). */
    public int bloodVampirismCooldownTicks = 20;

    /* ===================== BLOOD HARVEST ===================== */

    public double bloodHarvestRadius = 10.0;
    public int bloodHarvestCooldownTicks = 20 * 60;
    public int bloodHarvestKillWindowTicks = 20 * 20;

    /** Общая стоимость прочности при активации способностей кос. */
    public int scytheAbilityDurabilityCost = 100;


    /** Длительность дебаффов на цели от Blood Harvest (тики). */
    public int bloodHarvestSlownessTicks = 20 * 10;
    public int bloodHarvestBlindnessTicks = 20 * 10;
    public int bloodHarvestWeaknessTicks = 20 * 10;
    public int bloodHarvestGlowingTicks = 20 * 20;

    public int bloodHarvestSlownessAmplifier = 1;
    public int bloodHarvestWeaknessAmplifier = 1;

    /** Баффы владельцу при успешном килле в окне (тики). */
    public int bloodHarvestSuccessBuffTicks = 20 * 16;
    public int bloodHarvestSuccessSpeedAmplifier = 1;
    public int bloodHarvestSuccessStrengthAmplifier = 1;
    public int bloodHarvestSuccessRegenAmplifier = 1;

    /** Дебаффы владельцу при провале окна (тики). */
    public int bloodHarvestFailureDebuffTicks = 150;
    public int bloodHarvestFailureSlownessAmplifier = 1;
    public int bloodHarvestFailureWeaknessAmplifier = 1;

    /** Шанс дропа Bloody Essence с жителя (0..1). */
    public double bloodyEssenceVillagerDropChance = 0.05;

    /** Шанс дропа Bloody Essence с игрока (0..1). */
    public double bloodyEssencePlayerDropChance = 0.20;
}
