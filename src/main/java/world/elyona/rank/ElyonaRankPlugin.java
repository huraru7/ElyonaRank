package world.elyona.rank;

import org.bukkit.plugin.java.JavaPlugin;
import world.elyona.core.ElyonaCorePlugin;
import world.elyona.core.mimic.MimicMessenger;
import world.elyona.rank.command.RankCommand;
import world.elyona.rank.command.SeasonCommand;
import world.elyona.rank.command.TitleCommand;
import world.elyona.rank.listener.GuiListener;
import world.elyona.rank.listener.RankJoinListener;
import world.elyona.rank.listener.RankQuitListener;
import world.elyona.rank.listener.RankUpListener;
import world.elyona.rank.rank.RankManager;
import world.elyona.rank.rank.RankRepository;
import world.elyona.rank.rank.SeasonManager;
import world.elyona.rank.title.TitleLoader;
import world.elyona.rank.title.TitleManager;
import world.elyona.rank.title.TitleRepository;

public class ElyonaRankPlugin extends JavaPlugin {

    private static ElyonaRankPlugin instance;

    private RankRepository rankRepository;
    private SeasonManager seasonManager;
    private RankManager rankManager;
    private TitleLoader titleLoader;
    private TitleRepository titleRepository;
    private TitleManager titleManager;

    @Override
    public void onEnable() {
        instance = this;

        ElyonaCorePlugin core = ElyonaCorePlugin.getInstance();
        MimicMessenger mimic = core.getMimicMessenger();

        // ランクシステム
        rankRepository = new RankRepository(core.getDatabaseManager());
        rankRepository.initialize();
        seasonManager = new SeasonManager(this, rankRepository);
        rankManager = new RankManager(this, rankRepository, seasonManager);

        // 称号システム
        titleLoader = new TitleLoader(this);
        titleRepository = new TitleRepository(core.getDatabaseManager());
        titleRepository.initialize();
        titleManager = new TitleManager(this, titleRepository, titleLoader, mimic);

        // リスナー登録
        getServer().getPluginManager().registerEvents(
                new RankJoinListener(seasonManager, titleManager), this);
        getServer().getPluginManager().registerEvents(
                new RankQuitListener(seasonManager, titleManager), this);
        getServer().getPluginManager().registerEvents(
                new RankUpListener(this, rankManager, titleManager, mimic), this);
        getServer().getPluginManager().registerEvents(
                new GuiListener(), this);

        // コマンド登録
        getCommand("rank").setExecutor(new RankCommand(rankManager, seasonManager));
        getCommand("season").setExecutor(new SeasonCommand(this, seasonManager, rankManager, mimic));
        getCommand("title").setExecutor(new TitleCommand(titleManager, mimic));

        getLogger().info("ElyonaRank が有効化されました。");
    }

    @Override
    public void onDisable() {
        getLogger().info("ElyonaRank が無効化されました。");
    }

    public static ElyonaRankPlugin getInstance() { return instance; }
    public SeasonManager getSeasonManager() { return seasonManager; }
    public RankManager getRankManager() { return rankManager; }
    public TitleManager getTitleManager() { return titleManager; }
    public TitleLoader getTitleLoader() { return titleLoader; }
}
