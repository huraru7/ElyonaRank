package world.elyona.rank.title;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import world.elyona.rank.ElyonaRankPlugin;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TitleLoader {

    private final ElyonaRankPlugin plugin;
    private Map<String, TitleDefinition> titles = new HashMap<>();

    public TitleLoader(ElyonaRankPlugin plugin) {
        this.plugin = plugin;
        plugin.saveResource("titles.yml", false);
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "titles.yml");
        if (!file.exists()) {
            plugin.getLogger().warning("titles.yml が見つかりません。");
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        Map<String, TitleDefinition> loaded = new HashMap<>();

        if (cfg.isConfigurationSection("titles")) {
            for (String id : cfg.getConfigurationSection("titles").getKeys(false)) {
                String path = "titles." + id;
                String display = cfg.getString(path + ".display", id);
                String description = cfg.getString(path + ".description", "");
                String source = cfg.getString(path + ".source", "SPECIAL");
                loaded.put(id, new TitleDefinition(id, display, description, source));
            }
        }

        this.titles = Collections.unmodifiableMap(loaded);
        plugin.getLogger().info("称号を " + titles.size() + " 件読み込みました。");
    }

    public TitleDefinition getTitle(String id) {
        return titles.get(id);
    }

    public Map<String, TitleDefinition> getAllTitles() {
        return titles;
    }

    public boolean exists(String id) {
        return titles.containsKey(id);
    }
}
