package com.shipovskijkorp.scythes.mod.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loader-neutral compact Guide Book data model.
 *
 * <p>The storage model deliberately mirrors BCCE's compact guide resources: page mechanics/templates are stored
 * once, while each language only supplies arrays of visible strings. Minecraft-specific resource access lives in
 * the client screen adapters, so this class can be reused by Fabric, Forge and NeoForge.</p>
 */
public final class GuideResources {
    private static final Pattern TEXT_SLOT = Pattern.compile("\\{\\{sc_text:(\\d+)\\}\\}");
    private static final String LANGUAGES = "guide/languages.json";
    private static final String LAYOUTS = "guide/page_layouts.json";
    private static final String MANIFEST = "guide/manifest.json";
    private static final String RECIPES = "guide/recipes.json";
    private static final String TEXT_PREFIX = "guide/text/";

    private final String welcomePage;
    private final List<Entry> entries;
    private final Map<String, GuideDocument> documents;
    private final Map<String, GuideRecipe> recipes;

    private GuideResources(String welcomePage, List<Entry> entries, Map<String, GuideDocument> documents, Map<String, GuideRecipe> recipes) {
        this.welcomePage = welcomePage;
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        this.documents = Collections.unmodifiableMap(new LinkedHashMap<>(documents));
        this.recipes = Collections.unmodifiableMap(new LinkedHashMap<>(recipes));
    }

    public static GuideResources load(ResourceReader reader, String requestedLanguage) throws IOException {
        JsonObject languages = parseObject(reader.read(LANGUAGES, true), LANGUAGES);
        JsonObject layoutRoot = parseObject(reader.read(LAYOUTS, true), LAYOUTS);
        JsonObject manifest = parseObject(reader.read(MANIFEST, true), MANIFEST);
        JsonObject recipeRoot = parseObject(reader.read(RECIPES, true), RECIPES);

        String defaultLanguage = normalizeLanguage(getString(languages, "default", "en_us"));
        Map<String, String> aliases = readAliases(languages.getAsJsonObject("aliases"));
        Map<String, List<String>> fallbacks = readFallbacks(languages.getAsJsonObject("fallbacks"));
        String selectedLanguage = resolveAlias(normalizeLanguage(requestedLanguage), aliases);
        List<String> loadOrder = buildLoadOrder(defaultLanguage, selectedLanguage, aliases, fallbacks);

        Map<String, Template> templates = readTemplates(layoutRoot);
        Map<String, PageLayout> pages = readPages(layoutRoot, templates);
        Map<String, List<String>> localizedText = new LinkedHashMap<>();
        Map<String, String> recipeNames = new LinkedHashMap<>();
        for (String language : loadOrder) {
            String path = TEXT_PREFIX + language + ".json";
            String text = reader.read(path, language.equals(resolveAlias(defaultLanguage, aliases)));
            if (text == null) continue;
            JsonObject pack = parseObject(text, path);
            overlayTextPack(pack, pages, localizedText);
            overlayRecipeNames(pack, recipeNames);
        }

        Map<String, GuideDocument> documents = new LinkedHashMap<>();
        for (Map.Entry<String, PageLayout> entry : pages.entrySet()) {
            List<String> values = localizedText.get(entry.getKey());
            if (values == null) values = Collections.nCopies(entry.getValue().slots, "");
            if (values.size() < entry.getValue().slots) {
                List<String> padded = new ArrayList<>(values);
                while (padded.size() < entry.getValue().slots) padded.add("");
                values = padded;
            }
            documents.put(entry.getKey(), GuideDocument.parse(renderTemplate(entry.getValue().template, values)));
        }

        String welcomePage = getString(manifest, "welcome", "welcome");
        List<Entry> entries = new ArrayList<>();
        JsonArray manifestEntries = manifest.getAsJsonArray("entries");
        if (manifestEntries != null) {
            for (JsonElement element : manifestEntries) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                String id = getString(object, "id", "");
                String page = getString(object, "page", id);
                String item = getString(object, "item", "");
                int colour = parseColour(getString(object, "colour", "#9DD5C0"));
                if (!id.isBlank() && documents.containsKey(page)) {
                    entries.add(new Entry(id, page, item, colour));
                }
            }
        }
        Map<String, GuideRecipe> recipes = readRecipes(recipeRoot, recipeNames);
        return new GuideResources(welcomePage, entries, documents, recipes);
    }


    /** Reads the packed guide resources directly from the mod jar. */
    public static ResourceReader classpathReader() {
        return (relativePath, required) -> {
            String path = "/assets/scythes/" + relativePath;
            try (InputStream stream = GuideResources.class.getResourceAsStream(path)) {
                if (stream == null) {
                    if (required) throw new IOException("Missing guide resource " + path);
                    return null;
                }
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        };
    }

    /** Safe fallback shown if packed guide data is damaged or missing. */
    public static GuideResources fallback() {
        Map<String, GuideDocument> docs = new LinkedHashMap<>();
        docs.put("welcome", GuideDocument.parse("# Welcome\nThe guide data could not be loaded.\n<new_page/>\n## Scythes\nRestart the game or reinstall ScytheMod."));
        return new GuideResources("welcome", List.of(), docs, Map.of());
    }

    public String welcomePage() {
        return welcomePage;
    }

    public List<Entry> entries() {
        return entries;
    }

    public GuideDocument document(String page) {
        GuideDocument document = documents.get(page);
        return document == null ? GuideDocument.empty(page) : document;
    }

    public GuideRecipe recipe(String id) {
        return recipes.get(id);
    }

    public static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) return "en_us";
        return language.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static JsonObject parseObject(String text, String path) throws IOException {
        try {
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) throw new IOException("Guide resource is not a JSON object: " + path);
            return parsed.getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid guide JSON resource " + path, exception);
        }
    }

    private static Map<String, Template> readTemplates(JsonObject root) throws IOException {
        JsonObject object = root.getAsJsonObject("templates");
        if (object == null) throw new IOException("Guide layout resource has no 'templates' object");
        Map<String, Template> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject template = entry.getValue().getAsJsonObject();
            String body = getString(template, "template", "");
            int slots = template.has("slots") ? Math.max(0, template.get("slots").getAsInt()) : 0;
            result.put(entry.getKey(), new Template(body, slots));
        }
        return result;
    }

    private static Map<String, PageLayout> readPages(JsonObject root, Map<String, Template> templates) throws IOException {
        JsonObject object = root.getAsJsonObject("pages");
        if (object == null) throw new IOException("Guide layout resource has no 'pages' object");
        Map<String, PageLayout> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String templateName;
            if (entry.getValue().isJsonPrimitive()) {
                templateName = entry.getValue().getAsString();
            } else if (entry.getValue().isJsonObject()) {
                templateName = getString(entry.getValue().getAsJsonObject(), "template", "");
            } else {
                continue;
            }
            Template template = templates.get(templateName);
            if (template == null) throw new IOException("Unknown guide template '" + templateName + "' for page " + entry.getKey());
            result.put(entry.getKey(), new PageLayout(template.template, template.slots));
        }
        return result;
    }

    private static void overlayTextPack(JsonObject pack, Map<String, PageLayout> layouts,
        Map<String, List<String>> localizedText) {
        JsonObject pages = pack.getAsJsonObject("pages");
        if (pages == null) return;
        for (Map.Entry<String, JsonElement> entry : pages.entrySet()) {
            PageLayout layout = layouts.get(entry.getKey());
            if (layout == null || !entry.getValue().isJsonArray()) continue;
            List<String> target = localizedText.computeIfAbsent(entry.getKey(), ignored -> emptySlots(layout.slots));
            JsonArray array = entry.getValue().getAsJsonArray();
            int count = Math.min(array.size(), layout.slots);
            for (int index = 0; index < count; index++) {
                JsonElement value = array.get(index);
                if (value != null && !value.isJsonNull() && value.isJsonPrimitive()) {
                    target.set(index, value.getAsString());
                }
            }
        }
    }

    private static List<String> emptySlots(int count) {
        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) values.add("");
        return values;
    }

    private static String renderTemplate(String template, List<String> values) {
        Matcher matcher = TEXT_SLOT.matcher(template);
        StringBuffer output = new StringBuffer(template.length());
        while (matcher.find()) {
            int index;
            try {
                index = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                index = -1;
            }
            String replacement = index >= 0 && index < values.size() && values.get(index) != null
                ? values.get(index) : "";
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static Map<String, String> readAliases(JsonObject object) {
        Map<String, String> aliases = new LinkedHashMap<>();
        if (object == null) return aliases;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                aliases.put(normalizeLanguage(entry.getKey()), normalizeLanguage(entry.getValue().getAsString()));
            }
        }
        return aliases;
    }

    private static Map<String, List<String>> readFallbacks(JsonObject object) {
        Map<String, List<String>> fallbacks = new LinkedHashMap<>();
        if (object == null) return fallbacks;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!entry.getValue().isJsonArray()) continue;
            List<String> values = new ArrayList<>();
            for (JsonElement value : entry.getValue().getAsJsonArray()) {
                if (value.isJsonPrimitive()) values.add(normalizeLanguage(value.getAsString()));
            }
            fallbacks.put(normalizeLanguage(entry.getKey()), values);
        }
        return fallbacks;
    }

    private static String resolveAlias(String language, Map<String, String> aliases) {
        String current = normalizeLanguage(language);
        Set<String> visited = new HashSet<>();
        while (visited.add(current)) {
            String next = aliases.get(current);
            if (next == null || next.isBlank()) return current;
            current = normalizeLanguage(next);
        }
        return normalizeLanguage(language);
    }

    private static List<String> buildLoadOrder(String defaultLanguage, String selectedLanguage,
        Map<String, String> aliases, Map<String, List<String>> fallbacks) {
        String defaultCode = resolveAlias(defaultLanguage, aliases);
        String selectedCode = resolveAlias(selectedLanguage, aliases);
        List<String> order = new ArrayList<>();
        addLanguage(order, defaultCode);
        addFallbacks(order, selectedCode, aliases, fallbacks, new HashSet<>());
        addLanguage(order, selectedCode);
        return order;
    }

    private static void addFallbacks(List<String> order, String language, Map<String, String> aliases,
        Map<String, List<String>> fallbacks, Set<String> visiting) {
        String resolved = resolveAlias(language, aliases);
        if (!visiting.add(resolved)) return;
        List<String> parents = fallbacks.get(resolved);
        if (parents != null) {
            for (String parent : parents) {
                String parentResolved = resolveAlias(parent, aliases);
                addFallbacks(order, parentResolved, aliases, fallbacks, visiting);
                addLanguage(order, parentResolved);
            }
        }
        visiting.remove(resolved);
    }

    private static void addLanguage(List<String> order, String language) {
        if (!order.contains(language)) order.add(language);
    }

    private static String getString(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    private static int parseColour(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("#")) value = value.substring(1);
        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return 0x9DD5C0;
        }
    }


    private static void overlayRecipeNames(JsonObject pack, Map<String, String> names) {
        JsonObject object = pack.getAsJsonObject("recipe_names");
        if (object == null) return;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) names.put(entry.getKey(), entry.getValue().getAsString());
        }
    }

    private static Map<String, GuideRecipe> readRecipes(JsonObject root, Map<String, String> names) {
        Map<String, GuideRecipe> result = new LinkedHashMap<>();
        JsonObject object = root.getAsJsonObject("recipes");
        if (object == null) return result;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject recipe = entry.getValue().getAsJsonObject();
            List<RecipeSlot> slots = new ArrayList<>();
            JsonArray ingredients = recipe.getAsJsonArray("ingredients");
            for (int i = 0; i < 9; i++) {
                JsonObject slot = ingredients != null && i < ingredients.size() && ingredients.get(i).isJsonObject()
                    ? ingredients.get(i).getAsJsonObject() : null;
                slots.add(readRecipeSlot(slot, names));
            }
            RecipeSlot output = readRecipeSlot(recipe.getAsJsonObject("output"), names);
            result.put(entry.getKey(), new GuideRecipe(slots, output));
        }
        return result;
    }

    private static RecipeSlot readRecipeSlot(JsonObject object, Map<String, String> names) {
        if (object == null) return new RecipeSlot("", "", "");
        String item = getString(object, "item", "");
        String nameKey = getString(object, "name", item);
        String name = names.getOrDefault(nameKey, nameKey);
        return new RecipeSlot(item, nameKey, name);
    }
    public record Entry(String id, String page, String item, int colour) {}
    public record RecipeSlot(String item, String nameKey, String name) { public boolean empty() { return item == null || item.isBlank(); } }
    public record GuideRecipe(List<RecipeSlot> ingredients, RecipeSlot output) {
        public GuideRecipe { ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients)); }
    }

    @FunctionalInterface
    public interface ResourceReader {
        /** Returns null only when a non-required resource does not exist. */
        String read(String relativePath, boolean required) throws IOException;
    }

    private record Template(String template, int slots) {}
    private record PageLayout(String template, int slots) {}
}
