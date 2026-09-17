package com.shipovskijkorp.scythes.mod.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TooltipUtil {

    private TooltipUtil() {}

    public static void addHoldShiftHint(List<Text> tooltip) {
        tooltip.add(
                Text.translatable(
                                "tooltip.scythes.hold_shift",
                                Text.literal("Shift").formatted(Formatting.YELLOW)
                        )
                        .formatted(Formatting.DARK_GRAY)
        );
    }

    public static void addHoldAltHint(List<Text> tooltip) {
        tooltip.add(
                Text.translatable(
                                "tooltip.scythes.hold_alt",
                                Text.literal("Alt").formatted(Formatting.YELLOW)
                        )
                        .formatted(Formatting.DARK_GRAY)
        );
    }

    public static Text getScytheAbilityKeyText(Formatting formatting) {
        return ScytheModClient.getScytheAbilityKeyBoundText().copy().formatted(formatting);
    }

    public static void addWrapped(List<Text> tooltip, String translationKey, Formatting... formatting) {
        addWrapped(tooltip, Text.translatable(translationKey), formatting);
    }

    public static void addWrapped(List<Text> tooltip, Text text, Formatting... formatting) {
        String raw = text.getString();
        addWrappedRaw(tooltip, raw, formatting);
    }

    public static void addWrappedRaw(List<Text> tooltip, String raw, Formatting... formatting) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        // примерно как ванильные подсказки: до половины ширины экрана
        int maxWidth = Math.max(120, client.getWindow().getScaledWidth() / 2 - 16);

        String[] paragraphs = raw.split("\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                tooltip.add(Text.empty());
                continue;
            }

            for (String line : wrapLine(tr, paragraph, maxWidth)) {
                tooltip.add(Text.literal(line).formatted(formatting));
            }
        }
    }

    private static List<String> wrapLine(TextRenderer tr, String text, int maxWidth) {
        List<String> out = new ArrayList<>();

        String trimmed = text.strip();
        if (trimmed.isEmpty()) return out;

        String[] words = trimmed.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;

            // если одно "слово" шире лимита — режем по символам
            if (tr.getWidth(word) > maxWidth) {
                if (line.length() > 0) {
                    out.add(line.toString());
                    line.setLength(0);
                }

                int start = 0;
                while (start < word.length()) {
                    int end = start + 1;
                    while (end <= word.length() && tr.getWidth(word.substring(start, end)) <= maxWidth) {
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
            if (tr.getWidth(candidate) <= maxWidth) {
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
        // 10.0 -> "10", 6.5 -> "6.5"
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
