package world.elyona.rank.title;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * /title コマンドで開く称号選択GUI。
 * 所持称号数に応じて行数を動的に決定（最大54スロット）。
 */
public class TitleGui implements InventoryHolder {

    private final Inventory inventory;
    private final Player player;
    private final List<String> ownedTitleIds;
    private final TitleLoader loader;
    private final String activeTitleId;

    public TitleGui(Player player, List<String> ownedTitleIds, TitleLoader loader, String activeTitleId) {
        this.player = player;
        this.ownedTitleIds = ownedTitleIds;
        this.loader = loader;
        this.activeTitleId = activeTitleId;

        int size = ownedTitleIds.isEmpty() ? 9 : (int) (Math.ceil(ownedTitleIds.size() / 9.0) * 9);
        size = Math.min(54, Math.max(9, size));

        inventory = Bukkit.createInventory(this, size, Component.text("称号一覧"));
        buildGui();
    }

    private void buildGui() {
        for (int i = 0; i < ownedTitleIds.size() && i < inventory.getSize(); i++) {
            String titleId = ownedTitleIds.get(i);
            TitleDefinition def = loader.getTitle(titleId);
            if (def == null) continue;

            boolean isActive = titleId.equals(activeTitleId);
            Material mat = isActive ? Material.NETHER_STAR : Material.PAPER;

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(def.description(), NamedTextColor.GRAY));
            if (isActive) {
                lore.add(Component.text("▶ 装備中", NamedTextColor.YELLOW));
            } else {
                lore.add(Component.text("クリックで装備", NamedTextColor.GREEN));
            }
            lore.add(Component.text("ID: " + titleId, NamedTextColor.DARK_GRAY));

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("[" + def.display() + "]",
                    isActive ? NamedTextColor.GOLD : NamedTextColor.WHITE));
            meta.lore(lore);
            item.setItemMeta(meta);

            inventory.setItem(i, item);
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    /** スロットからtitleIdを取得（クリック時に使用） */
    public String getTitleIdAt(int slot) {
        if (slot < 0 || slot >= ownedTitleIds.size()) return null;
        return ownedTitleIds.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
