package world.elyona.rank.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import world.elyona.rank.rank.RankGui;
import world.elyona.rank.rank.RankManager;
import world.elyona.core.rank.RankTier;
import world.elyona.rank.rank.SeasonManager;

import java.util.List;
import java.util.stream.Collectors;

public class RankCommand implements CommandExecutor, TabCompleter {

    private final RankManager rankManager;
    private final SeasonManager seasonManager;

    public RankCommand(RankManager rankManager, SeasonManager seasonManager) {
        this.rankManager = rankManager;
        this.seasonManager = seasonManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用できます。");
            return true;
        }

        if (!seasonManager.hasActiveSeason()) {
            player.sendMessage("現在アクティブなシーズンはありません。");
            return true;
        }

        if (args.length == 0) {
            // 自分のランクをGUIで表示
            RankTier tier = rankManager.getRank(player);
            long exp = rankManager.getExp(player);
            new RankGui(player, tier, exp).open(player);
        } else {
            // 他プレイヤーのランクを確認
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                player.sendMessage("オンラインプレイヤーが見つかりません: " + args[0]);
                return true;
            }
            RankTier tier = rankManager.getRank(target);
            long exp = rankManager.getExp(target);
            player.sendMessage(target.getName() + " のランク: " + tier.displayName + " (EXP: " + exp + ")");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
    }
}
