package com.shipovskijkorp.scythes.mod.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class TooltipUtil {

    private TooltipUtil() {}

    public static boolean isShiftDown() {
        return hasShiftDown();
    }

    public static boolean isAltDown() {
        return hasAltDown();
    }

    public static boolean hasShiftDown() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.hasShiftDown();
    }

    public static boolean hasAltDown() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.hasAltDown();
    }

    public static Component getScytheAbilityKeyText(ChatFormatting formatting) {
        return ScytheModClient.getScytheAbilityKeyText(formatting);
    }

    public static void flush(List<Component> tooltip, Consumer<Component> textConsumer) {
        for (Component line : tooltip) {
            textConsumer.accept(line);
        }
    }

    public static void addHoldShiftHint(List<Component> tooltip) {
        tooltip.add(holdShiftHint());
    }

    public static void addHoldAltHint(List<Component> tooltip) {
        tooltip.add(holdAltHint());
    }

    public static void addHoldShiftHint(Consumer<Component> tooltip) {
        tooltip.accept(holdShiftHint());
    }

    public static void addHoldAltHint(Consumer<Component> tooltip) {
        tooltip.accept(holdAltHint());
    }

    private static Component holdShiftHint() {
        return Component.translatable(
                        "tooltip.scythes.hold_shift",
                        Component.literal("Shift").withStyle(ChatFormatting.YELLOW)
                )
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    private static Component holdAltHint() {
        return Component.translatable(
                        "tooltip.scythes.hold_alt",
                        Component.literal("Alt").withStyle(ChatFormatting.YELLOW)
                )
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    public static void addWrapped(List<Component> tooltip, String translationKey, ChatFormatting... formatting) {
        addWrapped(tooltip, Component.translatable(translationKey), formatting);
    }

    public static void addWrapped(List<Component> tooltip, Component text, ChatFormatting... formatting) {
        addWrappedRaw(tooltip, text.getString(), formatting);
    }

    public static void addWrapped(Consumer<Component> tooltip, String translationKey, ChatFormatting... formatting) {
        addWrapped(tooltip, Component.translatable(translationKey), formatting);
    }

    public static void addWrapped(Consumer<Component> tooltip, Component text, ChatFormatting... formatting) {
        List<Component> lines = new ArrayList<>();
        addWrappedRaw(lines, text.getString(), formatting);
        flush(lines, tooltip);
    }

    public static void addWrappedRaw(List<Component> tooltip, String raw, ChatFormatting... formatting) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        int maxWidth = Math.max(120, client.getWindow().getGuiScaledWidth() / 2 - 16);

        String[] paragraphs = raw.split("\\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                tooltip.add(Component.empty());
                continue;
            }

            for (String line : wrapLine(font, paragraph, maxWidth)) {
                tooltip.add(Component.literal(line).withStyle(formatting));
            }
        }
    }

    private static List<String> wrapLine(Font font, String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        String trimmed = text.strip();
        if (trimmed.isEmpty()) return out;

        String[] words = trimmed.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;

            if (font.width(word) > maxWidth) {
                if (line.length() > 0) {
                    out.add(line.toString());
                    line.setLength(0);
                }

                int start = 0;
                while (start < word.length()) {
                    int end = start + 1;
                    while (end <= word.length() && font.width(word.substring(start, end)) <= maxWidth) {
                        end++;
                    }
                    int cut = Math.max(start + 1, end - 1);
                    out.add(word.substring(start, cut));
                    start = cut;
                }
                continue;
            }

            if (line.length() == 0) {
                line.append(word);
                continue;
            }

            String candidate = line + " " + word;
            if (font.width(candidate) <= maxWidth) {
                line.append(' ').append(word);
            } else {
                out.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }

        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    public static String fmtNumber(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) {
            return String.format(Locale.ROOT, "%.0f", v);
        }
        return String.format(Locale.ROOT, "%.1f", v);
    }

    public static String fmtSecondsValue(int ticks) {
        double sec = ticks / 20.0;
        if (ticks % 20 == 0) return String.format(Locale.ROOT, "%.0f", sec);
        return String.format(Locale.ROOT, "%.1f", sec);
    }

    public static String fmtPercentValue(double frac) {
        double p = frac * 100.0;
        if (Math.abs(p - Math.rint(p)) < 1e-9) return String.format(Locale.ROOT, "%.0f", p);
        return String.format(Locale.ROOT, "%.1f", p);
    }
}
