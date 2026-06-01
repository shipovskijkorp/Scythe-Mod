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

    /* ===================== PLAGUE SCYTHE ===================== */

    public double plagueAuraRadius = 6.0;
    public int plagueAuraTickRate = 20;

    /** Длительность эффектов ауры (по умолчанию 3 секунды). */
    public int plagueAuraEffectTicks = 60;

    /** При каком остатке (тики) обновлять ауру. По умолчанию 1 сек. */
    public int plagueAuraRefreshThresholdTicks = 20;

    public double plagueActiveRadius = 6.0;

    /** Сколько длится активка. */
    public int plagueActiveTicks = 20 * 10;

    /** Как часто тикать урон активки (по умолчанию 1 раз в секунду). */
    public int plagueActiveTickRate = 20;

    /** Урон активки (за одно срабатывание). */
    public double plagueActiveDamage = 1.0;

    /** Дебаффы, которые даются при активации (тики). */
    public int plagueActiveDebuffTicks = 40;
    public int plagueActiveSlownessAmplifier = 1;
    public int plagueActiveWeaknessAmplifier = 1;

    /** Отдельный кулдаун на активку (по умолчанию 45 секунд). */
    public int plagueActiveCooldownTicks = 20 * 45;

    /** Бонус урона от недостающего HP: база и кап множителя. */
    public double plagueMissingHealthBaseDamage = 8.0;
    public double plagueMissingHealthMultiplierCap = 2.0;

    /* ===================== WITHERING SCYTHE ===================== */

    public double witheringAuraRadius = 5.0;
    public int witheringAuraTickRate = 20;

    /** Пассивка (по умолчанию 2 секунды эффектов). */
    public int witheringAuraWitherTicks = 40;
    public int witheringAuraSlownessTicks = 40;

    /** При каком остатке (тики) обновлять ауру. По умолчанию 1 сек. */
    public int witheringAuraRefreshThresholdTicks = 20;

    public double witheringActiveRadius = 6.0;

    /** Длительность "режима" после активки (10 секунд). */
    public int witheringActiveTicks = 20 * 10;

    /** Тик урона (1 раз в секунду). */
    public int witheringActiveTickRate = 20;

    /** Урон активки (за одно срабатывание). */
    public double witheringActiveDamage = 1.0;

    /** Кулдаун на активку (по умолчанию 30 секунд). */
    public int witheringActiveCooldownTicks = 20 * 30;

    /** Дебаффы на цель активки (2 секунды). */
    public int witheringDebuffTicks = 40;
    public int witheringDebuffSlownessAmplifier = 1;
    public int witheringDebuffWitherAmplifier = 0;

    /** Шанс дропа Bloody Essence с жителя (0..1). */
    public double bloodyEssenceVillagerDropChance = 0.05;

    /** Шанс дропа Bloody Essence с игрока (0..1). */
    public double bloodyEssencePlayerDropChance = 0.20;

    /** Пост-удар во время активки: сколько "вернуть" от срезанного брони. */
    public double witheringArmorIgnoreFraction = 0.20;

    /** База урона, используемая в формуле брони для расчёта "срезанного". */
    public double witheringArmorIgnoreBaseDamage = 8.0;
}
