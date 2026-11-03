package com.thelostcenturies.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Translatable components with the English text as fallback, so players without this mod on their
 * client (it's only needed on the server) still see readable messages instead of translation keys.
 */
public final class Lang {
    private static final Map<String, String> EN_US = load();

    private Lang() {}

    public static MutableComponent tr(String key, Object... args) {
        return Component.translatableWithFallback(key, EN_US.getOrDefault(key, key), args);
    }

    private static Map<String, String> load() {
        Map<String, String> map = new HashMap<>();
        try (InputStream in = Lang.class.getResourceAsStream("/assets/lostcenturies/lang/en_us.json")) {
            if (in != null) {
                JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                json.entrySet().forEach(e -> map.put(e.getKey(), e.getValue().getAsString()));
            }
        } catch (Exception ignored) {
            // fall back to raw keys
        }
        return map;
    }
}
