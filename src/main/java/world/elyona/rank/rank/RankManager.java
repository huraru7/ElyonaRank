package world.elyona.rank.rank;

import world.elyona.core.rank.RankTier;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import world.elyona.rank.ElyonaRankPlugin;
import world.elyona.core.event.ElyonaRankUpEvent;

public class RankManager {

    private final ElyonaRankPlugin plugin;
    private final RankRepository repository;
    private final SeasonManager seasonManager;

    public RankManager(ElyonaRankPlugin plugin, RankRepository repository,
                       SeasonManager seasonManager) {
        this.plugin = plugin;
        this.repository = repository;
        this.seasonManager = seasonManager;
    }

    /**
     * プレイヤーにEXPを付与し、ランクアップを検出してイベントを発火する。
     * メインスレッドから呼び出すこと（イベント発火のため）。
     */
    public void addExp(Player player, long exp) {
        if (!seasonManager.hasActiveSeason()) return;

        RankRepository.RankRecord record = seasonManager.getCachedRank(player.getUniqueId());
        if (record == null) {
            // 初回ロード後に再実行
            seasonManager.loadRank(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> addExpInternal(player, exp), 20L);
            return;
        }
        addExpInternal(player, exp);
    }

    private void addExpInternal(Player player, long exp) {
        RankRepository.RankRecord record = seasonManager.getCachedRank(player.getUniqueId());
        if (record == null) return;

        long newExp = record.expTotal() + exp;
        RankTier oldTier = record.tier();
        RankTier newTier = RankTier.fromExp(newExp);

        seasonManager.updateCache(player.getUniqueId(), newTier, newExp);

        if (newTier != oldTier) {
            // ランクアップ
            ElyonaRankUpEvent event = new ElyonaRankUpEvent(player, oldTier, newTier);
            Bukkit.getPluginManager().callEvent(event);
            plugin.getLogger().info("[Rank] " + player.getName() + " " + oldTier.name() + " → " + newTier.name() + " (EXP: " + newExp + ")");
        }
    }

    /** プレイヤーの現在ランクを返す */
    public RankTier getRank(Player player) {
        RankRepository.RankRecord record = seasonManager.getCachedRank(player.getUniqueId());
        return record != null ? record.tier() : RankTier.BRONZE;
    }

    /** プレイヤーの累計EXPを返す */
    public long getExp(Player player) {
        RankRepository.RankRecord record = seasonManager.getCachedRank(player.getUniqueId());
        return record != null ? record.expTotal() : 0L;
    }
}
