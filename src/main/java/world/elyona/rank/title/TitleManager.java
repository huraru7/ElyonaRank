package world.elyona.rank.title;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import world.elyona.rank.ElyonaRankPlugin;
import world.elyona.core.event.ElyonaTitleGrantEvent;
import world.elyona.core.mimic.MimicMessenger;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class TitleManager {

    private final ElyonaRankPlugin plugin;
    private final TitleRepository repository;
    private final TitleLoader loader;
    private final MimicMessenger mimic;

    /** オンラインプレイヤーのアクティブ称号キャッシュ */
    private final Map<UUID, String> activeTitleCache = new ConcurrentHashMap<>();

    public TitleManager(ElyonaRankPlugin plugin, TitleRepository repository,
                        TitleLoader loader, MimicMessenger mimic) {
        this.plugin = plugin;
        this.repository = repository;
        this.loader = loader;
        this.mimic = mimic;
    }

    /** ログイン時にアクティブ称号をロード */
    public CompletableFuture<Void> loadActiveTitle(UUID uuid) {
        return repository.getActiveTitle(uuid).thenAccept(titleId -> {
            if (titleId != null) {
                activeTitleCache.put(uuid, titleId);
            }
        });
    }

    /** ログアウト時にキャッシュを削除 */
    public void unloadActiveTitle(UUID uuid) {
        activeTitleCache.remove(uuid);
    }

    /**
     * 称号を付与する。
     * メインスレッドから呼び出す（イベント発火のため）。
     */
    public void grant(Player player, String titleId) {
        if (!loader.exists(titleId)) {
            plugin.getLogger().warning("存在しない称号ID: " + titleId);
            return;
        }
        repository.grantTitle(player.getUniqueId(), titleId).thenAccept(granted -> {
            if (!granted) return; // 既に所持
            Bukkit.getScheduler().runTask(plugin, () -> {
                ElyonaTitleGrantEvent event = new ElyonaTitleGrantEvent(player, titleId);
                Bukkit.getPluginManager().callEvent(event);

                TitleDefinition def = loader.getTitle(titleId);
                mimic.sendTo(player, "新しい称号「" + def.display() + "」を獲得しました！");
                plugin.getLogger().info("[Title] " + player.getName() + " に称号「" + titleId + "」を付与");
            });
        });
    }

    /** 称号を所持しているか非同期で確認 */
    public CompletableFuture<Boolean> has(Player player, String titleId) {
        return repository.hasTitle(player.getUniqueId(), titleId);
    }

    /** アクティブ称号を変更（LuckPermsのprefixも更新） */
    public void setActive(Player player, String titleId) {
        // 所持確認
        repository.hasTitle(player.getUniqueId(), titleId).thenAccept(owned -> {
            if (!owned) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        mimic.sendTo(player, "その称号を所持していません。"));
                return;
            }
            repository.setActiveTitle(player.getUniqueId(), titleId).thenRun(() -> {
                activeTitleCache.put(player.getUniqueId(), titleId);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    updateLuckPermsPrefix(player, loader.getTitle(titleId));
                    mimic.sendTo(player, "称号「" + loader.getTitle(titleId).display() + "」を装備しました。");
                });
            });
        });
    }

    /** アクティブ称号を外す */
    public void clearActive(Player player) {
        repository.clearActiveTitle(player.getUniqueId()).thenRun(() -> {
            activeTitleCache.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                updateLuckPermsPrefix(player, null);
                mimic.sendTo(player, "称号を外しました。");
            });
        });
    }

    /** アクティブ称号IDを取得（キャッシュから） */
    public String getActive(Player player) {
        return activeTitleCache.get(player.getUniqueId());
    }

    /** 所持称号リストを非同期取得 */
    public CompletableFuture<List<String>> getOwnedTitles(Player player) {
        return repository.getOwnedTitles(player.getUniqueId());
    }

    /** LuckPerms prefix を更新する */
    private void updateLuckPermsPrefix(Player player, TitleDefinition def) {
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user == null) return;

            // 既存のprefixをすべて削除
            user.data().clear(NodeType.PREFIX.predicate());

            if (def != null) {
                // 優先度100でprefixを設定
                user.data().add(PrefixNode.builder("[" + def.display() + "] ", 100).build());
            }

            // 非同期で保存
            lp.getUserManager().saveUser(user);
        } catch (Exception e) {
            plugin.getLogger().warning("LuckPerms prefix 更新に失敗: " + e.getMessage());
        }
    }
}
