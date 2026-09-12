package world.elyona.rank.rank;

import world.elyona.core.rank.RankTier;

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
 * /rank コマンドで開く 3行チェストGUI（27スロット）
 *
 * スロット配置:
 *  [0-8]  上段: 装飾
 *  [9-17] 中段: 左=前ランク, 中央=現ランク, 右=次ランク
 *  [18-26] 下段: 進捗バー (9マス)
 */
public class RankGui implements InventoryHolder {

    private final Inventory inventory;

    public RankGui(Player player, RankTier current, long exp) {
        inventory = Bukkit.createInventory(this, 27,
                Component.text("ランク情報: " + player.getName()));

        buildGui(current, exp);
    }

    private void buildGui(RankTier current, long exp) {
        // 上段・中段の装飾
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // 中央(スロット13): 現ランクアイコン
        RankTier next = current.next();
        long nextReq = next != null ? next.requiredExp : current.requiredExp;
        long progress = next != null ? exp - current.requiredExp : exp - current.requiredExp;
        long needed = next != null ? nextReq - current.requiredExp : 0;

        List<Component> currentLore = new ArrayList<>();
        currentLore.add(Component.text("累計EXP: " + exp, NamedTextColor.GRAY));
        if (next != null) {
            currentLore.add(Component.text("次のランクまで: " + Math.max(0, nextReq - exp) + " EXP", NamedTextColor.YELLOW));
        } else {
            currentLore.add(Component.text("最高ランク達成！", NamedTextColor.GOLD));
        }
        inventory.setItem(13, makeItem(current.iconMaterial, Component.text(current.displayName), currentLore));

        // 前ランク(スロット10-12)
        RankTier prev = current.previous();
        if (prev != null) {
            List<Component> prevLore = List.of(
                    Component.text("必要EXP: " + prev.requiredExp, NamedTextColor.GRAY)
            );
            ItemStack prevItem = makeItem(prev.iconMaterial, Component.text(prev.displayName), prevLore);
            inventory.setItem(10, prevItem);
            inventory.setItem(11, prevItem.clone());
            inventory.setItem(12, prevItem.clone());
        }

        // 次ランク(スロット14-16)
        if (next != null) {
            List<Component> nextLore = List.of(
                    Component.text("必要EXP: " + next.requiredExp, NamedTextColor.GRAY),
                    Component.text("報酬: " + next.crReward + " Cr", NamedTextColor.GOLD)
            );
            ItemStack nextItem = makeItem(next.iconMaterial, Component.text(next.displayName), nextLore);
            inventory.setItem(14, nextItem);
            inventory.setItem(15, nextItem.clone());
            inventory.setItem(16, nextItem.clone());
        }

        // 進捗バー(スロット18-26)
        int filled = (next == null || needed <= 0) ? 9 : (int) Math.min(9, progress * 9 / needed);
        for (int i = 0; i < 9; i++) {
            Material mat = i < filled ? current.iconMaterial : Material.GRAY_STAINED_GLASS_PANE;
            inventory.setItem(18 + i, makeItem(mat, Component.text(
                    next != null ? (i < filled ? "■" : "□") : "■ MAX", NamedTextColor.WHITE), null));
        }
    }

    private ItemStack makeItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        if (lore != null) meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
