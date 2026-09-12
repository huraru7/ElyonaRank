package world.elyona.rank.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import world.elyona.core.event.ElyonaRankUpEvent;
import world.elyona.core.mimic.MimicMessenger;
import world.elyona.rank.ElyonaRankPlugin;
import world.elyona.rank.rank.RankManager;
import world.elyona.core.rank.RankTier;
import world.elyona.rank.title.TitleManager;

/**
 * ランクアップ本体の通知・称号付与を担当する。
 * Cr報酬の付与・通知は ElyonaEconomy 側の RankRewardListener が
 * 同じ ElyonaRankUpEvent を個別にリッスンして行う（ドメイン分割）。
 */
public class RankUpListener implements Listener {

    private final ElyonaRankPlugin plugin;
    private final RankManager rankManager;
    private final TitleManager titleManager;
    private final MimicMessenger mimic;

    public RankUpListener(ElyonaRankPlugin plugin, RankManager rankManager,
                          TitleManager titleManager, MimicMessenger mimic) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.titleManager = titleManager;
        this.mimic = mimic;
    }

    @EventHandler
    public void onRankUp(ElyonaRankUpEvent event) {
        RankTier newRank = event.getNewRank();
        var player = event.getPlayer();

        // 本人への通知
        mimic.sendTo(player, "ランクアップ！ " + event.getOldRank().displayName + " → " + newRank.displayName);

        // 全体通知
        mimic.broadcast(player.getName() + " が " + newRank.displayName + " にランクアップしました！");

        // 称号付与
        if (newRank.titleId != null) {
            titleManager.grant(player, newRank.titleId);
        }
    }
}
