package world.elyona.rank.title;

import world.elyona.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * elyona_titles / elyona_active_title テーブルの所有者。
 * uuid列はElyonaCoreのelyona_playersを参照するFOREIGN KEYを持つ。
 */
public class TitleRepository {

    private final DatabaseManager db;

    public TitleRepository(DatabaseManager db) {
        this.db = db;
    }

    /** elyona_titles / elyona_active_title テーブルを作成する（onEnable時に同期実行） */
    public void initialize() {
        String[] sqls = {
                "CREATE TABLE IF NOT EXISTS elyona_titles (" +
                        "uuid VARCHAR(36) NOT NULL," +
                        "title_id VARCHAR(64) NOT NULL," +
                        "obtained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "PRIMARY KEY (uuid, title_id)," +
                        "FOREIGN KEY (uuid) REFERENCES elyona_players(uuid)" +
                        ")",
                "CREATE TABLE IF NOT EXISTS elyona_active_title (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "title_id VARCHAR(64)," +
                        "FOREIGN KEY (uuid) REFERENCES elyona_players(uuid)" +
                        ")"
        };

        try (Connection conn = db.getConnection()) {
            for (String sql : sqls) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("elyona_titles/elyona_active_title テーブルの作成に失敗しました", e);
        }
    }

    /** プレイヤーが所持している称号IDのリストを取得 */
    public CompletableFuture<List<String>> getOwnedTitles(UUID uuid) {
        return db.queryAsync(
                "SELECT title_id FROM elyona_titles WHERE uuid = ? ORDER BY obtained_at",
                ps -> ps.setString(1, uuid.toString()),
                rs -> {
                    List<String> list = new ArrayList<>();
                    while (rs.next()) list.add(rs.getString("title_id"));
                    return list;
                }
        );
    }

    /** 称号を付与（既に所持している場合はスキップ） */
    public CompletableFuture<Boolean> grantTitle(UUID uuid, String titleId) {
        // まず重複確認
        return db.queryAsync(
                "SELECT COUNT(*) FROM elyona_titles WHERE uuid = ? AND title_id = ?",
                ps -> {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, titleId);
                },
                rs -> rs.next() && rs.getInt(1) > 0
        ).thenCompose(alreadyOwned -> {
            if (alreadyOwned) return CompletableFuture.completedFuture(false);
            return db.executeAsync(
                    "INSERT INTO elyona_titles (uuid, title_id) VALUES (?, ?)",
                    ps -> {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, titleId);
                    }
            ).thenApply(v -> true);
        });
    }

    /** 指定称号を所持しているか確認 */
    public CompletableFuture<Boolean> hasTitle(UUID uuid, String titleId) {
        return db.queryAsync(
                "SELECT COUNT(*) FROM elyona_titles WHERE uuid = ? AND title_id = ?",
                ps -> {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, titleId);
                },
                rs -> rs.next() && rs.getInt(1) > 0
        );
    }

    /** アクティブ称号を取得 */
    public CompletableFuture<String> getActiveTitle(UUID uuid) {
        return db.queryAsync(
                "SELECT title_id FROM elyona_active_title WHERE uuid = ?",
                ps -> ps.setString(1, uuid.toString()),
                rs -> rs.next() ? rs.getString("title_id") : null
        );
    }

    /** アクティブ称号を設定（UPSERT） */
    public CompletableFuture<Void> setActiveTitle(UUID uuid, String titleId) {
        String sql = db.isMySql()
                ? "INSERT INTO elyona_active_title (uuid, title_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE title_id=?"
                : "INSERT INTO elyona_active_title (uuid, title_id) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET title_id=excluded.title_id";
        return db.executeAsync(sql, ps -> {
            ps.setString(1, uuid.toString());
            ps.setString(2, titleId);
            if (db.isMySql()) ps.setString(3, titleId);
        });
    }

    /** アクティブ称号を削除 */
    public CompletableFuture<Void> clearActiveTitle(UUID uuid) {
        return db.executeAsync(
                "DELETE FROM elyona_active_title WHERE uuid = ?",
                ps -> ps.setString(1, uuid.toString())
        );
    }
}
