package net.onewaycraft.oneWayTPA;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * Loads {@code lang.yml} from the plugin data folder and renders Adventure
 * {@link Component}s with legacy {@code &}-color-code support and {@code {placeholder}}
 * substitution. The {@code prefix} key is automatically prepended by {@link #send}.
 * <p>
 * If a key is missing the renderer returns a visible {@code [missing lang key: NAME]}
 * marker rather than failing silently — this is intentional so translation gaps surface
 * during testing.
 */
public final class MessageService {

    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();
    private final YamlConfiguration langConfig;

    public MessageService(JavaPlugin plugin) {
        File langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        this.langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public void send(CommandSender target, String key) {
        send(target, key, Collections.emptyMap());
    }

    public void send(CommandSender target, String key, Map<String, String> placeholders) {
        target.sendMessage(withPrefix(key, placeholders));
    }

    public Component withPrefix(String key, Map<String, String> placeholders) {
        return parseLegacy(raw("prefix") + raw(key), placeholders);
    }

    public Component noPrefix(String key, Map<String, String> placeholders) {
        return parseLegacy(raw(key), placeholders);
    }

    public Component prefix() {
        return legacySerializer.deserialize(raw("prefix"));
    }

    public Component parseLegacy(String legacy, Map<String, String> placeholders) {
        return legacySerializer.deserialize(applyPlaceholders(legacy, placeholders));
    }

    private String raw(String key) {
        String value = langConfig.getString("messages." + key);
        if (value == null) {
            return "&c[missing lang key: " + key + "] ";
        }
        return value;
    }

    public String rawString(String key, String fallback) {
        String value = langConfig.getString("messages." + key);
        return value != null ? value : fallback;
    }

    private String applyPlaceholders(String input, Map<String, String> placeholders) {
        String output = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            output = output.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return output;
    }
}
