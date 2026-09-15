package com.shipovskijkorp.scythes.mod.mixin;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetMixin {
    @Unique private static final String SCYTHES_ROOT = "scythes:root";
    @Unique private static final String SCYTHES_MASTERY = "scythes:money_cards_god_scythe";
    @Unique private static final int SCYTHES_MASTERY_X = 168;
    @Unique private static final Set<String> SCYTHES_FINAL_CHALLENGES = Set.of(
        "scythes:merciless",
        "scythes:impossible_intoxication",
        "scythes:super_necromancer",
        "scythes:three_days_rain",
        "scythes:supercooled_snow",
        "scythes:return_to_returner",
        "scythes:dumb_and_dumber"
    );
    @Unique private static final Map<AdvancementTab, Map<String, WeakReference<AdvancementWidget>>> SCYTHES_WIDGETS = new WeakHashMap<>();
    @Unique private static final Map<AdvancementTab, Set<String>> SCYTHES_COMPLETED = new WeakHashMap<>();

    @Shadow @Final private AdvancementTab tab;
    @Shadow @Final private PlacedAdvancement advancement;
    @Shadow @Final @Mutable private int x;
    @Shadow @Final @Mutable private int y;

    @Unique private String scythes$advancementId;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void scythes$positionMastery(AdvancementTab tab, MinecraftClient client, PlacedAdvancement advancement,
                                         AdvancementDisplay display, CallbackInfo ci) {
        scythes$advancementId = advancement.getAdvancementEntry().id().toString();
        scythes$register(tab, scythes$advancementId, (AdvancementWidget) (Object) this);
        if (!SCYTHES_MASTERY.equals(scythes$advancementId)) return;

        x = SCYTHES_MASTERY_X;
        AdvancementWidget root = scythes$lookup(tab, SCYTHES_ROOT);
        if (root != null) y = root.getY();
    }

    @Inject(method = "renderLines", at = @At("HEAD"), cancellable = true)
    private void scythes$renderMasteryArrows(DrawContext context, int originX, int originY, boolean border, CallbackInfo ci) {
        if (!SCYTHES_MASTERY.equals(scythes$advancementId)) return;
        if (!scythes$isMasteryRevealed(tab)) {
            ci.cancel();
            return;
        }

        AdvancementWidget root = scythes$lookup(tab, SCYTHES_ROOT);
        if (root != null) y = root.getY();

        for (String id : SCYTHES_FINAL_CHALLENGES) {
            AdvancementWidget source = scythes$lookup(tab, id);
            if (source != null) scythes$drawArrow(context, originX, originY, source, x, y, border);
        }
        ci.cancel();
    }

    @Inject(method = "setProgress", at = @At("TAIL"))
    private void scythes$trackChallengeProgress(AdvancementProgress progress, CallbackInfo ci) {
        if (!SCYTHES_FINAL_CHALLENGES.contains(scythes$advancementId)) return;
        Set<String> completed = SCYTHES_COMPLETED.computeIfAbsent(tab, ignored -> new HashSet<>());
        if (progress.isDone()) completed.add(scythes$advancementId);
        else completed.remove(scythes$advancementId);
    }

    @Inject(method = "renderWidgets", at = @At("HEAD"), cancellable = true)
    private void scythes$hideMasteryWidget(DrawContext context, int originX, int originY, CallbackInfo ci) {
        if (SCYTHES_MASTERY.equals(scythes$advancementId) && !scythes$isMasteryRevealed(tab)) ci.cancel();
    }

    @Inject(method = "drawTooltip", at = @At("HEAD"), cancellable = true)
    private void scythes$hideMasteryTooltip(DrawContext context, int originX, int originY, float alpha,
                                             int screenX, int screenY, CallbackInfo ci) {
        if (SCYTHES_MASTERY.equals(scythes$advancementId) && !scythes$isMasteryRevealed(tab)) ci.cancel();
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void scythes$hideMasteryHitbox(int originX, int originY, int mouseX, int mouseY,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (SCYTHES_MASTERY.equals(scythes$advancementId) && !scythes$isMasteryRevealed(tab)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean scythes$isMasteryRevealed(AdvancementTab tab) {
        Set<String> completed = SCYTHES_COMPLETED.get(tab);
        if (completed == null || completed.isEmpty()) return false;
        for (String id : SCYTHES_FINAL_CHALLENGES) {
            if (completed.contains(id)) return true;
        }
        return false;
    }

    @Unique
    private static void scythes$register(AdvancementTab tab, String id, AdvancementWidget widget) {
        SCYTHES_WIDGETS.computeIfAbsent(tab, ignored -> new LinkedHashMap<>()).put(id, new WeakReference<>(widget));
    }

    @Unique
    private static AdvancementWidget scythes$lookup(AdvancementTab tab, String id) {
        Map<String, WeakReference<AdvancementWidget>> byId = SCYTHES_WIDGETS.get(tab);
        if (byId == null) return null;
        WeakReference<AdvancementWidget> reference = byId.get(id);
        return reference == null ? null : reference.get();
    }

    @Unique
    private static void scythes$drawArrow(DrawContext context, int originX, int originY,
                                           AdvancementWidget source, int targetX, int targetY, boolean border) {
        int color = border ? 0xFF000000 : 0xFFFFFFFF;
        int thickness = border ? 3 : 1;
        int startX = originX + source.getX() + 26;
        int startY = originY + source.getY() + 13;
        int endX = originX + targetX - 2;
        int endY = originY + targetY + 13;
        int elbowX = startX + Math.max(8, (endX - startX) / 2);

        scythes$hLine(context, startX, elbowX, startY, thickness, color);
        scythes$vLine(context, elbowX, startY, endY, thickness, color);
        scythes$hLine(context, elbowX, endX - 5, endY, thickness, color);
        scythes$arrowHead(context, endX, endY, border, color);
    }

    @Unique
    private static void scythes$hLine(DrawContext context, int x1, int x2, int y, int thickness, int color) {
        int half = thickness / 2;
        context.fill(Math.min(x1, x2), y - half, Math.max(x1, x2) + 1, y + half + 1, color);
    }

    @Unique
    private static void scythes$vLine(DrawContext context, int x, int y1, int y2, int thickness, int color) {
        int half = thickness / 2;
        context.fill(x - half, Math.min(y1, y2), x + half + 1, Math.max(y1, y2) + 1, color);
    }

    @Unique
    private static void scythes$arrowHead(DrawContext context, int tipX, int centerY, boolean border, int color) {
        int size = border ? 5 : 4;
        for (int step = 0; step <= size; step++) {
            int half = size - step;
            int px = tipX - size + step;
            context.fill(px, centerY - half, px + 1, centerY + half + 1, color);
        }
    }
}
