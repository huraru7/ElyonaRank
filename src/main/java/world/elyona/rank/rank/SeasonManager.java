package world.elyona.rank.rank;

import world.elyona.core.rank.RankTier;

import org.bukkit.Bukkit;
import world.elyona.rank.ElyonaRankPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SeasonManager {

    private final ElyonaRankPlugin plugin;
    private final RankRepository repository;

    /** アクティブシーズン情報 */
    private volatile RankRepository.SeasonRecord activeSeason;

    /** オンラインプレイヤーのランクキャッシュ: UUID → RankRecord */
    private final Map<UUID, RankRepository.RankRecord> rankCache = new ConcurrentHashMap<>();

    public SeasonManager(ElyonaRankPlugin plugin, RankRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        loadActiveSeason();
    }

    private void loadActiveSeason() {
        repository.getActiveSeason().thenAccept(record -> {
            this.activeSeason = record;
            if (record != null) {
                plugin.getLogger().info("アクティブシーズン: " + record.name() + " (ID: " + record.seasonId() + ")");
            } else {
                plugin.getLogger().info("アクティブシーズンはありません。");
            }
        }).exceptionally(e -> {
            plugin.getLogger().warning("シーズン読み込みに失敗: " + e.getMessage());
            return null;
        });
    }

    /** 新シーズン開始 */
    public void startSeason(String name, Runnable onComplete) {
        repository.createSeason(name).thenAccept(seasonId -> {
            if (seasonId < 0) {
                plugin.getLogger().warning("シーズン作成に失敗しました。");
                return;
            }
            loadActiveSeason();
            rankCache.clear();
            Bukkit.getScheduler().runTask(plugin, onComplete);
        });
    }

    /** 現在のシーズンを終了し全員Bronzeリセット */
    public void endSeason(Runnable onComplete) {
        if (activeSeason == null) {
            Bukkit.getScheduler().runTask(plugin, onComplete);
            return;
        }
        int seasonId = activeSeason.seasonId();
        repository.endSeason(seasonId)
                .thenCompose(v -> repository.resetAllRanks(seasonId))
                .thenAccept(v -> {
                    // オンラインプレイヤーのキャッシュもリセット
                    rankCache.replaceAll((uuid, record) ->
                            new RankRepository.RankRecord(uuid, record.seasonId(), RankTier.BRONZE, 0L));
                    activeSeason = null;
                    Bukkit.getScheduler().runTask(plugin, onComplete);
                });
    }

    public RankRepository.SeasonRecord getActiveSeason() {
        return activeSeason;
    }

    public boolean hasActiveSeason() {
        return activeSeason != null;
    }

    // ---- Rank キャッシュ操作 ----

    public void loadRank(UUID uuid) {
        if (activeSeason == null) return;
        repository.getRank(uuid, activeSeason.seasonId()).thenAccept(record -> {
            if (record != null) rankCache.put(uuid, record);
        });
    }

    public void saveRank(UUID uuid) {
        RankRepository.RankRecord record = rankCache.get(uuid);
        if (record == null) return;
        repository.saveRank(uuid, record.seasonId(), record.tier(), record.expTotal());
    }

    public void saveAndRemoveRank(UUID uuid) {
        saveRank(uuid);
        rankCache.remove(uuid);
    }

    public RankRepository.RankRecord getCachedRank(UUID uuid) {
        return rankCache.get(uuid);
    }

    /**
     * ランクEXPをキャッシュ更新すると同時にDBへも即時反映する(write-through)。
     * 以前はキャッシュのみ更新しquit時にまとめて保存するwrite-back方式だったため、
     * サーバーが強制終了するとEXP進捗が失われる問題があった(Economyで見つかったのと同種のバグ)。
     */
    public void updateCache(UUID uuid, RankTier tier, long exp) {
        if (activeSeason == null) return;
        int seasonId = activeSeason.seasonId();
        rankCache.put(uuid, new RankRepository.RankRecord(uuid, seasonId, tier, exp));
        repository.saveRank(uuid, seasonId, tier, exp);
    }
}
