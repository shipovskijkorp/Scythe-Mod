package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
//? if >=26.2 {
//? } else {
import net.minecraft.world.entity.EntityType;
//? }
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
//? if >=26.2 {
import net.minecraft.world.entity.npc.villager.Villager;
//? } else {
//? }
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

public class BloodyEssenceDropHandler {

    /**
     * Stored as Object on purpose: 26.1.x changed/renamed the nested gamerule key type
     * across mapping sets, so referring to GameRules.Key directly breaks compilation.
     */
    private static volatile Object doMobLootRule;

    /** Called once by the loader after a credited kill, on the server thread. */
    public static void onKill(ServerLevel world, Entity entity, LivingEntity killedEntity) {
        if (!isDoMobLootEnabled(world)) return;
        if (!(entity instanceof ServerPlayer killer)) return;

        double chance;
//? if >=26.2 {
        if (killedEntity instanceof Villager) {
//? } else {
        if (killedEntity.getType() == EntityType.VILLAGER) {
//? }
            chance = ScytheBalance.Drops.VILLAGER_DROP_CHANCE;
        } else if (killedEntity instanceof ServerPlayer victim) {
            if (killer.isAlliedTo(victim)) return;
            chance = ScytheBalance.Drops.PLAYER_DROP_CHANCE;
        } else {
            return;
        }

        if (killer.getRandom().nextDouble() >= chance) return;

        ItemStack stack = new ItemStack(ScytheMod.BLOODY_ESSENCE, ScytheBalance.Drops.BLOODY_ESSENCE_COUNT);

        ItemEntity drop = new ItemEntity(
                world,
                killedEntity.getX(),
                killedEntity.getY(),
                killedEntity.getZ(),
                stack
        );

        drop.setDefaultPickUpDelay();
        world.addFreshEntity(drop);
    }

    private static boolean isDoMobLootEnabled(ServerLevel world) {
        Object rule = doMobLootRule;
        if (rule == null) {
            rule = findDoMobLootRule();
            doMobLootRule = rule;
        }

        return rule == null || readBooleanGameRule(world.getGameRules(), rule);
    }

    private static Object findDoMobLootRule() {
        String[] candidateNames = {
                "RULE_DOMOBLOOT",
                "RULE_DO_MOB_LOOT",
                "DO_MOB_LOOT"
        };

        for (String fieldName : candidateNames) {
            Object key = readGameRuleField(fieldName);
            if (key != null) {
                return key;
            }
        }

        for (Field field : GameRules.class.getDeclaredFields()) {
            String normalizedName = field.getName().replace("_", "").toUpperCase();
            if (normalizedName.contains("MOB") && normalizedName.contains("LOOT")) {
                Object key = readGameRuleField(field.getName());
                if (key != null) {
                    return key;
                }
            }
        }

        return null;
    }

    private static Object readGameRuleField(String fieldName) {
        try {
            Field field = GameRules.class.getDeclaredField(fieldName);
            if (!Modifier.isStatic(field.getModifiers())) {
                return null;
            }
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException ignored) {
            // Different 26.x mapping sets use different field names for the same gamerule.
        }

        return null;
    }

    private static boolean readBooleanGameRule(GameRules gameRules, Object rule) {
        try {
            for (Method method : GameRules.class.getMethods()) {
                if (!method.getName().equals("getBoolean") || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameterType = method.getParameterTypes()[0];
                if (!parameterType.isInstance(rule)) {
                    continue;
                }
                Object value = method.invoke(gameRules, rule);
                if (value instanceof Boolean booleanValue) {
                    return booleanValue;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // If the 26.x API changes again, fail open instead of breaking entity drops entirely.
        }

        return true;
    }
}
