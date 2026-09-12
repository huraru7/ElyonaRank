package world.elyona.rank.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import world.elyona.rank.rank.RankGui;
import world.elyona.rank.title.TitleGui;
import world.elyona.rank.title.TitleManager;

public class GuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        var holder = event.getInventory().getHolder();

        if (holder instanceof RankGui) {
            // ランクGUIはクリックをすべてキャンセル
            event.setCancelled(true);

        } else if (holder instanceof TitleGui titleGui) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;

            String titleId = titleGui.getTitleIdAt(slot);
            if (titleId == null) return;

            TitleManager titleManager = world.elyona.rank.ElyonaRankPlugin.getInstance().getTitleManager();
            String currentActive = titleManager.getActive(player);

            if (titleId.equals(currentActive)) {
                // 既に装備中ならば外す
                titleManager.clearActive(player);
            } else {
                titleManager.setActive(player, titleId);
            }
            player.closeInventory();
        }
    }
}
