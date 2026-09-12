package world.elyona.rank.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import world.elyona.rank.rank.SeasonManager;
import world.elyona.rank.title.TitleManager;

public class RankQuitListener implements Listener {

    private final SeasonManager seasonManager;
    private final TitleManager titleManager;

    public RankQuitListener(SeasonManager seasonManager, TitleManager titleManager) {
        this.seasonManager = seasonManager;
        this.titleManager = titleManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        seasonManager.saveAndRemoveRank(player.getUniqueId());
        titleManager.unloadActiveTitle(player.getUniqueId());
    }
}
