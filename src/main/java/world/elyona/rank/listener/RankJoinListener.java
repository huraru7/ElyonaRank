package world.elyona.rank.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import world.elyona.rank.ElyonaRankPlugin;
import world.elyona.rank.rank.SeasonManager;
import world.elyona.rank.title.TitleManager;

public class RankJoinListener implements Listener {

    private final SeasonManager seasonManager;
    private final TitleManager titleManager;

    public RankJoinListener(SeasonManager seasonManager, TitleManager titleManager) {
        this.seasonManager = seasonManager;
        this.titleManager = titleManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        seasonManager.loadRank(player.getUniqueId());
        titleManager.loadActiveTitle(player.getUniqueId()).exceptionally(e -> {
            ElyonaRankPlugin.getInstance().getLogger()
                    .warning("称号データ読み込みエラー(" + player.getName() + "): " + e.getMessage());
            return null;
        });
    }
}
