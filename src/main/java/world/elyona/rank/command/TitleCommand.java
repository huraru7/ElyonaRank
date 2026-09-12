package world.elyona.rank.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import world.elyona.core.mimic.MimicMessenger;
import world.elyona.rank.ElyonaRankPlugin;
import world.elyona.rank.title.TitleGui;
import world.elyona.rank.title.TitleManager;

import java.util.List;
import java.util.stream.Collectors;

public class TitleCommand implements CommandExecutor, TabCompleter {

    private final TitleManager titleManager;
    private final MimicMessenger mimic;

    public TitleCommand(TitleManager titleManager, MimicMessenger mimic) {
        this.titleManager = titleManager;
        this.mimic = mimic;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用できます。");
            return true;
        }

        if (args.length == 0) {
            // GUI表示
            openTitleGui(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (args.length < 2) { mimic.sendTo(player, "使用方法: /title set <称号ID>"); return true; }
                titleManager.setActive(player, args[1]);
            }
            case "clear" -> titleManager.clearActive(player);
            case "grant" -> {
                if (!player.hasPermission("elyona.admin")) {
                    mimic.sendTo(player, "権限がありません。");
                    return true;
                }
                if (args.length < 3) { mimic.sendTo(player, "使用方法: /title grant <player> <称号ID>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { mimic.sendTo(player, "プレイヤーが見つかりません: " + args[1]); return true; }
                titleManager.grant(target, args[2]);
                mimic.sendTo(player, target.getName() + " に称号「" + args[2] + "」を付与しました。");
            }
            default -> mimic.sendTo(player, "使用方法: /title [set <id>|clear|grant <player> <id>]");
        }
        return true;
    }

    private void openTitleGui(Player player) {
        titleManager.getOwnedTitles(player).thenAccept(ownedIds -> {
            String activeId = titleManager.getActive(player);
            Bukkit.getScheduler().runTask(
                    world.elyona.rank.ElyonaRankPlugin.getInstance(),
                    () -> new TitleGui(player, ownedIds, world.elyona.rank.ElyonaRankPlugin.getInstance().getTitleLoader(), activeId).open()
            );
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> subs = sender.hasPermission("elyona.admin")
                    ? List.of("set", "clear", "grant")
                    : List.of("set", "clear");
            return subs.stream().filter(c -> c.startsWith(prefix)).collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            String prefix = args[1].toLowerCase();
            return ElyonaRankPlugin.getInstance().getTitleLoader().getAllTitles().keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }

        if (args[0].equalsIgnoreCase("grant") && sender.hasPermission("elyona.admin")) {
            if (args.length == 2) {
                String prefix = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
            }
            if (args.length == 3) {
                String prefix = args[2].toLowerCase();
                return ElyonaRankPlugin.getInstance().getTitleLoader().getAllTitles().keySet().stream()
                        .filter(id -> id.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
            }
        }

        return List.of();
    }
}
