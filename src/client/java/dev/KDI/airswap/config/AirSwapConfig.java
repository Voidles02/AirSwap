package dev.KDI.airswap.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class AirSwapConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern ITEM_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    public boolean enabled = true;
    public boolean swapBack = true;
    public int swapBackDelay = 1;
    public boolean playersOnly = true;
    public double minimumFallDistance = 0.0;
    public boolean allSwords = true;
    public List<String> allowedSwords = new ArrayList<>(List.of(
            "minecraft:wooden_sword", "minecraft:stone_sword", "minecraft:copper_sword",
            "minecraft:iron_sword", "minecraft:golden_sword", "minecraft:diamond_sword",
            "minecraft:netherite_sword"));
    public boolean onlyHotbar = true;
    public boolean showSwapMessage = false;
    public boolean disableWhenSneaking = false;
    public int cooldownTicks = 0;

    public static AirSwapConfig read(Path path) throws IOException {
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw new IOException("Air Swap config must be a JSON object");
            }
            root = parsed.getAsJsonObject();
        }
        AirSwapConfig config = new AirSwapConfig();
        config.enabled = bool(root, "enabled", config.enabled);
        config.swapBack = bool(root, "swapBack", config.swapBack);
        config.swapBackDelay = integer(root, "swapBackDelay", config.swapBackDelay, 0, 10);
        config.playersOnly = bool(root, "playersOnly", config.playersOnly);
        config.minimumFallDistance = number(root, "minimumFallDistance", config.minimumFallDistance, 0.0, 10.0);
        config.allSwords = bool(root, "allSwords", config.allSwords);
        config.onlyHotbar = bool(root, "onlyHotbar", config.onlyHotbar);
        config.showSwapMessage = bool(root, "showSwapMessage", config.showSwapMessage);
        config.disableWhenSneaking = bool(root, "disableWhenSneaking", config.disableWhenSneaking);
        config.cooldownTicks = integer(root, "cooldownTicks", config.cooldownTicks, 0, 20);
        JsonElement allowed = root.get("allowedSwords");
        if (allowed != null && allowed.isJsonArray()) {
            List<String> valid = new ArrayList<>();
            for (JsonElement entry : allowed.getAsJsonArray()) {
                if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
                    String id = normalizeItemId(entry.getAsString());
                    if (id != null && !valid.contains(id)) {
                        valid.add(id);
                    }
                }
            }
            if (allowed.getAsJsonArray().isEmpty() || !valid.isEmpty()) {
                config.allowedSwords = valid;
            }
        }
        return config;
    }

    public void write(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "airswap-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static String normalizeItemId(String input) {
        String id = input.trim().toLowerCase(Locale.ROOT);
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        return ITEM_ID.matcher(id).matches() ? id : null;
    }

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        JsonElement element = root.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                ? element.getAsBoolean() : fallback;
    }

    private static double number(JsonObject root, String key, double fallback, double min, double max) {
        JsonElement element = root.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }
        try {
            double value = element.getAsDouble();
            return Double.isFinite(value) && value >= min && value <= max ? value : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int integer(JsonObject root, String key, int fallback, int min, int max) {
        double value = number(root, key, fallback, min, max);
        return value == Math.rint(value) ? (int) value : fallback;
    }
}