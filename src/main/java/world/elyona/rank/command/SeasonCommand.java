package world.elyona.rank.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import world.elyona.core.mimic.MimicMessenger;
import world.elyona.rank.ElyonaRankPlugin;
import world.elyona.rank.rank.RankManager;
import world.elyona.rank.rank.SeasonManager;

import java.util.List;
import java.util.stream.Collectors;

public class SeasonCommand implements CommandExecutor, TabCompleter {

    private final ElyonaRankPlugin plugin;
    private final SeasonManager seasonManager;
    private final RankManager rankManager;
    private final MimicMessenger mimic;

    public SeasonCommand(ElyonaRankPlugin plugin, SeasonManager seasonManager,
                         RankManager rankManager, MimicMessenger mimic) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
        this.rankManager = rankManager;
        this.mimic = mimic;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("elyona.admin")) {
            if (sender instanceof Player p) mimic.sendTo(p, "権限がありません。");
            else sender.sendMessage("権限がありません。");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("使用方法: /season <start <name>|end>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (args.length < 2) { sender.sendMessage("使用方法: /season start <シーズン名>"); return true; }
                if (seasonManager.hasActiveSeason()) {
                    sender.sendMessage("既にアクティブなシーズンが存在します。先に /season end で終了してください。");
                    return true;
                }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                seasonManager.startSeason(name, () -> {
                    sender.sendMessage("新シーズン「" + name + "」を開始しました。");
                    mimic.broadcast("新シーズン「" + name + "」が開始されました。ランキングはリセットされています。");
                });
            }
            case "end" -> {
                if (!seasonManager.hasActiveSeason()) {
                    sender.sendMessage("アクティブなシーズンはありません。");
                    return true;
                }
                String seasonName = seasonManager.getActiveSeason().name();
                seasonManager.endSeason(() -> {
                    sender.sendMessage("シーズン「" + seasonName + "」を終了しました。全員のランクがBronzeにリセットされました。");
                    mimic.broadcast("シーズン「" + seasonName + "」が終了しました。全員のランクがBronzeにリセットされました。");
                });
            }
            default -> sender.sendMessage("使用方法: /season <start <name>|end>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("elyona.admin")) return List.of();
        if (args.length != 1) return List.of(); // 第2引数以降(シーズン名)は自由入力のため候補を出さない

        String prefix = args[0].toLowerCase();
        return List.of("start", "end").stream()
                .filter(c -> c.startsWith(prefix))
                .collect(Collectors.toList());
    }
}
